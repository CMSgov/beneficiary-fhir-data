#!/usr/bin/env python

import argparse
from os import listdir
from pathlib import Path
from shutil import copyfile

import boto3

parser = argparse.ArgumentParser()
parser.add_argument("-s", "--source-bucket", required=True)
parser.add_argument("-d", "--destination-bucket", required=True)
parser.add_argument("-p", "--source-prefix", required=True)

args = parser.parse_args()

source_bucket = args.source_bucket
destination_bucket = args.destination_bucket
source_prefix = args.source_prefix

client = boto3.client("s3")


timestamp = [i for i in source_prefix.split("/") if i != ""][-1]

files = Path("./files")
files.mkdir(exist_ok=True)

new_incoming = Path(f"./Incoming/{timestamp}")
new_incoming.mkdir(parents=True, exist_ok=True)

response = client.list_objects_v2(
    Bucket=source_bucket, Delimiter="/", Prefix=source_prefix
)
for item in response["Contents"]:
    client.download_file(
        source_bucket, item["Key"], f"./{files.name}/{item["Key"].split("/")[-1]}"
    )

manifest_template = open("./manifest-template.xml").read()
id_num = 1
manifest_names = []
for f_name in listdir(files.name):
    if f_name.endswith(".csv"):
        new_manifest_path = f"{new_incoming.absolute()}/{id_num}_manifest.xml"
        manifest_names.append(f"{id_num}_manifest.xml")
        copyfile("./manifest-template.xml", new_manifest_path)
        copyfile(
            f"{files.name}/{f_name}",
            f"{new_incoming.absolute()}/{f_name.replace(".csv", ".txt")}",
        )
        with open(new_manifest_path, "w") as manifest:
            contents = (
                manifest_template.replace("{TIMESTAMP}", timestamp)
                .replace("{SEQUENCE_ID}", str(id_num))
                .replace("{NAME}", f_name.replace(".csv", ".txt"))
                .replace("{TYPE}", f_name.replace(".csv", "").upper())
                .replace("{EXPORT_TYPE}", "UPDATE")
            )
            manifest.write(contents)
            id_num += 1

with open(f"{new_incoming.absolute()}/ManifestList.done", "w") as f:
    f.write("\n".join(manifest_names))

for f_name in listdir(new_incoming.absolute()):
    full_path = f"{new_incoming.absolute()}/{f_name}"
    client.upload_file(
        full_path,
        destination_bucket,
        f"Incoming/{timestamp}/{f_name}",
    )
