# StickyFocus Android (minimal scaffold)

This folder contains a minimal Android app scaffold demonstrating:

- Show an Activity when the device is unlocked
- Let the user input a "now" task, save to SharedPreferences
- A persistent overlay that shows the latest task and current time
- A simple Share button to share the task with other apps

Notes
- The overlay requires the user to grant "Display over other apps" (SYSTEM_ALERT_WINDOW). Use the "オーバーレイ許可を開く" button in the Activity to open the settings screen.
- The app starts a foreground service to monitor unlock events (ACTION_USER_PRESENT). On unlock it starts the input activity.
- The overlay is implemented as a foreground service (`OverlayService`).

How to run
1. Open the `android` folder in Android Studio.
2. Build and install on a device (emulator may block overlay permissions).
3. Launch the app, grant overlay permission, and press the button once to start services.
4. Lock and unlock the device to see the input Activity appear.

Caveats & next steps
- This is a minimal scaffold. For production you should:
  - Use Room for persistence if you need history
  - Implement Usage Access or Accessibility Service to detect foreground app
  - Add safety around permissions and explain privacy
  - Handle battery optimizations and test across Android versions

