/*
 * Schema updates required for compatibility with RDA API Version 0.5.
 *
 * New tables:
 *   FissAuditTrails
 *   McsAdjustments
 *   McsAudits
 *   McsLocations
 *
 * Changed tables:
 *   FissClaims
 *   McsClaims
 *   McsDetails
 */

/************************** CREATES **************************/

/*
 * FissAuditTrails
 */
CREATE TABLE "pre_adj"."FissAuditTrails" (
    "dcn" varchar(23) NOT NULL,
    "priority" smallint NOT NULL,
    "lastUpdated" timestamp with time zone,
    "badtStatus" varchar(1),
    "badtLoc" varchar(5),
    "badtOperId" varchar(9),
    "badtReas" varchar(5),
    "badtCurrDate" date,
    CONSTRAINT "FissAuditTrails_key" PRIMARY KEY ("dcn", "priority"),
    CONSTRAINT "FissAuditTrails_parent" FOREIGN KEY ("dcn") REFERENCES "pre_adj"."FissClaims"("dcn")
);

/*
 * McsAdjustments
 */
CREATE TABLE "pre_adj"."McsAdjustments" (
    "idrClmHdIcn" varchar(15) NOT NULL,
    "priority" smallint NOT NULL,
    "lastUpdated" timestamp with time zone,
    "idrAdjDate" date,
    "idrXrefIcn" varchar(15),
    "idrAdjClerk" varchar(4),
    "idrInitCcn" varchar(15),
    "idrAdjChkWrtDt" date,
    "idrAdjBEombAmt" decimal(7,2),
    "idrAdjPEombAmt" decimal(7,2),
    CONSTRAINT "McsAdjustments_key" PRIMARY KEY ("idrClmHdIcn", "priority"),
    CONSTRAINT "McsAdjustments_parent" FOREIGN KEY ("idrClmHdIcn") REFERENCES "pre_adj"."McsClaims"("idrClmHdIcn")
);

/*
 * McsAudits
 */
CREATE TABLE "pre_adj"."McsAudits" (
    "idrClmHdIcn" varchar(15) NOT NULL,
    "priority" smallint NOT NULL,
    "lastUpdated" timestamp with time zone,
    "idrJAuditNum" int,
    "idrJAuditInd" varchar(1),
    "idrJAuditDisp" varchar(1),
    CONSTRAINT "McsAudits_key" PRIMARY KEY ("idrClmHdIcn", "priority"),
    CONSTRAINT "McsAudits_parent" FOREIGN KEY ("idrClmHdIcn") REFERENCES "pre_adj"."McsClaims"("idrClmHdIcn")
);

/*
 * McsLocations
 */
CREATE TABLE "pre_adj"."McsLocations" (
    "idrClmHdIcn" varchar(15) NOT NULL,
    "priority" smallint NOT NULL,
    "lastUpdated" timestamp with time zone,
    "idrLocClerk" varchar(4),
    "idrLocCode" varchar(3),
    "idrLocDate" date,
    "idrLocActvCode" varchar(1),
    CONSTRAINT "McsLocations_key" PRIMARY KEY ("idrClmHdIcn", "priority"),
    CONSTRAINT "McsLocations_parent" FOREIGN KEY ("idrClmHdIcn") REFERENCES "pre_adj"."McsClaims"("idrClmHdIcn")
);

/************************** CHANGES **************************/

/*
 * FissClaims
 */
