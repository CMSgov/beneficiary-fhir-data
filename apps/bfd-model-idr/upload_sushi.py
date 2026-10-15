import json
from pathlib import Path

import requests

MATCHBOX_SERVER = "http://localhost:18080/matchboxv3"


# populate matchbox's tx library
def upload_resources(resource_dir, server_url):
    print("Uploading resources to matchbox")
    try:
        if not resource_dir.exists():
            print(f"Resource directory not found: {resource_dir}")
            return

        for file_path in Path(resource_dir).glob("*.json"):
            with file_path.open() as f:
                resource = json.load(f)

            resource_type = resource.get("resourceType")
            if not resource_type:
                print(f"Skipping {file_path.name}: No resourceType found")
                continue

            if resource_type not in ["StructureDefinition", "CodeSystem"]:
                continue

            resource_id = resource.get("id")

            fhir_url = f"{server_url}/fhir"

            url = f"{fhir_url}/{resource_type}/{resource_id}"
            response = requests.put(
                url, json=resource, headers={"Content-Type": "application/fhir+json"}
            )

            if response.status_code not in [200, 201]:
                print(
                    f"Failed to upload {file_path.name}: {response.status_code} - {response.text}"
                )
            else:
                print(f"Successfully uploaded {file_path.name}")

    except Exception as e:
        print(f"Error uploading resources: {e}")


def main():
    script_dir = Path(__file__).parent.absolute()
    # Upload resources to matchbox
    sushi_generated_dir = script_dir / "sushi" / "fsh-generated" / "resources"
    upload_resources(sushi_generated_dir, MATCHBOX_SERVER)


if __name__ == "__main__":
    main()
