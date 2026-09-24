import json
import sys
from pathlib import Path

import requests

MATCHBOX_SERVER = "http://localhost:18080/matchboxv3"


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
            # slight con of not using the validator, need to specify the profile
            # to validate against.
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

        errors = (
            [issue for issue in outcome["issue"] if issue.get("severity") in ["error", "fatal"]]
            if "issue" in outcome
            else []
        )

        if errors:
            print(f"Validation failed with {len(errors)} errors. Note, not all errors are bad:")
            for err in errors:
                print(
                    f" - {err.get('diagnostics', 'N/A')} "
                    f"({err.get('code', 'N/a')}) "
                    f"({err.get('expression', 'N/a')})"
                )
            sys.exit(1)

        print("Yay, you did it! Success")

    except Exception as e:
        print(f"Error running conformance testing: {e}")
        sys.exit(1)


if __name__ == "__main__":
    input = sys.argv[1]
    output = input.replace(".json", "-validator-output.json")
    run_conformance_test(input, output)
