import click

from .gen_def import StructureDefinitionGenerator


@click.command
@click.option(
    "--source-directory",
    type=str,
    required=False,
    help="Pass the source directory containing the data dictionary files.",
    default="dictionary-support-files",
)
@click.option(
    "--output-directory",
    type=str,
    required=False,
    help="Pass the output directory for the result files.",
    default="structure-definition",
)
@click.option(
    "--fsh-directory",
    type=str,
    required=False,
    help="Pass the fsh source directory for the result files.",
    default="./sushi/fsh-generated/resources",
)
def main(source_directory: str, output_directory: str, fsh_directory: str) -> None:
    pharmacy_generator = StructureDefinitionGenerator(
        source_directory=source_directory,
        output_directory=output_directory,
        url="https://bluebutton.cms.gov/fhir/StructureDefinition/CMS-ExplanationOfBenefit-Pharmacy",
        profile="CMS",
        source_yaml="ExplanationOfBenefit-Pharmacy.yaml",
        version="2.2.0",
        name="ExplanationOfBenefit-Pharmacy-CMS",
        title="ExplanationOfBenefit Pharmacy CMS",
        description="""This is the CMS Pharmacy Profile.""",
        fsh_directory=fsh_directory,
        base_definition="https://hl7.org/fhir/us/carin-bb/STU2.2/en/StructureDefinition-C4BB-ExplanationOfBenefit-Pharmacy",
    )
    pharmacy_generator.run()


if __name__ == "__main__":
    main()
