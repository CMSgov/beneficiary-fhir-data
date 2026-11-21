import csv
import sys
from pathlib import Path
from typing import Any

import click
from daff import (  # pyright: ignore[reportMissingTypeStubs]
    Alignment,
    CompareFlags,
    CompareTable,
    Coopy,
    PythonTableView,
    TableDiff,
    TerminalDiffRender,
)
from schema_utils import TableSchema, fetch_schemas, get_table_name_from_file_name


def _load_csv_to_daff(file_path: Path) -> PythonTableView:
    with file_path.open(newline="", encoding="utf-8") as f:
        reader = csv.reader(f)
        data: list[list[str]] = list(reader)
    return PythonTableView(data)


def _compare_tables(local: PythonTableView, remote: PythonTableView) -> CompareTable:
    return Coopy.compareTables(local, remote)  # pyright: ignore[reportUnknownMemberType]


@click.command()
@click.argument(
    "src_dir",
    type=click.Path(exists=True, file_okay=False, path_type=Path),
)
@click.argument(
    "regen_dir",
    type=click.Path(exists=True, file_okay=False, path_type=Path),
)
def main(src_dir: Path, regen_dir: Path) -> None:
    for directory in (src_dir, regen_dir):
        if not Path.iterdir(directory):
            print(f"Error: Directory '{directory}' does not exist.")
            sys.exit(1)

    print(f"Comparing CSV files in '{src_dir}' vs '{regen_dir}'")
    print("==================================================")

    mismatch_count = 0
    original_csv_files = [file for file in src_dir.iterdir() if file.suffix == ".csv"]

    schemas: dict[str, TableSchema] = fetch_schemas()
    primary_keys_per_table: dict[str, set[Any]] = {
        table: set(schema.primary_keys) for table, schema in schemas.items()
    }

    for file in original_csv_files:
        file_name: str = file.name
        original_file: Path = src_dir / file_name
        regenerated_file: Path = regen_dir / file_name

        if Path.is_file(regenerated_file):
            original_table: PythonTableView = _load_csv_to_daff(original_file)
            regen_table: PythonTableView = _load_csv_to_daff(regenerated_file)
            compare_tables: CompareTable = _compare_tables(original_table, regen_table)
            alignment: Alignment = compare_tables.align()
            diff: list[Any] = []
            table_diff: PythonTableView = PythonTableView(diff)

            flags: CompareFlags = CompareFlags()
            # Show all columns so we can display pks even though they're unchanged.
            # This makes the rows with changes easier to identify to debug.
            flags.unchanged_column_context = 999
            highlighter: TableDiff = TableDiff(alignment, flags)
            highlighter.hilite(table_diff)  # pyright: ignore[reportUnknownMemberType]

            if not highlighter.hasDifference():
                print(f"MATCH: {file_name}")
            else:
                print(f"MISMATCH: {file_name}")
                mismatch_count += 1

                print("--- Sample of Changes Detected ---")
                if len(diff) > 1:
                    header_idx = 0
                    column_schema_changes_row = None
                    # if a column gets added or deleted daff marks the action ! for diff[0] and
                    # diff[1] will contain @@ and the column names. If no column name is added then
                    # diff[0] will contain @@ and the column names
                    for row_idx, row in enumerate(diff):
                        if row[0] == "!":
                            column_schema_changes_row = row
                        elif row[0] == "@@":
                            header_idx = row_idx
                            break

                    if column_schema_changes_row is not None:
                        added_cols = [
                            diff[header_idx][column_idx]
                            for column_idx, action in enumerate(column_schema_changes_row)
                            if column_idx > 0 and action == "+++"
                        ]
                        removed_cols = [
                            diff[header_idx][column_idx]
                            for column_idx, action in enumerate(column_schema_changes_row)
                            if column_idx > 0 and action == "---"
                        ]
                        if added_cols:
                            print(f"! Added columns: {', '.join(added_cols)}")
                        if removed_cols:
                            print(f"! Removed columns: {', '.join(removed_cols)}")

                    data_rows: list[Any] = diff[header_idx + 1 :]
                    # column 0 is the action column that shows symbols like -> (modified),
                    # ++ (added), or --- (deleted)
                    keep_columns: list[int] = [0]
                    sample_changed_rows: list[Any] = []
                    modified_rows_seen: set[Any] = set()

                    # if there are any modified cells get the field names, actual values, and pks
                    # to identify the row
                    for column_idx in range(1, len(diff[header_idx])):
                        column_name = diff[header_idx][column_idx]
                        table_name: str = get_table_name_from_file_name(
                            file_name.removesuffix(".csv")
                        )
                        pks: set[Any] = primary_keys_per_table[table_name]
                        is_pk: bool = column_name in pks

                        modified_row_idx = None
                        for row_idx, row in enumerate(data_rows):
                            if row[0] == "->" and "->" in row[column_idx]:
                                modified_row_idx = row_idx
                                break

                        if modified_row_idx is not None or is_pk:
                            keep_columns.append(column_idx)
                            # more than one column value can be modified so we don't want to print
                            # the same row more than once
                            if (
                                modified_row_idx is not None
                                and modified_row_idx not in modified_rows_seen
                            ):
                                modified_rows_seen.add(modified_row_idx)
                                # We'll only show one example per field that has had its value
                                # modified instead of showing 1k+ rows for the same modified field
                                sample_changed_rows.append(data_rows[modified_row_idx])

                    # If there are row inserts and/or deletes for the file, grab one example
                    # insert (+++) and/or one example delete (---). Only show the pks of the row
                    # to identify the row
                    for action in ("+++", "---"):
                        for row_idx, row in enumerate(data_rows):
                            if row[0] == action:
                                modified_rows_seen.add(row_idx)
                                sample_changed_rows.append(row)
                                break

                    rows_to_render: list[Any] = [diff[header_idx], *sample_changed_rows]
                    granular_diff: list[list[Any]] = [
                        [row[column_idx] for column_idx in keep_columns] for row in rows_to_render
                    ]
                    table_diff = PythonTableView(granular_diff)

                renderer: TerminalDiffRender = TerminalDiffRender()
                print(renderer.render(table_diff))  # pyright: ignore[reportUnknownMemberType]
                print("----------------------------------")
        else:
            print(f"'{file_name}' exists in {src_dir.name} but is missing in {regen_dir.name}")
            mismatch_count += 1

    print("==================================================")
    if mismatch_count > 0:
        print(
            f"{mismatch_count} file(s) do not have matching data content post regeneration. "
            + "Verify if this is intended."
        )
        sys.exit(1)
    else:
        print("Files are identical post regeneration. Verify if this is intended.")


if __name__ == "__main__":
    main()
