# Sahaya Reader (ସହାୟ ରିଡର୍)

> **An accessible native Android book reader and audiobook generator designed primarily for blind and visually impaired students to read English PDF textbooks, translate sentences into Odia (ଓଡ଼ିଆ), listen aloud with voice synthesis, and produce full-book audiobooks.**

---

## Highlights & Features

- **Designed for Accessibility First:**
  - 100% TalkBack compatible with semantic live announcements and roles.
  - Large touch targets (≥ 48dp), clear high-contrast focus rings, and WCAG AAA high-contrast dark theme.
  - External Bluetooth / hardware keyboard navigation support with single-key shortcuts.
  - No visual-only affordances; full state exposed via accessible labels.
- **Smart PDF Extraction & Scanned Page OCR:**
  - Uses Android's Storage Access Framework (SAF) for local and Google Drive PDFs.
  - Native `PdfRenderer` + Google ML Kit Text Recognition OCR for scanned or image-based pages.
  - Sentence segmentation engine preserves punctuation, formulas, abbreviations (e.g. `e.g.`, `Dr.`), and numbers.
- **Academic English to Odia Translation:**
  - Real Google Cloud Translation integration (`en` → `or`).
  - Room database offline caching prevents duplicate network translation requests.
- **Speech Synthesis:**
  - Native Android TextToSpeech engine as instant zero-network fallback with installed voice detection.
  - Cloud TTS support (`or-IN`) for natural Odia voice delivery.
  - Three reading modes: English, Odia, and Bilingual sentence-by-sentence with configurable delay.
- **Full Audiobook Generator & Media3 Player:**
  - Background WorkManager batch processor creates complete audiobooks without blocking the UI.
  - System progress notifications with page and unit counters.
  - Media3 (ExoPlayer) playback with speed adjustment, seek ±10s/30s, and exact position restoration.
- **Built-in Sample Textbook:**
  - Bundled *Introduction to Computer Science* pre-indexed demo for instant zero-configuration testing.

---

## Architecture Overview

```
                      ┌──────────────────────┐
                      │    PDF / Document    │
                      └──────────┬───────────┘
                                 │
                     ┌───────────▼───────────┐
                     │ PdfRenderer + ML Kit  │
                     └───────────┬───────────┘
                                 │
                     ┌───────────▼───────────┐
                     │ ReadingUnitProcessor  │
                     └───────────┬───────────┘
                                 │
     ┌───────────────────────────┼───────────────────────────┐
     │                           │                           │
┌────▼──────────────┐   ┌────────▼──────────┐   ┌────────────▼─────────┐
│ Room Database     │   │ Translation Engine│   │ Android & Cloud TTS  │
│ (Books, Pages,    │   │ (Local Cache +    │   │ (English & Odia      │
│  Units, Bookmarks)│   │  Cloud Translation│   │  Voice Synthesis)    │
└────┬──────────────┘   └────────┬──────────┘   └────────────┬─────────┘
     │                           │                           │
     └───────────────────────────┼───────────────────────────┘
                                 │
                     ┌───────────▼───────────┐
                     │  Jetpack Compose M3   │
                     │  Accessibility UI     │
                     └───────────────────────┘
```

---

## Keyboard Shortcuts

For students using external Bluetooth keyboards or desktop accessibility docks:

| Key | Action |
|:---|:---|
| **Space** | Play / Pause reading |
| **Arrow Up** | Previous reading unit |
| **Arrow Down** | Next reading unit |
| **Arrow Left** | Previous page |
| **Arrow Right** | Next page |
| **T** | Translate current reading unit into Odia |
| **E** | Switch to English reading mode |
| **O** | Switch to Odia reading mode |
| **B** | Switch to Bilingual reading mode |
| **R** | Repeat current unit |
| **A** | Open Audiobook generator |
| **K** | Toggle bookmark on current reading unit |
| **S** / **Escape** | Stop speech |
| **+** / **=** | Increase speech speed (+0.25x) |
| **-** | Decrease speech speed (-0.25x) |

---

## Step-by-Step Setup & Deployment Guide

