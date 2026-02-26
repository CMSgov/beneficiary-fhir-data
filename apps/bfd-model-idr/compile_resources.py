import argparse
import json
import subprocess
import sys
import requests
from pathlib import Path

MATCHBOX_SERVER = "http://localhost:8080/matchboxv3"


def run_command(cmd, cwd=None):
    try:
        result = subprocess.run(
            cmd, cwd=cwd, shell=True, check=True, text=True, capture_output=True
        )
        return result.stdout, result.stderr
    except subprocess.CalledProcessError as e:
        print("Error running command:", cmd)
        if e.stderr:
            print("Error output:", e.stderr)
        else:
            print("Error info (not necessarily stderr):", e)
        sys.exit(1)  # kill process and debug.


def get_referenced_maps(compiled_map_path):
    try:
        with Path(compiled_map_path).open() as f:
            map_data = json.load(f)

        referenced_maps = set()
        if "import" in map_data:
            for imp in map_data["import"]:
                map_name = "maps/" + imp.split("/")[-1] + ".map"
                referenced_maps.add(map_name)

        return referenced_maps
    except Exception as e:
        print("Error reading map:", e)
        return set()

def get_sushi_resources():
    try:
        sushi_dir = Path(__file__).parent.absolute() / "sushi" / "fsh-generated" / "resources"
        sushi_resources = [
            f"sushi/fsh-generated/resources/{file.name}" for file in sushi_dir.glob("*.json")
        ]
        return " ".join([f"-ig {resource}" for resource in sushi_resources])
    except Exception as e:
        print(f"Error getting SUSHI resources: {e}")
        return ""

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


def run_conformance_test(input_file, output_file):
    try:
        print(f"Running conformance testing against {MATCHBOX_SERVER}")

        with Path(input_file).open("r") as f:
            resource_content = json.load(f)

        url = f"{MATCHBOX_SERVER}/fhir/$validate"
        headers = {"Content-Type": "application/fhir+json", "Accept": "application/fhir+json"}
        params = {}

        # Extract profile from meta.profile if there is one
        if "meta" in resource_content and "profile" in resource_content["meta"]:
            profiles = resource_content["meta"]["profile"]
            if profiles:
                params["profile"] = profiles[0]
        else:
            # slight con of not using the validator, need to specify the profile to validate against.
            print(f"No profile found for {input_file}")
            return False

        response = requests.post(url, json=resource_content, headers=headers, params=params)
        try:
            outcome = response.json()
            with Path(output_file).open("w") as f:
                json.dump(outcome, f, indent=2)
        except Exception as e:
            print(f"error writing response: {e}")
            with Path(output_file).open("w") as f:
                f.write(response.text)
            return False

        print("Conformance test output saved to", output_file)

        errors = []
        if "issue" in outcome:
            for issue in outcome["issue"]:
                if issue.get("severity") in ["error", "fatal"]:
                    errors.append(issue)

        if errors:
            print(f"Validation failed with {len(errors)} errors. Note, not all errors are bad:")
            for err in errors:
                print(
                    f" - {err.get('diagnostics', 'N/A')} "
                    f"({err.get('code', 'N/a')}) "
                    f"({err.get('expression', 'N/a')})"
                )
            return False

        print("Yay, you did it! Success")
        return True

    except Exception as e:
        print(f"Error running conformance testing: {e}")
        return False


def main():
    # Parse args instead of just putting everything in separate READMEs.
    parser = argparse.ArgumentParser(description="Compile and execute FHIR structure maps.")
    parser.add_argument(
        "--map", "-m", type=str, help="Path to the structure map file", required=True
    )
    parser.add_argument(
        "--input", "-i", type=str, help="Path to the input JSON file", required=True
    )
    parser.add_argument(
        "--output", "-o", type=str, help="Path to the output JSON file", required=True
    )
    parser.add_argument(
        "--resource",
        "-r",
        type=str,
        help="Resource URL for the structure map",
        required=True,
    )
    parser.add_argument(
        "--test",
        "-t",
        action="store_true",
        help="Run conformance testing after transformation",
    )
    parser.add_argument(
        "--sushi",
        "-s",
        action="store_true",
        help="Use to run sushi beforehand.",
    )
    parser.add_argument(
        "--skip-structure-map-generation",
        action="store_true",
        help="Skip generating StructureMap (speeds up generating all resources if no changes)",
    )
    args = parser.parse_args()

    script_dir = Path(__file__).parent.absolute()

    # Generate Structure Definitions + CodeSystems
    if(args.sushi):
        print("Running sushi build")
        stdout, stderr = run_command("sushi build", cwd=script_dir / "sushi")
        print("SUSHI output:")
        print(stdout)
        if stderr:
            print("SUSHI errors:")
            print(stderr)

        # Upload resources to matchbox
        sushi_generated_dir = script_dir / "sushi" / "fsh-generated" / "resources"
        upload_resources(sushi_generated_dir, MATCHBOX_SERVER)

    sushi_resources = get_sushi_resources()

    compiled_map_path = f"StructureMaps/BFD-{Path(args.map).stem}-StructureMap.json"

    if not args.skip_structure_map_generation:
        # Compile FML files.
        print("Compiling FML ")
        compile_cmd = (
            f"java -jar validator_cli.jar -version 4.0.1 -ig {args.map} -compile {args.resource} \
            -output {compiled_map_path} -tx {MATCHBOX_SERVER}/tx"
        )
        print("Input compilation command was:" + compile_cmd)
        stdout, stderr = run_command(compile_cmd, cwd=script_dir)
        print("Compilation output:")
        print(stdout)
        if stderr:
            print("Compilation errors:")
            print(stderr)
    else:
        print("Skipping FML compilation, using existing structure map.")

    # Get referenced maps from the source map file
    referenced_maps = get_referenced_maps(compiled_map_path)
    map_imports = " ".join([f"-ig {map_file}" for map_file in referenced_maps])

    #Augment source file if needed. Currently just for providers.
    (input_file,) = {args.input}
    if 'EOB' in input_file:
        print("Augmenting input file")
        augmentation_cmd = f"python augment_sample_resources.py {args.input}"
        stdout, stderr = run_command(augmentation_cmd, cwd=script_dir)
        input_file = 'out/temporary-sample.json'

    print("Executing Transform")
    execute_cmd = f"java -jar validator_cli.jar {input_file} -output {args.output} -transform \
        {args.resource} -version 4.0.1 -ig {compiled_map_path} \
            -ig hl7.fhir.us.carin-bb#2.1.0 {map_imports} {sushi_resources} -tx {MATCHBOX_SERVER}/tx"
    stdout, stderr = run_command(execute_cmd, cwd=script_dir)

    print("Execution output:")
    print(stdout)
    if stderr:
        print("Execution errors:")
        print(stderr)

    if "error" in stdout.lower() or "error" in stderr.lower():
        print("Process completed with errors")
        sys.exit(1)
    else:
        print("Process completed successfully")

    # If we've passed --test then we'll run the output against C4BB.
    if args.test:
        success = run_conformance_test(args.output, "out/Validator-Output.json")
        if not success:
            sys.exit(1)


if __name__ == "__main__":
    main()
