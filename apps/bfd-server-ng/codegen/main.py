import argparse
import html
import json
import re
import sys


def gen_enum(fsh_output_file: str, int_codes=False):
    with open(
        f"../bfd-model-idr/sushi/fsh-generated/resources/{fsh_output_file}.json",
        "r",
    ) as f:
        content = f.read()
        json_content = json.loads(content)
        enum_vals = [
            get_enum_val(concept, int_codes)
            for concept in json_content["concept"]
            if concept["code"] != "~"
        ]
        with open(f"{sys.path[0]}/out/{fsh_output_file}.txt", "w") as f_out:
            f_out.write(",\n".join(enum_vals) + ";")


def get_enum_val(concept, int_codes):
    code = concept["code"]
    display = concept.get("display")
    if display:
        display = display.replace('"', "")
        display_javadoc = f" - {display}"
        display_val = f',"{display}'
        punct = "" if display.endswith(".") else "."
    else:
        display_javadoc = ""
        display_val = ""
        punct = "."
    prefix = "_" if re.match("\\d", code[0]) else ""
    if int_codes:
        code_fmt = code
    else:
        code_fmt = f'"{code}"'

    javadoc = html.escape(f"/**\n{code}{display_javadoc}{punct}\n*/", quote=False)
    return f"{javadoc}\n{prefix}{code}({code_fmt}{display_val})"


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", help="The JSON file name")
    parser.add_argument(
        "--int-codes", action="store_true", help="Use int codes instead of string codes"
    )
    args = parser.parse_args()

    gen_enum(args.file, args.int_codes)
