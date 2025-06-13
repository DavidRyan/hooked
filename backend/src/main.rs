pub mod db;
pub mod enrichment;
pub mod services;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    use actix_web::{App, HttpServer};

    HttpServer::new(|| {
        App::new()
            .service(services::get_catches)
            .service(services::get_catch_details)
    })
    .bind("127.0.0.1:8080")?
    .run()
    .await
}
