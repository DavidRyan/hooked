#!/bin/bash
# Update Catch
# Usage: ./05-update-catch.sh {catch_id}

if [ -z "$1" ]; then
  echo "Usage: $0 <catch_id>"
  echo "Example: $0 123e4567-e89b-12d3-a456-426614174000"
  exit 1
fi

curl -X PUT http://localhost:4000/api/user_catches/$1 \
  -H "Content-Type: application/json" \
  -d '{
    "user_catch": {
      "species": "Large Mouth Bass",
      "notes": "Updated notes - even better catch!"
    }
  }' \
  -w "\n"