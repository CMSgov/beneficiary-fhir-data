# BFD-858 - SPIKE : Aurora Upgrade db Engine
This README is built from a Confluence document that outlines an Aurora db engine upgrade path from 11.6 to 12.4 (https://confluence.cms.gov/pages/resumedraft.action?draftId=466267350&draftShareId=b3cec33a-3f09-4e26-a23c-883172837fea&)

## High Level Summary

AWS provides both the tools and best practices to upgrade an RDS Postgres database engine to next available major/minor version; the steps to safely upgrade the BFD database will be outline here. The most important planning component will be the timing of when to perform the database clone of the current BFD database. Once, a clone database has been created, the actual upgrade steps can begin.

Note: The following steps/examples apply to all BFD Aurora database(s):

-   bfd-prod
-   bfd-prod-sbx
-   bfd-test

### Plan the Postgres db Engine Upgrade Path

Prior to undertaking the Postgres db engine upgrade, we need to determine what version of the engine we are trying to enable; assuming the current version is 11.6, and we would like to get to version 12.4, we could determine what our upgrade options are via some AWS _cli_ commands:

    aws rds describe-db-engine-versions --engine aurora-postgresql \
    --engine-version 11.6 \
    --query 'DBEngineVersions[].ValidUpgradeTarget[?IsMajorVersionUpgrade == `true`]'
    [
       []
    ]

Providing the current Postgres engine version (_11.6_) and requesting a major version upgrade (_IsMajorVersionUpgrade == `true`_) we get back an empty upgrade path; there is no direct upgrade path from 11.6 to the next major version. To determine what minor version upgrade path exists, execute the same command but change the major version to false (_IsMajorVersionUpgrade == `false`_). That results in:

    [
      [
        {
            "Engine": "aurora-postgresql",
            "EngineVersion": "11.7",
            "Description": "Aurora PostgreSQL (Compatible with PostgreSQL 11.7)",
            "AutoUpgrade": false,
            "IsMajorVersionUpgrade": false
        },
        {
            "Engine": "aurora-postgresql",
            "EngineVersion": "11.8",
            "Description": "Aurora PostgreSQL (Compatible with PostgreSQL 11.8)",
            "AutoUpgrade": false,
            "IsMajorVersionUpgrade": false
        },
        {
            "Engine": "aurora-postgresql",
            "EngineVersion": "11.9",
            "Description": "Aurora PostgreSQL (Compatible with PostgreSQL 11.9)",
            "AutoUpgrade": true,
            "IsMajorVersionUpgrade": false
        }
    ]
]

AWS shows us the available minor upgrade version path(s) available; so assuming that we will first do a minor version upgrade, we can determine from which minor version we can get to 12.4.

    aws rds describe-db-engine-versions --engine aurora-postgresql \
    --engine-version 11.7 
    --query 'DBEngineVersions[].ValidUpgradeTarget[?IsMajorVersionUpgrade == `true`]'
    [
      [
        {
            "Engine": "aurora-postgresql",
            "EngineVersion": "12.4",
            "Description": "Aurora PostgreSQL (Compatible with PostgreSQL 12.4)",
            "AutoUpgrade": false,
            "IsMajorVersionUpgrade": true
        }
    ]
] 

So AWS tells us that with version 11.7 (or 11.8 or 11.9), we can upgrade the engine to 12.4. So our upgrade path becomes:

-   Upgrade the engine from 11.6 to 11.7
-   Then upgrade the engine from 11.7 to 12.4

### Clone Current BFD Write Database

The BFD database is updated with new data, generally on a weekly basis, using ETL operations performed by the RIF loader during weekend processing. The ETL process will determine if there is new CCW data available in S3 bucket(s), and if so the ETL process will apply the CCW data updates to the BFD database. So prior to cloning the current database, the following steps should be considered:

-   Ensure the the weekend ETL load process ran to a successful completion; verify that there are no data files in the _Incoming_ S3 bucket.
-   If no _Incoming_ data files, we can continue to cloning the BFD Aurora database via the AWS console ([https://console.aws.amazon.com/rds/home?region=us-east-1#databases](https://console.aws.amazon.com/rds/home?region=us-east-1#databases))
-   Select the RDS database you want to clone and then under the _Actions_ dropdown, select _Create Clone_; you will need to fill in things like clone name, etc. Since we are cloning, there is no need to change most of the current db settings.
-   The cloning operation will run upwards of 60 minutes; periodically refresh the RDS database console to determine when the cloning operation has completed.

### Upgrade the Postgres db Engine to 11.n

When the cloning operation has completed, we can begin the db engine upgrade operations; as noted, we'll need to perform two postgres engine upgrades:

-   From 11.6 to 11.[7 | 8 | 9]
-   Then 11.n to 12.4

Again using the AWS RDS console ([https://console.aws.amazon.com/rds/home?region=us-east-1#databases](https://console.aws.amazon.com/rds/home?region=us-east-1#databases)), select the clone database that was created in the previous step, and select the _Modify_ action (button). The Modify screen ([https://console.aws.amazon.com/rds/home?region=us-east-1#modify-cluster:id=bfd-prod-aurora-cluster](https://console.aws.amazon.com/rds/home?region=us-east-1#modify-cluster:id=bfd-prod-aurora-cluster)) has an _Engine Version_ drop-down; select the db engine you wish to upgrade to, select any other parameters that may be applicable (since we cloned most of the current settings are fine), and then hit the _Continue_ action (button) at bottom of screen. AWS will then initiate the engine upgrade.

Depending on the available Engine Version drop-down entries, this step may need to be performed multiple times to reach the target engine version (i.e., may have to upgrade to 11.6+, then to the next major version, 12.4).

### Post Database Engine Upgrade

Once the database engine has been upgraded, AWS best practices dictate that _autovacuum_ be enabled on the db instance; since the target db (clone) instance, it is also possible to perform a full vacuum at this time. Generally enabling _autovacuum_ should be sufficient at this point. The AWS best practices can be found at: [https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html).

### Verify Database Functionality

Once the database engine upgrade(s) and follow-up activities like _vacuum_ have been completed, we can now point a BFD test instance to the new database URL and perform API smoke-tests or other such exercising of the new db instance. When BFD services testing has satisfactorily completed, we can begin to perform a hot-standby of the Aurora RDS cluster substituting the new (just cloned/upgraded) database for the original BFD database URL.

## High Level Summary

AWS provides both the tools and best practices to upgrade an RDS Postgres database engine to next available major/minor version; the steps to safely upgrade the BFD database will be outline here. The most important planning component will be the timing of when to perform the database clone of the current BFD database. Once, a clone database has been created, the actual upgrade steps can begin.

Note: The following steps/examples apply to all BFD Aurora database(s):

-   bfd-prod
-   bfd-prod-sbx
-   bfd-test

### Plan the Postgres db Engine Upgrade Path

Prior to undertaking the Postgres db engine upgrade, we need to determine what version of the engine we are trying to enable; assuming the current version is 11.6, and we would like to get to version 12.4, we could determine what our upgrade options are via some AWS _cli_ commands:

    aws rds describe-db-engine-versions --engine aurora-postgresql \
    --engine-version 11.6 \
    --query 'DBEngineVersions[].ValidUpgradeTarget[?IsMajorVersionUpgrade == `true`]'
    [
       []
    ]

Providing the current Postgres engine version (_11.6_) and requesting a major version upgrade (_IsMajorVersionUpgrade == `true`_) we get back an empty upgrade path; there is no direct upgrade path from 11.6 to the next major version. To determine what minor version upgrade path exists, execute the same command but change the major version to false (_IsMajorVersionUpgrade == `false`_). That results in:

    [
      [
        {
            "Engine": "aurora-postgresql",
            "EngineVersion": "11.7",
            "Description": "Aurora PostgreSQL (Compatible with PostgreSQL 11.7)",
            "AutoUpgrade": false,
            "IsMajorVersionUpgrade": false
        },
        {
            "Engine": "aurora-postgresql",
            "EngineVersion": "11.8",
            "Description": "Aurora PostgreSQL (Compatible with PostgreSQL 11.8)",
            "AutoUpgrade": false,
            "IsMajorVersionUpgrade": false
        },
        {
            "Engine": "aurora-postgresql",
            "EngineVersion": "11.9",
            "Description": "Aurora PostgreSQL (Compatible with PostgreSQL 11.9)",
            "AutoUpgrade": true,
            "IsMajorVersionUpgrade": false
        }
    ]
]

AWS shows us the available minor upgrade version path(s) available; so assuming that we will first do a minor version upgrade, we can determine from which minor version we can get to 12.4.

    aws rds describe-db-engine-versions --engine aurora-postgresql \
    --engine-version 11.7 
    --query 'DBEngineVersions[].ValidUpgradeTarget[?IsMajorVersionUpgrade == `true`]'
    [
      [
        {
            "Engine": "aurora-postgresql",
            "EngineVersion": "12.4",
            "Description": "Aurora PostgreSQL (Compatible with PostgreSQL 12.4)",
            "AutoUpgrade": false,
            "IsMajorVersionUpgrade": true
        }
    ]
] 

So AWS tells us that with version 11.7 (or 11.8 or 11.9), we can upgrade the engine to 12.4. So our upgrade path becomes:

-   Upgrade the engine from 11.6 to 11.7
-   Then upgrade the engine from 11.7 to 12.4

### Clone Current BFD Write Database

The BFD database is updated with new data, generally on a weekly basis, using ETL operations performed by the RIF loader during weekend processing. The ETL process will determine if there is new CCW data available in S3 bucket(s), and if so the ETL process will apply the CCW data updates to the BFD database. So prior to cloning the current database, the following steps should be considered:

-   Ensure the the weekend ETL load process ran to a successful completion; verify that there are no data files in the _Incoming_ S3 bucket.
-   If no _Incoming_ data files, we can continue to cloning the BFD Aurora database via the AWS console ([https://console.aws.amazon.com/rds/home?region=us-east-1#databases](https://console.aws.amazon.com/rds/home?region=us-east-1#databases))
-   Select the RDS database you want to clone and then under the _Actions_ dropdown, select _Create Clone_; you will need to fill in things like clone name, etc. Since we are cloning, there is no need to change most of the current db settings.
-   The cloning operation will run upwards of 60 minutes; periodically refresh the RDS database console to determine when the cloning operation has completed.

### Upgrade the Postgres db Engine to 11.n

When the cloning operation has completed, we can begin the db engine upgrade operations; as noted, we'll need to perform two postgres engine upgrades:

-   From 11.6 to 11.[7 | 8 | 9]
-   Then 11.n to 12.4

Again using the AWS RDS console ([https://console.aws.amazon.com/rds/home?region=us-east-1#databases](https://console.aws.amazon.com/rds/home?region=us-east-1#databases)), select the clone database that was created in the previous step, and select the _Modify_ action (button). The Modify screen ([https://console.aws.amazon.com/rds/home?region=us-east-1#modify-cluster:id=bfd-prod-aurora-cluster](https://console.aws.amazon.com/rds/home?region=us-east-1#modify-cluster:id=bfd-prod-aurora-cluster)) has an _Engine Version_ drop-down; select the db engine you wish to upgrade to, select any other parameters that may be applicable (since we cloned most of the current settings are fine), and then hit the _Continue_ action (button) at bottom of screen. AWS will then initiate the engine upgrade.

Depending on the available Engine Version drop-down entries, this step may need to be performed multiple times to reach the target engine version (i.e., may have to upgrade to 11.6+, then to the next major version, 12.4).

### Post Database Engine Upgrade

Once the database engine has been upgraded, AWS best practices dictate that _autovacuum_ be enabled on the db instance; since the target db (clone) instance, it is also possible to perform a full vacuum at this time. Generally enabling _autovacuum_ should be sufficient at this point. The AWS best practices can be found at: [https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html).

### Verify Database Functionality

Once the database engine upgrade(s) and follow-up activities like _vacuum_ have been completed, we can now point a BFD test instance to the new database URL and perform API smoke-tests or other such exercising of the new db instance. When BFD services testing has satisfactorily completed, we can begin to perform a hot-standby of the Aurora RDS cluster substituting the new (just cloned/upgraded) database for the original BFD database URL.