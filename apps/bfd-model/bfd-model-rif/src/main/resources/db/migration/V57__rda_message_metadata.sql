CREATE TABLE "pre_adj"."RdaApiClaimMessageMetaData" (
    "metaDataId"     bigint                   NOT NULL PRIMARY KEY,
    "claimType"      char(1)                  NOT NULL,
    "sequenceNumber" bigint                   NOT NULL,
    "claimId"        varchar(25)              NOT NULL,
    "mbiId"          bigint,
    "claimState"     varchar(1),
    "receivedDate"   timestamp with time zone NOT NULL,
    CONSTRAINT "RdaApiClaimMessageMetaData_mbi" FOREIGN KEY ("mbiId") REFERENCES "pre_adj"."MbiCache"("mbiId")
);

/*
 * FIXME For consistency, sequence names should be mixed-case, but can't be, due
 * to https://hibernate.atlassian.net/browse/HHH-9431.
 *
 * We expect batch sizes to be around 25 so the increment shouldn't waste many id values.
 */
create sequence "pre_adj"."rda_api_claim_message_meta_data_meta_data_id_seq" ${logic.sequence-start} 1 ${logic.sequence-increment} 25 cycle;
