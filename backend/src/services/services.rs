use actix_web::{get, web, HttpResponse, Responder};
use serde::Serialize;
use tokio::sync::oneshot::channel;

#[derive(Serialize)]
pub struct Catch {
    id: u64,
    species: String,
    weight: f64,
    length: f64,
    photo_url: String,
}

#[get("/catches")]
async fn get_catches() -> impl Responder {
    let catches = vec![
        Catch {
            id: 1,
            species: "Salmon".to_string(),
            weight: 5.0,
            length: 20.0,
            photo_url: "https://via.placeholder.com/150".to_string(),
        },
        Catch {
            id: 2,
            species: "Tuna".to_string(),
            weight: 10.0,
            length: 30.0,
            photo_url: "https://via.placeholder.com/150".to_string(),
        },
        Catch {
            id: 3,
            species: "Tuna".to_string(),
            weight: 10.0,
            length: 30.0,
            photo_url: "https://via.placeholder.com/150".to_string(),
        },
        Catch {
            id: 4,
            species: "Tuna".to_string(),
            weight: 10.0,
            length: 30.0,
            photo_url: "https://via.placeholder.com/150".to_string(),
        },
        Catch {
            id: 5,
            species: "Tuna".to_string(),
            weight: 10.0,
            length: 30.0,
            photo_url: "https://via.placeholder.com/150".to_string(),
        },
    ];
    HttpResponse::Ok().json(catches)
}

#[get("/catch/{id}")]
async fn get_catch_details(web::Path(id): web::Path<u64>) -> impl Responder {
    let catch_details = Catch {
        id,
        species: "Salmon".to_string(),
        weight: 5.0,
        length: 20.0,
        photo_url: "https://via.placeholder.com/150".to_string(),
    };
    HttpResponse::Ok().json(catch_details)
}
