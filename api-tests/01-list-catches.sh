#!/bin/bash
# List All Catches
curl -X GET http://localhost:4000/api/user_catches \
  -H "Content-Type: application/json" \
  -w "\n"