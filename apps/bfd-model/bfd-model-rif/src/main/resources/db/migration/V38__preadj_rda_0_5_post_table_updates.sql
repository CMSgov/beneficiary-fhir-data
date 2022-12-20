/*
 * Schema updates required for compatibility with RDA API Version 0.5.
 *
 * Changed tables:
 *   McsDiagnosisCodes
 */

/************************** CHANGES **************************/


/*
 * McsDiagnosisCodes
 */

UPDATE "pre_adj"."McsDiagnosisCodes" SET "idrDiagCode"='' WHERE "idrDiagCode" IS NULL;
ALTER TABLE "pre_adj"."McsDiagnosisCodes" ALTER COLUMN "idrDiagCode" SET NOT NULL;
