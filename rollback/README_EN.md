# Qatar4u TV rollback package

This package puts the Galaxy Tab S6 (SM-T865) back the way it was before Qatar4u TV.

## What is inside

| File | Contents |
|---|---|
| `Changes_Log.csv` | Every change made to the tablet, with the command that undoes it |
| `Installed_Packages.txt` | The package list as it was before any change |
| `Backup_Instructions.md` | How to back up before you roll back |
| `Restore_Instructions.md` | The restore steps, in order |
| `ADB_Recovery_Commands.txt` | The restore commands, ready to paste |

## What was changed

- 44 Samsung apps disabled (Bixby, ad and telemetry services, AR stickers, Game Launcher and a few more). Disabled, not removed: each can be enabled again at any time.
- Animations set to half length.
- Screen kept on while charging.
- Qatar4u TV Home, then Qatar4u TV, installed, and the launcher made the Home app.

No system app was removed, no root was used and the system partition was not touched.

## Before you start

Do not run the restore commands before you have looked at the tablet's current state. Run these two and compare them with `Changes_Log.csv`:

```
adb shell pm list packages -d
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

Then follow `Restore_Instructions.md`.
