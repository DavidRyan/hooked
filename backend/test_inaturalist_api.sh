#!/bin/bash

# Test script for iNaturalist Computer Vision API
# This tests the same API endpoint that the Hooked app uses for species identification

echo "Testing iNaturalist Computer Vision API..."
echo "=========================================="

# Test with existing test image
if [ -f "test_image.jpg" ]; then
    echo "Testing with test_image.jpg:"
    curl -X POST "https://api.inaturalist.org/v1/computervision/score_image" \
        -F "image=@test_image.jpg" \
        -H "Accept: application/json" \
        --silent --show-error | jq '.'
    echo ""
fi

# Test with any PNG files in uploads
for img in priv/static/uploads/catches/*.png priv/static/uploads/catches/*.jpg; do
    if [ -f "$img" ]; then
        echo "Testing with $(basename "$img"):"
        curl -X POST "https://api.inaturalist.org/v1/computervision/score_image" \
            -F "image=@$img" \
            -H "Accept: application/json" \
            --silent --show-error | jq '.results[0].taxon.name // "No species detected"'
        echo ""
        break  # Just test one image
    fi
done

echo "API Documentation:"
echo "- Endpoint: https://api.inaturalist.org/v1/computervision/score_image"
echo "- Method: POST"
echo "- Content-Type: multipart/form-data"
echo "- Field name: image"
echo "- Response: JSON with results array containing taxon information"