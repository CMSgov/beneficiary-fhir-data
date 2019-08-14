{
    "widgets": [
        {
            "type": "text",
            "x": 0,
            "y": 0,
            "width": 24,
            "height": 6,
            "properties": {
                "markdown": "\n# AWS Metrics Dashboard - ${app}-${env}\n\nA link to this dashboard: [MainDashboard-${app}-${env}](#dashboards:name=MainDashboard-${app}-${env}).\n\n## Dashboard Sections\nSection | Description | Reference-Link\n----|-----\nELB | Elastic Load Balancing |https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/elb-metricscollected.html\nASG | Auto Scaling Group | https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/as-metricscollected.html\nEC2 | Elastic Compute Cloud ASG Aggregated | https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/GetMetricAutoScalingGroup.html\nRDS | Relational Database Service | https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/rds-metricscollected.html\nNAT | Network Address Translation Gateway | https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/nat-gateway-metricscollected.html\n"
            }
        },
        {
            "type": "text",
            "x": 0,
            "y": 6,
            "width": 24,
            "height": 3,
            "properties": {
                "markdown": "\n# ${app}-${env}\n## ELB - Elastic Load Balancing - https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/elb-metricscollected.html\n"
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 9,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/ELB", "HealthyHostCount", "LoadBalancerName", "${load_balancer_name}" ]
                ],
                "period": 300
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 9,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/ELB", "RequestCount", "LoadBalancerName", "${load_balancer_name}", { "stat": "Sum" } ]
                ],
                "period": 300,
                "title": "LB RequestCount"
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 15,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/ELB", "RequestCount", "LoadBalancerName", "${load_balancer_name}", "AvailabilityZone", "us-east-1a", { "stat": "Sum" } ],
                    [ "...", "us-east-1c", { "stat": "Sum" } ],
                    [ "...", "us-east-1b", { "stat": "Sum" } ]
                ],
                "period": 300,
                "title": "Per AZ RequestCount"
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 15,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/ELB", "EstimatedALBActiveConnectionCount", "LoadBalancerName", "${load_balancer_name}" ]
                ],
                "period": 300
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 21,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-P3D",
                "end": "P0D",
                "metrics": [
                    [ "AWS/ELB", "HTTPCode_Backend_2XX", "LoadBalancerName", "${load_balancer_name}", { "stat": "Sum" } ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 21,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-P3D",
                "end": "P0D",
                "metrics": [
                    [ "AWS/ELB", "HTTPCode_Backend_5XX", "LoadBalancerName", "${load_balancer_name}", { "stat": "Sum" } ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 6,
            "y": 21,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-P3D",
                "end": "P0D",
                "metrics": [
                    [ "AWS/ELB", "HTTPCode_ELB_5XX", "LoadBalancerName", "${load_balancer_name}", { "stat": "Sum" } ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 6,
            "y": 27,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-P3D",
                "end": "P0D",
                "metrics": [
                    [ "AWS/ELB", "HTTPCode_Backend_4XX", "LoadBalancerName", "${load_balancer_name}", { "stat": "Sum" } ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 27,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-P3D",
                "end": "P0D",
                "metrics": [
                    [ "AWS/ELB", "HTTPCode_Backend_3XX", "LoadBalancerName", "${load_balancer_name}", { "stat": "Sum" } ]
                ]
            }
        },


        {
            "type": "text",
            "x": 0,
            "y": 33,
            "width": 24,
            "height": 3,
            "properties": {
                "markdown": "\n# ${app}-${env}\n## Auto Scaling Group (ASG) - https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/as-metricscollected.html\n\nGroup Name: ${asg_name}\n"
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 36,
            "width": 24,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "metrics": [
                    [ "AWS/AutoScaling", "GroupDesiredCapacity", "AutoScalingGroupName", "${asg_name}" ],
                    [ ".", "GroupMaxSize", ".", "." ],
                    [ ".", "GroupMinSize", ".", "." ],
                    [ ".", "GroupInServiceInstances", ".", "." ]
                ],
                "region": "us-east-1"
            }
        },

        {
            "type": "text",
            "x": 0,
            "y": 42,
            "width": 24,
            "height": 3,
            "properties": {
                "markdown": "\n# ${app}-${env}\n## Elastic Compute Cloud (EC2) ASG Aggregated - https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/GetMetricAutoScalingGroup.html\n\nGroup Name: ${asg_name}\n"
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 45,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/EC2", "CPUUtilization", "AutoScalingGroupName", "${asg_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 45,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/EC2", "StatusCheckFailed_Instance", "AutoScalingGroupName", "${asg_name}" ],
                    [ ".", "StatusCheckFailed_System", ".", "." ],
                    [ ".", "StatusCheckFailed", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 51,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/EC2", "NetworkOut", "AutoScalingGroupName", "${asg_name}" ],
                    [ ".", "NetworkIn", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 51,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/EC2", "NetworkPacketsIn", "AutoScalingGroupName", "${asg_name}" ],
                    [ ".", "NetworkPacketsOut", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 57,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/EC2", "DiskWriteOps", "AutoScalingGroupName", "${asg_name}" ],
                    [ ".", "DiskReadOps", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 57,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/EC2", "DiskReadBytes", "AutoScalingGroupName", "${asg_name}" ],
                    [ ".", "DiskWriteBytes", ".", "." ]
                ]
            }
        },


        {
            "type": "text",
            "x": 0,
            "y": 63,
            "width": 24,
            "height": 3,
            "properties": {
                "markdown": "\n# ${app}-${env}\n## Relational Database Service (RDS) Metrics - https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/rds-metricscollected.html\n\nDB INSTANCE:\n* ${rds_name}\n"
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 69,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 69,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 75,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "NetworkReceiveThroughput", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 75,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "NetworkTransmitThroughput", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 81,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "DiskQueueDepth", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 87,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "ReadIOPS", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 18,
            "y": 87,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "WriteIOPS", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 81,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "ReadLatency", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 18,
            "y": 81,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "WriteLatency", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 87,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "ReadThroughput", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 6,
            "y": 87,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "WriteThroughput", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 6,
            "y": 93,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "FreeableMemory", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 93,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 93,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "start": "-PT12H",
                "end": "P0D",
                "metrics": [
                    [ "AWS/RDS", "SwapUsage", "DBInstanceIdentifier", "${rds_name}" ]
                ]
            }
        },

        {
            "type": "text",
            "x": 0,
            "y": 99,
            "width": 24,
            "height": 3,
            "properties": {
                "markdown": "\n# ${app}-${env}\n## Network Address Translation (NAT) Gateway Metrics - https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/nat-gateway-metricscollected.html\n\nNAT GATEWAY ID: ${nat_gw_name}\n"
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 102,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "metrics": [
                    [ "AWS/NATGateway", "ActiveConnectionCount", "NatGatewayId", "${nat_gw_name}" ]
                ],
                "region": "us-east-1"
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 102,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/NATGateway", "ConnectionAttemptCount", "NatGatewayId", "${nat_gw_name}" ],
                    [ ".", "ConnectionEstablishedCount", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 108,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/NATGateway", "PacketsInFromDestination", "NatGatewayId", "${nat_gw_name}" ],
                    [ ".", "PacketsOutToSource", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 108,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/NATGateway", "PacketsOutToDestination", "NatGatewayId", "${nat_gw_name}" ],
                    [ ".", "PacketsInFromSource", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 120,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/NATGateway", "IdleTimeoutCount", "NatGatewayId", "${nat_gw_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 6,
            "y": 120,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/NATGateway", "ErrorPortAllocation", "NatGatewayId", "${nat_gw_name}" ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 0,
            "y": 114,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/NATGateway", "BytesInFromDestination", "NatGatewayId", "${nat_gw_name}" ],
                    [ ".", "BytesOutToSource", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 114,
            "width": 12,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/NATGateway", "BytesOutToDestination", "NatGatewayId", "${nat_gw_name}" ],
                    [ ".", "BytesInFromSource", ".", "." ]
                ]
            }
        },
        {
            "type": "metric",
            "x": 12,
            "y": 120,
            "width": 6,
            "height": 6,
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "AWS/NATGateway", "PacketsDropCount", "NatGatewayId", "${nat_gw_name}" ]
                ]
            }
        }

    ]


}
