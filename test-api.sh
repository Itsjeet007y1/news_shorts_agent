#!/bin/bash

# API Testing Guide for News Shorts Agent with GCS
# This script provides curl examples for testing all API endpoints

set -e

# Configuration
BASE_URL="${1:-http://localhost:8080}"
API_PREFIX="/api/news"

echo "========================================="
echo "News Shorts Agent - API Testing Guide"
echo "========================================="
echo "Base URL: $BASE_URL"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test 1: Check connectivity
echo -e "${BLUE}TEST 1: Check Connectivity${NC}"
echo -e "${YELLOW}Endpoint: GET ${API_PREFIX}/testConnectivity${NC}"
curl -X GET "${BASE_URL}${API_PREFIX}/testConnectivity"
echo ""
echo ""

# Test 2: Get all news
echo -e "${BLUE}TEST 2: Get All News${NC}"
echo -e "${YELLOW}Endpoint: GET ${API_PREFIX}${NC}"
curl -X GET "${BASE_URL}${API_PREFIX}" | jq '.'
echo ""
echo ""

# Test 3: Create news with image
echo -e "${BLUE}TEST 3: Create News with Image${NC}"
echo -e "${YELLOW}Endpoint: POST ${API_PREFIX}${NC}"

# Create a sample image (100x100 red PNG)
SAMPLE_IMAGE="/tmp/test_image.jpg"
if [ ! -f "$SAMPLE_IMAGE" ]; then
    # Create a minimal valid JPEG using ImageMagick or fallback
    if command -v convert &> /dev/null; then
        convert -size 100x100 xc:red "$SAMPLE_IMAGE"
        echo -e "${GREEN}Created sample image: $SAMPLE_IMAGE${NC}"
    else
        echo -e "${YELLOW}ImageMagick not found. Using pre-existing image or create one manually.${NC}"
        echo -e "${YELLOW}To create a test image: convert -size 100x100 xc:red /tmp/test_image.jpg${NC}"
    fi
fi

if [ -f "$SAMPLE_IMAGE" ]; then
    echo "Creating news article with image..."
    RESPONSE=$(curl -s -X POST "${BASE_URL}${API_PREFIX}" \
        -F "title=Breaking News Story" \
        -F "content=This is an important news story with an image." \
        -F "description=A brief summary of the breaking news" \
        -F "sourceId=news_source_1" \
        -F "sourceName=News Agency" \
        -F "author=John Doe" \
        -F "category=Technology" \
        -F "language=en" \
        -F "image=@${SAMPLE_IMAGE}")

    echo "$RESPONSE" | jq '.'

    # Extract news ID for further tests
    NEWS_ID=$(echo "$RESPONSE" | jq -r '.id')
    echo -e "${GREEN}Created news with ID: $NEWS_ID${NC}"
else
    echo -e "${RED}Sample image not found. Skipping test.${NC}"
    NEWS_ID=1  # Use default ID for other tests
fi
echo ""
echo ""

# Test 4: Create news without image
echo -e "${BLUE}TEST 4: Create News without Image${NC}"
echo -e "${YELLOW}Endpoint: POST ${API_PREFIX}${NC}"
curl -s -X POST "${BASE_URL}${API_PREFIX}" \
    -F "title=Text Only News" \
    -F "content=News without image" \
    -F "description=Summary without image" \
    -F "category=General" | jq '.'
echo ""
echo ""

# Test 5: Get news by ID
echo -e "${BLUE}TEST 5: Get News by ID${NC}"
echo -e "${YELLOW}Endpoint: GET ${API_PREFIX}/${NEWS_ID}${NC}"
curl -s -X GET "${BASE_URL}${API_PREFIX}/${NEWS_ID}" | jq '.'
echo ""
echo ""

# Test 6: Get news by language
echo -e "${BLUE}TEST 6: Get News by Language${NC}"
echo -e "${YELLOW}Endpoint: GET ${API_PREFIX}?language=en${NC}"
curl -s -X GET "${BASE_URL}${API_PREFIX}?language=en" | jq '.'
echo ""
echo ""

# Test 7: Update news with new image
if [ $NEWS_ID != 1 ]; then
    echo -e "${BLUE}TEST 7: Update News with New Image${NC}"
    echo -e "${YELLOW}Endpoint: PUT ${API_PREFIX}/${NEWS_ID}${NC}"

    if [ -f "$SAMPLE_IMAGE" ]; then
        curl -s -X PUT "${BASE_URL}${API_PREFIX}/${NEWS_ID}" \
            -F "title=Updated News Title" \
            -F "content=Updated content with new information" \
            -F "description=Updated description" \
            -F "category=Technology" \
            -F "image=@${SAMPLE_IMAGE}" | jq '.'
    fi
    echo ""
    echo ""

    # Test 8: Update news without changing image
    echo -e "${BLUE}TEST 8: Update News without Changing Image${NC}"
    echo -e "${YELLOW}Endpoint: PUT ${API_PREFIX}/${NEWS_ID}${NC}"
    curl -s -X PUT "${BASE_URL}${API_PREFIX}/${NEWS_ID}" \
        -F "title=Another Update" \
        -F "content=Updated content without changing image" \
        -F "description=New description" \
        -F "category=Science" | jq '.'
    echo ""
    echo ""
fi

# Test 9: Create news bulk (without images)
echo -e "${BLUE}TEST 9: Create News Bulk${NC}"
echo -e "${YELLOW}Endpoint: POST ${API_PREFIX}/bulk${NC}"
curl -s -X POST "${BASE_URL}${API_PREFIX}/bulk" \
    -H "Content-Type: application/json" \
    -d '[
        {
            "title": "Bulk News 1",
            "content": "Content for bulk news 1",
            "description": "Description 1",
            "category": "Tech",
            "language": "en"
        },
        {
            "title": "Bulk News 2",
            "content": "Content for bulk news 2",
            "description": "Description 2",
            "category": "Science",
            "language": "en"
        }
    ]' | jq '.'
echo ""
echo ""

# Test 10: Delete news
if [ $NEWS_ID != 1 ]; then
    echo -e "${BLUE}TEST 10: Delete News${NC}"
    echo -e "${YELLOW}Endpoint: DELETE ${API_PREFIX}/${NEWS_ID}${NC}"
    curl -s -X DELETE "${BASE_URL}${API_PREFIX}/${NEWS_ID}" -v
    echo ""
    echo ""
fi

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Testing Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"

# Helpful notes
echo ""
echo -e "${BLUE}Notes:${NC}"
echo "1. Images are stored in Google Cloud Storage"
echo "2. Image URLs follow format: https://storage.googleapis.com/news-images-bucket/{uuid}.{ext}"
echo "3. Supported image types: JPEG, PNG, GIF, WebP"
echo "4. Maximum image size: 5 MB"
echo "5. All responses include 'imageUrl' field instead of 'base64Image'"
echo ""
echo -e "${BLUE}Error Codes:${NC}"
echo "- 400: Bad request (missing required fields or invalid image)"
echo "- 404: Resource not found"
echo "- 500: Server error (GCS upload failure)"
echo ""
echo -e "${BLUE}Useful jq filters:${NC}"
echo "  Extract image URL: | jq '.imageUrl'"
echo "  Extract all titles: | jq '.[].title'"
echo "  Pretty print: | jq '.'"
echo "  Count items: | jq 'length'"

