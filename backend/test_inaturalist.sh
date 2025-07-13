#!/bin/bash

echo "Testing iNaturalist Computer Vision API..."
echo "=========================================="

# Test with test_image.jpg
echo "Testing with test_image.jpg:"
curl -X POST "https://api.inaturalist.org/v1/computervision/score_image" \
  -F "image=@test_image.jpg" \
  -H "Accept: application/json"

echo -e "\n\n"

# Test with fish image from uploads
echo "Testing with fish image from uploads:"
curl -X POST "https://api.inaturalist.org/v1/computervision/score_image" \
  -F "image=@priv/static/uploads/catches/4104abd1-f078-4129-9790-cbf7d43545ba-test-fish.png" \
  -H "Accept: application/json"

echo -e "\n\n"

# Test with another fish image
echo "Testing with another fish image:"
curl -X POST "https://api.inaturalist.org/v1/computervision/score_image" \
  -F "image=@priv/static/uploads/catches/7ebca80b-2328-4b79-b4bc-279c36f6b67e-test-fish.png" \
  -H "Accept: application/json"

echo -e "\n\nDone!"