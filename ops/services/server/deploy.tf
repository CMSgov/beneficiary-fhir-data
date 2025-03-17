resource "aws_codedeploy_app" "server" {
  compute_platform = "ECS"
  name             = local.service
}

resource "aws_codedeploy_deployment_group" "server" {
  depends_on = [aws_iam_role_policy_attachment.codedeploy]

  app_name               = aws_codedeploy_app.server.name
  deployment_group_name  = "${local.name_prefix}-dg"
  deployment_config_name = "CodeDeployDefault.ECSAllAtOnce"
  service_role_arn       = aws_iam_role.codedeploy.arn

  blue_green_deployment_config {
    deployment_ready_option {
      action_on_timeout = "CONTINUE_DEPLOYMENT"
    }

    terminate_blue_instances_on_deployment_success {
      action                           = "TERMINATE"
      termination_wait_time_in_minutes = 0
    }
  }

  ecs_service {
    cluster_name = data.aws_ecs_cluster.main.cluster_name
    service_name = aws_ecs_service.server.name
  }

  deployment_style {
    deployment_option = "WITH_TRAFFIC_CONTROL"
    deployment_type   = "BLUE_GREEN"
  }

  auto_rollback_configuration {
    enabled = true
    events  = ["DEPLOYMENT_FAILURE"]
  }

  load_balancer_info {
    target_group_pair_info {
      prod_traffic_route {
        listener_arns = [aws_lb_listener.this[local.blue_state].arn]
      }

      test_traffic_route {
        listener_arns = [aws_lb_listener.this[local.green_state].arn]
      }

      target_group {
        name = aws_lb_target_group.this[0].name
      }

      target_group {
        name = aws_lb_target_group.this[1].name
      }
    }
  }
}

resource "null_resource" "codedeploy_server" {
  triggers = {
    task_definition_revision = aws_ecs_task_definition.server.revision
  }

  provisioner "local-exec" {
    environment = {
      APPSPEC_YAML = templatefile("${path.module}/templates/server-appspec.yaml.tftpl", {
        application_name      = aws_codedeploy_app.server.name
        deployment_group_name = aws_codedeploy_deployment_group.server.deployment_group_name
        task_definition_arn   = aws_ecs_task_definition.server.arn
        container_name        = local.service
        container_port        = local.server_port
      })
    }
    interpreter = ["/bin/bash", "-c"]
    command     = <<EOF
      deploy_id="$(aws deploy create-deployment --cli-input-yaml "$APPSPEC_YAML" --query "[deploymentId]" --output text)"

      while true; do
        deployment_status="$(aws deploy get-deployment \
            --deployment-id "$deploy_id" \
            --query "[deploymentInfo.status]" \
            --output text)"

        if [[ $deployment_status == "Succeeded" ]]; then
            echo "Deployment succeeded."
            break
        elif [[ $deployment_status == "Failed" || $deployment_status == "Stopped" ]]; then
            echo "Deployment failed!"
            exit 1
        fi

        echo "Status: $deployment_status..."
        echo "Sleeping for 5 seconds..."
        sleep 5
      done
    EOF
    quiet       = true
  }
}
