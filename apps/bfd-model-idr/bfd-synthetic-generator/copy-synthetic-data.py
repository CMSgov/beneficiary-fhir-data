import click
from snowflake.snowpark import Session
from snowflake_session import create_snowflake_session


def _database_exists(session: Session, database: str) -> bool:
    return len(session.sql(f"SHOW DATABASES LIKE '{database}'").collect()) > 0


@click.command
@click.option(
    "--source",
    type=str,
    required=True,
    help="Database to clone from e.g. BFD_TEST",
)
@click.option(
    "--target",
    type=str,
    required=True,
    help="Database to clone to create or replace e.g. BFD_SANDBOX",
)
def main(source: str, target: str) -> None:
    source_database = source.strip()
    target_databases = [t.strip() for t in target.split(",") if t.strip()]

    if not source_database or source_database in target_databases:
        raise click.UsageError(
            f"Source database name {source_database} is missing or source is also one of the "
            "target databases."
        )

    session = create_snowflake_session()
    try:
        if not _database_exists(session, source):
            raise click.UsageError(f"Source database {source_database} does not exist.")

        for target_database in target_databases:
            statement = f"CREATE OR REPLACE DATABASE {target_database} CLONE {source_database}"
            if _database_exists(session, target_database):
                click.confirm(
                    f"Target database {target_database} exists and will be dropped and replaced. "
                    f"Run: {statement} ?",
                    abort=True,
                )
            else:
                click.confirm(
                    f"Target database {target_database} does NOT exist and will be created. "
                    f"Run: {statement} ?",
                    abort=True,
                )
            print(f"Running: {statement}")
            result = session.sql(statement).collect()
            print(result[0].status)
            print("Finished!")
    finally:
        session.close()


if __name__ == "__main__":
    main()
