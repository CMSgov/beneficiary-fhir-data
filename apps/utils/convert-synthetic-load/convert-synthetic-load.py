import boto3
import argparse
from pathlib import Path
from os import listdir
from shutil import copyfile

parser = argparse.ArgumentParser()
parser.add_argument("bucket")
parser.add_argument("prefix")

args = parser.parse_args()
print(args.bucket)

client = boto3.client("s3")

s3 = boto3.resource("s3")
bucket = s3.Bucket(args.bucket)

timestamp = [i for i in args.prefix.split("/") if i != ""][-1]

files = Path("./files")
files.mkdir(exist_ok=True)

new_incoming = Path(f"./Incoming/{timestamp}")
new_incoming.mkdir(parents=True, exist_ok=True)


response = client.list_objects_v2(Bucket=args.bucket, Delimiter="/", Prefix=args.prefix)
for item in response["Contents"]:
    client.download_file(
        args.bucket, item["Key"], f"./{files.name}/{item["Key"].split("/")[-1]}"
    )

manifest_template = open("./manifest-template.xml", "r").read()
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
        args.bucket,
        f"Incoming/{timestamp}/{f_name}",
    )
