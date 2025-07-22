#!/bin/bash
# Create Catch With Image Only (no other required fields)
# Usage: ./08-create-catch-image-only.sh [/path/to/image.jpg]
# If no image provided, uses local test image

IMAGE_PATH="$1"
LOCAL_TEST_IMAGE="test-fish.png"

if [ -z "$IMAGE_PATH" ]; then
  if [ -f "$LOCAL_TEST_IMAGE" ]; then
    IMAGE_PATH="$LOCAL_TEST_IMAGE"
    echo "Using local test image: $IMAGE_PATH"
  else
    echo "Error: No image provided and local test image '$LOCAL_TEST_IMAGE' not found"
    echo "Usage: $0 [image_path]"
    echo "Example: $0 /Users/username/Pictures/fish.jpg"
    exit 1
  fi
elif [ ! -f "$IMAGE_PATH" ]; then
  echo "Error: Image file '$IMAGE_PATH' not found"
  echo "Usage: $0 [image_path]"
  echo "Example: $0 /Users/username/Pictures/fish.jpg"
  exit 1
fi

echo "Testing catch creation with image only (no other required fields)..."
curl -X POST http://localhost:4000/api/user_catches \
  -F "image=@$IMAGE_PATH" \
  -w "\n"