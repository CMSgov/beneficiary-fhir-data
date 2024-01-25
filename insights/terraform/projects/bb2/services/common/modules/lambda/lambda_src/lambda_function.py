# Copyright 2014, Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Amazon Software License (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#  http://aws.amazon.com/asl/
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.

"""
For processing data sent to Firehose by Cloudwatch Logs subscription filters.
Cloudwatch Logs sends to Firehose records that look like this:
{
  "messageType": "DATA_MESSAGE",
  "owner": "123456789012",
  "logGroup": "log_group_name",
  "logStream": "log_stream_name",
  "subscriptionFilters": [
    "subscription_filter_name"
  ],
  "logEvents": [
    {
      "id": "01234567890123456789012345678901234567890123456789012345",
      "timestamp": 1510109208016,
      "message": "log message 1"
    },
    {
      "id": "01234567890123456789012345678901234567890123456789012345",
      "timestamp": 1510109208017,
      "message": "log message 2"
    }
    ...
  ]
}
The data is additionally compressed with GZIP.
NOTE: It is suggested to test the cloudwatch logs processor lambda function in a pre-production environment to ensure
the 6000000 limit meets your requirements. If your data contains a sizable number of records that are classified as
Dropped/ProcessingFailed, then it is suggested to lower the 6000000 limit within the function to a smaller value
(eg: 5000000) in order to confine to the 6MB (6291456 bytes) payload limit imposed by lambda. You can find Lambda
quotas at https://docs.aws.amazon.com/lambda/latest/dg/gettingstarted-limits.html
The code below will:
1) Gunzip the data
2) Parse the json
3) Set the result to ProcessingFailed for any record whose messageType is not DATA_MESSAGE, thus redirecting them to the
   processing error output. Such records do not contain any log events. You can modify the code to set the result to
   Dropped instead to get rid of these records completely.
4) For records whose messageType is DATA_MESSAGE, extract the individual log events from the logEvents field, and pass
   each one to the transformLogEvent method. You can modify the transformLogEvent method to perform custom
   transformations on the log events.
5) Concatenate the result from (4) together and set the result as the data of the record returned to Firehose. Note that
   this step will not add any delimiters. Delimiters should be appended by the logic within the transformLogEvent
   method.
6) Any individual record exceeding 6,000,000 bytes in size after decompression and encoding is marked as
   ProcessingFailed within the function. The original compressed record will be backed up to the S3 bucket
   configured on the Firehose.
7) Any additional records which exceed 6MB will be re-ingested back into Firehose.
8) The retry count for intermittent failures during re-ingestion is set 20 attempts. If you wish to retry fewer number
   of times for intermittent failures you can lower this value.
"""

import base64
import json
import gzip
from io import BytesIO
import boto3

from datetime import datetime


def format_unix_timestamp(ts):
    """
    BB2-918 Returns an ISO 6801 format string in UTC that works well with AWS Glue/Athena
    ts = Unix timestamp integer
    """
    dt = datetime.utcfromtimestamp(ts / 1000)

    return (
        dt.replace(microsecond=0).isoformat().replace("+00:00", "")
        if dt is not None
        else None
    )


