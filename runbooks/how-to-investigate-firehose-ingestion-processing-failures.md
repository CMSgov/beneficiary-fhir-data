# How to Investigate Firehose Ingestion Processing Failures

Follow this runbook to investigate processing failures from Amazon Kinesis Data Firehose Delivery
Stream ingestion. As of writing, this refers specifically to the
`bfd-insights-bfd-ENV-cw-to-flattened-json` Lambda(s), but may include others that submit data to a
Firehose Delivery Stream in the future as well.

- [How to Investigate Firehose Ingestion Processing Failures](#how-to-investigate-firehose-ingestion-processing-failures)
  - [Glossary](#glossary)
  - [FAQ](#faq)
    - [How would "processing failures" occur?](#how-would-processing-failures-occur)
    - [Has there ever been processing failures?](#has-there-ever-been-processing-failures)
  - [Prerequisites](#prerequisites)
  - [Instructions](#instructions)

## Glossary

|                    Term                     |                                             Definition                                             |
| :-----------------------------------------: | :------------------------------------------------------------------------------------------------: |
|  [Amazon Kinesis Data Firehose][firehose]   | An ETL (extract, transform, load) service for capturing data and delivering data to various stores |
| [Firehose Delivery Stream][delivery-stream] |     The "underlying entity" of Kinesis. This is the component of Firehose that data is sent to     |

## FAQ

<!-- This section should contain frequently-asked-questions as sub-headers with answers as
sub-paragraphs. Since this is a living document, this section should be added to as needed. -->

### How would "processing failures" occur?

In this process, the Lambda that _transforms_ the incoming data before submitting it to the Delivery
Stream is responsible for determining whether the incoming data is able to be processed or not. As
of writing there is only a single Lambda used by BFD Insights that transforms, or _processes_, data
prior to Delivery Stream submission, and this Lambda's source code is available (relative to
repository root) at
`insights/terraform/projects/bfd/api-requests/lambda_src/bfd-cw-to-flattened-json.py`. This Lambda
is based upon the AWS-provided `kinesis-firehose-cloudwatch-logs-processor-python` Lambda Blueprint
and it is likely that future BFD Insights Lambdas will be based upon the very same Blueprint.

So, for Lambdas based upon the `kinesis-firehose-cloudwatch-logs-processor-python` Lambda Blueprint,
the only time when a processing failure could occur is when a **single** CloudWatch log record
exceeds 6 MB in size. Note that _collections_ of log records greater than 6 MB will be automatically
split and chunked before submission to the Delivery Stream.

For custom Lambdas, or for those we have modified heavily from the source Blueprint, their source
code may need to be investigated to understand when a processing failure could occur.

### Has there ever been processing failures?

Yes, as of writing we have had processing failures occur twice since the creation of the
`bfd-insights-bfd-ENV-cw-to-flattened-json` Lambda and corresponding Delivery Stream. The first such
batch of processing failures occurred on (MM-DD-YYYY) 09-29-2022, and the second batch occurred on
10-13-2022. Both were the result of using an outdated
`kinesis-firehose-cloudwatch-logs-processor-python` Lambda Blueprint as the base of the
`bfd-insights-bfd-ENV-cw-to-flattened-json` Lambda(s) that did not have any logic for automatically
splitting and chunking log record collections greater than 6 MBs. Instead, collections greater than
6 MBs in size would automatically get marked as `processing-failed` and would not be ingested by the
Delivery Stream.

Subsequently, the `bfd-insights-bfd-ENV-cw-to-flattened-json` Lambda(s) have been updated to use the
latest `kinesis-firehose-cloudwatch-logs-processor-python` AWS Blueprint which _does_ support
automatic chunking of large data, and so it is not expected that we will encounter further
processing failures in the same vein.

## Prerequisites

- Access to the BFD/CMS AWS account
- Permissions to access and download files from the Delivery Stream's S3 Bucket destination
- Familiarity with using command-line interface (CLI) tools
- Installation of the [`base64`][base64-cli] CLI tool, or equivalent
  - By default, MacOS provides a _similar_ utility with the same name and usage
- Installation of the [`gzip`][gzip-cli] CLI tool, or equivalent

## Instructions

1. Finding the S3 bucket and error output path:
   1. In any browser, navigate to <https://aws.amazon.com> and sign-in. The AWS starting page should
      load, and a searchbar should be visible at the top of the page
   2. In the searchbar, search for "kinesis" and select the Kinesis service when it appears. A new
      page should with three cards listing "Data Streams", "Data Firehose", and "Data Analytics"
   3. Under "Data Firehose", click the number under "Total delivery streams". This will take you to
      a new page listing all of the Delivery Streams under our AWS account
   4. In the list of Delivery Streams, click on the matching Delivery Stream that needs to be
      investigated. A new page should load showing information about the Delivery Stream
      1. For example, the Delivery Stream that corresponds with the
         `bfd-insights-bfd-prod-cw-to-flattened-json` Lambda is the
         `bfd-insights-bfd-prod-firehose-ingester` Delivery Stream
   5. Click on "Configuration"
   6. Under "Destination settings", note down the S3 bucket _and_ the S3 bucket error output prefix.
      This will be the destination where processing failures will be written to
      1. Note that, for this case, `!{firehose:error-output-type}` will become `processing-failed`
         in the resulting error prefix
2. Now that you have the S3 bucket and path where the processing failures are stored, you can
   download the failed files from said bucket and path. This runbook will not go into detail how to
   do this, so consult AWS's documentation if your are unsure of how to download files from an S3
   bucket. Subsequent steps assume you have downloaded these files to your local machine
3. Open the downloaded file(s) in any text editor

   1. Note that these files are large, so editors like `vim` or (Windows-only) Notepad++ would be
      best suited to viewing them.
   2. Each line of each file is a JSON object with 7 properties, and each object should have the
      following structure (note that the following JSON has been _formatted_):

      ```json
      {
        "attemptsMade": 4,
        "arrivalTimestamp": 1665653487107,
        "errorCode": "Lambda.FunctionError",
        "errorMessage": "The Lambda function was successfully invoked but it returned an error result.",
        "attemptEndingTimestamp": 1665678887550,
        "rawData": "H4sIA...",
        "lambdaArn": "..."
      }
      ```

4. Copy _all_ of the `rawData` string (excluding quotes) and paste it into a new file. Save this
   file as something meaningful
   1. This runbook assumes this file is named `data.base64`
   2. `rawData` is the `gzip`'d `base64`-encoded raw incoming data that the Lambda attempted to
      process, but failed to do so. Subsequent steps will decode and decompress this "raw data" into
      a JSON file
5. Open a terminal to the directory where the file from Step 4 is located
6. In the terminal, run the following command (replacing `data.base64` and `data.json` with the name
   you gave the file from Step 4):

   ```bash
   cat data.base64 | base64 -d | gzip -d > data.json
   ```

   1. `data.json` (replace "data" with the equivalent name from Step 4) should now contain a JSON
      object following the following structure (assuming the Lambda is processing CloudWatch Logs):

   ```json
    {
    "messageType": "DATA_MESSAGE",
    "owner": "...",
    "logGroup": "...",
    "logStream": "...",
    "subscriptionFilters": ["..."],
    "logEvents": [
      {
        "id": "...",
        "timestamp": 1664471296685,
        "message": "..."
      },
      ...
   ```

7. Repeat Steps 3 - 6 for each file and each JSON object in the file that you want to investigate

With the `rawData` now decompressed and decoded, you can investigate the data that the Lambda
attempted to process and determine if it was malformed, too large, etc. since the `rawData` JSON
_is_ the data that caused the failure. For example, `rawData` JSON for CloudWatch logs will contain
records, in the `logEvents` array, for each CloudWatch log event _including_ the raw message.

[delivery-stream]: https://docs.aws.amazon.com/firehose/latest/dev/what-is-this-service.html#key-concepts
[firehose]: https://aws.amazon.com/kinesis/data-firehose/
[base64-cli]: https://linux.die.net/man/1/base64
[gzip-cli]: https://www.gnu.org/software/gzip/manual/gzip.html
