# Stability candidate — 26 October 2026

This change is a review candidate, not a published release. Version remains 1.3.0 until approval.

## Changes

- Pushes and pull requests run unit tests, lint, and a release build. Successful builds upload a candidate APK for 30 days. Test reports are uploaded even if validation fails.
- Publishing is separate: a manual workflow on `main` with `publish` explicitly enabled, after validation succeeds. Existing releases and tags are retained; an existing version tag stops publication.
- Location rejects fixes older than five minutes, invalid coordinates, or accuracy worse than 5 km. If no suitable cached fix exists, request one coarse network fix for up to 15 seconds. Failure leaves the selected city unchanged. No background location permission or tracking is introduced.
- Added location policy boundary tests and Gulf prayer calculation invariants across seasons and New Year. These invariants do not certify agreement with an official timetable.

## Claude review handoff

Claude has not reviewed these changes. Connect Claude Code to this repository separately, then ask it to review this branch against main, focusing on:

1. GitHub Actions event conditions, permissions, fork pull requests, preservation of historical releases, and version checks.
2. Android 10 coarse-location permission, provider availability, cancellation, stale fixes, and activity recreation.
3. Prayer tests: distinguish mathematical invariants from independently sourced reference times.

Do not merge or publish during review. Record findings with file/line references; rerun validation after fixes.

## Validation status

Local Gradle execution was attempted but blocked while downloading Gradle 8.9 (`Network is unreachable`). No local Android build or unit-test pass is claimed. GitHub Actions must provide the build, lint, and test evidence for this exact commit.

The historical `TEST_REPORT.md` concerns version 1.0.0 and is not acceptance evidence for this candidate.

## Device acceptance before publication

All items below are pending; use the candidate APK on the SM-T865 with Android 10.

- Export settings before installing. Confirm the candidate has the same signing certificate as the installed APK; do not uninstall to bypass a mismatch.
- Deny location: city selection remains usable. Enable location: recent fix succeeds. Disable location/network: bounded failure keeps the previous city. Repeated presses do not start duplicate requests; close the activity during a request.
- Check prayer times against an independently sourced local timetable, both methods, offsets, midnight transition and Ramadan configuration.
- Use a real Bluetooth remote: all rows/settings, long OK, Back, return from another app, and wake from screensaver. Repeat with Arabic RTL and English.
- Test DeX and HDMI separately, including sound, resizing, focus, and return from apps. DeX keeps its own Home screen.
- Confirm weather refresh after at least one full hour, offline cache and reconnection, and after changing city.
- Export/import settings; reboot manually; verify chosen Home and PIN.
- Capture repeatable frame timing for cold launch, wallpaper rotation and scrolling before considering performance changes. No performance improvement is claimed by this candidate.
- Keep the known-good APK and settings backup. APK downgrade may require ADB downgrade support; do not erase app data without owner approval.

## Publish after approval

Increment versionCode/versionName, update release notes, rerun validation and device checks. Merge the approved change, then manually run the workflow on main with publish enabled. Main pushes alone do not publish.
