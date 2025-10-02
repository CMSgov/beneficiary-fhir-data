Run migrations against a localhost database:

```sh
./migrate-local.sh
```

Run migrations in a deployed environment (replace the value of `BFD_ENV` with the environment name you want to target)

```sh
BFD_ENV=1234-test ./migrate-env.sh
```
