# Local Development Setup Guide

## Prerequisites

### System Requirements
- **OS:** Windows 10+, macOS 10.15+, or Linux (Ubuntu 20.04+)
- **Java:** JDK 17 or higher
- **Maven:** 3.9+
- **Git:** 2.30+

### Cloud Setup
- **Google Cloud Account** (free tier available)
- **gcloud CLI** installed and configured
- **Active billing** on Google Cloud Project

---

## Step-by-Step Setup

### 1. Install Java 17

#### Windows (using Chocolatey)
```powershell
choco install temurin17
```

#### macOS (using Homebrew)
```bash
brew install openjdk@17
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get install openjdk-17-jdk
```

### 2. Install Maven

#### Windows (using Chocolatey)
```powershell
choco install maven
```

#### macOS (using Homebrew)
```bash
brew install maven
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get install maven
```

### 3. Install Google Cloud SDK

#### Windows
Download from: https://cloud.google.com/sdk/docs/install#windows

```powershell
# After installation, initialize
gcloud init
gcloud auth login
```

#### macOS
```bash
brew install --cask google-cloud-sdk
gcloud init
gcloud auth login
```

#### Linux (Ubuntu/Debian)
```bash
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init
gcloud auth login
```

### 4. Create Google Cloud Project

```bash
# Set your desired project ID
gcloud projects create news-shorts-project --name="News Shorts Agent"

# Set it as active
gcloud config set project news-shorts-project

# Enable required APIs
gcloud services enable storage-api.googleapis.com
gcloud services enable cloudbuild.googleapis.com
```

### 5. Create Service Account

```bash
# Create service account
gcloud iam service-accounts create news-agent-sa \
    --display-name="News Agent Service Account"

# Get the email
SERVICE_ACCOUNT_EMAIL=$(gcloud iam service-accounts list --filter="displayName:News Agent Service Account" --format="value(email)")
echo $SERVICE_ACCOUNT_EMAIL

# Grant Storage Admin role
gcloud projects add-iam-policy-binding news-shorts-project \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/storage.admin"

# Create and download key
gcloud iam service-accounts keys create ~/news-agent-key.json \
    --iam-account=${SERVICE_ACCOUNT_EMAIL}

echo "Service account key saved to: ~/news-agent-key.json"
```

### 6. Create GCS Bucket

```bash
# Set bucket name (must be globally unique)
BUCKET_NAME="news-images-bucket-$(date +%s)"

# Create bucket in us-central1
gsutil mb -p news-shorts-project -l us-central1 gs://${BUCKET_NAME}

# Make bucket private (default) - Note: Uniform access disabled to allow ACLs for public image access
# gsutil uniformbucketlevelaccess set on gs://${BUCKET_NAME}

echo "Bucket created: gs://${BUCKET_NAME}"
```

### 7. Clone and Setup Repository

```bash
# Clone the repository
git clone <your-repo-url> news_shorts_agent
cd news_shorts_agent

# Install dependencies
mvn clean install
```

### 8. Configure Local Environment

#### Windows (PowerShell)
```powershell
# Set environment variable
$env:GOOGLE_APPLICATION_CREDENTIALS = "$HOME\news-agent-key.json"

# Verify
echo $env:GOOGLE_APPLICATION_CREDENTIALS
```

#### Windows (Command Prompt)
```cmd
setx GOOGLE_APPLICATION_CREDENTIALS "%USERPROFILE%\news-agent-key.json"
# Restart terminal for changes to take effect
```

#### macOS/Linux
```bash
# Add to ~/.bash_profile, ~/.zshrc, or ~/.bashrc
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/news-agent-key.json"

# Apply changes
source ~/.bash_profile  # or appropriate shell config file
```

### 9. Update Configuration

Edit `src/main/resources/application.properties`:

```properties
# Replace with your actual bucket name
gcs.bucket-name=news-images-bucket-YOUR_UNIQUE_ID

# Optional: Add logging
logging.level.com.example.news.service.GcsService=DEBUG
logging.level.com.google.cloud=INFO
```

