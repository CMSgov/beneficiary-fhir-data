ALTER TABLE idr.prior_auth
ADD COLUMN bfd_order_refer_careteam_name character varying(135),
ADD COLUMN bfd_order_refer_npi_type      integer,
ADD COLUMN bfd_render_careteam_name      character varying(135),
ADD COLUMN bfd_render_npi_type           integer,
ADD COLUMN bfd_att_phy_careteam_name     character varying(135),
ADD COLUMN bfd_att_phy_npi_type          integer;