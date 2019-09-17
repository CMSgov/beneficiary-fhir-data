*Database restore procedure*  
This is a semi-manual process adapted from the procedures and scripts documented here: https://github.com/CMSgov/bluebutton-ansible-playbooks-data/tree/master/dev/db_devops/restore  

1. Stand up an EC2 instance in the same VPC and AZ as the master database you want to restore to
2. Create and attach an st1 volume of appropriate size and mount it to `/u01` for compatibility
3. Install git and the postgresql96 client package here: https://yum.postgresql.org/repopackages.php#pg96
4. Clone this repo
5. Create the directory `/u01/backups/fhirdb` for compatibility
6. Edit `s3sync.sh` for your values and run in the background: `nohup ./s3sync.sh >/dev/null 2>&1 &`
7. Edit `dbsetup.sql` for your values and run: `psql postgres://(RDS USERNAME)@(RDS URI):5432/(RDS TEMP DB) -f dbsetup.sql`
8. Edit `dbrestore.sh` for your values and run in the background: `nohup ./dbrestore.sh >/dev/null 2>&1 &`