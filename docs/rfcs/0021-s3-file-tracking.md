# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0021-s3-file-tracking`
* Start Date: 2023-12-12
* RFC PR: [beneficiary-fhir-data/rfcs#0021](https://github.com/CMSgov/beneficiary-fhir-data/pull/2092)
* JIRA Ticket(s):
    * [BFD-3052](https://jira.cms.gov/browse/BFD-3052)

A proposal to eliminate the S3 file move operation currently used by the CCW pipeline job to remove processed files from its `Incoming` directory.
Several options are proposed.

## Status
[Status]: #status

* Status: Proposed <!-- (Proposed/Approved/Rejected/Implemented) -->
* Implementation JIRA Ticket(s):
    * n/a

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Status](#status)
* [Table of Contents](#table-of-contents)
* [Terminology](#terminology)
* [Background](#background)
* [Proposed Solution](#proposed-solution)
    * [Option B](#option-b-only-move-the-manifest-file)
    * [Option MF](#option-mf)
    * [Option DF](#option-df)
    * [Option T](#option-t)
    * [Option P](#option-p)
    * [Process Integration](#process-integration)
* [Prior Art](#prior-art)
* [Future Possibilities](#future-possibilities)
* [Addenda](#addenda)

## Terminology
[Terminoligy]: #terminology

This document refers to files and folders in the context of S3 because it is most natural to think of S3 as a file system.
To translate into S3 terminology simply think "object" for "file", "prefix" for "folder", and "key" for "path".

This document also refers to a "move" operation.
S3 does not support a genuine move so in fact a move consists of copying the old file to the new path and then deleting the old file.
A move is expensive and is not atomic (copy could succeed and delete fail, thus leaving both old and new files present in bucket).

## Background
[Background]: #background

The S3 bucket used to receive inbound files from CCW has two top level folders: `Incoming` for new files to be processed and `Done` for files that have been processed.
When the CCW pipeline finishes processing all files referenced by a particular manifest file it moves the files from their folder under `Incoming` into an equivalent folder under `Done`.
Moving the files ensures that the pipeline will not try to process them again in the future.

This move process has some pros and cons:

* Pro: Keeps the Incoming tree as small as possible.
* Con: Moving large files has an extra cost in time, I/O, and money.
* Con: If a move fails the pipeline might reprocess files the next time it runs.
* Con: Interrupting the CCW pipeline has to wait for pending move operations to complete before the pipeline process can exit.

This document outlines possible alternatives to this S3 move process.
All of them share the con that they will leave the Incoming tree to grow indefinitely rather than pruning it as files are processed.
This would add additional start-up time for the CCW pipeline when it reads a list of all Incoming files.
However the number of file names per year is relatively small so this extra overhead should be minimal.

The focus of this document is eliminating the move step from the CCW pipeline.
Nothing precludes a background process outside of the pipeline from periodically moving or deletng processed files.
Such a background process could also compress the files as it moves them.
This document will discuss ideas for how those processes might work in the future possibilities section.

## Proposed Solution
[Proposed Solution]: #proposed-solution

This section lists, in order of increasing complexity, several possible alternatives to the S3 move operation.
The author recommends that BFD implement Option-P (full progress tracking), possibly with the addition of status file upload from option MF.

### Option B: Only Move the Manifest File

As silly as this sounds the minimal option is simply to move the manifest files after processing while leaving the data files in place.
The manifest files are tiny and drive the entire process.
Moving one requires little time or expense and accomplishes the goal of signalling that the file has been processed.

Pros:

* Easy to implement.
* Allows external processes to react to the movement of the manifest to signal completion of processing.
* Minimizes overhead of moving files.

Cons:

* Splits manifests from their data files.
* No tracking in our database.
* Leaves the `Incoming` tree in a mess.  Was the manifest ever there?  Or did we move it?

### Option MF: Add A Manifest Status File In Same Bucket Directory
[Option MF]: #option-mf

Instead of moving files to another directory in the bucket just upload a tiny file next to the processed manifest file.
The uploaded file would use the same name as original but with `_status` added to its base name.
E.g. `0_manifest.xml` status file name is `0_manifest_status.xml`.

Implementation Highlights:

* Replace the move task with a status file upload task.
* Add status file existence check when deciding if a manifest needs to be processed.

Pros:

* Easy to implement.
* Fast final stage in processing a manifest.
* Leaves a record in S3.

Cons:

* No tracking in our database.
* Adds still more files to the Incoming tree that have to be ignored on pipeline startup.

### Option DF: Add A Data Status File In Same Bucket Directory
[Option DF]: #option-df

Same as option MF except that pipeline also uploads a status file for each data file.
Would allow resume after interrupt to skip entire files.

Pros:

* Easy to implement.
* Leaves a record in S3.

Cons:

* No tracking in our database.
* Adds still more files to the Incoming tree that have to be ignored later.

### Option T: Add Tables
[Option T]: #option-t

Adds new tables to track all files from S3: `manifest_files` and `data_files`.
Include basic tracking columns in each table:

* `s3_path` indicates path of the file in the S3 bucket.
* `status` indicates things like `new`, `started`, and `completed`, `rejected`.
* `discovery_date` indicates when the file was first observed in S3.
* `completion_date` indicates when the file was fully processed.

The `data_files` table would also include a reference to its manifest file record as well as its RIF data type.

This could complement the existing `loaded_files` table.

Pros:

* Tracks files in database for easy auditing.
* Final processing step is simply a database update, no S3 interaction needed.
* Progress tracking is decoupled from S3, allowing S3 files to be deleted when desired.
* No database migration is necessary.  Existing `loaded_files` and `loaded_batches` tables remain unchanged.

Cons:

* No indication in S3 of whether a file has been processed yet.

### Option P: Option T Plus Progress and Change Tracking
[Option P]: #option-p

Adds progress tracking and auditing to all tables.

* Adds `record_progress` to `data_files` record.
  * Contains record (line) number of the highest record number known to have all prior records already stored.
  * Logic is similar to RDA API sequence number tracking.
* Adds `data_file_id` column to beneficiary and claim tables.
  * Nullable so no change to existing records.
  * Allows every record to be traced back to the file that last updated it.
* Adds `batch_number` column to beneficiary and claim tables.
  * Nullable so no change to existing records.
  * Allows every record to be traced back to the batch that last updated it.
  * Allows batches to be reconstructed after the fact when debugging.
* Mostly obsoletes the `loaded_files` and `loaded_batches` tables.  They can be retained or removed as desired.

Notes:

* Tracks file progress using line numbers rather than byte offsets.
  * Byte offsets are brittle in the face of possible file edits/corrections.
  * Skipping lines can be done using a simple `filter` operator in parsing `Flux`.
* `record_progress` values can be tracked by reusing the `SequenceNumberTracker` class from the RDA pipeline.
* `record_progress` updates can be done by adding a `flatMap` after the batch processing `flatMap` in `RifLoader`.
  * Decoupling from batch updates eliminates chances of transaction conflicts.
  * Using `flatMap` in existing flux means no new threads or synchronization needed.
  * Progress values can be updated eagerly or limited by requiring minimum elapsed time between updates to minimize database overhead.
* Only the most recent change to a record will be linked.

Pros:

* The `record_progress` column allows fast restarts following a failed or interrupted load.
* The `data_file_id` and `batch_number` columns provide full audit of where a claim or bene update originated and how they were batched together.
* Eliminates the need for two existing tables: `loaded_files` and `loaded_batches`.
* No database migration is necessary.  Existing `loaded_files` and `loaded_batches` tables can remain unchanged or be dropped.

Cons:

* Added storage costs for the new columns.
* Added logic for the `record_progress` updates.
* Adding columns to bene and all claims table requires migration.
* Added database I/O to populate the new columns and track progress.

### Integration With External Processes
[Process Integration]: #process-integration

Some external processes (notably lambda functions) look for and react to the movement of files out of the `Incoming` tree to coordinate their actions with the pipeline.
Eliminating the move will necessarily break that method.

Depending on the option selected, an external process could either look for the appearance of a status file or query the database to look for a status change.
The database query option would be complicated and tightly couple the process with the pipeline's database.
An easier integration option would be to modify the pipeline to send status updates to an SQS queue.
This would follow the pattern used by the database migrator.

The application could push a message to the queue for events such as:

* starting up
* discovering a file
* downloading a file
* processing a file
* finished processing a file
* shutting down

Applications could then react to any of these messages as they see fit without being tightly coupled to the pipeline or its database schema.


## Prior Art
[Prior Art]: #prior-art

BFD currently uses two tables to provide rudimentary tracking of imported data:

* `loaded_files`: Records that processing of a file of a given type was started at a specified time.  There is no link back to an S3 file or any indication of whether or not the load was successful.
* `loaded_batches`: Records that a batch was loaded from a file and provides a string listing the affected beneficiary ids.  No indication is provided of which records in the file were included in the batch.

The RDA API tracks progress by storing a sequence number (unique update number provided to it by the API) in a table.
Because batches are stored in parallel and may be written to the database out of order a class was written to track all sequence numbers in flight and the highest sequence number for which all recoreds with a lower number have been successfulyl written to the database.

## Future Possibilities
[Future Possibilities]: #future-possibilities

The current solution effectively archives files forever.
Files are moved into the `Done` folder for permanent storage and forgotten.
The utility of retaining these files for the long term is an open question.
Even if the files should be retained there are alternative archiving options that might be more cost effective.

How should archived files be stored in S3?
Since RIF data files are text they should be highly compressible.
Compressing them before archiving them would require some up front CPU time but would save on storage and I/O costs.
However, the move to parquet format in the near future would make compression irrelevant.

The CCW manifest file is not particularly useful for understanding an archived file.
Ideally we should add our own meta data to the archive to indicate what version of BFD it was compatible with.
This would simplify any effort to reingest a file in the future after CCW formats have changed.

Whichever proposal we adopt for eliminating the S3 file move step from the pipeline there will still be a need in the future to clean up the `Incoming` directory.
We might create a new pipeline job that checks for completed files in the `Incoming` tree and either deletes or archives them.
The same job might also delete files from the archive after a configurable TTL has expired.
The job should be idempotent so that it can fail or be interrupted and automatically clean up any mess the next time it runs.

## Addenda
[Addenda]: #addenda

n/a
