# RDA API Data Ingestion Pipeline

This project produces a JAR file containing the ETL pipeline jobs to ingest partially adjudicated claims data
from the RDA API.

Key classes in this project include:

| Class                 | Description                                                                                                                |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------|
| `RdaFissClaimLoadJob` | Implementation of `PipelineJob` that calls the RDA API to receive a stream of FISS claims and stores them in the database. |
| `RdaMcsClaimLoadJob`  | Implementation of `PipelineJob` that calls the RDA API to receive a stream of MCS claims and stores them in the database.  |
| `AbstractRdaLoadJob` | Common base class for both jobs that implements most of the functionality. |
| `RdaSink` | Interface for objects that transform raw RDA API protocol objects and convert them into database entities and to store those entities in the database. |
| `RdaSource` | Interface for objects that invoke an RDA API RPC and invoke an `RdaSink` to transform and store the returned objects. |
| `GrpcStreamCaller` | Abstract base class for objects that interact directly with RDA API RPC calls. |

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