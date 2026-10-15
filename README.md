# BeatGlow Studio for Android

The full BeatGlow Studio web app, wrapped in an Android app. Everything (fonts, video encoders) is bundled, so it works offline.

## Easiest: build the APK online with GitHub (no Android Studio)
1. Create a free GitHub account and a new empty repository (public or private).
2. Upload ALL files from this folder to the repository (Add file -> Upload files). Keep the folder structure, including the hidden `.github` folder.
3. Open the **Actions** tab -> **Build Android APK** -> **Run workflow**. It takes about 5 minutes.
4. Open the finished run -> **Artifacts** -> download **BeatGlow-APK**, unzip it, and copy `app-debug.apk` to your phone.
5. On the phone, open the APK and allow "Install unknown apps" for your file manager when asked.

## Or: build with Android Studio
1. Install Android Studio (JDK 17 is included).
2. File -> Open -> choose this folder, wait for Gradle sync.
3. Build -> Build APK(s). The file appears at `app/build/outputs/apk/debug/app-debug.apk`.

## Using the app
* Needs Android 10 or newer and a recent Android System WebView / Chrome.
* Saved videos go to **Movies/BeatGlow**, thumbnails to **Pictures/BeatGlow**, text files to **Downloads/BeatGlow**.
* While exporting, keep the app open. The screen stays on automatically.
* Phones are slower than PCs: use Lite mode, HD 720p and short clips first.
