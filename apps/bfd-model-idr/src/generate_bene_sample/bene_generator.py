class SampleGenerator:
    def __init__(self, source_directory: str, output_directory: str):
        self.source_directory = source_directory
        self.output_directory = output_directory

    def run(self, bene_sk: str) -> None:
        print(f"Found run with bene: {bene_sk}")
