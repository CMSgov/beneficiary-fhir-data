/*
 * Add column used to track version of server that produced each claim.
 */
alter table "pre_adj"."FissClaims" add "apiSource" varchar(64);
alter table "pre_adj"."McsClaims" add "apiSource" varchar(64);
