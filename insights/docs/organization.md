# Organization

To help people new to the BFD-Insights project, we should use the same names and organization in S3 buckets, Athena databases, and Glue workflows. 
This document lists the names used and the conventions they follow. Please keep it up to date. 

## Concepts

### Lake

A lake is a set of databases and their query engines, plus the ELT jobs and workflows to load and transform the data within. As such, a lake is more of a concept than an actual object. Note: the production data lake `prod-lake` has data from both the `prod` and `prod-sbx` environments. 

Visulization tools like QuickSight conceptually sit outside the data lake. 

### Projects 
Projects contain all the resources and data for a particular DASG project. The abbreviated name of the projects are used. Examples include `ab2d`, `bb2`, `bfd`, `bcda`, and `dpc`. `dasg` is sometime to used as a project name to indicate concerns that cross-project boundaries. 

### Moderate and High Sensitivity
At the first level, a lake's data is divided according to the sensitivity of the data. There are different buckets for each sensitivity level. 

- **high:** Contains information which are highly sensitive. PII data has high sensitivity. 
- **moderate:** Contains data which are moderately sensitive. DASG logs have moderate sensitivity. Logs are scrubbed not to contain high sensitivity data. 

### Groups 
There are 4 groups of users defined: 
- **Admins** are .gov employees who setup security policies.
- **Analysts** are Data Engineers who
have access to all except security configs.
- **Authors** create QuickSight dashboards.
- **Readers** are leadership and product stakeholders with access to read QuickSight dashboards. 

## Naming Conventions

### All resources

- Resources start with `bfd-insights` to distinguish them from other account resources
- Where tagging is supported, included tags are: `business`, 
`product` `sensitivity`, and `project`.  

### Buckets
Bucket names reflect the sensitivity of the information they contain. Since s3 buckets need to be globally unique names, they also include the account-id to ensure they unique. Example: `bfd-insights-moderate-577373831711`

### Top-level Folders
At the top level of a bucket, these folders are setup:

- **users** folder for specific users to store data and query results
- **databases** folder for data from projects
- **adhoc** folder to hold miscellaneous 

### Table Folders

Folders that hold data follow the following convention: 
```
<database>/<table>/<partitions>
```
Where
- **database** is the abbreviated name of the project plus any other suffixes if there are multiple databases in a project
- **table** describes the content of the table
- **partition** are folders used by table partitions. These must follow the Hive convention of `<partition_name>=<value>`
