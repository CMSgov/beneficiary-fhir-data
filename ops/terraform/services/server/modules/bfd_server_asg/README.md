## Autoscaling group

What's included:

- Autoscaling group
- Launch configuration
- Autoscaling policies (based on CPU usage)
- Cloudwatch alarms (to inform autoscaling policies)
- Security group with ingress rules to allow CI access to running EC2 instances
- Associated load balancer

Example usage:

```
module "asg" {
  source = "../modules/resources/asg"

  env_config    = {env = "test", tags={}, vpc_id =  }
  role          = "api"
  layer         = "app"

  launch_config = {
    key_name      = "instance-key-pair-name"
    ami_id        = "ami-aabbccx"
    instance_type = "m3.medium"
  }

  lb_config     = {
    name        = "elb-name-goes-here"
    port        = 7443         
  }

  asg_config    = {
    min         = 1
    max         = 2
    desired     = 1
  }

  mgmt_config   = {
    ci_cidrs    = ["1.2.3.4/32", "5.6.7.8/32"]
    tool_sg     = "sg-aabbccdd"
    vpn_sg      = "sb-eeffgghh"
  }
}
```
