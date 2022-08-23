{
    "widgets": [
        {
            "height": 1,
            "width": 24,
            "y": 0,
            "x": 0,
            "type": "text",
            "properties": {
                "markdown": "# Requests"
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 1,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/count/coverageAll/all" ],
                    [ ".", "http-requests/count/eobAll/all" ],
                    [ ".", "http-requests/count/metadata/all" ],
                    [ ".", "http-requests/count/patientAll/all" ]
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
            "height": 12,
            "width": 3,
            "y": 1,
            "x": 18,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/count-500/ab2d" ],
                    [ ".", "http-requests/count-500/bb" ],
                    [ ".", "http-requests/count-500/bcda" ],
                    [ ".", "http-requests/count-500/dpc" ],
                    [ ".", "http-requests/count-500/test" ]
                ],
                "view": "singleValue",
                "region": "us-east-1",
                "title": "Count 500 Errors by Partner",
                "period": 300,
                "stat": "Sum",
                "sparkline": true,
                "stacked": true,
                "liveData": true
            }
        },
        {
            "height": 12,
            "width": 3,
            "y": 1,
            "x": 21,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/count-not-2xx/ab2d" ],
                    [ ".", "http-requests/count-not-2xx/bb" ],
                    [ ".", "http-requests/count-not-2xx/bcda" ],
                    [ ".", "http-requests/count-not-2xx/dpc" ],
                    [ ".", "http-requests/count-not-2xx/test" ]
                ],
                "view": "singleValue",
                "region": "us-east-1",
                "title": "HTTP non 2XXs",
                "period": 300,
                "stat": "Sum",
                "sparkline": true,
                "stacked": true,
                "liveData": true
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 7,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/count/coverageAll/bb" ],
                    [ ".", "http-requests/count/eobAll/bb" ],
                    [ ".", "http-requests/count/metadata/bb" ],
                    [ ".", "http-requests/count/patientAll/bb" ]
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
            "y": 1,
            "x": 6,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/count/coverageAll/bcda" ],
                    [ ".", "http-requests/count/eobAll/bcda" ],
                    [ ".", "http-requests/count/metadata/bcda" ],
                    [ ".", "http-requests/count/patientAll/bcda" ]
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
            "y": 7,
            "x": 12,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/count/coverageAll/test" ],
                    [ ".", "http-requests/count/eobAll/test" ],
                    [ ".", "http-requests/count/metadata/test" ],
                    [ ".", "http-requests/count/patientAll/test" ]
                ],
                "view": "timeSeries",
                "stacked": true,
                "title": "Test Request Count",
                "region": "us-east-1",
                "stat": "Sum",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 7,
            "x": 6,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/count/coverageAll/dpc" ],
                    [ ".", "http-requests/count/eobAll/dpc" ],
                    [ ".", "http-requests/count/metadata/dpc" ],
                    [ ".", "http-requests/count/patientAll/dpc" ]
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
            "y": 1,
            "x": 12,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/count/coverageAll/ab2d" ],
                    [ ".", "http-requests/count/eobAll/ab2d" ],
                    [ ".", "http-requests/count/metadata/ab2d" ],
                    [ ".", "http-requests/count/patientAll/ab2d" ]
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
            "height": 1,
            "width": 24,
            "y": 13,
            "x": 0,
            "type": "text",
            "properties": {
                "markdown": "# Latency by Partners"
            }
        },
        {
            "height": 5,
            "width": 9,
            "y": 14,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/eobAll/ab2d" ],
                    [ ".", "http-requests/latency/eobAll/bb" ],
                    [ ".", "http-requests/latency/eobAll/bcda" ],
                    [ ".", "http-requests/latency/eobAll/test" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Explanation Of Benefit P50 Latency",
                "region": "us-east-1",
                "period": 300,
                "stat": "p50"
            }
        },
        {
            "height": 5,
            "width": 7,
            "y": 14,
            "x": 9,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/eobAll/ab2d" ],
                    [ ".", "http-requests/latency/eobAll/bb" ],
                    [ ".", "http-requests/latency/eobAll/bcda" ],
                    [ ".", "http-requests/latency/eobAll/test" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Explanation Of Benefit P99 Latency",
                "region": "us-east-1",
                "stat": "p99",
                "period": 300
            }
        },
        {
            "height": 5,
            "width": 8,
            "y": 14,
            "x": 16,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/eobAll/ab2d" ],
                    [ ".", "http-requests/latency/eobAll/bb" ],
                    [ ".", "http-requests/latency/eobAll/bcda" ],
                    [ ".", "http-requests/latency/eobAll/test" ]
                ],
                "view": "singleValue",
                "stacked": false,
                "title": "Explanation Of Benefit Max Latency",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300,
                "sparkline": true,
                "liveData": true
            }
        },
        {
            "height": 6,
            "width": 9,
            "y": 19,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/coverageAll/ab2d" ],
                    [ ".", "http-requests/latency/coverageAll/bb" ],
                    [ ".", "http-requests/latency/coverageAll/bcda" ],
                    [ ".", "http-requests/latency/coverageAll/test" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Coverage P50 Latency",
                "region": "us-east-1",
                "period": 300,
                "stat": "p50"
            }
        },
        {
            "height": 6,
            "width": 9,
            "y": 19,
            "x": 9,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/coverageAll/ab2d" ],
                    [ ".", "http-requests/latency/coverageAll/bb" ],
                    [ ".", "http-requests/latency/coverageAll/bcda" ],
                    [ ".", "http-requests/latency/coverageAll/test" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Coverage P99 Latency",
                "region": "us-east-1",
                "stat": "p99",
                "period": 300
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 19,
            "x": 18,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/coverageAll/ab2d" ],
                    [ ".", "http-requests/latency/coverageAll/bb" ],
                    [ ".", "http-requests/latency/coverageAll/bcda" ],
                    [ ".", "http-requests/latency/coverageAll/test" ]
                ],
                "view": "singleValue",
                "stacked": false,
                "title": "Coverage Max Latency",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300
            }
        },
        {
            "height": 5,
            "width": 9,
            "y": 25,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/metadata/ab2d" ],
                    [ ".", "http-requests/latency/metadata/bb" ],
                    [ ".", "http-requests/latency/metadata/bcda" ],
                    [ ".", "http-requests/latency/metadata/test" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Metadata P50 Latency",
                "region": "us-east-1",
                "period": 300,
                "stat": "p50"
            }
        },
        {
            "height": 5,
            "width": 9,
            "y": 25,
            "x": 9,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/metadata/ab2d" ],
                    [ ".", "http-requests/latency/metadata/bb" ],
                    [ ".", "http-requests/latency/metadata/bcda" ],
                    [ ".", "http-requests/latency/metadata/test" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Metadata P99 Latency",
                "region": "us-east-1",
                "stat": "p99",
                "period": 300
            }
        },
        {
            "height": 5,
            "width": 6,
            "y": 25,
            "x": 18,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/metadata/ab2d" ],
                    [ ".", "http-requests/latency/metadata/bb" ],
                    [ ".", "http-requests/latency/metadata/bcda" ],
                    [ ".", "http-requests/latency/metadata/test" ]
                ],
                "view": "singleValue",
                "stacked": false,
                "title": "Metadata Max Latency",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300,
                "sparkline": true,
                "liveData": true
            }
        },
        {
            "height": 6,
            "width": 9,
            "y": 30,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/patientAll/ab2d" ],
                    [ ".", "http-requests/latency/patientAll/bb" ],
                    [ ".", "http-requests/latency/patientAll/bcda" ],
                    [ ".", "http-requests/latency/patientAll/test" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Patient P50 Latency",
                "region": "us-east-1",
                "period": 900,
                "stat": "p50",
                "annotations": {
                    "horizontal": [
                        {
                            "label": "ALERT: 15-minute Mean /v*/fhir/Patient by_contract=true count=4000",
                            "value": 24000
                        },
                        {
                            "label": "WARN: 15-minute Mean /v*/fhir/Patient by_contract=true count=4000",
                            "value": 17000
                        }
                    ]
                }
            }
        },
        {
            "height": 6,
            "width": 9,
            "y": 30,
            "x": 9,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/patientAll/ab2d" ],
                    [ ".", "http-requests/latency/patientAll/bb" ],
                    [ ".", "http-requests/latency/patientAll/bcda" ],
                    [ ".", "http-requests/latency/patientAll/test" ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Patient P99 Latency",
                "region": "us-east-1",
                "stat": "p99",
                "period": 900,
                "annotations": {
                    "horizontal": [
                        {
                            "label": "ALERT: 15-minute Mean /v*/fhir/Patient by_contract=true count=4000",
                            "value": 24000
                        },
                        {
                            "label": "WARN: 15-minute Mean /v*/fhir/Patient by_contract=true count=4000",
                            "value": 17000
                        },
                        {
                            "label": "ALERT: 15-minute 99th Percentile /v*/fhir/Patient by_contract=true count=4000",
                            "value": 40000
                        },
                        {
                            "label": "WARN: 15-minute 99th Percentile /v*/fhir/Patient by_contract=true count=4000",
                            "value": 120000
                        }
                    ]
                }
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 30,
            "x": 18,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/patientAll/ab2d" ],
                    [ ".", "http-requests/latency/patientAll/bb" ],
                    [ ".", "http-requests/latency/patientAll/bcda" ],
                    [ ".", "http-requests/latency/patientAll/test" ]
                ],
                "view": "singleValue",
                "stacked": false,
                "title": "Patient Max Latency",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 900,
                "annotations": {
                    "horizontal": [
                        {
                            "label": "ALERT: /v*/fhir/Patient by_contract=true count=4000",
                            "value": 24000
                        },
                        {
                            "label": "WARN: /v*/fhir/Patient by_contract=true count=4000",
                            "value": 17000
                        },
                        {
                            "label": "ALERT: 15-minute 99th /v*/fhir/Patient by_contract=false Percentile (BULK) 50% Bulk Timeout",
                            "value": 400
                        },
                        {
                            "label": "WARN: 15-minute 99th /v*/fhir/Patient by_contract=false Percentile (BULK) 150ms",
                            "value": 120
                        },
                        {
                            "label": "ALERT: 15-minute 99th /v*/fhir/Patient by_contract=false Percentile (NON-BULK) 220ms",
                            "value": 220
                        },
                        {
                            "label": "WARN: 15-minute 99th /v*/fhir/Patient by_contract=false Percentile (NON-BULK) 150ms",
                            "value": 150
                        }
                    ]
                },
                "sparkline": true,
                "liveData": true
            }
        },
        {
            "height": 1,
            "width": 24,
            "y": 36,
            "x": 0,
            "type": "text",
            "properties": {
                "markdown": "# Latency SLO by endpoint"
            }
        },
        {
            "height": 4,
            "width": 24,
            "y": 37,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/all/all", { "stat": "p50", "color": "#2ca02c", "label": "http-requests/latency/all/all mean" } ],
                    [ "...", { "color": "#ff7f0e", "stat": "p99" } ],
                    [ "...", { "color": "#d62728" } ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Latency - All Requests",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300
            }
        },
        {
            "height": 8,
            "width": 12,
            "y": 57,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/coverageAll/all", { "stat": "p50", "color": "#2ca02c", "label": "http-requests/latency/coverageAll/all mean" } ],
                    [ "...", { "color": "#ff7f0e", "stat": "p99" } ],
                    [ "...", { "color": "#d62728" } ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Latency Percentiles - Coverage Requests",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300,
                "annotations": {
                    "horizontal": [
                        {
                            "label": "ALERT: 15-minute Mean 170ms",
                            "value": 170
                        },
                        {
                            "label": "WARN: 15-minute Mean 120ms",
                            "value": 120
                        },
                        {
                            "label": "ALERT: 15-minute 99th Percentile (BULK) 50% Bulk Timeout",
                            "value": 400
                        },
                        {
                            "label": "WARN: 15-minute 99th Percentile (BULK) 150ms",
                            "value": 120
                        },
                        {
                            "label": "ALERT: 15-minute 99th Percentile (NON-BULK) 220ms",
                            "value": 220
                        },
                        {
                            "label": "WARN: 15-minute 99th Percentile (NON-BULK) 150ms",
                            "value": 150
                        }
                    ]
                }
            }
        },
        {
            "height": 8,
            "width": 12,
            "y": 41,
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
        },
        {
            "height": 8,
            "width": 12,
            "y": 41,
            "x": 12,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/patientAll/all", { "stat": "p50", "color": "#2ca02c", "region": "us-east-1" } ],
                    [ "${dashboard_namespace}", "http-requests/latency/patientAll/all", { "color": "#ff7f0e", "stat": "p99", "region": "us-east-1" } ],
                    [ "${dashboard_namespace}", "http-requests/latency/patientAll/all", { "color": "#d62728", "region": "us-east-1" } ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Latency - Patient Requests",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300,
                "annotations": {
                    "horizontal": [
                        {
                            "label": "ALERT: /v*/fhir/Patient by_contract=false",
                            "value": 170
                        },
                        {
                            "label": "WARN: /v*/fhir/Patient by_contract=false",
                            "value": 120
                        }
                    ]
                }
            }
        },
        {
            "height": 8,
            "width": 12,
            "y": 49,
            "x": 12,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}", "http-requests/latency/metadata/all", { "stat": "p50", "color": "#2ca02c", "label": "http-requests/latency/metadata/all mean" } ],
                    [ "...", { "color": "#ff7f0e", "stat": "p99" } ],
                    [ "...", { "color": "#d62728" } ]
                ],
                "view": "timeSeries",
                "stacked": false,
                "title": "Latency Percentiles - Metadata Requests",
                "region": "us-east-1",
                "stat": "Maximum",
                "period": 300
            }
        }
    ]
}