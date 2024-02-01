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

create sequence "pre_adj"."rda_api_claim_message_meta_data_meta_data_id_seq" as bigint ${logic.sequence-start} 1 ${logic.sequence-increment} 25 no cycle;
