#!/usr/bin/env python

import argparse
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

timestamp = [part for part in source_prefix.split("/") if part][-1]

files = Path("files")
files.mkdir(exist_ok=True)

new_incoming = Path(f"Incoming/{timestamp}")
new_incoming.mkdir(parents=True, exist_ok=True)

response = client.list_objects_v2(Bucket=source_bucket, Delimiter="/", Prefix=source_prefix)
for item in response.get("Contents", []):
    key = item.get("Key")
    if not key:
        continue
    client.download_file(source_bucket, key, str(files / Path(key).name))

manifest_template = Path("manifest-template.xml").read_text()
manifest_names: list[str] = []

for id_num, csv_file in enumerate(sorted(files.glob("*.csv")), start=1):
    manifest_name = f"{id_num}_manifest.xml"
    manifest_path = new_incoming / manifest_name
    txt_name = f"{csv_file.stem}.txt"

    manifest_names.append(manifest_name)
    copyfile("manifest-template.xml", manifest_path)
    copyfile(csv_file, new_incoming / txt_name)

    manifest_path.write_text(
        manifest_template.replace("{TIMESTAMP}", timestamp)
        .replace("{SEQUENCE_ID}", str(id_num))
        .replace("{NAME}", txt_name)
        .replace("{TYPE}", csv_file.stem.upper())
        .replace("{EXPORT_TYPE}", "UPDATE")
    )

(new_incoming / "ManifestList.done").write_text("\n".join(manifest_names))

for local_file in sorted(new_incoming.iterdir()):
    client.upload_file(
        str(local_file),
        destination_bucket,
        f"Incoming/{timestamp}/{local_file.name}",
    )
