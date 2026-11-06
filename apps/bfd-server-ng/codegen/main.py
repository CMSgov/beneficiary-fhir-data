import argparse
import html
import json
import re
import sys
from pathlib import Path
from typing import Any


def gen_enum(fsh_output_file: str, int_codes: bool = False) -> None:
    source = Path(f"../bfd-model-idr/sushi/fsh-generated/resources/{fsh_output_file}.json")
    with source.open() as f:
        content = f.read()
        json_content = json.loads(content)
        enum_vals = [
            get_enum_val(concept, int_codes)
            for concept in json_content["concept"]
            if concept["code"] != "~"
        ]
        output = Path(sys.path[0]) / "out" / f"{fsh_output_file}.txt"
        with output.open("w") as f_out:
            f_out.write(",\n".join(enum_vals) + ";")


def get_enum_val(concept: dict[str, Any], int_codes: bool) -> str:
    code = concept["code"]
    display = concept["display"].replace('"', "")
    prefix = "_" if re.match("\\d", code[0]) else ""
    code_fmt = code if int_codes else f'"{code}"'
    punct = "" if display.endswith(".") else "."
    javadoc = html.escape(f"/**\n{code} - {display}{punct}\n*/", quote=False)
    return f'{javadoc}\n{prefix}{code}({code_fmt},"{display}")'


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", help="The JSON file name")
    parser.add_argument(
        "--int-codes", action="store_true", help="Use int codes instead of string codes"
    )
    args = parser.parse_args()

    gen_enum(args.file, args.int_codes)
