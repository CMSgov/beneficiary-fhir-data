# BB2 Project information

## Names

| Category | Name |
|----------|------|
| Project  | `bb2`                            |
| Bucket   | `bfd-insights-bb2-BFD_ACCT_NUM`  |
| CMK      | `arn:aws:kms:us-east-1:BFD_ACCT_NUM:key/KEYID` | 
| Database | `bb2` |
| tag:project | `bb2` |

## Raw Log Schema

The raw logs from the bb2 server will be NDJSON files that are GZIPed. The raw logs are partitioned and
ordered by delivery time. 

```
[
{name="instance_id",  type="string",    comment="AWS instance id recording the event"},
{name="component",    type="string",    comment="Always bb2.web"},
{name="vpc",          type="string",    comment="dev, prod, impl, etc."},
{name="log_name",     type="string",    comment="BB2 log name"},
{name="message",      type="string",    comment="JSON object"},
]
```

## Event Normalization

Per the developing standard for DASG Audit events, every normalized events needs:

| Area    | Requirement                     | BB2 Value                   |
|------   |---------------------------------|-----------------------------|
| Time    | event period/timestamp in UTC Z | Timestamp in message        |
| Source  | Recording software              | Component, Instance id, VPC |
| Agent   |                                 | Application from message    |
| Entity  |                                 | From message                |
| Span    | Correlation ids                 | From message                | 
