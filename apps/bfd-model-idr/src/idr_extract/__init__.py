import click
from idr_pipeline.load_synthetic import tables

from .extractor import SnowflakeExecutor


@click.command()
def main() -> None:
    run_export()


def run_export() -> None:
    print("Starting Export.")
    executor = SnowflakeExecutor()
    for table in executor.get_tables():
        print(f"Exporting {table.table_name}.")
        export_table = [
            x for x in tables 
                if x["table"].casefold() == f"{table.schema_name}.{table.table_name}".casefold()
        ]
        if len(export_table) == 1:
            csv_file = export_table[0]["csv_name"]
        else:
            csv_file = f"SYNTHETIC_{table.table_name}.csv"
        executor.export(
            csv_file,
            f"SELECT * FROM {table.schema_name}.{table.table_name}"
        )
    print("Export Complete!")

