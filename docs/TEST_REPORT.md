# تقرير الاختبار · Test report — Qatar4u TV 1.0.0

Tested on 23 October 2026 on the real tablet: Galaxy Tab S6 SM-T865, Android 10, build T865XXU2BTC7.
Input was sent as key events over ADB (`input keyevent`), which the app receives exactly like a USB or
Bluetooth keyboard or remote. Evidence is a screenshot sheet per test, kept on the build computer in
`Downloads\tab-s6-backup\` (named below); none of them is a mock-up or an emulator.

**Result key:** ✅ passed with evidence · ⚠️ passed with a note · ❌ failed · ⏳ not run yet (needs hardware or the owner)

| # | Test | Result | Evidence and notes |
|---|---|---|---|
| 1 | Build, unit tests, release APK | ✅ | `gradlew testDebugUnitTest assembleRelease` succeeded; 7 prayer-time tests passed, including Doha 23 Oct 2026 within 3 minutes of the published Umm al-Qura times. APK 4.9 MB. |
| 2 | Install and run on Android 10 | ✅ | `adb install` Success; `mCurrentFocus` = `net.qatar4u.tv/.MainActivity`. |
| 3 | Set as Home, Home key returns to it | ✅ | `resolve-activity HOME` = `net.qatar4u.tv/.MainActivity`; `t-launch.jpg` step 2. |
| 4 | Arabic right-to-left layout, Cairo font, Latin digits | ✅ | `shot-s.jpg`, `recon.jpg`. |
| 5 | First card focused on start, gold frame and zoom | ✅ | `shot-s.jpg`: YouTube framed. |
| 6 | D-pad through every Home row and back up | ✅ | `nav-home.jpg`: Watch → All apps → Google Play → Library and back to the weather card. |
| 7 | Settings by remote only: open, move through sections, into options, Back | ✅ | `nav-settings.jpg`, `t-saver.jpg`: each section previews on focus; Back returns to Home with focus kept. |
| 8 | Menu key opens the app menu | ✅ | `nav-settings.jpg` step 12. |
| 9 | Open an installed app (YouTube) and come back with Home | ✅ | `t-launch.jpg`: focus `com.google.android.youtube/...HomeActivity`, then back to Qatar4u TV. |
| 10 | Prayer times offline | ✅ | Calculated on the tablet; they stayed on screen with Wi-Fi and mobile data off (`t-offline.jpg`). |
| 11 | Weather from Open-Meteo | ✅ | 33°, clear, humidity 52–55%, wind 3–4 km/h for Doha (`shot-s.jpg`). |
| 12 | Weather with the connection lost | ✅ | Wi-Fi and data off, refresh pressed: card shows "offline · last reading 17:32" in red and greys the temperature (`t-offline.jpg`). |
| 13 | Weather when the connection comes back | ✅ | Wi-Fi and data on again: refreshed by itself within 30 s, "last update 17:40" (`recon.jpg`). |
| 14 | Hourly background refresh | ⚠️ | WorkManager job scheduled (60 min, network required). One full hour not yet observed; check the "last update" time after an hour. |
| 15 | Screensaver: dims, shows clock and next prayer, any key exits | ✅ | `t-saver.jpg` steps 13–14. |
| 16 | Wallpapers from the tablet, rotation | ✅ | Owner's photos and illustrations both shown (`shot-s.jpg`, `recon.jpg`); rotation every 30 min by default. |
| 17 | Memory | ✅ | 121 MB PSS on a 5.3 GB tablet (`dumpsys meminfo`). |
| 18 | Smoothness | ⚠️ | 152 frames, 90th percentile 29 ms, 37% janky, measured across screen changes and the first wallpaper decode. Scrolling within Home is smooth to the eye; worth a longer measurement. |
| 19 | Change city | ⏳ | Implemented (Settings > City & location); not changed during this session to keep Doha. |
| 20 | Backup and restore of settings | ⏳ | Implemented (file save and open); needs the owner to pick a file location on the tablet. |
| 21 | Restart the tablet | ⏳ | The remote shell refuses `adb reboot`. The owner restarts once and checks that Qatar4u TV comes back as Home. |
| 22 | Real Bluetooth remote | ⏳ | Needs the remote. Settings > Remote control shows each key's name for this test. |
| 23 | Samsung DeX on the TV | ⏳ | Needs the USB-C to HDMI cable. Expected: opened from the DeX app list, resizable, not the DeX home. |
| 24 | HDMI mirroring and sound over HDMI | ⏳ | Needs the cable and the TV. |
| 25 | Android 12 | ⏳ | The tablet is on Android 10; not updated during this session. |
| 26 | Battery use over a day | ⏳ | Needs a day of use: Settings > Battery > Qatar4u TV. |
