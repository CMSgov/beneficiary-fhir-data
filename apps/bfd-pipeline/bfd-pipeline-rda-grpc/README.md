# RDA API Data Ingestion Pipeline

This project produces a JAR file containing the ETL pipeline jobs to ingest partially adjudicated claims data
from the RDA API.

Key classes in this project include:

| Class                 | Description                                                                                                                                            |
|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RdaFissClaimLoadJob` | Implementation of `PipelineJob` that calls the RDA API to receive a stream of FISS claims and stores them in the database.                             |
| `RdaMcsClaimLoadJob`  | Implementation of `PipelineJob` that calls the RDA API to receive a stream of MCS claims and stores them in the database.                              |
| `RdaServerJob`        | Implementation of `PipelineJob` that runs a mock RDA API server available for use by claim load jobs in the same process.                              |
| `AbstractRdaLoadJob`  | Common base class for both jobs that implements most of the functionality.                                                                             |
| `RdaSink`             | Interface for objects that transform raw RDA API protocol objects and convert them into database entities and to store those entities in the database. |
| `RdaSource`           | Interface for objects that invoke an RDA API RPC and invoke an `RdaSink` to transform and store the returned objects.                                  |
| `GrpcStreamCaller`    | Abstract base class for objects that interact directly with RDA API RPC calls.                                                                         |

Configuration of the pipeline jobs is performed using environment variables defined in
the `gov.cms.bfd.pipeline.app.AppConfiguration` class.

## DLQ (Dead Letter Queue)

The RDA messages received from the upstream API can sometimes contain values that were unexpected by
the pipeline.  To preserve the integrity of the data (as we currently understand it at any given point),
the ingestion job will log the message to a table in the database (`message_errors`) along with details
about why the message couldn't be ingested.

Being that the messages contain PII/PHI, standard logging practices would not be acceptable, thus
utilizing the database offers a safe to store and retrieve the messages for later debugging.

Once per job run, the DLQ table will be re-processed, attempting to re-fetch the offending messages from
the RDA API to attempt to ingest them again.
 - If the message is ingested successfully (meaning an upstream
bug was fixed, or we changed our ingestion rules), the message is flagged as `resolved`, and no attempts to
reprocess it will be made again in the future.
 - If the message no longer exists in the RDA API (meaning
there must be a new message that replaced it), the DLQ entry is marked as `obsolete`, and no attempts to
reprocess it will be made again in the future.
 - If the message still can not be ingested, it will remain
to be marked as `unresolved`, and the job will try to ingest it again the next time it starts up.

## RdaServer

The `RdaServer` class provides an implementation of the RDA API server for use in testing or to populate data in the
prod-sbx environment.  Provides static methods to launch the RDA API server either listening on a local port or
using gRPC "in-process" mode.  The static methods automatically release resources once the server is no longer in use.

The mock server can serve claim updates for FISS or MCS (or both) in one of three modes:

* NDJSON files on local disk
* NDJSON files in an S3 bucket
* Randomly generated claim updates

The server can optionally insert random validation errors into the claim updates to facilitate pipeline testing.
NDJSON files must be in protobuf JSON format (exported using `com.google.protobuf.util.JsonFormat`) with one
claim update per line.  Updates should be in increasing sequence number order.

When reading NDJSON files from an S3 bucket all of the files must be in the same logical directory and must have names
matching this regex: `^(fiss|mcs)(-(\d+)-(\d+))?\.ndjson(\.gz)?$`.  The prefix denotes the type of claims in the file
and the numbers (if present) denote the range of sequence numbers in the file.  Multiple files can be provided.
Using smaller files can improve responsiveness when a client requests a non-zero starting sequence number.
Gzipped files are automatically decompressed as they are read. 

S3 data files are downloaded to a local directory before being streamed to clients.  They can be stored in a permanent
directory or in a temporary directory.  See `gov.cms.bfd.pipeline.rda.grpc.server.RdaMessageSourceFactory.Config`
for details.

## RdaServerJob

A `PipelineJob` implementation that runs an in-process `RdaServer` to serve either S3 or random claim updates. 
This is used in the `prod-sbx` environment to populate the database with Synthea data.
