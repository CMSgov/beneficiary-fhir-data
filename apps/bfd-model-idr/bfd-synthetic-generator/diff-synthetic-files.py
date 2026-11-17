import csv
import sys
from pathlib import Path

import click
import daff
from schema_utils import fetch_schemas, get_table_name_from_file_name


def load_csv_to_daff(file_path):
    with open(file_path, "r", newline="", encoding="utf-8") as f:
        reader = csv.reader(f)
        data = list(reader)
    return daff.PythonTableView(data)


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
    original_csv_files = Path.iterdir(src_dir)
    schemas = fetch_schemas()
    primary_keys_per_table = {table: set(schema.primary_keys) for table, schema in schemas.items()}

    for file in original_csv_files:
        file_name = file.name
        original_file = src_dir / file_name
        regenerated_file = regen_dir / file_name

        if Path.is_file(regenerated_file):
            original_table = load_csv_to_daff(original_file)
            regen_table = load_csv_to_daff(regenerated_file)
            alignment = daff.Coopy.compareTables(original_table, regen_table).align()
            diff = []
            table_diff = daff.PythonTableView(diff)

            flags = daff.CompareFlags()
            # Show all columns so we can display pks even though they're unchanged.
            # This makes the rows with changes easier to identify to debug.
            flags.unchanged_column_context = 999
            highlighter = daff.TableDiff(alignment, flags)
            highlighter.hilite(table_diff)

            if not highlighter.hasDifference():
                print(f"MATCH: {file_name}")
            else:
                print(f"MISMATCH: {file_name}")
                mismatch_count += 1

                print("--- Sample of Changes Detected ---")
                if len(diff) > 1:
                    header_idx = 0
                    data_rows = diff[header_idx + 1 :]
                    # column 0 is the action column that shows symbols like -> (modified),
                    # ++ (added), or --- (deleted)
                    keep_columns = [0]
                    sample_changed_rows = []
                    modified_rows_seen = set()

                    # if there are any modified cells get the field names,actual values, and pks
                    # to identify the row
                    for column_index in range(1, len(diff[header_idx])):
                        column_name = diff[header_idx][column_index]
                        table_name = get_table_name_from_file_name(file_name.removesuffix(".csv"))
                        pks = primary_keys_per_table[table_name]
                        is_pk = column_name in pks

                        modified_row_idx = None
                        for row_idx, row in enumerate(data_rows):
                            if row[0] == "->" and "->" in row[column_index]:
                                modified_row_idx = row_idx
                                break

                        if modified_row_idx is not None or is_pk:
                            keep_columns.append(column_index)
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

                    rows_to_render = diff[: header_idx + 1] + sample_changed_rows
                    granular_diff = [
                        [row[column_idx] for column_idx in keep_columns] for row in rows_to_render
                    ]
                    table_diff = daff.PythonTableView(granular_diff)

                renderer = daff.TerminalDiffRender()
                print(renderer.render(table_diff))
                print("----------------------------------")
        else:
            print(f"'{file}' exists in {src_dir} but is missing in {regen_dir}")
            sys.exit(1)

    print("==================================================")
    if mismatch_count > 0:
        print(
            f"{mismatch_count} file(s) do not have matching data content post regeneration. "
            + "Verify if this is intended."
        )
    else:
        print("Files are identical post regeneration. Verify if this is intended.")


if __name__ == "__main__":
    main()
