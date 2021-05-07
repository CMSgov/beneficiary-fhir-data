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