def transformLogEvent(log_event):
    """Transform each log event.
    The default implementation below just extracts the message and appends a newline to it.
    Args:
    log_event (dict): The original log event. Structure is {"id": str, "timestamp": long, "message": str}
    Returns:
    str: The transformed log event.
    BB2-918 Modified the transform format for BB2
    """
    ts = log_event["timestamp"]
    cw_log_event_id = log_event["id"]

    try:
        # This is the BB2 app log event
        event_message = log_event.get("message")
        event_dict = json.loads(event_message)
    except json.decoder.JSONDecodeError:
        # NOTE:  Some BB2 logging has malformed JSON
        event_dict = {
            "env": "UNKNOWN-JSON-DECODE-ERROR",
            "name": "UNKNOWN-JSON-DECODE-ERROR",
        }

    out_dict = {
        "time_of_event": format_unix_timestamp(ts),
        "component": "bb2.web",
        "vpc": event_dict.get("env"),
        "log_name": event_dict.get("name"),
        "cw_log_event_id": cw_log_event_id,
    }

    # The BB2 event has a nested message JSON as well
    # message_dict = event_dict["message"]
    message_dict = event_dict.get("message", None)

    if message_dict is None:
        # This condition is generally caused by non-Django format logging,
        #   like ngnix & uwsgi python exceptions.
        message_dict = {
            "type": "bfd-insights-log-lambda-error",
            "mesg": "Warning message_dict is None. Could not parse original JSON message.",
            "log_event": log_event,
        }

    # Update with message fields
    try:
        out_dict.update(message_dict)
    except ValueError:
        # BB2-1809: Log and skip record with value error
        message_dict = {
            "type": "bfd-insights-log-lambda-error",
            "mesg": "Warning message_dict had a ValueError. Could not parse original JSON message.",
        }
        out_dict.update(message_dict)

    return json.dumps(out_dict) + "\n"


def processRecords(records):
    for r in records:
        data = base64.b64decode(r["data"])
        striodata = BytesIO(data)
        with gzip.GzipFile(fileobj=striodata, mode="r") as f:
            data = json.loads(f.read())

        recId = r["recordId"]
        """
        CONTROL_MESSAGE are sent by CWL to check if the subscription is reachable.
        They do not contain actual data.
        """
        if data["messageType"] == "CONTROL_MESSAGE":
            yield {"result": "Dropped", "recordId": recId}
        elif data["messageType"] == "DATA_MESSAGE":
            joinedData = "".join([transformLogEvent(e) for e in data["logEvents"]])
            dataBytes = joinedData.encode("utf-8")
            encodedData = base64.b64encode(dataBytes)
            if len(encodedData) <= 6000000:
                yield {"data": encodedData, "result": "Ok", "recordId": recId}
            else:
                yield {"result": "ProcessingFailed", "recordId": recId}
        else:
            yield {"result": "ProcessingFailed", "recordId": recId}


def putRecordsToFirehoseStream(streamName, records, client, attemptsMade, maxAttempts):
    failedRecords = []
    codes = []
    errMsg = ""
    # if put_record_batch throws for whatever reason, response['xx'] will error out, adding a check for a valid
    # response will prevent this
    response = None
    try:
        response = client.put_record_batch(
            DeliveryStreamName=streamName, Records=records
        )
    except Exception as e:
        failedRecords = records
        errMsg = str(e)

    # if there are no failedRecords (put_record_batch succeeded), iterate over the response to gather results
    if not failedRecords and response and response["FailedPutCount"] > 0:
        for idx, res in enumerate(response["RequestResponses"]):
            # (if the result does not have a key 'ErrorCode' OR if it does and is empty) => we do not need to re-ingest
            if "ErrorCode" not in res or not res["ErrorCode"]:
                continue

            codes.append(res["ErrorCode"])
            failedRecords.append(records[idx])

        errMsg = "Individual error codes: " + ",".join(codes)

    if len(failedRecords) > 0:
        if attemptsMade + 1 < maxAttempts:
            print(
                "Some records failed while calling PutRecordBatch to Firehose stream, retrying. %s"
                % (errMsg)
            )
            putRecordsToFirehoseStream(
                streamName, failedRecords, client, attemptsMade + 1, maxAttempts
            )
        else:
            raise RuntimeError(
                "Could not put records after %s attempts. %s"
                % (str(maxAttempts), errMsg)
            )


