# `bfd-db-migrator-ng`

Flyway-based migrations sub-project.

## Running Migrations

### Against a Live Environment

> [!IMPORTANT]
> Make sure you are authenticated via Kion to the appropriate AWS Account in your shell prior to running any of the commands below

Run migrations in a live environment (e.g. `prod`, `9999-test`, etc.) with `./migrate.sh` by specifying the target environment via the `BFD_ENV` environment variable or as the first command-line argument:

```sh
BFD_ENV=1234-test ./migrate.sh
```

or:

```sh
./migrate.sh 1234-test
```

## Adding Migrations

To create a new migration, add a new file in the `migrations` folder.
Migration names use the following format: `{major_version}_{minor_version}_{patch_version}__{description}.sql`.

## Container Image

The `bfd-platform-migrator-ng` Container Image is based-off of the [official Flyway Image](https://hub.docker.com/r/flyway/flyway) and simply bundles the migrations and SQL callbacks from `./migrations` into the image. It is possible to use the Container to run both local and live migrations; e.g. to run a local migration:

```sh
podman run \
    -v ./migrations:/app/migrations \
    -e "FLYWAY_URL=jdbc:snowflake://..." \
    -e "FLYWAY_USER=<user>" \
    --network host \
    --rm <migrator-image>
```

## Granting Permissions

Snowflake requires a lot of permissions to allow the migrator/pipeline to work.
Here are the list of commands in case they need to be re-ran in the future for both schemas 
(CMS_VDM_VIEW_MDCR_PRD & CMS_EDP_VIEW_CVM_PRAU_PRD):

```sql
GRANT USAGE ON DATABASE BFD_{ENV} TO {ENV}_SERVICE_USER;
GRANT USAGE ON WAREHOUSE BFD TO ROLE {ENV}_SERVICE_USER;
CREATE SCHEMA IF NOT EXISTS CMS_VDM_VIEW_MDCR_PRD;
GRANT USAGE ON SCHEMA CMS_VDM_VIEW_MDCR_PRD TO {ENV}_SERVICE_USER;
GRANT SELECT ON ALL TABLES IN SCHEMA CMS_VDM_VIEW_MDCR_PRD TO ROLE {ENV}_SERVICE_USER;
GRANT SELECT ON FUTURE TABLES IN SCHEMA CMS_VDM_VIEW_MDCR_PRD TO ROLE {ENV}_SERVICE_USER;
GRANT INSERT ON ALL TABLES IN SCHEMA CMS_VDM_VIEW_MDCR_PRD TO ROLE {ENV}_SERVICE_USER;
GRANT INSERT ON FUTURE TABLES IN SCHEMA CMS_VDM_VIEW_MDCR_PRD TO ROLE {ENV}_SERVICE_USER;
GRANT UPDATE ON ALL TABLES IN SCHEMA CMS_VDM_VIEW_MDCR_PRD TO ROLE {ENV}_SERVICE_USER;
GRANT UPDATE ON FUTURE TABLES IN SCHEMA CMS_VDM_VIEW_MDCR_PRD TO ROLE {ENV}_SERVICE_USER;
GRANT DELETE ON ALL TABLES IN SCHEMA CMS_VDM_VIEW_MDCR_PRD TO ROLE {ENV}_SERVICE_USER;
GRANT DELETE ON FUTURE TABLES IN SCHEMA CMS_VDM_VIEW_MDCR_PRD TO ROLE {ENV}_SERVICE_USER;
GRANT CREATE TABLE ON SCHEMA CMS_VDM_VIEW_MDCR_PRD TO {ENV}_SERVICE_USER;
```
