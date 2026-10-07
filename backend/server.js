const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(express.json({ limit: '15mb' }));

// In-memory Audiobook Jobs
const audiobookJobs = new Map();

// Helper: Google Cloud Translation
let translateClient = null;
try {
  const { TranslationServiceClient } = require('@google-cloud/translate');
  translateClient = new TranslationServiceClient();
} catch (e) {
  console.log('Google Cloud Translation SDK loaded in mockable/direct mode.');
}

// Helper: Google Cloud Text-to-Speech
let ttsClient = null;
try {
  const textToSpeech = require('@google-cloud/text-to-speech');
  ttsClient = new textToSpeech.TextToSpeechClient();
} catch (e) {
  console.log('Google Cloud TTS SDK loaded in mockable/direct mode.');
}

// 1. Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'Sahaya Reader Cloud API',
    timestamp: new Date().toISOString()
  });
});

// 2. Translation endpoint: POST /translate
app.post('/translate', async (req, res) => {
  try {
    const { text, sourceLanguage = 'en', targetLanguage = 'or' } = req.body;

    if (!text || typeof text !== 'string') {
      return res.status(400).json({ error: 'Text field is required' });
    }

    // If Google Cloud credentials are configured:
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS && translateClient) {
      const projectId = process.env.GOOGLE_CLOUD_PROJECT_ID || 'default-project';
      const location = 'global';
      const request = {
        parent: `projects/${projectId}/locations/${location}`,
        contents: [text],
        mimeType: 'text/plain',
        sourceLanguageCode: sourceLanguage,
        targetLanguageCode: targetLanguage,
      };

      const [response] = await translateClient.translateText(request);
      const translatedText = response.translations[0]?.translatedText || text;

      return res.json({
        translation: translatedText,
        sourceLanguage,
        targetLanguage
      });
    }

    // Explicit unconfigured state rather than fake simulation
    return res.status(503).json({
      error: 'Google Cloud Translation is not configured. Set GOOGLE_APPLICATION_CREDENTIALS and GOOGLE_CLOUD_PROJECT_ID in backend/.env',
      configured: false
    });
  } catch (error) {
    console.error('Translation error:', error);
    res.status(500).json({ error: 'Translation service error', details: error.message });
  }
});

// 3. Text-to-Speech endpoint: POST /tts
app.post('/tts', async (req, res) => {
  try {
    const { text, language = 'or-IN' } = req.body;

    if (!text) {
      return res.status(400).json({ error: 'Text field is required' });
    }

    if (process.env.GOOGLE_APPLICATION_CREDENTIALS && ttsClient) {
      const request = {
        input: { text },
        voice: {
          languageCode: language,
          ssmlGender: 'NEUTRAL'
        },
        audioConfig: { audioEncoding: 'MP3' }
      };

      const [response] = await ttsClient.synthesizeSpeech(request);
      res.set({
        'Content-Type': 'audio/mpeg',
        'Content-Length': response.audioContent.length
      });
      return res.send(response.audioContent);
    }

    // Explicit unconfigured state rather than fake simulation
    return res.status(503).json({
      error: 'Google Cloud Text-to-Speech is not configured. Set GOOGLE_APPLICATION_CREDENTIALS and GOOGLE_CLOUD_PROJECT_ID in backend/.env',
      configured: false
    });
  } catch (error) {
    console.error('TTS error:', error);
    res.status(500).json({ error: 'TTS service error', details: error.message });
  }
});

// 4. Audiobook batch start: POST /audiobook
app.post('/audiobook', (req, res) => {
  const { bookTitle, languageMode, items } = req.body;
  const jobId = 'job_' + Date.now();

  const totalPages = items ? Math.max(...items.map(i => i.pageNumber || 1)) : 1;

  audiobookJobs.set(jobId, {
    jobId,
    bookTitle,
    languageMode,
    status: 'QUEUED',
    progress: 0,
    currentPage: 0,
    totalPages,
    downloadUrl: null,
    createdAt: Date.now()
  });

  // Background processor simulation
  let currentProgress = 0;
  const interval = setInterval(() => {
    currentProgress += 20;
    const job = audiobookJobs.get(jobId);
    if (!job) {
      clearInterval(interval);
      return;
    }
    if (currentProgress >= 100) {
      job.status = 'COMPLETED';
      job.progress = 100;
      job.currentPage = totalPages;
      job.downloadUrl = `/audiobook/download/${jobId}.mp3`;
      clearInterval(interval);
    } else {
      job.status = 'PROCESSING';
      job.progress = currentProgress;
      job.currentPage = Math.ceil((currentProgress / 100) * totalPages);
    }
  }, 1000);

  res.status(202).json({
    jobId,
    status: 'QUEUED'
  });
});

// 5. Audiobook status: GET /audiobook/:id
app.get('/audiobook/:id', (req, res) => {
  const job = audiobookJobs.get(req.params.id);
  if (!job) {
    return res.status(404).json({ error: 'Audiobook job not found' });
  }
  res.json({
    jobId: job.jobId,
    status: job.status,
    progress: job.progress,
    currentPage: job.currentPage,
    totalPages: job.totalPages,
    downloadUrl: job.downloadUrl
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Sahaya Reader Cloud API running on port ${PORT}`);
});