def putRecordsToKinesisStream(streamName, records, client, attemptsMade, maxAttempts):
    failedRecords = []
    codes = []
    errMsg = ""
    # if put_records throws for whatever reason, response['xx'] will error out, adding a check for a valid
    # response will prevent this
    response = None
    try:
        response = client.put_records(StreamName=streamName, Records=records)
    except Exception as e:
        failedRecords = records
        errMsg = str(e)

    # if there are no failedRecords (put_record_batch succeeded), iterate over the response to gather results
    if not failedRecords and response and response["FailedRecordCount"] > 0:
        for idx, res in enumerate(response["Records"]):
            # (if the result does not have a key 'ErrorCode' OR if it does and is empty) => we do not need to re-ingest
            if "ErrorCode" not in res or not res["ErrorCode"]:
                continue

            codes.append(res["ErrorCode"])
            failedRecords.append(records[idx])

        errMsg = "Individual error codes: " + ",".join(codes)

    if len(failedRecords) > 0:
        if attemptsMade + 1 < maxAttempts:
            print(
                "Some records failed while calling PutRecords to Kinesis stream, retrying. %s"
                % (errMsg)
            )
            putRecordsToKinesisStream(
                streamName, failedRecords, client, attemptsMade + 1, maxAttempts
            )
        else:
            raise RuntimeError(
                "Could not put records after %s attempts. %s"
                % (str(maxAttempts), errMsg)
            )


def createReingestionRecord(isSas, originalRecord):
    if isSas:
        return {
            "data": base64.b64decode(originalRecord["data"]),
            "partitionKey": originalRecord["kinesisRecordMetadata"]["partitionKey"],
        }
    else:
        return {"data": base64.b64decode(originalRecord["data"])}


def getReingestionRecord(isSas, reIngestionRecord):
    if isSas:
        return {
            "Data": reIngestionRecord["data"],
            "PartitionKey": reIngestionRecord["partitionKey"],
        }
    else:
        return {"Data": reIngestionRecord["data"]}


def lambda_handler(event, context):
    isSas = "sourceKinesisStreamArn" in event
    streamARN = event["sourceKinesisStreamArn"] if isSas else event["deliveryStreamArn"]
    region = streamARN.split(":")[3]
    streamName = streamARN.split("/")[1]
    records = list(processRecords(event["records"]))
    projectedSize = 0
    dataByRecordId = {
        rec["recordId"]: createReingestionRecord(isSas, rec) for rec in event["records"]
    }
    putRecordBatches = []
    recordsToReingest = []
    totalRecordsToBeReingested = 0

    for idx, rec in enumerate(records):
        if rec["result"] != "Ok":
            continue
        projectedSize += len(rec["data"]) + len(rec["recordId"])
        # 6000000 instead of 6291456 to leave ample headroom for the stuff we didn't account for
        if projectedSize > 6000000:
            totalRecordsToBeReingested += 1
            recordsToReingest.append(
                getReingestionRecord(isSas, dataByRecordId[rec["recordId"]])
            )
            records[idx]["result"] = "Dropped"
            del records[idx]["data"]

        # split out the record batches into multiple groups, 500 records at max per group
        if len(recordsToReingest) == 500:
            putRecordBatches.append(recordsToReingest)
            recordsToReingest = []

    if len(recordsToReingest) > 0:
        # add the last batch
        putRecordBatches.append(recordsToReingest)

    # iterate and call putRecordBatch for each group
    recordsReingestedSoFar = 0
    if len(putRecordBatches) > 0:
        client = (
            boto3.client("kinesis", region_name=region)
            if isSas
            else boto3.client("firehose", region_name=region)
        )
        for recordBatch in putRecordBatches:
            if isSas:
                putRecordsToKinesisStream(
                    streamName, recordBatch, client, attemptsMade=0, maxAttempts=20
                )
            else:
                putRecordsToFirehoseStream(
                    streamName, recordBatch, client, attemptsMade=0, maxAttempts=20
                )
            recordsReingestedSoFar += len(recordBatch)
            print(
                "Reingested %d/%d records out of %d"
                % (
                    recordsReingestedSoFar,
                    totalRecordsToBeReingested,
                    len(event["records"]),
                )
            )
    else:
        print("No records to be reingested")

    return {"records": records}
