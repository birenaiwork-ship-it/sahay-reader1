# Sahaya Reader — Cloud Backend Service

This backend service provides secure server-side **Google Cloud Translation** (English to Odia `or`) and **Google Cloud Text-to-Speech** (Odia `or-IN` and English `en-US`), keeping all API keys and service account credentials off client devices.

## API Endpoints

1. **`GET /health`**
   - Returns `{ status: "ok", service: "Sahaya Reader Cloud API" }`
2. **`POST /translate`**
   - Request: `{"text": "Hello world", "sourceLanguage": "en", "targetLanguage": "or"}`
   - Response: `{"translation": "ନମସ୍କାର ପୃଥିବୀ", "sourceLanguage": "en", "targetLanguage": "or"}`
3. **`POST /tts`**
   - Request: `{"text": "...", "language": "or-IN"}`
   - Response: Audio byte stream (`audio/mpeg`)
4. **`POST /audiobook`**
   - Request: `{"bookTitle": "...", "languageMode": "ODIA", "items": [...]}`
   - Response: `{"jobId": "job_123", "status": "QUEUED"}`
5. **`GET /audiobook/:id`**
   - Response: `{"jobId": "job_123", "status": "COMPLETED", "progress": 100, "downloadUrl": "..."}`

## Deployment to Google Cloud Run

```bash
# 1. Authenticate with gcloud
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

# 2. Build and deploy container to Cloud Run
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/sahaya-backend
gcloud run deploy sahaya-backend \
  --image gcr.io/YOUR_PROJECT_ID/sahaya-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GOOGLE_CLOUD_PROJECT_ID=YOUR_PROJECT_ID
```
