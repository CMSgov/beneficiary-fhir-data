${logic.psql-only} /* public schema */
${logic.psql-only} -- drop colums, constraints, tables, views, sequences
${logic.psql-only} alter table beneficiaries drop column a1;
${logic.psql-only} drop view v2;
${logic.psql-only} alter table t1 drop constraint uc2;
${logic.psql-only} alter table t1 drop column c2;
${logic.psql-only} drop sequence q cascade;
${logic.psql-only} drop function inc2(i integer);
${logic.psql-only} drop table t1;
${logic.psql-only} 
${logic.psql-only} /* rda */
${logic.psql-only} alter table rda.fiss_claims drop column a1;
${logic.psql-only} drop view rda.v2;
${logic.psql-only} alter table rda.t1 drop constraint uc2;
${logic.psql-only} alter table rda.t1 drop column c2;
${logic.psql-only} drop sequence rda.q cascade;
${logic.psql-only} drop function rda.inc2(i integer);
${logic.psql-only} drop table rda.t1;
${logic.psql-only} 
${logic.psql-only} /* new schema */
${logic.psql-only} drop view foo.v2;
${logic.psql-only} alter table foo.t1 drop constraint uc2;
${logic.psql-only} alter table foo.t1 drop column c2;
${logic.psql-only} drop sequence foo.q cascade;
${logic.psql-only} drop function foo.inc2(i integer);
${logic.psql-only} alter table foo.t1 drop column a1;
${logic.psql-only} drop table foo.t1;
${logic.psql-only} drop schema foo;
