/*
 * Drops all hfj objects
 */

-- DROP hfj_* TABLES FROM PROD------------------------------
 DROP TABLE IF EXISTS public.hfj_spidx_token CASCADE;
 DROP TABLE IF EXISTS public.hfj_spidx_uri CASCADE;
 DROP TABLE IF EXISTS public.hfj_spidx_string CASCADE;
 DROP TABLE IF EXISTS public.hfj_subscription CASCADE;
 DROP TABLE IF EXISTS public.hfj_spidx_number CASCADE;
 DROP TABLE IF EXISTS public.hfj_search_result CASCADE;
 DROP TABLE IF EXISTS public.hfj_spidx_quantity CASCADE;
 DROP TABLE IF EXISTS public.hfj_res_ver CASCADE;
 DROP TABLE IF EXISTS public.hfj_subscription_flag_res CASCADE;
 DROP TABLE IF EXISTS public.hfj_spidx_coords CASCADE;
 DROP TABLE IF EXISTS public.hfj_res_link CASCADE;
 DROP TABLE IF EXISTS public.hfj_res_tag CASCADE;
 DROP TABLE IF EXISTS public.hfj_spidx_date CASCADE;
 DROP TABLE IF EXISTS public.hfj_search_include CASCADE;
 DROP TABLE IF EXISTS public.hfj_search CASCADE;
 DROP TABLE IF EXISTS public.hfj_resource CASCADE;
 DROP TABLE IF EXISTS public.hfj_forced_id CASCADE;
 DROP TABLE IF EXISTS public.hfj_history_tag CASCADE;
 DROP TABLE IF EXISTS public.hfj_tag_def CASCADE;
 
 -- DROP RELATED SEQUENCES --------------------------
 DROP SEQUENCE IF EXISTS seq_codesystem_pid CASCADE;
 DROP SEQUENCE IF EXISTS seq_codesystemver_pid CASCADE;
 DROP SEQUENCE IF EXISTS seq_concept_pc_pid CASCADE;
 DROP SEQUENCE IF EXISTS seq_concept_pid CASCADE;
 DROP SEQUENCE IF EXISTS seq_resource_history_id CASCADE;
 DROP SEQUENCE IF EXISTS seq_resource_id CASCADE;
 DROP SEQUENCE IF EXISTS seq_search CASCADE;
 DROP SEQUENCE IF EXISTS seq_search_inc CASCADE;
 DROP SEQUENCE IF EXISTS seq_search_res CASCADE;
 DROP SEQUENCE IF EXISTS seq_spidx_coords CASCADE;
 DROP SEQUENCE IF EXISTS seq_spidx_date CASCADE;
 DROP SEQUENCE IF EXISTS seq_spidx_number CASCADE;
 DROP SEQUENCE IF EXISTS seq_spidx_quantity CASCADE;
 DROP SEQUENCE IF EXISTS seq_spidx_string CASCADE;
 DROP SEQUENCE IF EXISTS seq_spidx_token CASCADE;
 DROP SEQUENCE IF EXISTS seq_spidx_uri CASCADE;