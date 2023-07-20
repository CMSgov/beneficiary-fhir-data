# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0013-bfd-server-startup-check`
* Start Date: 2022-04-24
* RFC PR: [beneficiary-fhir-data/rfcs#0013](https://github.com/CMSgov/beneficiary-fhir-data/pull/1074)
* JIRA Ticket(s):
    * [BFD-1663](https://jira.cms.gov/browse/BFD-1663)

This RFC proposes a minor change to existing bfd-server on-host health checking to satisfy the requirements of a reliable startup check.

## Status
[Status]: #status

* Status: Implemented
* Implementation JIRA Ticket(s):
    * [BFD-1685](https://jira.cms.gov/browse/BFD-1685)

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Status](#status)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
    * [Proposed Solution: Detailed Design](#proposed-solution-detailed-design)
    * [Proposed Solution: Unresolved Questions](#proposed-solution-unresolved-questions)
    * [Proposed Solution: Drawbacks](#proposed-solution-drawbacks)
    * [Proposed Solution: Notable Alternatives](#proposed-solution-notable-alternatives)
* [Prior Art](#prior-art)
* [Future Possibilities](#future-possibilities)
* [Addendums](#addendums)

## Motivation
[Motivation]: #motivation

### Issue

AWS Classic Load Balancers have limited health check functionality leading to scenarios where traffic can be routed to unhealthy (unsuccessfully started) hosts. In the best case, bfd-server deployments may exhibit brief periods of downtime. In the worst case, the bfd-server may oscillate between healthy and unhealthy application states, resulting in preventable deployment failures.

### Discussion

#### Supporting Infrastructure

The Beneficiary FHIR Data Server (_bfd-server_) is hosted in the Amazon Web Services (_AWS_) public cloud across a collection of Elastic Compute Cloud (_EC2_) instances that comprise an Auto Scaling Group (_ASG_). The ASG is responsible for automatically and horizontally scaling bfd-server **out** to meet performance goals as well as scaling these instances back **in** to meet budgetary requirements. In addition to scaling, the ASG uses a combination of [status checks provided by EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/monitoring-system-instance-status-check.html#types-of-instance-status-checks) and health checks reported by the load balancer (discussed in the following paragraph) to automatically replace unhealthy instances.

To avoid [saturating](https://sre.google/sre-book/monitoring-distributed-systems/#saturation) any single instance in the ASG with excessive traffic, requests intended for the bfd-server are balanced by Elastic Load Balancing (_ELB_) through a Classic Load Balancer (_CLB_). As mentioned above, the status of the CLB health check is used in part by the ASG to determine whether a host needs to be replaced or not. Additionally, the CLB uses the status of these health checks to determine which hosts should or should not receive traffic. As of this RFC, the health check is effectively a TCP ping against each bfd-server instance port, 7443. The health check designates hosts as healthy when the TCP ping is successful. Because the bfd-server application starts listening on port 7443 almost immediately after starting (but importantly, before reaching a fully started state), the CLB health check can designate non-started hosts as healthy. This is especially common during instance replacement as part of automated deployments. When unprepared hosts receive bfd-server traffic, the requests result in an error.

#### Host-Based Health Checks

In addition to the health checks external to the bfd-server application instance, the instances themselves have an internal health check mechanism running inside the service startup script, encoded in the `stop_service_if_failing` function. Today, this performs local, authenticated queries against a set of pre-defined API endpoints to determine whether the application is behaving normally. If any errors are detected, it works as a _kill switch_ and stops the service.

#### Health Check Review

So far in this discussion, the term _health check_ has been used to generally describe methods for determining the operating state of an application instance. However, this term is somewhat under specified. There are a variety of health checks that assist in achieving reliability targets. To be a little more precise, it might be helpful to adopt the health check (or _probe_) concepts from the kubernetes ecosystem, despite the obvious difference in hosting strategies. Kubernetes supports probes for _liveness_, _readiness_, and _startup_ where they each answer different questions about a service's health throughout the application instance's life cycle.

_Liveness_ can be applicable throughout the lifetime of the application instance and answers the question: "is the application alive or dead?" In kubernetes, this relates to whether or not a container should be restarted. _Liveness probes_ are configured at the operator's discretion and support a variety of probing methods including [command execution](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-a-liveness-command), [HTTP requests](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-a-liveness-http-request), [TCP requests](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-a-tcp-liveness-probe), and [gRPC requests](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-a-grpc-liveness-probe). **As this relates to the bfd-server, both the EC2 status checks and the CLB TCP health check help answer the liveness question for the ASG: should a given instance be replaced?**

_Readiness_ is also applicable throughout the lifetime of the application instance, but it answers a slightly different question: "is the application ready to receive requests?" In kubernetes, the status of this check may be transitory (e.g. during startup, spikes in saturation), where operators neither want to restart/replace the running instance, nor should traffic be routed to a non-ready instance. [A readiness probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-readiness-probes) can also be helpful if _liveness_ and _readiness_ criteria are different, and there is significant difference (_significance_ varies, but generally greater than the latency service level objective) between the first successful liveness probe and first successful readiness probe. **Because the intent of the bfd-server's `stop_service_if_failing` is to prevent client traffic from reaching a faulty instance, it was tempting to compare this to a readiness probe. However, it only functions at the startup; naturally, it's more directly comparable to a startup probe. To be clear, there is no analog to the readiness probe in the existing infrastructure, noted as an area for [future possibilities below](#future-possibilities).**

_Startup_ is most relevant to the early stages of an application instance's lifetime. Startup answers the question: "has the application started?" [Startup probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/#define-startup-probes) continues to ask this until they succeed (or reach a failure threshold). Once successful, _liveness_ and _readiness_ probes take over. The status of this check is similar to a grace period (`initialDelaySeconds` settings in kubernetes for the relevant probes) and they can be helpful when an application is slow to start. It's especially helpful when there is any variability in startup times. Specifically in kubernetes, startup probes interact with liveness and readiness probes to prevent them from prematurely restarting or recreating application instances before they have fully started. **Again, the `stop_service_if_failing` function runs at startup and can intuitively be compared to a startup probe. Additionally, startup probes are somewhat similar to grace periods, so it's tempting to directly compare them with the ASG's `health check grace period`. However, startup probes have additional application state awareness to prevent other probes from functioning until the application has started.**

Again, the BFD systems are not hosted with kubernetes, and outside of kubernetes the comparisons are challenging to make. Still, the concepts help direct the conversation. The table below summarizes the comparison of kubernetes-style probes to the existing bfd-server health checking strategy.

| bfd-server Component  | Type     | Implementation                     | State    |
|-----------------------|----------|------------------------------------|----------|
| EC2 Instance          | startup  | `stop_service_if_failing` function | partial  |
| Classic Load Balancer | liveness | TCP Ping on Instance port 7443     | complete |
| EC2 Status Checks     | liveness | AWS Default                        | complete |
| Auto Scaling Group    | startup  | 430s health check grace period     | partial  |

## Proposed Solution
[Proposed Solution]: #proposed-solution

The bfd-server needs a health check that goes beyond mere _liveness_. It needs to reliably identify each application instance as fully started only after the instance is fully provisioned and prepared to process requests. In short, bfd-server needs a real startup check. The existing `stop_service_if_failing` function already meets many of the requirements for such a check. It just needs the ability to signal the instance's startup state to the CLB. This _signaling_ can be achieved through the introduction of a single, temporary firewall rule that initially disallows external traffic to the instance's service port, 7443, and subsequently removes the rule once all of the existing health criteria have been satisfied.

#### Visualize the Proposal

##### Existing Health Checks and Startup
Below is a _simplified_ depiction of the existing health checks in the bfd-server. The instance is regarded as healthy as soon as port 7443 is available, but that can be **before** the service has fully started.

![2022-04 BFD Server Health Checks](./resources/0013-2022-04-bfd-server-health-checks.svg)

##### Proposed Health Checks and Readiness

Below depicts the proposed health checks in the bfd-server. The instance is continues to be regarded as healthy as soon as port 7443 is available. This solution leverages that simplicity and delays the availability of 7443 to _external_ traffic until the _internal_ startup health check succeeds. In other words, traffic is only permitted when the internal health check is satisfied.

![Proposed BFD Server Health Checks](./resources/0013-proposed-bfd-server-health-checks.svg)

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Extending the `stop_service_if_failing` function to fully satisfy the requirements of a startup check requires the following:
1. Augment the bfd-server service account with sudoers access, restricted to `/sbin/iptables` via ansible definition
2. Adjust the templated `bfd-server.sh.j2` file with the following changes:
    - At the beginning of the `stop_service_if_failing` function, disallow external traffic on port 7443:
    
        ```bash
        sudo iptables -A INPUT -p tcp -i eth0 --dport 7443 -j REJECT
        ```

    - Where the script currently produces the log message "Server started properly", allow external traffic on port 7443:

        ```bash
        sudo iptables -D INPUT -p tcp -i eth0 --dport 7443 -j REJECT
        ```
### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

* [x] Arguments for and against certain solutions hinge in part on the notion that lazily loaded application data sources could have a negative impact on the performance. However, the evidence is unclear. Do the initial requests executed in the on-host health check ward off a cold-start or similar?
  The arguments against alternative solutions may be a little weaker upon further investigation. The _institutional memory_ surrounding the original implementation of the `stop_service_if_failing` suggests that it was not necessarily intended to avoid cold-start issues. If it does avoid these issues, it might be considered a happy accident.
* [x] What is the recommended firewall on amazon Linux 2 in 2022? Is `iptables` still auspicious?
  `iptables` continues to be an obvious, acceptable solution.
  - `ufw` can be used as a frontend for utilities like `iptables`
  - `ufw` **is** available via epel which in turn is available via `amazon-linux-extras`
  - `epel` does not appear to be enabled in the CMS base images by default
  - `iptables` appears to be installed in base CMS images
  - `iptables` is also part of `@amzn2-core` and easily added if missing

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

N/A.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

#### Health Check Alternatives in Elastic Load Balancing

If it were only an issue of a lossy health check with what amounts to a TCP ping, AWS CLBs do support HTTP and HTTPS checks. While it might seem like a viable alternative, there are clear reasons to avoid this strategy.

**Ultimately, such a solution will rely on the existing `stop_service_if_failing` function or something like it.**
Because the bfd-server authentication strategy involves mutual TLS (_mTLS_) and the CLB health check mechanism cannot be supplied with a certificate for authentication, bfd-server would need to support endpoints that respond with an HTTP status code of 200 **without** authentication. This alone would require a non-trivial amount of work, but even if the bfd-server could respond this way, such endpoints would still rely on the functionality of  `stop_service_if_failing` or similar as the definitive startup source. This is because the checks involved are masking and/or compensating for some complexity in bfd-server. `stop_service_if_failing`, through the set of endpoints that it evaluates, eagerly loads otherwise lazily loaded application data sources. Without these first synthetic requests, the first _real_ client requests may exhibit unacceptable performance. Additionally, these requests exercise the hot code paths involving the database which provides confidence that the first _real_ client traffic will succeed in fetching the requested database resources.

Much of the same applies to the _seemingly_ obvious alternative: the Network Load Balancer (_NLB_). While NLBs could satisfy the load balancing requirements, including pass through for mTLS, the additional sophistication within Target Group health checking still falls short of providing holistic startup checks for this system, ultimately relying on host-based, scripted health checks. The Application Load Balancer on the other hand, would not only be similarly limited, but additionally unable to satisfy the mTLS requirements.

#### More Complex Solutions

##### Reverse Proxying with Nginx or Otherwise

Dedicated reverse proxies like [HAProxy](https://www.haproxy.org/) or those implemented in popular web servers like [Nginx](https://nginx.org) are easy to configure and offer solutions to a number of problems including SSL/TLS offloading and termination; selective routing or advanced load balancing; and rate limiting to name a few. Health checks are first-class members in Nginx and could easily augment the existing CLB strategy. Unfortunately, many of the problems that something like Nginx might address already have successful solutions in place, and much of the additional functionality that a tool like Nginx could provide would go unused. In short, it's overkill when compared to the proposed solution, which satisfies the requirements and introduces little in the way of maintenance overhead. Like the other alternatives mentioned above, introducing Nginx or similar would only be partial solution, still relying on some other mechanism for identifying a successful startup.

##### Split Control Plane

Unlike other, similarly configured systems, bfd-server utilizes a largely [immutable infrastructure](https://www.digitalocean.com/community/tutorials/what-is-immutable-infrastructure) pattern with little in the way of ongoing updates, automated or otherwise, post-deployment. When developers or operators wish to make changes to the system, the _happy path_ involves rebuilding the system with the desired changes via deployment, rather than _mutating_ the existing one. However, if a _control plane_ should exist here, it might only consist of the automated Jenkins deployment pipeline, the application of terraform-defined infrastructure-as-code within it, and the suite of AWS API endpoints the pipeline uses. With this, the EC2 instances are created and automatically registered with the CLB. This is easy to maintain and simple to reason about. However, it's possible to forego the automatic EC2 instance registration with the CLB via the ASG and instead leave the registration up to scripted workflows on the instances themselves. In this way, the instances could register with the appropriate services only after they have more fully started, which satisfies the spirit of the proposed startup check. While this strategy would _work_, it's hard to imagine that increasing the individual EC2 instances privileges to directly interact with the hosting infrastructure is wise. What's more, splitting the roles and responsibilities could make the system more difficult to fully understand.

## Prior Art
[Prior Art]: #prior-art

N/A

## Future Possibilities
[Future Possibilities]: #future-possibilities

### Deployments

Improvements to the existing on-host health check to make it a more specific startup check and will inspire confidence in the existing deployment process. What's more, a reliable startup check is a prerequisite to the development of alternative deployment strategies, such as [blue-green deployments](https://en.wikipedia.org/wiki/Blue-green_deployment) and [feature toggle deployments](https://en.wikipedia.org/wiki/Feature_toggle). The adoption of these or other deployment strategies may dramatically decrease deployment risk and toil while further increasing deployment confidence, reliability, and frequency.

### Readiness Checking

What's missing from the proposal is the introduction of reliable readiness checks. This will require further consideration moving forward to ensure that the traffic is handled most effectively and that instances aren't suffering from periods of saturation as a result of mismanaged load balancing.

### DevSecOps

This proposal is _very_ simplistic and the initial feedback to this proposal included concern over the targeted privilege escalation for `iptables`, which could be avoided by re-imagining aspects of the startup script as cooperative systemd units, discretely responsible for health checking and port management. There could be other benefits from such a solution, however, they may be considered out of scope for this specific proposal.

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)
