#!/bin/bash
# Create Catch Without Image

curl -X POST http://localhost:4000/api/user_catches \
  -H "Content-Type: application/json" \
  -d '{
    "user_catch": {
      "species": "Bass",
      "location": "Lake Michigan",
      "latitude": 41.8781,
      "longitude": -87.6298,
      "caught_at": "2024-01-15T14:30:00",
      "notes": "Great catch on a sunny day!"
    }
  }' \
  -w "\n"