# How to Update for RDA API Changes

1. Synch RDA API changes
   1. Clone/pull latest changes from RDA API repo
       - https://github.cms.gov/medicare-payment-modernization/rda-api
   2. Copy changes from RDA API proto files to BFD proto files
        - RDA API: https://github.cms.gov/medicare-payment-modernization/rda-api/tree/main/proto/src/main/proto/rda/v1
        - BFD: https://github.cms.gov/medicare-payment-modernization/rda-api/tree/main/proto/src/main/proto/rda/v1
   3. Update VERSION.txt in BFD repo
        - https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc/src/main/proto/rda/VERSION.txt
2. Update RDA schema/entity mappings
   1. Diff changes between RDA and BFD proto files
   2. Make changes to appropriate YAML mapping files
        - https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-model/bfd-model-rda/mappings
        - **ALL** properties/classes should be captured, even if ultimately not served to end users.
        - New properties get added to existing YAML mapping files
        - New proto files get added as new YAML mapping files
   4. Create migration file for synchronizing schema with mapping changes
        - Migrations: https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-model/bfd-model-rif/src/main/resources/db/migration
        - The [bfd-model-rda](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-model/bfd-model-rda) module can be built to auto-generate a full sql script to aid in migration creation
   5. Run [bfd-db-migrator](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-db-migrator) tests to ensure validity of migration
3. Update [bfd-pipeline-rda-grpc](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc) module source code
   1. Update [RdaService.java](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc/src/main/java/gov/cms/bfd/pipeline/rda/grpc/server/RdaService.java) with with new RDA_PROTO_VERSION
   2. Update the Random*ClaimGenerators for local testing with new columns/entities
   3. Update tests with new data fields, for example:
      - [FissClaimRdaSinkIT](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc/src/test/java/gov/cms/bfd/pipeline/rda/grpc/sink/direct/FissClaimRdaSinkIT.java)
      - [FissClaimStreamCallerIT](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc/src/test/java/gov/cms/bfd/pipeline/rda/grpc/source/FissClaimStreamCallerIT.java)
      - [FissClaimTransformerTest](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc/src/test/java/gov/cms/bfd/pipeline/rda/grpc/source/FissClaimTransformerTest.java)
      - [StandardGrpcRdaSourceIT](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc/src/test/java/gov/cms/bfd/pipeline/rda/grpc/source/StandardGrpcRdaSourceIT.java)
      - etc.
4. Create FHIR mappings for new RDA fields
   1. Pull new fields from RDA data dictionary
   2. Reach out to FHIR SME to research new fields and identify appropriate FHIR mappings
   3. Vet new mappings for what has value to customers, not all fields may get mapped
   4. Create / update Claim / ClaimResponse model structure [in confluence](https://confluence.cms.gov/display/BCDA/PACA+FHIR+Resources)
      - New mapping entries in tables
      - Update Claim / ClaimResponse sample JSON responses for FISS / MCS claim types.
5. Update [bfd-pipeline-rda-bridge](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-pipeline/bfd-pipeline-rda-bridge) module source code
   1. Identify feasible synthetic CCW data values for corresponding new RDA fields
   2. Update transformer with new field mappings
   3. Update tests with new expectations
      - May need to update the test RIF (csv) files with more/better data for new data fields
6. Deploy SBX update
   1. Create new synthetic RDA data using the synthetic CCW data from synthea
      - Synthea release sets are in s3://bfd-public-test-data/data-synthetic/Synthea-Data/ (organized by release date)
      - Directions to run the bridge are in the module [README.md](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-pipeline/bfd-pipeline-rda-bridge)
      - Synthetic RDA messages for PROD-SBX get gzipped and copied to s3://bfd-prod-sbx-etl-577373831711/rda_api_messages/
      - The manifest file gets updated with the new release sets
7. Update FHIR API with new FHIR mappings
   1. Add any new coding required coding systems
      - Try to use existing systems where possible and where it enforces consistency
      - PAC specific systems for FISS/MCS go in [BBCodingSystems](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-server/bfd-server-war/src/main/java/gov/cms/bfd/server/war/commons/BBCodingSystems.java)
   2. Update IT [endpoint responses](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v2) to reflect new expectations