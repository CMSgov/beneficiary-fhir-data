# Blue Button Data Pipeline Design Decisions

This document details the reasoning behind some of the design decisions that have been made in the development of this system.

## Configuration and Secrets Management

There are several common options for passing in configuration values to an application:

1. Required command line arguments, e.g `myapp --db-server-url="jdbc:foo:bar"`
2. Java system properties, e.g. `java -jar myapp.jar -Ddb.server.url="jdbc:foo:bar"`
3. Environment variables, e.g. `DB_SERVER_URL="jdbc:foo:bar" myapp`
4. A network configuration service such as [ZooKeeper](https://zookeeper.apache.org/), e.g. `myapp --config=1.2.3.4:5678`.
    * The address of such a service should almost always itself be configurable at runtime. Options 1 through 3 here can be used to specify that address.
5. A configuration file, e.g. `myapp --config=/etc/myapp/foo.conf`
    * As in the example above, the location of an application's configuration file should almost always itself be configurable at runtime. Options 1 through 3 here can be used to specify that path.

There does not appear to a solid industry consensus stating that one of these approaches is always the correct one. Probably the most authoritative source on the subject, [The Twelve-Factor App](http://12factor.net), calls for Option #3 (environment variables) to be adopted universally in [The Twelve-Factor App: Config](http://12factor.net/config). It's a reasonable position. However, there are plenty of equally persuasive arguments against this approach. The following Hacker News discussion is an excellent discussion around the problem in general, with a particular focus on security: [Hacker News: Environment Variables Considered Harmful for Your Secrets](https://news.ycombinator.com/item?id=8826024). Overall, there's lots of debate around this issue and not enough consensus to declare a clear "best choice."

A brief sidebar on the relative security of these approaches: Most discussions of this issue that focus on security fail to include any real [threat modeling](https://alexgaynor.net/2016/jul/29/intro-to-threat-modeling/). There are two main classes of vectors here: accidental leaking, and targeted compromise by an active attacker. All of the options except for #4 (network config service) are subject to accidental leaking of one form or another, such as in error reporting, logging, not-properly-secured backups, etc. With active attackers, most of the options here are equally vulnerable. If an attacker can inject code into the application or gain shell access as the application user, they can gain access to the processes' memory, arguments, environment, configuration files, and network interface. The only exception to this line of reasoning is that option #1 (command line arguments) is vulnerable to shell access from *any* user on the system (as command line arguments  for all processes are visible to all users). Overall, security isn't a real factor in this decision, aside from eliminating option #1 for secrets.

As with most things, this decision likely comes down to "what makes the most sense for our application and organization?" The simplest approach of the five would be #3 (environment variables), which are reasonably secured and widely supported. The most user friendly approach is likely #1 (command line arguments), which are easily discoverable if a `--help` option is supported. The most flexible approaches are #4 (network config service) and #5 (configuration files), which easily allow for hierarchical, repeating, and structured configuration options.

Some additional considerations:

* Most of these options can be combined in various ways.
    * For example: a configuration file with property expressions (e.g. `<someSetting>${someEnvVariable}</someSetting>`) that are resolved at runtime.
* Any application that will be deployed as a WAR has more slightly different options.
    * Command line arguments are unavailable.
    * The canonical approach with WARs is to use the servlet context's `<init-params/>`.
        * This is usually a **terrible** option, as those values end up being baked into the WAR; they're not easily configurable at runtime.
* With the recent proliferation of microservices and auto-scaling deployments, network configuration services are becoming more popular.
    * The main benefit here seems to be the centralization provided by such services. This absolutely comes with an increased cost for complexity, though.
* Java applications cache the complete environment variables' state (`System.getenv()`) at startup.
    * This can present a problem in tests, where config values often need to be modified dynamically. This can be managed by ensuring configuration management code using environment variables also "falls back" to another source that *is* modifiable at runtime, such as option #2 (Java system properties). 

Overall, the simplest approach, option #3 (environment variables) is the correct choice for the Blue Button applications. If our applications had more complex configuration needs, such as requiring highly structured configuration data, the correct choice would be option #5 (configuration files). We'll keep an eye out for use-cases that would be particularly well-met by option #4 (network configuration services), but at this time, no such use cases are envisioned. As is typical, the simplest approach is the correct one here.

Of course, there are some caveats to this approach, based on the above discussion:

* For test purposes, some applications may need to allow Java system properties to override environment variable values. This allows configuration settings to be easily passed in during tests.
* Care must be taken to ensure that any environment variable that includes a password, API key, etc. include the word "secret" in its name. The application must then filter any environment variables dumps in error reports, diagnostic pages, etc. to mask such variables' values, to prevent accidental disclosure.
* Security practices must account for the fact that any attacker that can run commands or code on the system as the application user will be able to gain access to that application's secrets.
