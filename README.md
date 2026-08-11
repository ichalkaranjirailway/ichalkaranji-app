# इचलकरंजी रेल्वे — Android App

A native WebView-shell Android app that loads your live site
(`https://ichalkaranjirailway.github.io/htk-ich/`). Whenever you update the
website on GitHub Pages, the app shows the new content automatically — no
Play Store update needed for content changes.

## 1. Why a WebView shell (not TWA)

Two free options were considered:

- **TWA (Trusted Web Activity)**: relies on Chrome, needs a
  `.well-known/assetlinks.json` file matched to your exact signing
  certificate, and gives very little control over the back-button
  confirmation dialog, custom offline screen, or splash screen.
- **Custom WebView shell (chosen)**: a small native Android app with one
  `WebView`. Full control over every feature you asked for — back button
  behavior, pull-to-refresh, offline message, splash screen, PDF/link
  handling, share button — while still being 100% free, simple to build,
  and easy to maintain.

## 2. What's in this project

```
IchalkaranjiRailway/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/ichalkaranjirailway/htkich/MainActivity.kt
        └── res/
            ├── layout/activity_main.xml
            ├── menu/menu_main.xml
            ├── values/strings.xml
            ├── values/colors.xml
            ├── values/themes.xml
            ├── drawable/ic_share.xml, ic_reload.xml
            ├── mipmap-*/ic_launcher.png, ic_launcher_round.png
            ├── mipmap-anydpi-v26/ic_launcher.xml (adaptive icon)
            └── xml/network_security_config.xml, file_paths.xml
```

## 3. Setup — from zero to APK (free, using Android Studio)

1. Install **Android Studio** (free): https://developer.android.com/studio
2. Unzip the project folder you downloaded from this chat.
3. Open Android Studio → **Open** → select the `IchalkaranjiRailway` folder.
4. Android Studio will say the Gradle wrapper is missing and offer to
   generate it — click **OK / Use Gradle wrapper**. (This just downloads
   the free Gradle build tool automatically; nothing to pay.)
5. Wait for **Gradle Sync** to finish (bottom status bar). First sync can
   take a few minutes as it downloads the Android SDK/build tools if
   needed.
6. Connect your Android phone via USB (enable **Developer Options → USB
   debugging** on the phone) — or use the built-in emulator.
7. Click the green **Run ▶** button. The app builds and launches on your
   device. That's your debug APK, already installed and running.

## 4. Generating a Debug APK file (to share/install manually)

- Menu: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
- When it finishes, click the **locate** link in the notification, or find
  it at:
  `app/build/outputs/apk/debug/app-debug.apk`
- Copy that file to your phone (or send via WhatsApp/Drive) and tap it to
  install. You may need to allow **"Install unknown apps"** for whichever
  app you used to open the file — Android will prompt you.

## 5. Generating a signed Release APK (for real distribution)

1. **Build → Generate Signed Bundle / APK**
2. Choose **APK** → **Next**
3. Click **Create new...** to make a signing key (only needed once — keep
   this `.jks` file and its passwords safe forever; you'll need the exact
   same key for every future update):
   - Key store path: choose a save location
   - Password: choose a strong password
   - Alias + alias password
   - Validity: 25+ years
   - Fill in your name/organization (can be simple, e.g. "Ichalkaranji
     Railway Kruti Samiti")
4. Next → choose **release** build variant → **V1 + V2 signature** → Finish.
5. Output: `app/build/outputs/apk/release/app-release.apk` — this is what
   you share for real installs.

## 6. Generating an AAB (only needed if you publish on Google Play)

Same as above (**Build → Generate Signed Bundle / APK**) but choose
**Android App Bundle** instead of APK in step 2. Output:
`app/build/outputs/bundle/release/app-release.aab`.

Note: publishing on Google Play itself requires a **one-time $25 Google
Play Developer registration fee** — that is the only step in this whole
workflow that isn't free. Building, testing, and directly sharing the APK
outside the Play Store costs nothing.

## 7. App icon

A railway-track icon (two stations connected by a rail line) is already
generated and included at all required sizes (`mipmap-mdpi` through
`mipmap-xxxhdpi`, plus an adaptive icon). If you'd like to refine it later,
use **Android Studio → right-click `res` → New → Image Asset**, and
provide your own 1024×1024 PNG as the source.

## 8. What you do NOT need to touch

- No API keys, tokens, or secrets are used anywhere in this app.
- No permissions are requested beyond `INTERNET` and
  `ACCESS_NETWORK_STATE` (needed just to detect offline status and show the
  Marathi offline message instead of a blank screen).

## 9. Testing checklist

Everything from your original checklist is implemented: JS/DOM
storage/localStorage enabled, HTTPS-only, back-button with the Marathi
"App मधून बाहेर पडायचे आहे का?" confirmation, pull-to-refresh, offline
screen, PDFs/Drive links/mailto/tel/WhatsApp/YouTube/social links all
routed to the right external app or browser, native share button in the
toolbar, splash screen, no unnecessary permissions, no secrets in the APK.

Manually verify on your phone: signature/petition button, vote form,
gallery images, timeline filters, and the WhatsApp share buttons on the
site all behave as expected once installed.

---

## 10. Optional: PWA support for the website itself (separate from the app)

These files are provided **separately** in `IchalkaranjiRailway_PWA_files/`
so you can add them to your `htk-ich` GitHub repo if you also want the
website itself to be installable as a PWA in Chrome. This is **optional**
and does not affect the Android app above, which loads your site directly
regardless of whether you add these.

Files to add to your repo (do not remove anything existing):

1. `manifest.json` → place at the root of `htk-ich/`
2. `sw.js` → place at the root of `htk-ich/`
3. `icons/icon-192.png`, `icons/icon-512.png`,
   `icons/icon-512-maskable.png` → place in an `icons/` folder

Then add these two lines inside the `<head>` of your `index.html`:

```html
<link rel="manifest" href="manifest.json">
<meta name="theme-color" content="#0B3D91">
```

And near the end of your existing page script (or in a small new
`<script>` tag before `</body>`), add:

```html
<script>
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('sw.js');
  }
</script>
```

That's it — nothing else on your existing site changes.
