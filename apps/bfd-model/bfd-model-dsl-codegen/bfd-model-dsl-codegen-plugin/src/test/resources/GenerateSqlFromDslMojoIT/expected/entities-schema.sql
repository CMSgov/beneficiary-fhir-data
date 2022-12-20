/*
 ************************** WARNING **************************
 * SQL code in this file is intended to serve as a starting point for creating migration files.
 * It is should be reviewed manually for performance/optimization when applied to specific use cases.
 */

/************************** CREATES **************************/


/*
 * FissClaims
 */
CREATE TABLE "pre_adj"."FissClaims" (
    "dcn" varchar(23) NOT NULL,
    "sequenceNumber" bigint NOT NULL,
    "currStatus" varchar(1) NOT NULL,
    "provStateCd" varchar(2),
    "medaProvId" varchar(13),
    "medaProv_6" varchar(6),
    "totalChargeAmount" decimal(11,2),
    "receivedDate" date,
    "lastUpdated" timestamp with time zone,
    "pracLocAddr1" varchar(max),
    "lobCd" varchar(1),
    "servTypeCdMapping" varchar(20),
    "servTypeCd" varchar(1),
    "freqCd" varchar(1),
    "apiSource" varchar(24),
    "paidDt" date,
    "admSource" char(1),
    CONSTRAINT "FissClaims_key" PRIMARY KEY ("dcn")
);

/*
 * FissPayers
 */
CREATE TABLE "pre_adj"."FissPayers" (
    "dcn" varchar(23) NOT NULL,
    "priority" smallint NOT NULL,
    "payerType" varchar(20),
    "payersId" varchar(1),
    "estAmtDue" decimal(11,2),
    "beneRel" varchar(2),
    "insuredName" varchar(25),
    "insuredSex" varchar(1),
    "insuredRelX12" varchar(2),
    "insuredDob" date,
    "insuredDobText" varchar(9),
    "lastUpdated" timestamp with time zone,
    CONSTRAINT "FissPayers_key" PRIMARY KEY ("dcn", "priority"),
    CONSTRAINT "FissPayers_parent" FOREIGN KEY ("dcn") REFERENCES "pre_adj"."FissClaims"("dcn")
);

/*
 * FissProcCodes
 */
CREATE TABLE "pre_adj"."FissProcCodes" (
    "dcn" varchar(23) NOT NULL,
    "priority" smallint NOT NULL,
    "procCode" varchar(10) NOT NULL,
    "procFlag" varchar(4),
    "procDate" date,
    "lastUpdated" timestamp with time zone,
    CONSTRAINT "FissProcCodes_key" PRIMARY KEY ("dcn", "priority"),
    CONSTRAINT "FissProcCodes_parent" FOREIGN KEY ("dcn") REFERENCES "pre_adj"."FissClaims"("dcn")
);

/*
 * MbiCache
 */
CREATE TABLE rda.MbiCache (
    mbiId bigint NOT NULL,
    mbi varchar(11) NOT NULL,
    hash varchar(64) NOT NULL,
    oldHash varchar(64),
    lastUpdated timestamp with time zone NOT NULL,
    CONSTRAINT MbiCache_key PRIMARY KEY (mbiId)
);


/************************** ADDS **************************/


/*
 * FissClaims
 */
ALTER TABLE "pre_adj"."FissClaims" ADD "dcn" varchar(23) NOT NULL;
ALTER TABLE "pre_adj"."FissClaims" ADD "sequenceNumber" bigint NOT NULL;
ALTER TABLE "pre_adj"."FissClaims" ADD "currStatus" varchar(1) NOT NULL;
ALTER TABLE "pre_adj"."FissClaims" ADD "provStateCd" varchar(2);
ALTER TABLE "pre_adj"."FissClaims" ADD "medaProvId" varchar(13);
ALTER TABLE "pre_adj"."FissClaims" ADD "medaProv_6" varchar(6);
ALTER TABLE "pre_adj"."FissClaims" ADD "totalChargeAmount" decimal(11,2);
ALTER TABLE "pre_adj"."FissClaims" ADD "receivedDate" date;
ALTER TABLE "pre_adj"."FissClaims" ADD "lastUpdated" timestamp with time zone;
ALTER TABLE "pre_adj"."FissClaims" ADD "pracLocAddr1" varchar(max);
ALTER TABLE "pre_adj"."FissClaims" ADD "lobCd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "servTypeCdMapping" varchar(20);
ALTER TABLE "pre_adj"."FissClaims" ADD "servTypeCd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "freqCd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "apiSource" varchar(24);
ALTER TABLE "pre_adj"."FissClaims" ADD "paidDt" date;
ALTER TABLE "pre_adj"."FissClaims" ADD "admSource" char(1);

/*
 * FissPayers
 */
ALTER TABLE "pre_adj"."FissPayers" ADD "dcn" varchar(23) NOT NULL;
ALTER TABLE "pre_adj"."FissPayers" ADD "priority" smallint NOT NULL;
ALTER TABLE "pre_adj"."FissPayers" ADD "payerType" varchar(20);
ALTER TABLE "pre_adj"."FissPayers" ADD "payersId" varchar(1);
ALTER TABLE "pre_adj"."FissPayers" ADD "estAmtDue" decimal(11,2);
ALTER TABLE "pre_adj"."FissPayers" ADD "beneRel" varchar(2);
ALTER TABLE "pre_adj"."FissPayers" ADD "insuredName" varchar(25);
ALTER TABLE "pre_adj"."FissPayers" ADD "insuredSex" varchar(1);
ALTER TABLE "pre_adj"."FissPayers" ADD "insuredRelX12" varchar(2);
ALTER TABLE "pre_adj"."FissPayers" ADD "insuredDob" date;
ALTER TABLE "pre_adj"."FissPayers" ADD "insuredDobText" varchar(9);
ALTER TABLE "pre_adj"."FissPayers" ADD "lastUpdated" timestamp with time zone;

/*
 * FissProcCodes
 */
ALTER TABLE "pre_adj"."FissProcCodes" ADD "dcn" varchar(23) NOT NULL;
ALTER TABLE "pre_adj"."FissProcCodes" ADD "priority" smallint NOT NULL;
ALTER TABLE "pre_adj"."FissProcCodes" ADD "procCode" varchar(10) NOT NULL;
ALTER TABLE "pre_adj"."FissProcCodes" ADD "procFlag" varchar(4);
ALTER TABLE "pre_adj"."FissProcCodes" ADD "procDate" date;
ALTER TABLE "pre_adj"."FissProcCodes" ADD "lastUpdated" timestamp with time zone;

/*
 * MbiCache
 */
ALTER TABLE rda.MbiCache ADD mbiId bigint NOT NULL;
ALTER TABLE rda.MbiCache ADD mbi varchar(11) NOT NULL;
ALTER TABLE rda.MbiCache ADD hash varchar(64) NOT NULL;
ALTER TABLE rda.MbiCache ADD oldHash varchar(64);
ALTER TABLE rda.MbiCache ADD lastUpdated timestamp with time zone NOT NULL;
