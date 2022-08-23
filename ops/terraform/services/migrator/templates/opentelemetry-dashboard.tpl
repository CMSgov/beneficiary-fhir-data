{
    "widgets": [
            {
            "height": 1,
            "width": 24,
            "y": 136,
            "x": 0,
            "type": "text",
            "properties": {
                "markdown": "#BFD-Migrator OpenTelemtry Metrics\n---\n"
            }
        },
        {
            "height": 6,
            "width": 8,
            "y": 132,
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
                "title": "BFD-migrator Dropwizard Heap"
            }
        },
        {
            "height": 3,
            "width": 24,
            "y": 132,
            "x": 0,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "singleValue",
                "stacked": false,
                "region": "us-east-1",
                "title": "BFD-migrator hikaricp-3.0  DB Client Connections",
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
            "height": 6,
            "width": 8,
            "y": 132,
            "x": 8,
            "type": "metric",
            "properties": {
                "sparkline": true,
                "view": "timeSeries",
                "stacked": true,
                "region": "us-east-1",
                "title": "BFD-migrator Dropwizard G1 Young/Old Count",
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
            "y": 132,
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
                "title": "BFD-migrator Dropwizard heap",
                "stat": "Average",
                "period": 300,
                "singleValueFullPrecision": false,
                "setPeriodToTimeRange": false,
                "trend": true
            }
        },
        {
            "height": 3,
            "width": 24,
            "y": 132,
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
                "title": "BFD-migrator Hikari Pool Connections"
            }
        },
        {
            "height": 6,
            "width": 8,
            "y": 132,
            "x": 16,
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
                "title": " BFD-migrator HikariPool-1.pool.Usage, HikariPool-1.pool.Wait"
            }
        },
        {
            "height": 3,
            "width": 18,
            "y": 132,
            "x": 0,
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
                "title": "BFD-migrator http.client.duration, http.server.active_requests, http.server.duration"
            }
        },
        {
            "height": 5,
            "width": 20,
            "y": 132,
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
                "title": "BFD-migrator Dropwizard non-heap"
            }
        },
        {
            "height": 25,
            "width": 9,
            "y": 132,
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
                "title": "BFD-migratorDropwizard pools"
            }
        },
        {
            "height": 6,
            "width": 24,
            "y": 132,
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
            "y": 132,
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
                "title": "BFD-migrator total.committed, total.init, total.max, total.used"
            }
        },
        {
            "height": 5,
            "width": 4,
            "y": 132,
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
                "title": "BFD-migrator Dropwizard heap",
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
            "y": 132,
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
                "title": "BFD-migrator Dropwizard non-heap usage",
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
            "y": 132,
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
                "title": "BFD-migrator Dropwizard Compressed Class Space"
            }
        },
        {
            "height": 25,
            "width": 4,
            "y": 132,
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
                "title": "BFD-migrator Pools Usage",
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
            "y": 132,
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
                "title": "BFD-migrator Dropwizard Pools G1-Eden"
            }
        },
        {
            "height": 5,
            "width": 11,
            "y": 132,
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
                "title": "BFD-migrator Dropwizard Pools Survivor"
            }
        },
        {
            "height": 5,
            "width": 11,
            "y": 132,
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
                "title": "BFD-migrator Dropwizard Pools G1-Old"
            }
        },
        {
            "height": 5,
            "width": 11,
            "y": 132,
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
                "title": "BFD-migrator Dropwizard Pools Metaspace"
            }
        }
    ]
}