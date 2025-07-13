#!/bin/bash
# Create Catch With Image
# Usage: ./04-create-catch-with-image.sh [/path/to/image.jpg]
# If no image provided, downloads test image from URL

IMAGE_PATH="$1"
REMOTE_IMAGE_URL="https://encrypted-tbn2.gstatic.com/images?q=tbn:ANd9GcSslOX0rjChLi3Mjpqp3kQ4mILk-fG0PUVtEK1GJDPt-_9LJg1OHuKCcgeXpzftPrUJp48Ls9J9tezMIwywVb7LTA"
TEMP_IMAGE="/tmp/test-fish-download.jpg"

if [ -z "$IMAGE_PATH" ]; then
  echo "No image path provided. Downloading test image..."
  curl -s -o "$TEMP_IMAGE" "$REMOTE_IMAGE_URL"
  if [ ! -f "$TEMP_IMAGE" ]; then
    echo "Error: Failed to download test image"
    exit 1
  fi
  IMAGE_PATH="$TEMP_IMAGE"
  echo "Using downloaded image: $IMAGE_PATH"
elif [ ! -f "$IMAGE_PATH" ]; then
  echo "Error: Image file '$IMAGE_PATH' not found"
  echo "Usage: $0 [image_path]"
  echo "Example: $0 /Users/username/Pictures/fish.jpg"
  echo "Or run without arguments to use test image"
  exit 1
fi

curl -X POST http://localhost:4000/api/user_catches \
  -F "user_catch[species]=Bass" \
  -F "user_catch[location]=Lake Michigan" \
  -F "user_catch[latitude]=41.8781" \
  -F "user_catch[longitude]=-87.6298" \
  -F "user_catch[caught_at]=2024-01-15T14:30:00" \
  -F "user_catch[notes]=Great catch with photo!" \
  -F "image=@$IMAGE_PATH" \
  -w "\n"

# Cleanup downloaded image if we created it
if [ "$IMAGE_PATH" = "$TEMP_IMAGE" ]; then
  rm -f "$TEMP_IMAGE"
fi