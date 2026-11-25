import click

from gen_structure_def.gen_def import StructureDefinitionGenerator

EOB_YAML: str = "ExplanationOfBenefit.yaml"


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
    "--delete-temp-files",
    type=bool,
    required=False,
    help="Allows for temp files to persist for debugging.",
    default=True,
)
@click.option(
    "--type",
    type=str,
    required=False,
    help="Type to run.",
    default=None,
)
def main(
    source_directory: str, output_directory: str, delete_temp_files: bool, type: str | None
) -> None:

    if not type or type.lower() == "pharmacy":
        pharmacy_generator: StructureDefinitionGenerator = StructureDefinitionGenerator(
            source_directory=source_directory,
            output_directory=output_directory,
            url="https://bluebutton.cms.gov/fhir/StructureDefinition/CMS-ExplanationOfBenefit-Pharmacy",
            profile="CMS",
            source_yaml="ExplanationOfBenefit-Pharmacy.yaml",
            version="2.2.0",
            name="ExplanationOfBenefit-Pharmacy-CMS",
            title="ExplanationOfBenefit Pharmacy CMS",
            description="""This is the CMS Pharmacy Profile.""",
            base_definition="https://hl7.org/fhir/us/carin-bb/STU2.2/en/StructureDefinition-C4BB-ExplanationOfBenefit-Pharmacy",
            resource_type="ExplanationOfBenefit",
            output_file_name="CMS-ExplanationOfBenefit-Pharmacy.json",
            applies_to=["Pharmacy"],
        )
        pharmacy_generator.delete_tmp_files = delete_temp_files
        pharmacy_generator.run()

    if not type or type.lower() == "inpatient":
        inpatient_generator: StructureDefinitionGenerator = StructureDefinitionGenerator(
            source_directory=source_directory,
            output_directory=output_directory,
            url="https://bluebutton.cms.gov/fhir/StructureDefinition/CMS-ExplanationOfBenefit-Inpatient-Institutional",
            profile="CMS",
            source_yaml=EOB_YAML,
            version="2.2.0",
            name="ExplanationOfBenefit-Inpatient-CMS",
            title="ExplanationOfBenefit Inpatient Institutional CMS",
            description="""This is the CMS Inpatient Institutional Profile.""",
            base_definition="https://build.fhir.org/ig/HL7/carin-bb/en/StructureDefinition-C4BB-ExplanationOfBenefit-Inpatient-Institutional",
            resource_type="ExplanationOfBenefit",
            output_file_name="CMS-ExplanationOfBenefit-Inpatient-Institutional.json",
            applies_to=["Inpatient"],
        )
        inpatient_generator.delete_tmp_files = delete_temp_files
        inpatient_generator.sources = ["NCH", "FISS"]
        inpatient_generator.run()

    if not type or type.lower() == "outpatient":
        outpatient_generator: StructureDefinitionGenerator = StructureDefinitionGenerator(
            source_directory=source_directory,
            output_directory=output_directory,
            url="https://bluebutton.cms.gov/fhir/StructureDefinition/CMS-ExplanationOfBenefit-Outpatient-Institutional",
            profile="CMS",
            source_yaml=EOB_YAML,
            version="2.2.0",
            name="ExplanationOfBenefit-Outpatient-CMS",
            title="ExplanationOfBenefit Outpatient Institutional CMS",
            description="""This is the CMS Outpatient Institutional Profile.""",
            base_definition="https://build.fhir.org/ig/HL7/carin-bb/en/StructureDefinition-C4BB-ExplanationOfBenefit-Outpatient-Institutional",
            resource_type="ExplanationOfBenefit",
            output_file_name="CMS-ExplanationOfBenefit-Outpatient-Institutional.json",
            applies_to=["Outpatient"],
        )
        outpatient_generator.delete_tmp_files = delete_temp_files
        outpatient_generator.sources = ["NCH", "FISS"]
        outpatient_generator.run()

    if not type or type.lower() == "outpatient-non-clinician":
        profession_non_clin_generator: StructureDefinitionGenerator = StructureDefinitionGenerator(
            source_directory=source_directory,
            output_directory=output_directory,
            url="https://bluebutton.cms.gov/fhir/StructureDefinition/CMS-ExplanationOfBenefit-Professional-NonClinician",
            profile="CMS",
            source_yaml=EOB_YAML,
            version="2.2.0",
            name="ExplanationOfBenefit-Professional-NonClinician-CMS",
            title="ExplanationOfBenefit Professional Non Clinician CMS",
            description="""This is the CMS Professional Non Clinician Profile.""",
            base_definition="https://build.fhir.org/ig/HL7/carin-bb/en/StructureDefinition-C4BB-ExplanationOfBenefit-Professional-NonClinician",
            resource_type="ExplanationOfBenefit",
            output_file_name="CMS-ExplanationOfBenefit-Professional-NonClinician.json",
            applies_to=["Carrier", "DME"],
        )
        profession_non_clin_generator.delete_tmp_files = delete_temp_files
        profession_non_clin_generator.sources = ["NCH", "MCS", "VMS"]
        profession_non_clin_generator.run()


if __name__ == "__main__":
    main()