### 10. Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/news-shorts-agent-0.0.1-SNAPSHOT.jar
```

The application should now be running at: **http://localhost:8080**

---

## Verification Checklist

- [x] Java 17 installed: `java -version`
- [x] Maven installed: `mvn -version`
- [x] gcloud CLI installed: `gcloud --version`
- [x] Google Cloud project created: `gcloud config list`
- [x] Service account created with key file
- [x] Storage API enabled: `gcloud services list --enabled | grep storage`
- [x] GCS bucket created: `gsutil ls -b gs://news-images-bucket-*`
- [x] Environment variable set: Check `echo $GOOGLE_APPLICATION_CREDENTIALS`
- [x] Code compiled successfully: `mvn clean compile`
- [x] Application runs: `mvn spring-boot:run`

---

## First Test Run

Once the application is running, test it with:

```bash
# Test connectivity
curl http://localhost:8080/api/news/testConnectivity

# Create test image (if using ImageMagick)
convert -size 100x100 xc:red /tmp/test.jpg

# Create news with image
curl -X POST http://localhost:8080/api/news \
  -F "title=Test Article" \
  -F "content=Testing GCS integration" \
  -F "description=Test description" \
  -F "image=@/tmp/test.jpg"

# Get all news
curl http://localhost:8080/api/news | jq '.'

# Get specific news
curl http://localhost:8080/api/news/1 | jq '.'
```

---

## IDE Setup

### IntelliJ IDEA

1. **Open Project:**
   - File → Open → Select project folder
   - Wait for Maven to sync dependencies

2. **Set Java Version:**
   - File → Project Structure → Project
   - Set SDK to JDK 17
   - Set Language Level to 17

3. **Configure Run Configuration:**
   - Run → Edit Configurations
   - Add new "Maven" configuration
   - Set Command: `spring-boot:run`
   - Set Environment variables:
     ```
     GOOGLE_APPLICATION_CREDENTIALS=/path/to/news-agent-key.json
     ```

4. **Run Application:**
   - Click green Run button or press Shift+F10

### VSCode

1. **Install Extensions:**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Google Cloud Code

2. **Create Launch Configuration:**
   - Create `.vscode/launch.json`:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Spring Boot App",
         "request": "launch",
         "mainClass": "com.example.news.NewsShortsAgentApplication",
         "projectName": "news-shorts-agent",
         "env": {
           "GOOGLE_APPLICATION_CREDENTIALS": "/path/to/news-agent-key.json"
         }
       }
     ]
   }
   ```

3. **Run Application:**
   - Press F5 to start debugging

---

## Common Issues & Solutions

### Issue: "GCS bucket does not exist"
```
Error: The specified bucket does not exist
```
**Solution:**
```bash
# Verify bucket exists
gsutil ls -b gs://news-images-bucket

# Create if missing
gsutil mb gs://news-images-bucket
```

### Issue: "GOOGLE_APPLICATION_CREDENTIALS not set"
```
Error: google.auth.exceptions.DefaultCredentialsError
```
**Solution:**
```bash
# Check if environment variable is set
echo $GOOGLE_APPLICATION_CREDENTIALS

# If empty, set it
export GOOGLE_APPLICATION_CREDENTIALS="$HOME/news-agent-key.json"

# Verify service account key file exists
ls -la $GOOGLE_APPLICATION_CREDENTIALS
```

### Issue: "Permission denied" errors
```
Error: com.google.api.gax.rpc.PermissionDeniedException: Permission denied
```
**Solution:**
```bash
# Check service account has Storage Admin role
gcloud projects get-iam-policy news-shorts-project \
    --flatten="bindings[].members" \
    --filter="bindings.members:serviceAccount:*" \
    --format="table(bindings.role)"

# Re-grant if needed
gcloud projects add-iam-policy-binding news-shorts-project \
    --member="serviceAccount:$(gcloud iam service-accounts list --filter=displayName:News* --format='value(email)')" \
    --role="roles/storage.admin"
```

### Issue: "Maven dependencies not downloading"
```
Error: [ERROR] Failed to execute goal on project news-shorts-agent
```
**Solution:**
```bash
# Clear Maven cache
mvn clean install -U

