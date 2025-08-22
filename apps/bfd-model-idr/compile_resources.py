import argparse
import json
import subprocess
import sys
from pathlib import Path


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


def get_structure_definitions():
    try:
        source_dir = Path(__file__).parent.absolute() / "StructureDefinitions" / "Source"
        structure_defs = [
            f"StructureDefinitions/Source/{file.name}" for file in source_dir.glob("*.json")
        ]
        return " ".join([f"-ig {def_file}" for def_file in structure_defs])
    except Exception as e:
        print("Error reading StructureDef", e)
        return ""


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


def run_conformance_test(input_file, output_file):
    try:
        script_dir = Path(__file__).parent.absolute()

        structure_defs = get_structure_definitions()
        sushi_resources = get_sushi_resources()

        print("Running conformance testing")
        test_cmd = f"java -jar validator_cli.jar {input_file} -output {output_file} -version 4.0.1 \
            {structure_defs} -ig hl7.fhir.us.carin-bb#2.1.0 {sushi_resources}"
        stdout, stderr = run_command(test_cmd, cwd=script_dir)

        print("Conformance test output:")
        print(stdout)
        if stderr:
            print("Conformance test errors:")
            print(stderr)

        if "error" in stdout.lower() or "error" in stderr.lower():
            print("Try again, ran into errors.")
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
    args = parser.parse_args()

    script_dir = Path(__file__).parent.absolute()

    # When we convert the existing StructureDefinitions to .fsh we won't need this,
    # it'll just generate them with SUSHI.
    structure_defs = get_structure_definitions()

    # Generate Structure Definitions + CodeSystems
    print("Running sushi build")
    stdout, stderr = run_command("sushi build", cwd=script_dir / "sushi")
    print("SUSHI output:")
    print(stdout)
    if stderr:
        print("SUSHI errors:")
        print(stderr)

    sushi_resources = get_sushi_resources()
    print(f"Using SUSHI resources: {sushi_resources}")

    # Compile FML files.
    print("Compiling FML ")
    compiled_map_path = f"StructureMaps/BFD-{Path(args.map).stem}-StructureMap.json"
    compile_cmd = (
        f"java -jar validator_cli.jar -ig {args.map} -compile {args.resource} -version 4.0.1 \
        -output {compiled_map_path}"
    )
    print("Input compilation command was:" + compile_cmd)
    stdout, stderr = run_command(compile_cmd, cwd=script_dir)
    print("Compilation output:")
    print(stdout)
    if stderr:
        print("Compilation errors:")
        print(stderr)

    # Get referenced maps from the source map file
    referenced_maps = get_referenced_maps(compiled_map_path)
    map_imports = " ".join([f"-ig {map_file}" for map_file in referenced_maps])

    print("Executing Transform")
    execute_cmd = f"java -jar validator_cli.jar {args.input} -output {args.output} -transform \
        {args.resource} -version 4.0.1 -ig {compiled_map_path} {structure_defs} \
            -ig hl7.fhir.us.carin-bb#2.1.0 {map_imports} {sushi_resources}"
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
