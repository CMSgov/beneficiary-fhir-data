{
    "widgets": [
        {
            "height": 6,
            "width": 8,
            "y": 0,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "messages/count/error" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Messages Error Count",
                "region": "us-east-1",
                "stat": "Average",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 8,
            "y": 0,
            "x": 8,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "messages/count/datasetfailed" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Messages Failed Dataset Count",
                "region": "us-east-1",
                "stat": "Average",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 8,
            "y": 0,
            "x": 16,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "messages/count/skipped" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Messages Skipped Count",
                "region": "us-east-1",
                "stat": "Sum",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 24,
            "y": 6,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "CWAgent", "mem_used_percent", "InstanceId", "${instance_id}", { "label": "Percentage of memory used in the last 5 minutes"} ]
                ],
                "view": "timeSeries",
                "stacked": true,
                "title": "Percentage of Memory Used",
                "region": "us-east-1",
                "stat": "Average",
                "period": 300
            }
        }
    ]
}
