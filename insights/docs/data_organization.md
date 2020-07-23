# Organization

To help people new to the BFD-Insights project, we should use the same names and organization in S3 buckets, Athena databases, and Glue workflows. 
This document lists the names used and the conventions they follow. Please keep it up to date. 

## Concepts

### Lake

A lake is a set of databases and their query engines, plus the ELT jobs and workflows to load and transform the data within. As such, a lake is more of a concept than an actual object. Note: the production data lake `prod-lake` has data from both the `prod` and `prod-sbx` environments. 

Visulization tools like QuickSight conceptually sit outside the data lake. 

### Projects 
Projects represent a particular DASG project. The abbreviated name of the projects are used. Examples include `ab2d`, `bb2`, `bfd`, `bcda`, and `dpc`. `dasg` is sometime to used as a project name to indicate concerns that cross-project boundaries. 

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

## Resources Naming Conventions

### All resources

- Resources start with `bfd-insights` to distinguish them from other account resources
- Where tagging is supported, included tags are: `business`, 
`product` `sensitivity`, and `project`.  

### Buckets
There are two forms of bucket names: a per project name or a sensitivity name. Since s3 buckets need to be globally unique names, they also include the account-id to ensure they unique. 

Examples: `bfd-insights-moderate-577373831711`, `bfd-insights-ab2d-577373831711`

### Top-level Folders
At the top level of a bucket, these folders are setup:

- **users** (optional) folder for specific users to store data and query results
- **databases** folder for databases
- **adhoc** (optional) folder to hold miscellaneous 

### Databases and Tables

Databases names follow this convention: `<project><_suffix>`. The project is required, but any suffix can follow including no suffix.

Table names should be simple and reflect the contents of the table.

All database and table names should be lower-cased and only inlcude letters, numbers and _. They should not include `bfd` or `insights` as a prefix. 

### Data Folders 

Folders that hold data follow the following convention: 
```
/databases/<database>/<table>/<partitions>
```
Where
- **database** is the abbreviated name of the project plus any other suffixes if there are multiple databases in a project
- **table** describes the content of the table
- **partition** are folders used by table partitions. These must follow the Hive convention of `<partition_name>=<value>`

All names should be lower cased and only inlcude letters, numbers and _. 

### Users Folders
All user folders follow this convention. 

```
/users/<user-name>
/users/<user-name>/query_results
```

### Components
The component names are used to provide a human readable name to independently running software. Each EC2 image or ECS container should have a component name, for example. There is a cross-project component table, so each component name needs to made unique by adding the project name. 

```
<project_name>.<component>
```
Examples: `bb2.web`, `dpc.api`, and `bcda.worker`