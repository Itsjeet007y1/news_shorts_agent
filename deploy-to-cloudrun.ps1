# Cloud Run Deployment Script for News Shorts Agent (Windows PowerShell)
# This script automates the deployment of the refactored application to Google Cloud Run

param(
    [string]$Region = "us-central1",
    [string]$BucketName = "news-images-bucket",
    [string]$ServiceName = "news-shorts-agent"
)

# Error action preference
$ErrorActionPreference = "Stop"

# Color functions
function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "→ $Message" -ForegroundColor Yellow
}

# Header
Write-Host "=== News Shorts Agent - Cloud Run Deployment ===" -ForegroundColor Yellow
Write-Host ""

# Check if gcloud is installed
try {
    $gcloud = gcloud version 2>&1 | Select-Object -First 1
    Write-Success "gcloud CLI found"
} catch {
    Write-Error-Custom "gcloud CLI is not installed. Please install it first."
    exit 1
}

# Get project ID
$projectId = & gcloud config get-value project
if ([string]::IsNullOrEmpty($projectId)) {
    Write-Error-Custom "No Google Cloud project set. Please run: gcloud config set project PROJECT_ID"
    exit 1
}

$imageName = "gcr.io/${projectId}/${ServiceName}"

Write-Host ""
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Success "Project ID: $projectId"
Write-Success "Region: $Region"
Write-Success "GCS Bucket: $BucketName"
Write-Success "Service Name: $ServiceName"

# Step 1: Enable required APIs
Write-Host ""
Write-Info "Step 1: Enabling required Google Cloud APIs..."
try {
    & gcloud services enable cloudbuild.googleapis.com 2>&1 | Out-Null
    & gcloud services enable run.googleapis.com 2>&1 | Out-Null
    & gcloud services enable storage-api.googleapis.com 2>&1 | Out-Null
    Write-Success "APIs enabled"
} catch {
    Write-Error-Custom "Failed to enable APIs: $_"
    exit 1
}

# Step 2: Create GCS bucket if it doesn't exist
Write-Host ""
Write-Info "Step 2: Ensuring GCS bucket exists..."
try {
    $bucketExists = & gsutil ls -b "gs://${BucketName}" 2>&1 | Select-Object -First 1
    if ($bucketExists) {
        Write-Success "Bucket already exists: gs://$BucketName"
    } else {
        Write-Info "Creating bucket: gs://$BucketName"
        & gsutil mb -p $projectId -l $Region "gs://${BucketName}"
        Write-Success "Bucket created"
    }
} catch {
    Write-Error-Custom "Failed to manage bucket: $_"
}

# Step 3: Set bucket permissions
Write-Host ""
Write-Info "Step 3: Configuring bucket permissions..."
try {
    & gsutil uniformbucketlevelaccess set on "gs://${BucketName}"
    Write-Success "Bucket set to uniform access control"
} catch {
    Write-Error-Custom "Failed to set bucket permissions: $_"
}

# Step 4: Build Docker image using Cloud Build
Write-Host ""
Write-Info "Step 4: Building Docker image using Cloud Build..."
Write-Info "(This may take 5-10 minutes)"
try {
    & gcloud builds submit --tag "${imageName}:latest" --timeout=1800s
    Write-Success "Image built and pushed to Container Registry"
} catch {
    Write-Error-Custom "Failed to build image: $_"
    exit 1
}

# Step 5: Set up service account permissions
Write-Host ""
Write-Info "Step 5: Setting up service account permissions..."
try {
    $cloudRunSa = "${projectId}@appspot.gserviceaccount.com"
    & gcloud projects add-iam-policy-binding $projectId `
        --member="serviceAccount:${cloudRunSa}" `
        --role="roles/storage.admin" `
        --condition=None 2>&1 | Out-Null
    Write-Success "Service account permissions configured"
} catch {
    Write-Error-Custom "Failed to configure permissions: $_"
}

# Step 6: Deploy to Cloud Run
Write-Host ""
Write-Info "Step 6: Deploying to Cloud Run..."
try {
    & gcloud run deploy $ServiceName `
        --image "${imageName}:latest" `
        --platform managed `
        --region $Region `
        --memory 512Mi `
        --cpu 1 `
        --allow-unauthenticated `
        --timeout 300 `
        --max-instances 100 `
        --set-env-vars "GCS_BUCKET_NAME=${BucketName}" `
        --service-account "${cloudRunSa}"
    Write-Success "Deployment successful"
} catch {
    Write-Error-Custom "Failed to deploy to Cloud Run: $_"
    exit 1
}

# Step 7: Get service URL
Write-Host ""
Write-Info "Step 7: Getting service details..."
try {
    $serviceUrl = & gcloud run services describe $ServiceName `
        --platform managed `
        --region $Region `
        --format='value(status.url)'
    Write-Success "Service deployed!"
} catch {
    Write-Error-Custom "Failed to get service URL: $_"
    exit 1
}

# Summary
Write-Host ""
Write-Host "=== Deployment Summary ===" -ForegroundColor Green
Write-Host "Service Name: $ServiceName" -ForegroundColor Cyan
Write-Host "Service URL: $serviceUrl" -ForegroundColor Cyan
Write-Host "Region: $Region" -ForegroundColor Cyan
Write-Host "GCS Bucket: gs://$BucketName" -ForegroundColor Cyan
Write-Host "Image: $imageName" -ForegroundColor Cyan

Write-Host ""
Write-Host "Test Commands:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Test connectivity:" -ForegroundColor Gray
Write-Host "curl -X GET '$serviceUrl/api/news/testConnectivity'" -ForegroundColor White
Write-Host ""
Write-Host "Create news with image:" -ForegroundColor Gray
Write-Host "curl -X POST '$serviceUrl/api/news' -F 'title=Test' -F 'content=Content' -F 'image=@image.jpg'" -ForegroundColor White
Write-Host ""
Write-Host "Get all news:" -ForegroundColor Gray
Write-Host "curl -X GET '$serviceUrl/api/news'" -ForegroundColor White

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Green

