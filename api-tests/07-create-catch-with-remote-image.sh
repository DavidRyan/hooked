#!/bin/bash
# Create Catch With Remote Image
# Downloads image from URL and submits it

IMAGE_URL="https://encrypted-tbn2.gstatic.com/images?q=tbn:ANd9GcSslOX0rjChLi3Mjpqp3kQ4mILk-fG0PUVtEK1GJDPt-_9LJg1OHuKCcgeXpzftPrUJp48Ls9J9tezMIwywVb7LTA"
TEMP_IMAGE="/tmp/test-fish-remote.jpg"

echo "Downloading test image..."
curl -s -o "$TEMP_IMAGE" "$IMAGE_URL"

if [ ! -f "$TEMP_IMAGE" ]; then
  echo "Error: Failed to download image"
  exit 1
fi

echo "Creating catch with downloaded image..."
curl -X POST http://localhost:4000/api/user_catches \
  -F "user_catch[species]=Unknown Fish" \
  -F "user_catch[location]=Test Location" \
  -F "user_catch[latitude]=40.7128" \
  -F "user_catch[longitude]=-74.0060" \
  -F "user_catch[caught_at]=2024-01-15T14:30:00" \
  -F "user_catch[notes]=Test catch with remote image for species identification" \
  -F "image=@$TEMP_IMAGE" \
  -w "\n"

# Cleanup
rm -f "$TEMP_IMAGE"