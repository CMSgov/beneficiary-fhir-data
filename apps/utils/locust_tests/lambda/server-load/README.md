```mermaid
sequenceDiagram
    autonumber
    actor u as User
    participant csh as controller.sh
    Note over csh: controller.sh runs main<br>locust process
    participant cpy as controller.py
    participant n as node.py &lambda;
    participant sqs as SQS
    participant sns as SNS
    participant asg as ASG
    u -->> csh: Starts controller.sh
    activate csh
    u -->> cpy: Starts controller.py
    activate cpy
    loop Until autoscaling notification received via SNS subscription<br>or maximum number of nodes reached
        par
            cpy -->> n: Invoke worker node &lambda;
            cpy -->> sqs: Check for scaling event
        and
            n --> asg: Run load tests against ASG via port 443
            n --> csh: Report load test statistics via port 5557
            n -->> sqs: Check for scaling event
        end
    end
    alt When scaling event occurs
        asg -->> sns: Scaling event notification
        sns -->> sqs: Forward scaling event notification
        par
            cpy -->> cpy: Stop spawning nodes
            n -->> n: Stop running locust worker
        end
        Note over csh: Locust worker death causes<br>termination of controller
    end
    deactivate csh
    deactivate cpy
