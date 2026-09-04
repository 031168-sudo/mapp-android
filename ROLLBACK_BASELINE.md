# AlfaSputnik rollback baseline

This branch is reserved for rebuilding from the known-working APK baseline.

Authoritative working APK (local reference):
- SHA-256: `1186d2521626141ac65d4d76029261f01ad0c745a16a4460ebf9fdfe4a8aba1c`
- Size: 1,536,963 bytes

Required change:
- Remove only the visible `statusDiag` diagnostic block from Status.
- Do not change the map.
- Do not change signal refresh logic.
- Keep the diagnostic runtime objects/references so the existing JavaScript remains valid.

The APK baseline is not represented by the original source ZIP alone; the working APK contains a later `assets/code.js`. Therefore this branch must be built from that APK-derived code, not from the original ZIP's `code.js`.

Local prepared source archive:
- SHA-256: `5adb127f3ba925970860c42e49ddba877f3d8f91d72daf803b81010d4c808e89`
- The prepared `assets/code.js` passes `node --check`.
