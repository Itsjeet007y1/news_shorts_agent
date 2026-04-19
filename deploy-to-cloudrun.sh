#!/bin/bash

# Cloud Run Deployment Script for News Shorts Agent
# This script automates the deployment of the refactored application to Google Cloud Run

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== News Shorts Agent - Cloud Run Deployment ===${NC}"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI is not installed. Please install it first.${NC}"
    exit 1
fi

# Get project ID
PROJECT_ID=$(gcloud config get-value project)
if [ -z "$PROJECT_ID" ]; then
    echo -e "${RED}Error: No Google Cloud project set. Please run: gcloud config set project PROJECT_ID${NC}"
    exit 1
fi

REGION=${1:-us-central1}
BUCKET_NAME="news-images-bucket"
SERVICE_NAME="news-shorts-agent"
IMAGE_NAME="gcr.io/${PROJECT_ID}/${SERVICE_NAME}"

echo -e "${GREEN}Project ID: ${PROJECT_ID}${NC}"
echo -e "${GREEN}Region: ${REGION}${NC}"
echo -e "${GREEN}GCS Bucket: ${BUCKET_NAME}${NC}"

# Step 1: Enable required APIs
echo -e "\n${YELLOW}Step 1: Enabling required Google Cloud APIs...${NC}"
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable storage-api.googleapis.com
echo -e "${GREEN}✓ APIs enabled${NC}"

# Step 2: Create GCS bucket if it doesn't exist
echo -e "\n${YELLOW}Step 2: Ensuring GCS bucket exists...${NC}"
if gsutil ls -b gs://${BUCKET_NAME} &> /dev/null; then
    echo -e "${GREEN}✓ Bucket already exists: gs://${BUCKET_NAME}${NC}"
else
    echo -e "${YELLOW}Creating bucket: gs://${BUCKET_NAME}${NC}"
    gsutil mb -p ${PROJECT_ID} -l ${REGION} gs://${BUCKET_NAME}
    echo -e "${GREEN}✓ Bucket created${NC}"
fi

# Step 3: Set bucket permissions (make it private)
echo -e "\n${YELLOW}Step 3: Configuring bucket permissions...${NC}"
gsutil uniformbucketlevelaccess set on gs://${BUCKET_NAME}
echo -e "${GREEN}✓ Bucket set to uniform access control${NC}"

# Step 4: Build Docker image using Cloud Build
echo -e "\n${YELLOW}Step 4: Building Docker image...${NC}"
gcloud builds submit --tag ${IMAGE_NAME}:latest --timeout=1800s
echo -e "${GREEN}✓ Image built and pushed to Container Registry${NC}"

# Step 5: Get the default Cloud Run service account
echo -e "\n${YELLOW}Step 5: Setting up service account permissions...${NC}"
CLOUD_RUN_SA="${PROJECT_ID}@appspot.gserviceaccount.com"

# Grant Cloud Run service account Storage Admin role
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member serviceAccount:${CLOUD_RUN_SA} \
    --role roles/storage.admin \
    --condition=None 2>/dev/null || true

echo -e "${GREEN}✓ Service account permissions configured${NC}"

# Step 6: Deploy to Cloud Run
echo -e "\n${YELLOW}Step 6: Deploying to Cloud Run...${NC}"
gcloud run deploy ${SERVICE_NAME} \
    --image ${IMAGE_NAME}:latest \
    --platform managed \
    --region ${REGION} \
    --memory 512Mi \
    --cpu 1 \
    --allow-unauthenticated \
    --timeout 300 \
    --max-instances 100 \
    --set-env-vars GCS_BUCKET_NAME=${BUCKET_NAME} \
    --service-account ${CLOUD_RUN_SA}

echo -e "${GREEN}✓ Deployment successful${NC}"

# Step 7: Get service URL
echo -e "\n${YELLOW}Step 7: Getting service details...${NC}"
SERVICE_URL=$(gcloud run services describe ${SERVICE_NAME} \
    --platform managed \
    --region ${REGION} \
    --format='value(status.url)')

echo -e "${GREEN}✓ Service deployed!${NC}"
echo -e "\n${GREEN}=== Deployment Summary ===${NC}"
echo -e "Service Name: ${GREEN}${SERVICE_NAME}${NC}"
echo -e "Service URL: ${GREEN}${SERVICE_URL}${NC}"
echo -e "Region: ${GREEN}${REGION}${NC}"
echo -e "GCS Bucket: ${GREEN}gs://${BUCKET_NAME}${NC}"
echo -e "Image: ${GREEN}${IMAGE_NAME}:latest${NC}"

echo -e "\n${YELLOW}Test the deployment:${NC}"
echo "curl -X GET '${SERVICE_URL}/api/news/testConnectivity'"

echo -e "\n${YELLOW}Create a news item with image:${NC}"
echo "curl -X POST '${SERVICE_URL}/api/news' \\"
echo "  -F 'title=Test News' \\"
echo "  -F 'content=Test content' \\"
echo "  -F 'image=@/path/to/image.jpg'"

echo -e "\n${GREEN}=== Deployment Complete ===${NC}"

