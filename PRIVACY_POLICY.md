# Privacy Policy for Drawer Launcher

**Last Updated:** November 15, 2026

Drawer Launcher ("Drawer", "we", "our") is designed with a strict **offline-first, zero-telemetry** architecture. We believe your home screen, personal apps, notifications, and tasks belong exclusively to you.

---

## 1. Zero Data Collection and Zero Telemetry

- **No Remote Servers:** Drawer does not operate external tracking servers, analytics endpoints, or remote databases.
- **No Analytics SDKs:** Drawer contains zero third-party analytics SDKs, advertising libraries, or behavioural tracking tools (no Firebase Analytics, no Google Analytics, no Facebook SDK, no advertising IDs).
- **No Crash Uploads:** Drawer does not automatically transmit crash logs, stack traces, or device diagnostic reports across the internet.

---

## 2. On-Device Data Storage and Processing

All data generated while using Drawer remains strictly on your local device storage:

- **App Database & Preferences:** Your dock layout, app groups, to-do items, place reminders, and display settings are stored locally in private application sandboxes using Android Room and DataStore.
- **Local AI & App Search:** On-device search and categorization run 100% locally on your device hardware. Queries and search keystrokes are never transmitted across a network.
- **Backups:** When you export a launcher backup, the JSON archive is saved directly to your local file system or your chosen local folder. We cannot access your backup files.

---

## 3. Android System Permissions and Justifications

Drawer requests only permissions necessary to deliver launcher functionality and on-device utilities:

| Permission | Purpose | Data Handling |
| :--- | :--- | :--- |
| `QUERY_ALL_PACKAGES` | Allows Drawer to list, search, and launch installed applications on your device as your primary home screen. | Processed entirely on-device; never transmitted or shared. |
| `REQUEST_DELETE_PACKAGES` | Allows you to uninstall applications directly from the home screen or app drawer. | Standard Android system intent. |
| `BIND_ACCESSIBILITY_SERVICE` | Enables optional "Double-tap to lock screen" gesture (`GLOBAL_ACTION_LOCK_SCREEN`). | Drawer cannot and does not inspect screen content (`canRetrieveWindowContent=false`) or record gestures. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Displays notification badges and optional privacy masking on your home screen dock. | Notification text and counts are read locally into temporary memory and discarded. Never saved to remote storage. |
| `ACCESS_FINE_LOCATION` & `FOREGROUND_SERVICE_LOCATION` | Triggers place-based to-do alerts when you arrive at or leave custom reminder locations. | Coordinates are checked against your saved to-do places locally on-device. No location telemetry is ever uploaded or shared. |
| `POST_NOTIFICATIONS` & `SCHEDULE_EXACT_ALARM` | Delivers timely to-do reminders, deadline alerts, and ongoing service status. | Scheduled and handled locally via Android AlarmManager and WorkManager. |
| `USE_BIOMETRIC` | Authenticates access to the Hidden App Vault. | Managed securely by the Android OS BiometricPrompt API; biometrics never touch app code. |

---

## 4. Third-Party Services and Links

Drawer does not share, sell, rent, or trade your personal information with any third party. The application operates independently of third-party cloud services.

---

## 5. Children's Privacy

Drawer does not collect any personal information from anyone, including children under the age of 13.

---

## 6. Changes to this Policy

If we modify this Privacy Policy, the updated document will be published in this repository with an updated revision date.

---

## 7. Contact Us

If you have questions about this Privacy Policy or Drawer Launcher's privacy architecture, please open an issue on our GitHub repository:
- **Repository:** [https://github.com/urunkarpm/Drawer](https://github.com/urunkarpm/Drawer)
