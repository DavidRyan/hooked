#!/bin/bash
# Create Catch With Image
# Usage: ./04-create-catch-with-image.sh /path/to/image.jpg

if [ -z "$1" ]; then
  echo "Usage: $0 <image_path>"
  echo "Example: $0 /Users/username/Pictures/fish.jpg"
  exit 1
fi

if [ ! -f "$1" ]; then
  echo "Error: Image file '$1' not found"
  exit 1
fi

curl -X POST http://localhost:4000/api/user_catches \
  -F "user_catch[species]=Bass" \
  -F "user_catch[location]=Lake Michigan" \
  -F "user_catch[latitude]=41.8781" \
  -F "user_catch[longitude]=-87.6298" \
  -F "user_catch[caught_at]=2024-01-15T14:30:00" \
  -F "user_catch[notes]=Great catch with photo!" \
  -F "image=@$1" \
  -w "\n"