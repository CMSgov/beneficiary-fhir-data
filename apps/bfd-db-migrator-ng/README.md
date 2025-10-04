Run migrations against a localhost database:

```sh
./migrate-local.sh
```

Run migrations in a live environment (replace the value of `BFD_ENV` with the environment name you want to target).

```sh
BFD_ENV=1234-test ./migrate-env.sh
```

To create a new migration, add a new file in the `migrations` folder.
Migration names use the following format: `{major_version}_{minor_version}_{patch_version}__{description}.sql`.
