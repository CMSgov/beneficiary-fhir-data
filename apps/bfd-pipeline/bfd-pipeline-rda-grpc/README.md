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
