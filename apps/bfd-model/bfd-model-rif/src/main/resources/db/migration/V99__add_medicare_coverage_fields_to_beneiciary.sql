-- This script is to add the Medicare Part A Start and End Dates,
-- Part B Start and End Dates, and Part D Start and End Dates to the Beneficiaries table.

ALTER TABLE public.beneficiaries ADD part_a_coverage_start_date date;
ALTER TABLE public.beneficiaries ADD part_a_coverage_end_date date;
ALTER TABLE public.beneficiaries ADD part_b_coverage_start_date date;
ALTER TABLE public.beneficiaries ADD part_b_coverage_end_date date;
ALTER TABLE public.beneficiaries ADD part_d_coverage_start_date date;
ALTER TABLE public.beneficiaries ADD part_d_coverage_end_date date;
