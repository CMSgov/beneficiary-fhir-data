import os
import pytest
from testcontainers.postgres import PostgresContainer

from db import run_pipeline
from source.db_executor import PostgresFetcher


@pytest.fixture(scope="session", autouse=True)
def psql_url():
    os.environ["TESTCONTAINERS_RYUK_DISABLED"] = "1"
    with PostgresContainer("postgres:16") as postgres:
        psql_url = postgres.get_connection_url()
        yield psql_url


class TestPipeline:
    def test_pipeline(self, psql_url: str):
        run_pipeline(PostgresFetcher(100_000))
        assert 3 == 3