# Or delete .m2 folder and rebuild
rm -rf ~/.m2/repository
mvn clean install
```

### Issue: Port 8080 already in use
```
Error: Failed to start server. Port 8080 is already in use.
```
**Solution:**
```bash
# Use different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Or kill process on port 8080
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# macOS/Linux
lsof -i :8080
kill -9 <PID>
```

### Issue: Java version mismatch
```
Error: [ERROR] JDK version is 11, but 17 is required
```
**Solution:**
```bash
# Check Java version
java -version

# Set JAVA_HOME to Java 17
# Windows
set JAVA_HOME=C:\Program Files\Eclipse Temurin\jdk-17.0.x

# macOS/Linux
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Verify
java -version
```

---

## Useful Commands

### Maven Commands
```bash
# Clean and build
mvn clean install

# Run tests only
mvn test

# Skip tests during build
mvn clean install -DskipTests

# Build with debug output
mvn clean install -X

# Check for dependency issues
mvn dependency:tree
```

### gcloud Commands
```bash
# List projects
gcloud projects list

# Set active project
gcloud config set project PROJECT_ID

# List service accounts
gcloud iam service-accounts list

# List GCS buckets
gsutil ls

# Monitor GCS bucket
gsutil du -s gs://news-images-bucket

# View bucket contents
gsutil ls gs://news-images-bucket
```

### GCS (gsutil) Commands
```bash
# Create bucket
gsutil mb gs://bucket-name

# Delete bucket
gsutil rm -r gs://bucket-name

# Upload file
gsutil cp /path/to/file gs://bucket-name/

# Download file
gsutil cp gs://bucket-name/file /path/to/local

# Delete file
gsutil rm gs://bucket-name/file

# List objects
gsutil ls gs://bucket-name

# Set ACL (make public)
gsutil acl ch -u AllUsers:R gs://bucket-name/file

# Get object info
gsutil stat gs://bucket-name/file
```

---

## Next Steps After Setup

1. **Run Test Suite:**
   ```bash
   ./test-api.sh http://localhost:8080
   ```

2. **Monitor Logs:**
   ```bash
   tail -f spring-boot.log
   ```

3. **Check GCS Bucket:**
   ```bash
   gsutil ls gs://news-images-bucket
   ```

4. **View Cloud Console:**
   - Open: https://console.cloud.google.com
   - Navigate to Cloud Storage → Buckets
   - Watch files appear in real-time

5. **Enable Debug Logging:**
   - Edit `application.properties`
   - Add: `logging.level.com.example.news=DEBUG`

---

## Environment Summary

Create a reference file for your environment:

### Windows (save as `environment.ps1`)
```powershell
# News Shorts Agent - Local Environment Setup
$env:JAVA_HOME = "C:\Program Files\Eclipse Temurin\jdk-17.x.x"
$env:MAVEN_HOME = "C:\apache-maven-3.9.x"
$env:GOOGLE_APPLICATION_CREDENTIALS = "$HOME\news-agent-key.json"
$env:GCP_PROJECT = "news-shorts-project"

# Add to PATH
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"

Write-Host "Environment configured for News Shorts Agent"
Write-Host "Java: $(java -version 2>&1 | Select-Object -First 1)"
Write-Host "Maven: $(mvn -version 2>&1 | Select-Object -First 1)"
Write-Host "GCS Credentials: $env:GOOGLE_APPLICATION_CREDENTIALS"
```

### Linux/macOS (save as `environment.sh`)
```bash
#!/bin/bash
# News Shorts Agent - Local Environment Setup

export JAVA_HOME=/usr/libexec/java_home -v 17
export MAVEN_HOME=$HOME/.m2
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/news-agent-key.json
export GCP_PROJECT=news-shorts-project

echo "Environment configured for News Shorts Agent"
echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Maven: $(mvn -version 2>&1 | head -n 1)"
echo "GCS Credentials: $GOOGLE_APPLICATION_CREDENTIALS"
```

---

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review logs: `tail -f logs/spring-boot.log`
3. Check GCP console for quota/billing issues
4. Consult Google Cloud documentation

