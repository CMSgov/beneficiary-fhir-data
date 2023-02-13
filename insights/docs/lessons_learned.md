# Lessons Learned
Notes to our future selves about the mistakes and pains that we went through.

## S3 Encryption
CMS has a recommendation/policy or best practice of using AWS-KMS with AWS managed key. Buckets are setup with encryption as the default, but engineers need to configure services to use this. Each bucket has a unique key. This policy means that every S3 `GetObject` and `PutObject` operation needs permissions for the KMS `Encrypt`, `GenerateDataKey*`, and `Decrypt` operations. If an S3 operation comes back as access-denied, you should check both the S3 and KMS permissions for the principal.  

## Cross-Account Access
Cross-account access varies for each AWS resource or service. Most have little support for it. Kinesis Firehose, an essential component in the design, does not support it, for example. S3 and KMS, however, do. So, S3 buckets are the entry-points into the system. 

A subtle note that came up while writing Terraform scripts. The resource policy controls Cross-account access in an S3 bucket. Unlike IAM policies, there can be only one S3 policy document per bucket. So, as we broke the project in multiple sub-projects, it became clear that we needed to have one bucket per project.

## QuickSight and Permissions
In many ways, QuickSight is a separate product from AWS. Its security permissions are controlled by its control panel, not IAM, for example. Underneath the QuickSight permission system is an IAM role: `aws-quicksight-service-role-v0`. The QS control panel edits this role. If you modify this role directly or if you change the resources that QS needs to access, QS may start to have unexplained permission problems. For example, QS couldn't list workgroups. The fix is to rebuild the QS service role. The following link explains this recovery. 

https://aws.amazon.com/premiumsupport/knowledge-center/quicksight-permission-errors/

The current deployment script keeps QS happy, but future scripts may need to take over managing the QS service role explicitly. 

Another problem, QS doesn't have support CMK, which is CMS policy to use. The current workaround is:
- Use KMS grants to give the QS role access to the keys. These grants must be redone every time you rebuild the QS role. There is a script todo this. 
- Use Athena workgroups that override client settings to force query results to use SSE-KMS. 

More useful links:
- https://docs.aws.amazon.com/quicksight/latest/user/troubleshoot-athena-insufficient-permissions.html

- https://aws.amazon.com/premiumsupport/knowledge-center/quicksight-deny-policy-allow-bucket/

### Make a plan for cost estimation and monitoring

 - Perform cost estimates early in development on a small sample of the data to assess viability on the complete dataset. 
 - Continue performing cost estimations while scaling up the data volume to ensure that costs are scaling as expected. 
 - Continue performing cost monitoring even after going live to ensure that the cost profile is not changing unexpectedly. 
 - Monitor spending across all AWS services. Some services may have significant usage that is not obvious (KMS for instance).
 - Maintain a spreadsheet while developing pipelines to understand the impacts to cost of fixes or enhancements.
 - Don't run long or intensive Glue jobs on the weekend.

### Configure AWS Budgets and monitor proactively with AWS Cost Explorer

 - Create individual alarms for services that are used by the pipeline (Glue, Athena, S3, KMS, etc).
 - Create an overall alarm for all services.
 - Be mindful of the once a year billing spike when services are pre-purchased.
 - Ensure that all resources are tagged in a way that makes cost monitoring easy.

### Learn the cost differences and tradeoffs between different AWS services and capabilities

 - Athena is billed by the TB scanned ($5/TB as of this writing) regardless of the query complexity.
 - Glue is billed by the DPU hour ($0.44/DPU hour as of this writing) regardless of the size of data processed.
 - Both Athena and Glue will incur costs for S3 and KMS.
 - Athena has a query timeout of 30 minutes and cannot open more than 100 partitions.
   - This is both an annoying limitation and a fail-safe that avoids runaway spending.

Given these constraints, it is possible for some use cases to load large volumes of data with Athena (using
`insert into select`) at a much cheaper AWS cost than with Glue. However, the 30 minute timeout and 100 partition
maximum may result in spending engineer time (another aspect of cost that should be considered) to break up the job
manually. For a practical example, see this [Runbook](../../runbooks/how-to-load-cloudwatch-historical-data.md).

### Know your AWS features and their cost implications

 - Use S3 Bucket keys for insights buckets that accumulate many small objects to save on KMS costs.
   - Firehose unavoidably produces many small objects which quickly become costly to decrypt for reporting jobs.
   - Bucket keys reduce this overhead significantly.
 - Consider using Parquet format for wide tables.
   - Parquet will reduce the amount of data accessed by Athena for many queries since only the columns in the query will
     be scanned. This saves on Athena cost and makes the queries faster.
   - Parquet is more costly during ingestion if using Glue jobs and can be more complex to configure and work with.
 - Cloudwatch provides an export feature that can be used to load historical data but it has some quirks.
   - Export target bucket cannot be configured with KMS.
   - The export format does not match the export format from Cloudwatch subscriptions.
   - This Grok filter can be used to extract the message as a string `%{TIMESTAMP_ISO8601:timestamp:string} %{GREEDYDATA:message:string}`
   - Athena provides good support for JSON extraction which can be used on a Glue table using the Grok filter.
 