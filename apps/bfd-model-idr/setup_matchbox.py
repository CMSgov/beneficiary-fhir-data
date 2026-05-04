import argparse
import http
import json
import logging
import tarfile
from collections import defaultdict
from pathlib import Path

import requests

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)


def download_package(package_id, version, download_dir, session):
    """Download pkg if not local."""
    download_dir.mkdir(exist_ok=True)
    package_path = download_dir / f"{package_id}-{version}.tgz"

    if package_path.exists():
        return package_path

    url = f"https://packages.fhir.org/{package_id}/{version}"
    logger.info("Downloading %s", package_id)

    response = session.get(url, timeout=30)
    response.raise_for_status()
    with package_path.open("wb") as f:
        f.write(response.content)
    return package_path


def parse_manifest(manifest_file):
    """Parse manifest."""
    path = Path(manifest_file)
    if not path.exists():
        logger.error("Manifest %s not found.", manifest_file)
        return {}

    package_map = defaultdict(list)
    current_package = None

    with path.open() as f:
        for raw_line in f:
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("[") and line.endswith("]"):
                current_package = line[1:-1]
            elif current_package:
                package_map[current_package].append(line)
    return package_map


def upload_resource(base_url, resource_json, session):
    """Upload resource to Matchbox."""
    rtype = resource_json.get("resourceType")
    rid = resource_json.get("id")
    if not rtype or not rid:
        return

    url = f"{base_url}/{rtype}/{rid}"
    try:
        resp = session.put(
            url,
            json=resource_json,
            headers={"Content-Type": "application/fhir+json"},
            timeout=60,
        )
        if resp.status_code in [http.HTTPStatus.OK, http.HTTPStatus.CREATED]:
            logger.info("Uploaded %s/%s", rtype, rid)
        else:
            logger.error("Failed %s/%s: %s", rtype, rid, resp.status_code)
    except Exception as e:
        logger.error("Error uploading %s: %s", rid, e)



def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:8080/matchboxv3/fhir")
    parser.add_argument("--manifest", default="matchbox_profiles.txt")
    args = parser.parse_args()

    session = requests.Session()
    package_map = parse_manifest(args.manifest)
    download_dir = Path("temp_packages")

    for pkg_info, resources in package_map.items():
        if "#" not in pkg_info:
            continue

        pkg_id, version = pkg_info.split("#")
        try:
            pkg_path = download_package(pkg_id, version, download_dir, session)
            logger.info("Processing %s", pkg_info)

            resource_set = set(resources)
            with tarfile.open(pkg_path, "r:gz") as tar:
                for member in tar.getmembers():
                    if member.name.endswith(".json"):
                        fname = Path(member.name).name.replace(".json", "")
                        if fname in resource_set:
                            extracted_file = tar.extractfile(member)
                            if extracted_file is not None:
                                content = json.loads(extracted_file.read())
                                upload_resource(args.url, content, session)
                                resource_set.remove(fname)

            for missing in resource_set:
                logger.warning("Not found in package: %s", missing)

        except Exception as e:
            logger.error("Error processing %s: %s", pkg_info, e)


if __name__ == "__main__":
    main()
