# Qatar4u TV

A home screen for the Samsung Galaxy Tab S6 (SM-T865) that turns it into a living-room entertainment hub:
large gold-framed cards you can drive with a Bluetooth remote, prayer times calculated on the tablet,
weather from Open-Meteo, Qatar landmark wallpapers and the streaming apps you already have. It runs on
stock Android 8 and later, with no root and no system changes.

[العربية](README_AR.md)

## What it does

- **Home screen built for a remote.** Every card, button and setting is reachable with Up, Down, Left,
  Right, OK and Back. The focused card gets a gold frame and a slight zoom. Touch and mouse work too.
- **Prayer times without the internet.** Umm al-Qura (Fajr 18.5°, Isha 90 minutes after Maghrib, 120 in
  Ramadan) or the Qatar calendar style (Fajr 18°, Isha 90 minutes). Per-prayer offsets in minutes, the next
  prayer highlighted with a countdown, and the Umm al-Qura Hijri date.
- **Weather from Open-Meteo**, no API key. Temperature, humidity, wind and sky, cached on the tablet. When
  the tablet is offline it shows the last reading with its time, marked as offline. Refreshes about hourly
  in the background and on demand.
- **Entertainment hub.** Lists only the apps that are actually installed and opens them with their own
  launch intent. YouTube, Netflix, Shahid, OSN+, STARZPLAY, VLC, Kodi and Plex are recognised;
  missing ones open their Google Play page. Favourites, reordering and hiding. A local video library row.
- **Qatar wallpapers** stored on the tablet: the owner's own photos of Qatar.
  Changes on a schedule you pick, or stays pinned. Add more pictures from the tablet.
- **Screensaver** that dims the wallpaper and moves the clock every minute to spare the AMOLED panel,
  plus a system screensaver (Settings > Display > Screen saver).
- **Arabic (right to left) and English**, with the Cairo font.
- **Settings** protected by an optional PIN: language, city or location, prayer method and offsets,
  weather, wallpapers, apps, accent colour, screensaver, remote test, TV edge margin, display and DeX
  information, backup and restore to a file, and a read-only device optimisation page.

## What needs the real tablet, and what Android does not allow

- **Samsung DeX keeps its own home screen.** In DeX, open Qatar4u TV from the app list and maximise it.
  The app keeps running and resizes when it moves between the tablet and the TV.
- **A TV remote over HDMI-CEC normally does not reach a tablet.** Use a Bluetooth remote, a keyboard
  or a mouse. Settings > Remote control shows the name of every key that arrives, for testing.
- **The app cannot disable or remove other apps.** The optimisation page lists disabled and installed
  apps and opens their App info page; disabling stays an ADB step the owner approves (see `rollback/`).

## Build

Android Studio's SDK (platform 34, build-tools 34) and its bundled JDK.

```
./gradlew testDebugUnitTest assembleRelease          # Linux, macOS
powershell -ExecutionPolicy Bypass -File tools\build-windows.ps1   # Windows: APK and rollback zip in dist\
```

Release builds are signed with the local debug key so they install straight away; use your own key
before publishing anywhere.

## Install on the tablet

See [docs/INSTALL_AR.md](docs/INSTALL_AR.md). In short: USB debugging on, `adb devices` shows the tablet,
then `tools\install-windows.ps1`.

## Privacy

Weather requests use Open-Meteo over HTTPS. The optional Download apps section contacts AFTVnews and the destination chosen by the owner, then uses Android DownloadManager to download an APK. No credentials or browser sessions are copied. Location is
asked for only when you tap "Use my location" and stays on the tablet. No analytics, no accounts.

## Credits

Weather data by [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0). Cairo font, SIL Open Font
License (`licenses/Cairo-OFL.txt`). Wallpaper photos supplied by the owner, who holds the rights to them.

## Downloader codes (1.5.0 candidate)

Settings > Download apps accepts AFTVnews numeric Downloader codes (including Arabic digits) and direct HTTPS APK URLs. Use the remote keypad or type/paste a link, show the destination, then explicitly download. Progress, cancellation/deletion and reopening the last download are supported across app restarts. Only one download is managed at a time; starting another removes the previous completed/failed file.

Codes are URL shortcuts: host your APK first and generate a code at https://go.aftvnews.com/. This app does not upload files or generate AFTVnews codes. Pages requiring JavaScript, login or extra clicks open in an external browser; HTTP-only destinations are not supported. Resolution depends on the public shortener page and may need updating if its format changes.

Install APK validates the package archive and opens Android's installer. If prompted, allow installation from Qatar4u TV, return, and press Install APK again. Every install requires Android confirmation. Split APK bundles (APKS/XAPK), silent installation and automatic updating are not supported.

Validation: unit tests cover input normalization, the manual redirect parser and rejection of unsupported schemes/credentials. Before publishing, test a known developer-hosted APK code on Android 10, including consent, denied permission, completion, cancellation, offline/reconnection and process recreation. No arbitrary third-party app was installed during development.
