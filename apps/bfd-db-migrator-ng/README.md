# `bfd-db-migrator-ng`

Flyway-based migrations sub-project.

## Running Migrations

### Against Local Environment

Run migrations against a `localhost` database:

```sh
./migrate.sh
```

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

The `bfd-platform-migrator-ng` Container Image is based-off of the [official Flyway Image](https://hub.docker.com/r/flyway/flyway) and simply bundles the migrations and SQL callbacks from `./migrations` and `./callbacks` into the image. It is possible to use the Container to run both local and live migrations; e.g. to run a local migration:

```sh
podman run \
    -v ./migrations:/app/migrations \
    -v ./callbacks:/app/callbacks \
    -e "FLYWAY_URL=jdbc:postgresql://localhost:5432/fhirdb" \
    -e "FLYWAY_USER=bfd" \
    -e "FLYWAY_PASSWORD=InsecureLocalDev" \
    --network host \
    --rm <migrator-image>
```
