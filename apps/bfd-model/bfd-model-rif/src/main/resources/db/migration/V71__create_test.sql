${logic.psql-only} /* public schema */
${logic.psql-only} -- create/alter tables, views, sequences, and functions
${logic.psql-only} create table t1(c1 serial, c2 integer);
${logic.psql-only} alter table t1 add constraint uc2 unique(c2);
${logic.psql-only} create sequence q start 1 owned by t1.c1;
${logic.psql-only} create view v1 as select * from t1;
${logic.psql-only} create function inc(i integer) returns integer as $$ begin return i + 1; end; $$ language plpgsql;
${logic.psql-only} alter table beneficiaries add column a1 integer unique;
${logic.psql-only} alter view v1 rename to v2;
${logic.psql-only} alter sequence q restart with 100;
${logic.psql-only} alter function inc(i integer) rename to inc2;
${logic.psql-only} 
${logic.psql-only} /* rda */
${logic.psql-only} create table rda.t1(c1 serial, c2 integer);
${logic.psql-only} alter table rda.t1 add constraint rda_uc2 unique(c2);
${logic.psql-only} create sequence rda.q start 1 owned by rda.t1.c1;
${logic.psql-only} create view rda.v1 as select * from rda.t1;
${logic.psql-only} create function rda.inc(i integer) returns integer as $$ begin return i + 1; end; $$ language plpgsql;
${logic.psql-only} alter table rda.fiss_claims add column a1 integer unique;
${logic.psql-only} alter view rda.v1 rename to v2;
${logic.psql-only} alter sequence rda.q restart with 100;
${logic.psql-only} alter function rda.inc(i integer) rename to inc2;
${logic.psql-only} 
${logic.psql-only} /* new schema */
${logic.psql-only} create schema if not exists foo;
${logic.psql-only} create table foo.t1(c1 serial, c2 integer);
${logic.psql-only} alter table foo.t1 add constraint foo_uc2 unique(c2);
${logic.psql-only} create sequence foo.q start 1 owned by foo.t1.c1;
${logic.psql-only} create view foo.v1 as select * from foo.t1;
${logic.psql-only} create function foo.inc(i integer) returns integer as $$ begin return i + 1; end; $$ language plpgsql;
${logic.psql-only} alter table foo.t1 add column a1 integer unique;
${logic.psql-only} alter view foo.v1 rename to v2;
${logic.psql-only} alter sequence foo.q restart with 100;
${logic.psql-only} alter function foo.inc(i integer) rename to inc2;
