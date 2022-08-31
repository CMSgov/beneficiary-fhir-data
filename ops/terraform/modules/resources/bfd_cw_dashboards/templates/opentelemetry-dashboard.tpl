{
    "widgets": [
        {
            "height": 1,
            "width": 24,
            "y": 0,
            "x": 0,
            "type": "text",
            "properties": {
                "markdown": "# Dashboard Directory \n\n
                [button:SLI Dashboard]([https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=bfd-server-${env})\n
                [button:BFD Application Log Metrics]([https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=bfd-server-${env})\n
                [button:Host Metrics]([https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=bfd-server-${env})\n
                [button:AWS Service Dashboards]([https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=bfd-server-${env})\n
                [button:OpenTelemetry Metrics]([https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#dashboards:name=bfd-server-${env})\n\n:"
                }
        },
        
        {
            "height": 1,
            "width": 24,
            "y": 5,
            "x": 0,
            "type": "text",
            "properties": {
                "markdown": "#BFD-Server OpenTelemtry Metrics\n---\n"
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 1,
            "x": 0,
            "type": "metric",
            "properties": {
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "non-heap.used", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "heap.used", ".", "." ]
                ],
                "title": "Dropwizard Heap"
            }
        },
        {
            "height": 3,
            "width": 10,
            "y": 7,
            "x": 6,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "CarrierClaimTransformer.transform", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "CarrierClaimTransformerV2.transform", ".", "." ]
                ],
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "title": "Dropwizard - Carrier Claim Transformer",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false
            }
        },
        {
            "height": 3,
            "width": 9,
            "y": 10,
            "x": 6,
            "type": "metric",
            "properties": {
                "sparkline": false,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "com.codahale.metrics.servlet.InstrumentedFilter.activeRequests", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "com.codahale.metrics.servlet.InstrumentedFilter.requests", ".", "." ]
                ],
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "title": "Dropwizard InstrumentedFilter",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false
            }
        },
        {
            "height": 3,
            "width": 24,
            "y": 13,
            "x": 0,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "title": "hikaricp-3.0  DB Client Connections",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "db.client.connections.create_time", "OTelLib", "io.opentelemetry.hikaricp-3.0" ],
                    [ ".", "db.client.connections.idle.min", ".", "." ],
                    [ ".", "db.client.connections.max", ".", "." ],
                    [ ".", "db.client.connections.pending_requests", ".", "." ],
                    [ ".", "db.client.connections.usage", ".", "." ],
                    [ ".", "db.client.connections.use_time", ".", "." ],
                    [ ".", "db.client.connections.wait_time", ".", "." ]
                ]
            }
        },
        {
            "height": 3,
            "width": 10,
            "y": 4,
            "x": 6,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "title": "Dropwizard DME Claim Transformer",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "DMEClaimTransformer.transform", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "DMEClaimTransformerV2.transform", ".", "." ]
                ],
                "liveData": true,
                "setPeriodToTimeRange": false,
                "trend": true
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 7,
            "x": 0,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "timeSeries",
                "stacked": true,
                "region": "us-east-1",
                "title": "Dropwizard G1 Young/Old Count",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "G1-Old-Generation.count", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "G1-Young-Generation.count", ".", "." ]
                ]
            }
        },
        {
            "height": 5,
            "width": 20,
            "y": 19,
            "x": 4,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "heap.used", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "heap.max", ".", "." ],
                    [ ".", "heap.init", ".", "." ],
                    [ ".", "heap.committed", ".", "." ]
                ],
                "sparkline": true,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "title": "Dropwizard heap",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "setPeriodToTimeRange": false,
                "trend": true
            }
        },
        {
            "height": 3,
            "width": 8,
            "y": 7,
            "x": 16,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": true,
                "region": "us-east-1",
                "title": "Dropwizard HHA Claim Transformer",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "setPeriodToTimeRange": false,
                "trend": true,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "HHAClaimTransformer.transform", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "HHAClaimTransformerV2.transform", ".", "." ]
                ]
            }
        },
        {
            "height": 3,
            "width": 24,
            "y": 16,
            "x": 0,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "HikariPool-1.pool.ActiveConnections", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "HikariPool-1.pool.ConnectionCreation", ".", "." ],
                    [ ".", "HikariPool-1.pool.IdleConnections", ".", "." ],
                    [ ".", "HikariPool-1.pool.MaxConnections", ".", "." ],
                    [ ".", "HikariPool-1.pool.MinConnections", ".", "." ],
                    [ ".", "HikariPool-1.pool.PendingConnections", ".", "." ],
                    [ ".", "HikariPool-1.pool.TotalConnections", ".", "." ]
                ],
                "region": "us-east-1",
                "title": "Hikari Pool Connections"
            }
        },
        {
            "height": 6,
            "width": 6,
            "y": 29,
            "x": 0,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "HikariPool-1.pool.Usage", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "HikariPool-1.pool.Wait", ".", "." ]
                ],
                "setPeriodToTimeRange": true,
                "title": "HikariPool-1.pool.Usage, HikariPool-1.pool.Wait"
            }
        },
        {
            "height": 3,
            "width": 8,
            "y": 4,
            "x": 16,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "HospiceClaimTransformer.transform", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "HospiceClaimTransformerV2.transform", ".", "." ]
                ],
                "trend": true,
                "title": "Dropwizard Hospice Claim Transformer",
                "period": 300
            }
        },
        {
            "height": 3,
            "width": 18,
            "y": 1,
            "x": 6,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "http.client.duration", "OTelLib", "io.opentelemetry.okhttp-3.0" ],
                    [ ".", "http.server.active_requests", ".", "io.opentelemetry.jetty-8.0" ],
                    [ ".", "http.server.duration", ".", "." ]
                ],
                "title": "http.client.duration, http.server.active_requests, http.server.duration"
            }
        },
        {
            "height": 3,
            "width": 9,
            "y": 10,
            "x": 15,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "InpatientClaimTransformer.transform", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "InpatientClaimTransformerV2.transform", ".", "." ]
                ],
                "title": "Dropwizard Inpatient Claim Transformer"
            }
        },
        {
            "height": 6,
            "width": 18,
            "y": 29,
            "x": 6,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "MetricRegistry.query.eobs_by_bene_id.carrier", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0", { "label": "eobs_by_bene_id.carrier" } ],
                    [ ".", "MetricRegistry.query.eobs_by_bene_id.dme", ".", ".", { "label": "eobs_by_bene_id.dme" } ],
                    [ ".", "MetricRegistry.query.eobs_by_bene_id.hha", ".", ".", { "label": "eobs_by_bene_id.hha" } ],
                    [ ".", "MetricRegistry.query.eobs_by_bene_id.hospice", ".", ".", { "label": "eobs_by_bene_id.hospice" } ],
                    [ ".", "MetricRegistry.query.eobs_by_bene_id.inpatient", ".", ".", { "label": "eobs_by_bene_id.inpatient" } ],
                    [ ".", "MetricRegistry.query.eobs_by_bene_id.outpatient", ".", ".", { "label": "eobs_by_bene_id.outpatient" } ],
                    [ ".", "MetricRegistry.query.eobs_by_bene_id.pde", ".", ".", { "label": "eobs_by_bene_id.pde" } ],
                    [ ".", "MetricRegistry.query.eobs_by_bene_id.snf", ".", ".", { "label": "eobs_by_bene_id.snf" } ]
                ],
                "sparkline": false,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": true,
                "trend": false,
                "stat": "Average",
                "period": 300,
                "title": "MetricRegistry.query.eobs_by",
                "liveData": true
            }
        },
        {
            "height": 5,
            "width": 20,
            "y": 24,
            "x": 4,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "non-heap.committed", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "non-heap.init", ".", "." ],
                    [ ".", "non-heap.max", ".", "." ],
                    [ ".", "non-heap.used", ".", "." ]
                ],
                "title": "Dropwizard non-heap"
            }
        },
        {
            "height": 7,
            "width": 24,
            "y": 35,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "OutpatientClaimTransformer.transform", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0", { "id": "m1" } ],
                    [ ".", "OutpatientClaimTransformerV2.transform", ".", ".", { "id": "m2" } ],
                    [ ".", "CarrierClaimTransformerV2.transform", ".", ".", { "id": "m3" } ],
                    [ ".", "PartDEventTransformer.transform", ".", ".", { "id": "m4" } ],
                    [ ".", "HospiceClaimTransformerV2.transform", ".", ".", { "id": "m5" } ],
                    [ ".", "HospiceClaimTransformer.transform", ".", ".", { "id": "m6" } ],
                    [ ".", "HHAClaimTransformer.transform", ".", ".", { "id": "m7" } ],
                    [ ".", "DMEClaimTransformerV2.transform", ".", ".", { "id": "m8" } ],
                    [ ".", "SNFClaimTransformer.transform", ".", ".", { "id": "m9" } ],
                    [ ".", "CoverageTransformerV2.transform.part_d", ".", ".", { "id": "m10" } ],
                    [ ".", "DMEClaimTransformer.transform", ".", ".", { "id": "m11" } ],
                    [ ".", "InpatientClaimTransformerV2.transform", ".", ".", { "id": "m12" } ],
                    [ ".", "CoverageTransformer.transform.part_d", ".", ".", { "id": "m13" } ],
                    [ ".", "CoverageTransformer.transform.part_c", ".", ".", { "id": "m14" } ],
                    [ ".", "SNFClaimTransformerV2.transform", ".", ".", { "id": "m15" } ],
                    [ ".", "PartDEventTransformerV2.transform", ".", ".", { "id": "m16" } ],
                    [ ".", "CoverageTransformerV2.transform.part_b", ".", ".", { "id": "m17" } ],
                    [ ".", "InpatientClaimTransformer.transform", ".", ".", { "id": "m18" } ],
                    [ ".", "CoverageTransformerV2.transform.part_a", ".", ".", { "id": "m19" } ],
                    [ ".", "HHAClaimTransformerV2.transform", ".", ".", { "id": "m20" } ],
                    [ ".", "CoverageTransformerV2.transform.part_c", ".", ".", { "id": "m21" } ],
                    [ ".", "CoverageTransformer.transform.part_b", ".", ".", { "id": "m22" } ],
                    [ ".", "CoverageTransformer.transform.part_a", ".", ".", { "id": "m23" } ],
                    [ ".", "CarrierClaimTransformer.transform", ".", ".", { "id": "m24" } ]
                ],
                "sparkline": false,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": true,
                "trend": false,
                "stat": "Average",
                "period": 300,
                "title": "Dropwizard Transformers",
                "liveData": true
            }
        },
        {
            "height": 25,
            "width": 9,
            "y": 42,
            "x": 15,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "pools.Compressed-Class-Space.init", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "pools.Compressed-Class-Space.max", ".", "." ],
                    [ ".", "pools.Compressed-Class-Space.usage", ".", "." ],
                    [ ".", "pools.Compressed-Class-Space.used", ".", "." ],
                    [ ".", "pools.G1-Eden-Space.committed", ".", "." ],
                    [ ".", "pools.G1-Eden-Space.init", ".", "." ],
                    [ ".", "pools.G1-Eden-Space.max", ".", "." ],
                    [ ".", "pools.G1-Eden-Space.used", ".", "." ],
                    [ ".", "pools.G1-Eden-Space.used-after-gc", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.committed", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.init", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.max", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.used", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.used-after-gc", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.committed", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.init", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.max", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.used", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.used-after-gc", ".", "." ],
                    [ ".", "pools.Metaspace.committed", ".", "." ],
                    [ ".", "pools.Metaspace.init", ".", "." ],
                    [ ".", "pools.Metaspace.max", ".", "." ],
                    [ ".", "pools.Metaspace.used", ".", "." ]
                ],
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "title": "Dropwizard pools"
            }
        },
        {
            "height": 6,
            "width": 24,
            "y": 67,
            "x": 0,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "start": "-PT1H",
                "end": "P0D",
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "process.runtime.jvm.classes.current_loaded", "OTelLib", "io.opentelemetry.runtime-metrics" ],
                    [ ".", "process.runtime.jvm.classes.loaded", ".", "." ],
                    [ ".", "process.runtime.jvm.classes.unloaded", ".", "." ],
                    [ ".", "process.runtime.jvm.cpu.utilization", ".", "." ],
                    [ ".", "process.runtime.jvm.memory.committed", ".", "." ],
                    [ ".", "process.runtime.jvm.memory.init", ".", "." ],
                    [ ".", "process.runtime.jvm.memory.limit", ".", "." ],
                    [ ".", "process.runtime.jvm.memory.usage", ".", "." ],
                    [ ".", "process.runtime.jvm.system.cpu.load_1m", ".", "." ],
                    [ ".", "process.runtime.jvm.system.cpu.utilization", ".", "." ],
                    [ ".", "process.runtime.jvm.threads.count", ".", "." ]
                ],
                "title": "process.runtime.jvm"
            }
        },
        {
            "height": 3,
            "width": 24,
            "y": 73,
            "x": 0,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "start": "-PT1H",
                "end": "P0D",
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "total.used", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "total.max", ".", "." ],
                    [ ".", "total.init", ".", "." ],
                    [ ".", "total.committed", ".", "." ]
                ],
                "title": "total.committed, total.init, total.max, total.used"
            }
        },
        {
            "height": 5,
            "width": 4,
            "y": 19,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "heap.usage", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ]
                ],
                "sparkline": true,
                "view": "gauge",
                "stacked": false,
                "region": "us-east-1",
                "title": "Dropwizard heap",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "setPeriodToTimeRange": false,
                "trend": true,
                "yAxis": {
                    "left": {
                        "min": 0,
                        "max": 1
                    }
                }
            }
        },
        {
            "height": 5,
            "width": 4,
            "y": 24,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "non-heap.usage", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ]
                ],
                "sparkline": true,
                "view": "gauge",
                "stacked": false,
                "region": "us-east-1",
                "title": "Dropwizard non-heap usage",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "setPeriodToTimeRange": false,
                "trend": true,
                "yAxis": {
                    "left": {
                        "min": 0,
                        "max": 1
                    }
                }
            }
        },
        {
            "height": 5,
            "width": 11,
            "y": 42,
            "x": 4,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "pools.Compressed-Class-Space.init", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "pools.Compressed-Class-Space.max", ".", "." ],
                    [ ".", "pools.Compressed-Class-Space.used", ".", "." ],
                    [ ".", "pools.Compressed-Class-Space.committed", ".", "." ]
                ],
                "sparkline": true,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "title": "Dropwizard Compressed Class Space"
            }
        },
        {
            "height": 25,
            "width": 4,
            "y": 42,
            "x": 0,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "pools.Compressed-Class-Space.usage", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "pools.G1-Eden-Space.usage", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.usage", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.usage", ".", "." ],
                    [ ".", "pools.Metaspace.usage", ".", "." ]
                ],
                "sparkline": true,
                "view": "gauge",
                "stacked": false,
                "region": "us-east-1",
                "title": "Pools Usage",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "setPeriodToTimeRange": false,
                "trend": true,
                "yAxis": {
                    "left": {
                        "min": 0,
                        "max": 1
                    }
                }
            }
        },
        {
            "height": 5,
            "width": 11,
            "y": 47,
            "x": 4,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "pools.G1-Eden-Space.init", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "pools.G1-Eden-Space.used", ".", "." ],
                    [ ".", "pools.G1-Eden-Space.used-after-gc", ".", "." ],
                    [ ".", "pools.G1-Eden-Space.max", ".", "." ],
                    [ ".", "pools.G1-Eden-Space.committed", ".", "." ]
                ],
                "sparkline": true,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "title": "Dropwizard Pools G1-Eden"
            }
        },
        {
            "height": 5,
            "width": 11,
            "y": 57,
            "x": 4,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "pools.G1-Survivor-Space.init", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "pools.G1-Survivor-Space.max", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.committed", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.used-after-gc", ".", "." ],
                    [ ".", "pools.G1-Survivor-Space.used", ".", "." ]
                ],
                "sparkline": true,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "title": "Dropwizard Pools Survivor"
            }
        },
        {
            "height": 5,
            "width": 11,
            "y": 52,
            "x": 4,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "pools.G1-Old-Gen.used-after-gc", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "pools.G1-Old-Gen.max", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.used", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.committed", ".", "." ],
                    [ ".", "pools.G1-Old-Gen.init", ".", "." ]
                ],
                "sparkline": true,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "title": "Dropwizard Pools G1-Old"
            }
        },
        {
            "height": 5,
            "width": 11,
            "y": 62,
            "x": 4,
            "type": "metric",
            "properties": {
                "metrics": [
                    [ "${dashboard_namespace}/AOTMetricNS", "pools.Metaspace.max", "OTelLib", "io.opentelemetry.dropwizard-metrics-4.0" ],
                    [ ".", "pools.Metaspace.init", ".", "." ],
                    [ ".", "pools.Metaspace.committed", ".", "." ],
                    [ ".", "pools.Metaspace.used", ".", "." ]
                ],
                "sparkline": true,
                "view": "timeSeries",
                "stacked": false,
                "region": "us-east-1",
                "setPeriodToTimeRange": false,
                "trend": true,
                "stat": "Average",
                "period": 300,
                "title": "Dropwizard Pools Metaspace"
            }
        }
    ]
}