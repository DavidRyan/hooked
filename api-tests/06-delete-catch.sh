#!/bin/bash
# Delete Catch
# Usage: ./06-delete-catch.sh {catch_id}

if [ -z "$1" ]; then
  echo "Usage: $0 <catch_id>"
  echo "Example: $0 123e4567-e89b-12d3-a456-426614174000"
  exit 1
fi

curl -X DELETE http://localhost:4000/api/user_catches/$1 \
  -w "\n"