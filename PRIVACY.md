# Privacy Policy — ICAI Batch Checker

**Last updated:** 2026-09-27

ICAI Batch Checker is an independent, unofficial utility. It is **not affiliated with,
endorsed by, or connected to** the Institute of Chartered Accountants of India (ICAI).

## What the app does

The app checks publicly available batch/seat listings on the ICAI online registration
portal and can notify you when seats open. It has no accounts, no backend server of its
own, no ads and no analytics or tracking SDKs.

## Data handling

All data stays on your device, except for the two network operations you explicitly
trigger or configure:

1. **ICAI portal requests** — When you add targets and enable monitoring, the app
   fetches the public pages of `www.icaionlineregistration.org` needed to display seat
   availability for the Region/Center/Course combinations you configured. These requests
   contain only the dropdown values you selected; no personal identifiers are sent.

2. **Telegram alerts (optional)** — If you configure a Telegram Bot Token and Chat ID,
   alert messages (course, center, batch name, seat count, dates) are sent to the
   Telegram API (`api.telegram.org`) so your bot can message you. You provide these
   credentials yourself; they are stored only on your device in a private file that is
   **excluded from cloud backup and device transfer**. Removing the configuration, or
   the app, deletes them.

## Not collected

- No names, phone numbers, e-mail addresses, location, contacts, photos or files.
- No analytics, crash reporting, advertising or attribution identifiers.
- No data is sold or shared with any third party.

## Local storage

- App settings, monitored targets and the activity log are stored locally
  (SharedPreferences and a Room database). Uninstalling the app removes them.
- The Telegram bot token/chat ID live in a separate preferences file excluded from
  Android's auto-backup (`dataExtractionRules`/`fullBackupContent`).

## Your control

- Monitoring can be paused or stopped at any time from the app.
- Alerts require the Android notification permission, which you can revoke in system
  settings; the app then simply cannot show notifications.
- Deleting the Telegram configuration disables all Telegram traffic immediately.

## Contact

Issues and source code: the project's public GitHub repository (see README.md).
