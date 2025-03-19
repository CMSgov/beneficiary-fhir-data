Placeholder directories + structures for mapping between IDR and FHIR.

To download the FHIR Validator:
https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar

To execute FHIR Mapping language transforms against the sample data.
To compile the StructureMap:
java -jar validator_cli.jar -ig maps/Coverage-Base.map -compile https://bfd.cms.gov/MappingLanguage/maps/CoverageBase -version 4.0.1 -output StructureMaps/bfd_coverage_base_structuremap.json 

To execute:
Fee for Service Sample:
java -jar validator_cli.jar sample-data/CoverageFFSSample.json -output outputs/CoverageFFS.json -transform https://bfd.cms.gov/MappingLanguage/maps/CoverageBase -version 4.0.1 -ig StructureMaps/bfd_coverage_base_structuremap.json -ig StructureDefinitions/Source/CoverageBase.json

Validating sample resources against self-defined StructureDefinitions:
java -jar validator_cli.jar sample-data/<input>.json -ig StructureDefinitions/Source/<applicable structure definition>.json

Validate against C4BB:
java -jar validator_cli.jar outputs/<output_json>.json -ig hl7.fhir.us.carin-bb#2.1.0

EOB example:
java -jar validator_cli.jar outputs/CoverageFFS.json -ig hl7.fhir.us.carin-bb#2.1.0