### A. Install Android Studio
1. Download **Android Studio Ladybug** (or later) from [developer.android.com/studio](https://developer.android.com/studio).
2. Install the Android SDK Platform for **API 35/36** and Android SDK Build-Tools.
3. Ensure JDK 17 or JDK 21 is set as the default Gradle JDK.

### B. Open the Project
1. Launch Android Studio.
2. Select **Open** and choose the root directory of this repository (`sahaya-reader`).

### C. Sync Gradle
1. Allow Android Studio to automatically download dependencies and sync the project via `libs.versions.toml`.
2. To sync manually from the terminal:
   ```bash
   gradle --refresh-dependencies
   ```

### D. Create / Configure the Backend Service
The backend service handles server-side Google Cloud Translation and Text-to-Speech:
1. Navigate to the `backend/` directory:
   ```bash
   cd backend
   npm install
   ```
2. Copy the environment variables template:
   ```bash
   cp .env.example .env
   ```
3. Start the backend locally:
   ```bash
   npm start
   ```
   The backend will run on port `8080` (accessible from the Android Emulator at `http://10.0.2.2:8080/`).

### E. Configure Google Cloud Translation
1. In the [Google Cloud Console](https://console.cloud.google.com/), create a new project or select an existing one.
2. Enable the **Cloud Translation API**:
   ```bash
   gcloud services enable translate.googleapis.com
   ```

### F. Configure Google Cloud Text-to-Speech
1. In Google Cloud Console, enable the **Cloud Text-to-Speech API**:
   ```bash
   gcloud services enable texttospeech.googleapis.com
   ```
2. Cloud TTS natively provides the `or-IN` language voice for Odia.

### G. Configure Authentication
1. In Google Cloud Console, navigate to **IAM & Admin** → **Service Accounts**.
2. Create a service account with the role:
   - `Cloud Translation API User`
   - `Cloud Text-to-Speech API User`
3. Generate and download a JSON key file.
4. Set the path in `backend/.env`:
   ```env
   GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/service-account-key.json
   GOOGLE_CLOUD_PROJECT_ID=your-gcp-project-id
   ```

### H. Put Backend URL into the Android App
1. Open **Settings** in the Sahaya Reader app.
2. Under **Cloud Translation & TTS Server**, enter the server URL:
   - For Android Emulator: `http://10.0.2.2:8080/`
   - For Physical Device on local Wi-Fi: `http://192.168.x.x:8080/`
   - For Deployed Cloud Run instance: `https://your-service-url.run.app/`
3. Tap **Test** to verify connection, then tap **Save URL**.

### I. Run the App on an Emulator
1. In Android Studio, open the **Device Manager** and create a Pixel device running Android 14+ (API 34/35).
2. Click **Run 'app'** (`Shift + F10`).
3. Tap **Sample Textbook** on the home screen to test immediately.

### J. Run the App on a Physical Android Phone
1. Enable **Developer Options** and **USB Debugging** on the Android device.
2. Connect the phone via USB.
3. Select your physical phone in the target device dropdown and click **Run**.

### K. Test with TalkBack
1. On the test phone, go to **Settings** → **Accessibility** → **TalkBack** and toggle it **ON**.
2. Use TalkBack gestures:
   - Swipe right: Move focus forward.
   - Swipe left: Move focus backward.
   - Double-tap: Activate focused element.
3. Verify that all elements announce clear descriptions, e.g. *"Continue reading Introduction to Computer Science, page 1"*, *"Play reading"*, *"Unit 2 of 6"*.

### L. Build Debug APK
Run:
```bash
gradle :app:assembleDebug
```
The APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`.

### M. Build Release APK
Run:
```bash
gradle :app:assembleRelease
```
The unsigned release APK will be generated at: `app/build/outputs/apk/release/app-release-unsigned.apk`.

### N. Sign the Application
```bash
# Generate a release keystore if not already created
keytool -genkey -v -keystore my-release-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000

# Align and sign using apksigner
zipalign -v -p 4 app/build/outputs/apk/release/app-release-unsigned.apk app-release-aligned.apk
apksigner sign --ks my-release-key.jks --ks-key-alias upload --out app-release-signed.apk app-release-aligned.apk
```

### O. Prepare for Google Play Store Release
1. Build an Android App Bundle (AAB):
   ```bash
   gradle :app:bundleRelease
   ```
2. Output: `app/build/outputs/bundle/release/app-release.aab`.
3. In Google Play Console:
   - Upload the signed `.aab` file.
   - Declare Accessibility Service compatibility in the store listing.
   - Fill out the Content Rating and Privacy Policy questionnaires.

---

## License

Designed and built for educational empowerment and digital accessibility.
