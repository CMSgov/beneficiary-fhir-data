import argparse
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
    display = concept["display"].replace('"', "")
    prefix = "_" if re.match("\\d", code[0]) else ""
    if int_codes:
        code_fmt = code
    else:
        code_fmt = f'"{code}"'
    punct = "" if display.endswith(".") else "."
    javadoc = f"/**\n{code} - {display}{punct}\n*/"
    return f'{javadoc}\n{prefix}{code}({code_fmt},"{display}")'


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", help="The JSON file name")
    parser.add_argument(
        "--int-codes", action="store_true", help="Use int codes instead of string codes"
    )
    args = parser.parse_args()

    gen_enum(args.file, args.int_codes)
