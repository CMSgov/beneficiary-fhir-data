resource "aws_security_group" "alb_sg" {
  name        = "alb-sg"
  vpc_id      = data.aws_vpc.this.id
  description = "Allow HTTP and HTTPS traffic to ALB"

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_lb" "static_lb" {
  name               = "static-lb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = data.aws_subnets.env_subnets.ids
}

resource "aws_lb_target_group" "static_tg" {
  depends_on = [aws_vpc_endpoint.s3]

  name        = "static-tg"
  port        = 443
  protocol    = "HTTPS"
  vpc_id      = data.aws_vpc.this.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 5
    unhealthy_threshold = 3
    matcher             = "200,307,405"
  }
}

data "aws_network_interface" "vpce_eni" {
  count = length(tolist(data.aws_subnets.env_subnets.ids))
  # network_interface_ids is a set by default, and sets have no index in Terraform, so we need to
  # convert it to a list. Then, to make it consistent between runs, we sort it
  id = sort(tolist(aws_vpc_endpoint.s3.network_interface_ids))[count.index]
}

resource "aws_lb_target_group_attachment" "static_tg_attachment" {
  depends_on = [aws_vpc_endpoint.s3]

  for_each = {
    for k, v in data.aws_network_interface.vpce_eni :
    v.id => v
  }
  target_group_arn = aws_lb_target_group.static_tg.arn
  target_id        = each.value.private_ip
  port             = 443

  #   count            = length([aws_vpc_endpoint.s3.network_interface_ids])
  #   target_group_arn = aws_lb_target_group.static_tg.arn
  #   target_id        = element([aws_vpc_endpoint.s3.network_interface_ids], count.index)
  #   port             = 443
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.static_lb.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = data.aws_acm_certificate.env_issued.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.static_tg.arn
  }
}