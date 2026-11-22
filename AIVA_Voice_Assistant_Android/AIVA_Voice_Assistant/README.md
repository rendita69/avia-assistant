# AIVA — Safe Multilingual Android Voice Assistant (English, অসমীয়া, हिन्दी)

AIVA is a modern Material 3 Android voice assistant built with Kotlin & Jetpack Compose. It analyzes every voice command before acting so your phone stays safe from accidental calls, wrong messages, or payment mistakes.

## Easiest Way to Get the Installable `.apk` (No PC Required!)

1. Create a free account at **github.com** and create a new repository (for example, `aiva-assistant`).
2. Upload all files inside this folder to your repository (keep the `.github/workflows/build-apk.yml` file included).
3. Open your repository on GitHub → tap the **Actions** tab → select **Build AIVA Android APK** → tap **Run workflow**.
4. Wait ~2 minutes for the green checkmark, tap the finished run, and download **AIVA-Voice-Assistant-APK** (`app-debug.apk`).
5. Tap `app-debug.apk` on your Android phone (allow "Install unknown apps" when prompted) and open **AIVA Assistant**!

## Alternative: Build Locally in Android Studio

1. Open this folder in **Android Studio (Ladybug / Koala or newer)** with JDK 17.
2. Wait for Gradle sync to finish.
3. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Copy `app/build/outputs/apk/debug/app-debug.apk` to your Android phone and install it.

## How to Use & Prevent Mistakes

- **Vault Tab (Set Up First):** Add your frequent call aliases (for example, `Mom` with aliases `মা, माँ, Mummy`) and verified UPI payees (Name + UPI ID like `rahul@okaxis`).
- **Safe UPI Payments:** Say *"Pay 500 rupees to Rahul"*, *"ৰাহুলক ৫০০ টকা পঠিওৱা"*, or *"राहुल को 500 रुपये भेजो"*. AIVA verifies the name & amount on screen and opens your UPI app (GPay, PhonePe, Paytm, BHIM) pre-filled so **you enter your UPI PIN manually**. (Android & NPCI banking rules never allow any app to bypass your UPI PIN.)
- **Calling:** Say *"Call Mom"* or *"মা-ক ফোন কৰা"*. By default, AIVA pauses on the Verify Card; you can toggle **Confirm Before Calling** in Settings if you prefer instant dialing.
- **Far-Field Listening:** Enable **Far-Field ON** on the home screen for extended listening windows. Note that physical smartphone microphones reliably capture voice at ~2–5 meters in quiet rooms; at 10 meters, background noise and hardware mic limits affect accuracy.
- **Optional Gemini AI Brain:** Works 100% offline with built-in English/Assamese/Hindi rules. For even smarter natural-language understanding, paste a free Gemini API key in **Settings** (stored encrypted on-device).

## Easy Updates & Customization

- **Colors & Look:** Edit `app/src/main/java/com/aiva/assistant/ui/theme/Theme.kt`
- **Voice Commands & Languages:** Edit `app/src/main/java/com/aiva/assistant/engine/CommandAnalyzer.kt`
- **Phone Actions:** Edit `app/src/main/java/com/aiva/assistant/engine/PhoneActionExecutor.kt`