ALTER TABLE "pre_adj"."FissClaims" ADD "rejectCd" varchar(5);
ALTER TABLE "pre_adj"."FissClaims" ADD "fullPartDenInd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "nonPayInd" varchar(2);
ALTER TABLE "pre_adj"."FissClaims" ADD "xrefDcnNbr" varchar(23);
ALTER TABLE "pre_adj"."FissClaims" ADD "adjReqCd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "adjReasCd" varchar(2);
ALTER TABLE "pre_adj"."FissClaims" ADD "cancelXrefDcn" varchar(23);
ALTER TABLE "pre_adj"."FissClaims" ADD "cancelDate" date;
ALTER TABLE "pre_adj"."FissClaims" ADD "cancAdjCd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "originalXrefDcn" varchar(23);
ALTER TABLE "pre_adj"."FissClaims" ADD "paidDt" date;
ALTER TABLE "pre_adj"."FissClaims" ADD "admDate" date;
ALTER TABLE "pre_adj"."FissClaims" ADD "admSource" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "primaryPayerCode" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "attendPhysId" varchar(16);
ALTER TABLE "pre_adj"."FissClaims" ADD "attendPhysLname" varchar(17);
ALTER TABLE "pre_adj"."FissClaims" ADD "attendPhysFname" varchar(18);
ALTER TABLE "pre_adj"."FissClaims" ADD "attendPhysMint" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "attendPhysFlag" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "operatingPhysId" varchar(16);
ALTER TABLE "pre_adj"."FissClaims" ADD "operPhysLname" varchar(17);
ALTER TABLE "pre_adj"."FissClaims" ADD "operPhysFname" varchar(18);
ALTER TABLE "pre_adj"."FissClaims" ADD "operPhysMint" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "operPhysFlag" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "othPhysId" varchar(16);
ALTER TABLE "pre_adj"."FissClaims" ADD "othPhysLname" varchar(17);
ALTER TABLE "pre_adj"."FissClaims" ADD "othPhysFname" varchar(18);
ALTER TABLE "pre_adj"."FissClaims" ADD "othPhysMint" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "othPhysFlag" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "xrefHicNbr" varchar(12);
ALTER TABLE "pre_adj"."FissClaims" ADD "procNewHicInd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "newHic" varchar(12);
ALTER TABLE "pre_adj"."FissClaims" ADD "reposInd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "reposHic" varchar(12);
ALTER TABLE "pre_adj"."FissClaims" ADD "mbiSubmBeneInd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "adjMbiInd" varchar(1);
ALTER TABLE "pre_adj"."FissClaims" ADD "adjMbi" varchar(11);
ALTER TABLE "pre_adj"."FissClaims" ADD "medicalRecordNo" varchar(17);

/*
 * FissDiagnosisCodes
 */
ALTER TABLE "pre_adj"."FissDiagnosisCodes" ALTER COLUMN "diagPoaInd"  varchar(1) NULL;

/*
 * McsClaims
 */
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAssignment" varchar(1);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrClmLevelInd" varchar(1);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrHdrAudit" int;
ALTER TABLE "pre_adj"."McsClaims" ADD "idrHdrAuditInd" varchar(1);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrUSplitReason" varchar(1);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrJReferringProvNpi" varchar(10);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrJFacProvNpi" varchar(10);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrUDemoProvNpi" varchar(10);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrUSuperNpi" varchar(10);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrUFcadjBilNpi" varchar(10);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbPickupAddresLine1" varchar(25);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbPickupAddresLine2" varchar(20);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbPickupCity" varchar(20);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbPickupState" varchar(2);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbPickupZipcode" varchar(9);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbDropoffName" varchar(24);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbDropoffAddrLine1" varchar(25);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbDropoffAddrLine2" varchar(20);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbDropoffCity" varchar(20);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbDropoffState" varchar(2);
ALTER TABLE "pre_adj"."McsClaims" ADD "idrAmbDropoffZipcode" varchar(9);

/*
 * McsDetails
 */
ALTER TABLE "pre_adj"."McsDetails" ADD "idrTos" varchar(1);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrTwoDigitPos" varchar(2);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlRendType" varchar(2);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlRendSpec" varchar(2);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlRendNpi" varchar(10);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlRendProv" varchar(10);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrKDtlFacProvNpi" varchar(10);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbPickupAddres1" varchar(25);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbPickupAddres2" varchar(20);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbPickupCity" varchar(20);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbPickupState" varchar(2);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbPickupZipcode" varchar(9);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbDropoffName" varchar(24);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbDropoffAddrL1" varchar(25);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbDropoffAddrL2" varchar(20);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbDropoffCity" varchar(20);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbDropoffState" varchar(2);
ALTER TABLE "pre_adj"."McsDetails" ADD "idrDtlAmbDropoffZipcode" varchar(9);
