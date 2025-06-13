import json
import re


def gen_enum(fsh_output_file: str, int_codes=False):
    with open(
        f"../../bfd-model/bfd-model-idr/sushi/fsh-generated/resources/{fsh_output_file}",
        "r",
    ) as f:
        content = f.read()
        json_content = json.loads(content)
        enum_vals = [
            get_enum_val(concept, int_codes) for concept in json_content["concept"]
        ]
        with open("out/" + fsh_output_file.replace(".json", ".txt"), "w") as f_out:
            f_out.write(",\n".join(enum_vals) + ";")


def get_enum_val(concept, int_codes):
    code = concept["code"]
    prefix = ""
    if re.match("\\d", code[0]):
        prefix = "_"
    if code == "~":
        return f'NA("","{concept['display']}")'
    else:
        if not int_codes:
            code = f'"{code}"'
        return f"{prefix}{code}({code},\"{concept['display']}\")"


if __name__ == "__main__":
    gen_enum("CodeSystem-CLM-REV-CNTR-CD.json")
    gen_enum("CodeSystem-CLM-DDCTBL-COINSRNC-CD.json")
    gen_enum("CodeSystem-Benefit-Balance.json")
    gen_enum("CodeSystem-CLM-TYPE-CD.json", int_codes=True)
