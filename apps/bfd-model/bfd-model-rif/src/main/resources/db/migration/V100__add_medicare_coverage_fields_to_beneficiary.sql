-- This script is to add the Medicare Part A Start and End Dates,
-- Part B Start and End Dates, and Part D Start and End Dates to the Beneficiaries table.

ALTER TABLE public.beneficiaries ADD pta_cvrg_strt_dt date;
ALTER TABLE public.beneficiaries ADD pta_cvrg_end_dt date;
ALTER TABLE public.beneficiaries ADD ptb_cvrg_strt_dt date;
ALTER TABLE public.beneficiaries ADD ptb_cvrg_end_dt date;
ALTER TABLE public.beneficiaries ADD ptd_cvrg_strt_dt date;
ALTER TABLE public.beneficiaries ADD ptd_cvrg_end_dt date;
