CREATE INDEX ON idr.prior_auth (utn) WHERE utn NOT LIKE '-%';
CREATE INDEX ON idr.prior_auth_item (utn) WHERE utn NOT LIKE '-%';
