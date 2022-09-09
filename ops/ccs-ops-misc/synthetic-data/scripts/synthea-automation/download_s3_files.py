import sys
import boto3
from pathlib import Path


def get_file_folders(s3_client, bucket_name, prefix=""):
    file_names = []
    folders = []

    default_kwargs = {
        "Bucket": bucket_name,
        "Prefix": prefix
    }
    next_token = ""

    while next_token is not None:
        updated_kwargs = default_kwargs.copy()
        if next_token != "":
            updated_kwargs["ContinuationToken"] = next_token

        response = s3_client.list_objects_v2(**default_kwargs)
        contents = response.get("Contents")

        for result in contents:
            key = result.get("Key")
            if key[-1] == "/":
                folders.append(key)
            else:
                file_names.append(key)

        next_token = response.get("NextContinuationToken")

    return file_names, folders


def download_files(s3_client, bucket_name, local_path, file_names, folders):

    local_path = Path(local_path)

    for folder in folders:
        folder_path = Path.joinpath(local_path, folder)
        folder_path.mkdir(parents=True, exist_ok=True)

    for file_name in file_names:
        file_path = Path.joinpath(local_path, file_name)
        file_path.parent.mkdir(parents=True, exist_ok=True)
        print(f"file_path: {file_path}")
        s3_client.download_file(
            bucket_name,
            file_name,
            str(file_path)
        )


def main(args):
    client = boto3.client("s3")
    s3_bucket = args[0]
    target_dir = args[1]

    file_names, folders = get_file_folders(client, s3_bucket)
    download_files(
        client, s3_bucket, target_dir, file_names, folders
    )

if __name__ == "__main__":
    main(sys.argv[1:])