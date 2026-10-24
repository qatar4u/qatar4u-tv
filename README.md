# Qatar4u TV

A home screen for the Samsung Galaxy Tab S6 (SM-T865) that turns it into a living-room entertainment hub:
large gold-framed cards you can drive with a Bluetooth remote, prayer times calculated on the tablet,
weather from Open-Meteo, Qatar landmark wallpapers and the streaming apps you already have. It runs on
stock Android 10 and later, with no root and no system changes.

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

The only network request is the Open-Meteo forecast for your city's coordinates, over HTTPS. Location is
asked for only when you tap "Use my location" and stays on the tablet. No analytics, no accounts.

## Credits

Weather data by [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0). Cairo font, SIL Open Font
License (`licenses/Cairo-OFL.txt`). Wallpaper photos supplied by the owner, who holds the rights to them.
