use crate::config::AppConfig;
use crate::error;
use crate::models::PartDEvent;
use diesel::pg::PgConnection;
use diesel::prelude::*;

pub fn establish_connection(app_config: &AppConfig) -> ConnectionResult<PgConnection> {
    PgConnection::establish(app_config.database_url.as_ref())
}

pub fn claims_by_bene_id_partd(
    db_connection: &PgConnection,
    search_bene_id: &str,
) -> error::Result<Vec<PartDEvent>> {
    use crate::schema_views::claims_partd::dsl::*;
    let results = claims_partd
        .filter(BENE_ID.eq(search_bene_id))
        .load::<PartDEvent>(db_connection)
        .map_err(|err| error::AppError::DieselResultError(err));
    results
}
