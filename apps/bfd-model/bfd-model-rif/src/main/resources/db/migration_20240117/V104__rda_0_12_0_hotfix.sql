-- Hot fix for the 0.12 RDA API update, remove not null constraint on non_bill_rev_code

alter table rda.fiss_revenue_lines alter column non_bill_rev_code drop not null;
