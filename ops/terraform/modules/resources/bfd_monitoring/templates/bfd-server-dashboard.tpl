{
    "widgets": [
        {
            "height": 6,
            "width": 18,
            "y": 0,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/count/metadata/all" ],
                    [ ".", "json/http-requests/count/coverageAll/all" ],
                    [ ".", "json/http-requests/count/patientAll/all" ],
                    [ ".", "json/http-requests/count/eobAll/all" ]
                ],
                "view": "timeSeries",
                "stacked": true,
                "title": "Request Count All",
                "region": "us-east-1",
                "stat": "Sum",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 6,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/count/metadata/bb" ],
                    [ ".", "json/http-requests/count/coverageAll/bb" ],
                    [ ".", "json/http-requests/count/patientAll/bb" ],
                    [ ".", "json/http-requests/count/eobAll/bb" ]
                ],
                "view": "timeSeries",
                "stacked": true,
                "title": "BB Request Count",
                "region": "us-east-1",
                "stat": "Sum",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 6,
            "x": 12,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/count/metadata/bcda" ],
                    [ ".", "json/http-requests/count/coverageAll/bcda" ],
                    [ ".", "json/http-requests/count/patientAll/bcda" ],
                    [ ".", "json/http-requests/count/eobAll/bcda" ]
                ],
                "view": "timeSeries",
                "stacked": true,
                "title": "BCDA Request Count",
                "region": "us-east-1",
                "stat": "Sum",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 6,
            "x": 6,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/count/metadata/mct" ],
                    [ ".", "json/http-requests/count/coverageAll/mct" ],
                    [ ".", "json/http-requests/count/patientAll/mct" ],
                    [ ".", "json/http-requests/count/eobAll/mct" ]
                ],
                "view": "timeSeries",
                "stacked": true,
                "title": "MCT Request Count",
                "region": "us-east-1",
                "stat": "Sum",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 12,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/latency/eobAll/bb" ],
                    [ ".", "json/http-requests/latency/eobAll/bcda" ],
                    [ ".", "json/http-requests/latency/eobAll/mct" ],
                    [ ".", "json/http-requests/latency/eobAll/ab2d" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "EOB P50 Latency",
                "region": "us-east-1",
                "period": 300,
                "stat": "p50"
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 12,
            "x": 6,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/latency/eobAll/bb" ],
                    [ ".", "json/http-requests/latency/eobAll/bcda" ],
                    [ ".", "json/http-requests/latency/eobAll/mct" ],
                    [ ".", "json/http-requests/latency/eobAll/ab2d" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "EOB P95 Latency",
                "region": "us-east-1",
                "stat": "p95",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 12,
            "x": 12,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/latency/eobAll/bb" ],
                    [ ".", "json/http-requests/latency/eobAll/bcda" ],
                    [ ".", "json/http-requests/latency/eobAll/mct" ],
                    [ ".", "json/http-requests/latency/eobAll/ab2d" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "EOB P99 Latency",
                "region": "us-east-1",
                "stat": "p99",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 12,
            "x": 18,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/latency/eobAll/bb" ],
                    [ ".", "json/http-requests/latency/eobAll/bcda" ],
                    [ ".", "json/http-requests/latency/eobAll/mct" ],
                    [ ".", "json/http-requests/latency/eobAll/ab2d" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "EOB Max Latency",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300
            }
        },
        {
            "height": 5,
            "width": 12,
            "y": 23,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "AWS/EC2", "CPUUtilization", { "visible": false } ],
                    [ ".", ".", "InstanceType", "c5.4xlarge", { "color": "#2ca02c", "stat": "Average" } ],
                    [ "...", { "color": "#d62728" } ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 3600,
                "title": "ASG CPU Usage"
            }
        },
        {
            "height": 3,
            "width": 24,
            "y": 28,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/count-500/bb" ],
                    [ ".", "json/http-requests/count-500/bcda" ],
                    [ ".", "json/http-requests/count-500/mct" ],
                    [ ".", "json/http-requests/count-500/dpc" ],
                    [ ".", "json/http-requests/count-500/ab2d" ]
                ],
                "view": "singleValue",
                "region": "us-east-1",
                "title": "Total number of HTTP 500s errors in the last 24 hours",
                "period": 86400,
                "stat": "Sum"
            }
        },
        {
            "height": 3,
            "width": 24,
            "y": 31,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/count-not-2xx/bb" ],
                    [ ".", "json/http-requests/count-not-2xx/bcda" ],
                    [ ".", "json/http-requests/count-not-2xx/mct" ],
                    [ ".", "json/http-requests/count-not-2xx/dpc" ],
                    [ ".", "json/http-requests/count-not-2xx/ab2d" ]
                ],
                "view": "singleValue",
                "region": "us-east-1",
                "title": "Total number of HTTP non-2XXs errors in the last 24 hours",
                "period": 86400,
                "stat": "Sum"
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 6,
            "x": 18,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/count/metadata/dpc" ],
                    [ ".", "json/http-requests/count/coverageAll/dpc" ],
                    [ ".", "json/http-requests/count/patientAll/dpc" ],
                    [ ".", "json/http-requests/count/eobAll/dpc" ]
                ],
                "view": "timeSeries",
                "stacked": true,
                "region": "us-east-1",
                "stat": "Sum",
                "period": 300,
                "title": "DPC Request Count"
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 0,
            "x": 18,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/count/metadata/ab2d" ],
                    [ ".", "json/http-requests/count/coverageAll/ab2d" ],
                    [ ".", "json/http-requests/count/patientAll/ab2d" ],
                    [ ".", "json/http-requests/count/eobAll/ab2d" ]
                ],
                "view": "timeSeries",
                "stacked": true,
                "region": "us-east-1",
                "title": "AB2D Request Count",
                "period": 300,
                "stat": "Sum"
            }
        },
        {
            "height": 10,
            "width": 12,
            "y": 18,
            "x": 12,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "json/http-requests/latency/all/all", { "stat": "Average", "color": "#2ca02c" } ],
                    [ "...", { "color": "#ff7f0e", "stat": "p99" } ],
                    [ "...", { "color": "#d62728" } ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Latency Percentiles - All Requests",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300
            }
        },
        {
            "height": 5,
            "width": 12,
            "y": 18,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "AWS/AutoScaling", "GroupInServiceInstances", "AutoScalingGroupName", "${asg_name}" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 3600,
                "title": "ASG Instance Counts"
            }
        }
    ]
}
