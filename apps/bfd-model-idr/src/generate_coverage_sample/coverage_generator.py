from typing import Any


class Result:
    def __init__(self, result_json: dict[str, Any], output_file: str):
        self.result_json = result_json
        self.output_file = output_file

    result_json: dict[str, Any]
    output_file: str


class SampleGenerator:
    def __init__(self, source_directory: str, output_directory: str):
        self.source_directory = source_directory
        self.output_directory = output_directory

    def run(self) -> None:
        """Generate Coverage sample JSON from the given input directory."""
        print(f"Source CSV Directory: {self.source_directory}")
        print(f"Output Directory: {self.output_directory}")
