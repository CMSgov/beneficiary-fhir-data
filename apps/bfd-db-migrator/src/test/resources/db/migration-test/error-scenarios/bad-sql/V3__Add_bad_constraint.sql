/*
 * Example migration file that is meant to test an error with the sql execution during a migration.
 */

-- Error on this due to trying to add a non-existent/bad constraint --
alter table "Beneficiaries"
   add constraint "HHAClaims_beneficiaryId_to_Beneficiaries" 
   foreign key ("unknown-key")
   references "BeneficiariesDoesNotExist";