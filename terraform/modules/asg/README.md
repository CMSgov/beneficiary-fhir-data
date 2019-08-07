## Autoscaling group

What's included:

- Autoscaling group
- Launch configuration
- Autoscaling policies (based on CPU usage)
- Cloudwatch alarms (to inform autoscaling policies)
- Security group with ingress rules to allow CI access to running EC2 instances

Example usage:

```
module "asg" {
  source = "../modules/asg"

  app           = "bfd"
  stack         = "dev"
  env           = "DEV"
  vpc_id        = "vpc-aabbccx"
  key_name      = "instance-key-pair-name"
  ami_id        = "ami-aabbccx"
  instance_type = "m3.medium"
  elb_name      = "elb-name-goes-here"
  asg_min       = 1
  asg_max       = 2
  asg_desired   = 1
  azs           = ["us-east-1a", "us-east-1b"]
  ci_cidrs      = ["1.2.3.4/32", "5.6.7.8/32"]
  app_sg_id     = "sg-aabbccdd"
  vpn_sg_id     = "sb-eeffgghh"
}
```
