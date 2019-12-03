use crate::config::AppConfig;
use crate::error;
use crate::models::structs::PartDEvent;
use actix_web::web;
use diesel::pg::PgConnection;
use diesel::prelude::*;
use diesel::r2d2::{ConnectionManager, Pool, PooledConnection};

pub type PgPool = Pool<ConnectionManager<PgConnection>>;

/// Note: think I'm going to need this eventually, so leaving it for now
#[allow(dead_code)]
pub type PgPooledConnection = PooledConnection<ConnectionManager<PgConnection>>;

pub fn create_db_connection_pool(app_config: &AppConfig) -> error::Result<PgPool> {
    let database_url: &str = app_config.database_url.as_ref();
    let manager = ConnectionManager::<PgConnection>::new(database_url);
    Pool::builder()
        .max_size(app_config.database_pool_size)
        .build(manager)
        .map_err(|err| error::AppError::DieselPoolError(err))
}

pub fn claims_partd_by_bene_id(
    db_pool: web::Data<PgPool>,
    search_bene_id: &str,
) -> error::Result<Vec<PartDEvent>> {
    use crate::schema_views::claims_partd::dsl::*;
    let db_connection = db_pool
        .get()
        .map_err(|err| error::AppError::DieselPoolError(err))?;
    let results = claims_partd
        .filter(bene_id.eq(search_bene_id))
        .load::<PartDEvent>(&db_connection)
        .map_err(|err| error::AppError::DieselResultError(err));
    results
}
