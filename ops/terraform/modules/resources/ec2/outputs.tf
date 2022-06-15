output "ec2_id" {
  value       = "${element(aws_instance.main.*.id, 0)}"
  description = "The Instance ID of the BFD Pipeline EC2 Instance"
}
