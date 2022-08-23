{
    "widgets": [
        {
            "height": 1,
            "width": 24,
            "y": 65,
            "x": 0,
            "type": "text",
            "properties": {
                "markdown": "# Service Metrics"
            }
        },
        {
            "height": 9,
            "width": 24,
            "y": 66,
            "x": 0,
            "type": "explorer",
            "properties": {
                "metrics": [
                    {
                        "metricName": "CPUUtilization",
                        "resourceType": "AWS::EC2::Instance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "MetadataNoToken",
                        "resourceType": "AWS::EC2::Instance",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "NetworkIn",
                        "resourceType": "AWS::EC2::Instance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "NetworkOut",
                        "resourceType": "AWS::EC2::Instance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "mem_used_percent",
                        "resourceType": "AWS::EC2::Instance",
                        "stat": "Average"
                    }
                ],
                "labels": [
                    {
                        "key": "Environment",
                        "value": "${env}"
                    },
                    {
                        "key": "Name",
                        "value": "bfd-${env}-fhir"
                    }
                ],
                "widgetOptions": {
                    "legend": {
                        "position": "bottom"
                    },
                    "view": "timeSeries",
                    "stacked": false,
                    "rowsPerPage": 1,
                    "widgetsPerRow": 3
                },
                "period": 300,
                "splitBy": "Name",
                "region": "us-east-1",
                "title": "AWS Host Metrics"
            }
        },
        {
            "height": 9,
            "width": 24,
            "y": 93,
            "x": 0,
            "type": "explorer",
            "properties": {
                "metrics": [
                    {
                        "metricName": "CPUUtilization",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "ReadLatency",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "DatabaseConnections",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "FreeStorageSpace",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "FreeableMemory",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "ReadThroughput",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "ReadIOPS",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "WriteLatency",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "WriteThroughput",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    },
                    {
                        "metricName": "WriteIOPS",
                        "resourceType": "AWS::RDS::DBInstance",
                        "stat": "Average"
                    }
                ],
                "labels": [
                    {
                        "key": "Environment",
                        "value": "${env}"
                    }
                ],
                "widgetOptions": {
                    "legend": {
                        "position": "right"
                    },
                    "view": "timeSeries",
                    "stacked": false,
                    "rowsPerPage": 1,
                    "widgetsPerRow": 3
                },
                "period": 300,
                "splitBy": "Environment",
                "region": "us-east-1",
                "title": "RDS Metrics"
            }
        },
        {
            "height": 9,
            "width": 24,
            "y": 84,
            "x": 0,
            "type": "explorer",
            "properties": {
                "metrics": [
                    {
                        "metricName": "BackendConnectionErrors",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "DesyncMitigationMode_NonCompliant_Request_Count",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "HTTPCode_Backend_2XX",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "HTTPCode_Backend_3XX",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "HTTPCode_Backend_4XX",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "HTTPCode_Backend_5XX",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "HTTPCode_ELB_4XX",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "HTTPCode_ELB_5XX",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "HealthyHostCount",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Average"
                    },
                    {
                        "metricName": "Latency",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Average"
                    },
                    {
                        "metricName": "RequestCount",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "SpilloverCount",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "SurgeQueueLength",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Average"
                    },
                    {
                        "metricName": "UnHealthyHostCount",
                        "resourceType": "AWS::ElasticLoadBalancing::LoadBalancer",
                        "stat": "Average"
                    }
                ],
                "labels": [
                    {
                        "key": "Environment",
                        "value": "${env}"
                    }
                ],
                "widgetOptions": {
                    "legend": {
                        "position": "right"
                    },
                    "view": "timeSeries",
                    "stacked": false,
                    "rowsPerPage": 1,
                    "widgetsPerRow": 3
                },
                "period": 300,
                "splitBy": "#",
                "region": "us-east-1",
                "title": "Load Balancer Metrics"
            }
        },
        {
            "height": 9,
            "width": 24,
            "y": 75,
            "x": 0,
            "type": "explorer",
            "properties": {
                "metrics": [
                    {
                        "metricName": "VolumeReadBytes",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "VolumeWriteBytes",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "VolumeReadOps",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "VolumeTotalReadTime",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Average"
                    },
                    {
                        "metricName": "VolumeWriteOps",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "VolumeTotalWriteTime",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Average"
                    },
                    {
                        "metricName": "VolumeIdleTime",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Average"
                    },
                    {
                        "metricName": "VolumeQueueLength",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Average"
                    },
                    {
                        "metricName": "BurstBalance",
                        "resourceType": "AWS::EC2::Volume",
                        "stat": "Average"
                    }
                ],
                "labels": [
                    {
                        "key": "Environment",
                        "value": "${env}"
                    },
                    {
                        "key": "Name",
                        "value": "bfd-${env}-fhir"
                    }
                ],
                "widgetOptions": {
                    "legend": {
                        "position": "hidden"
                    },
                    "view": "timeSeries",
                    "stacked": false,
                    "rowsPerPage": 1,
                    "widgetsPerRow": 3
                },
                "period": 300,
                "splitBy": "Name",
                "region": "us-east-1",
                "title": "EBS Volume Metrics"
            }
        },
        {
            "height": 9,
            "width": 24,
            "y": 102,
            "x": 0,
            "type": "explorer",
            "properties": {
                "metrics": [
                    {
                        "metricName": "IncomingLogEvents",
                        "resourceType": "AWS::Logs::LogGroup",
                        "stat": "Sum"
                    },
                    {
                        "metricName": "IncomingBytes",
                        "resourceType": "AWS::Logs::LogGroup",
                        "stat": "Sum"
                    }
                ],
                "aggregateBy": {
                    "key": "",
                    "func": ""
                },
                "labels": [
                    {
                        "key": "Environment",
                        "value": "${env}"
                    }
                ],
                "widgetOptions": {
                    "legend": {
                        "position": "right"
                    },
                    "view": "timeSeries",
                    "stacked": false,
                    "rowsPerPage": 1,
                    "widgetsPerRow": 3
                },
                "period": 300,
                "splitBy": "#",
                "region": "us-east-1",
                "title": "Log Metrics"
            }
        },
        {
            "height": 8,
            "width": 12,
            "y": 49,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/eobAll/all", { "stat": "p50", "color": "#2ca02c", "label": "http-requests/latency/eobAll/all mean" } ],
                    [ "...", { "color": "#ff7f0e", "stat": "p99" } ],
                    [ "...", { "color": "#d62728" } ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Latency Percentiles - Explanation Of Benefit Requests",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300,
                "annotations": {
                    "horizontal": [
                        {
                            "label": "ALERT: EOB - 15-minute 99P (BULK)",
                            "value": 1680
                        },
                        {
                            "label": "WARN: EOB - 15-minute 99P (BULK)",
                            "value": 840
                        },
                        {
                            "label": "ALERT: EOB - 15-minute 99P (NON-BULK)",
                            "value": 1000
                        },
                        {
                            "label": "WARN: EOB - 15-minute 99P (NON-BULK)",
                            "value": 700
                        },
                        {
                            "label": "ALERT: EOB - 15-minute 50P",
                            "value": 600
                        },
                        {
                            "label": "WARN: EOB- 15-minute 50P",
                            "value": 420
                        }
                    ]
                }
            }
        }
    ]
}