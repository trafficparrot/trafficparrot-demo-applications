# iOS Localhost Test App

A simple iOS app for testing HTTP GET requests to localhost URLs, useful for testing against local mock servers like Traffic Parrot.

## Features

- Configurable URL field (default: http://localhost:8081/test)
- Send GET requests to any URL
- Display response status, headers, and body
- Configured to allow HTTP connections to localhost and 127.0.0.1
- Handles large responses (truncates display at 10KB to prevent UI freezing)
- Monospaced font display with scrollable response area

## Build and Run Commands

### Build the app
```bash
xcodebuild -project LocalhostTestApp.xcodeproj -scheme LocalhostTestApp -sdk iphonesimulator -configuration Debug build -derivedDataPath build
```

### List available simulators
```bash
xcrun simctl list devices available | grep -E "iPhone|iPad"
```

### Boot a simulator
```bash
xcrun simctl boot "iPhone 16"
```

### Open Simulator UI
```bash
open -a Simulator
```

### Install the app on booted simulator
```bash
xcrun simctl install booted build/Build/Products/Debug-iphonesimulator/LocalhostTestApp.app
```

### Launch the app
```bash
xcrun simctl launch booted com.example.LocalhostTestApp
```

### Uninstall the app (if needed)
```bash
xcrun simctl uninstall booted com.example.LocalhostTestApp
```

### Terminate the app (if needed)
```bash
xcrun simctl terminate booted com.example.LocalhostTestApp
```

## All-in-one Commands

### Clean, build, and run
```bash
# Build
xcodebuild -project LocalhostTestApp.xcodeproj -scheme LocalhostTestApp -sdk iphonesimulator -configuration Debug clean build -derivedDataPath build

# Boot simulator (if not already running)
xcrun simctl boot "iPhone 16"

# Open Simulator app
open -a Simulator

# Install and launch
xcrun simctl install booted build/Build/Products/Debug-iphonesimulator/LocalhostTestApp.app
xcrun simctl launch booted com.example.LocalhostTestApp
```

### Reinstall and relaunch (for updates)
```bash
xcrun simctl terminate booted com.example.LocalhostTestApp
xcrun simctl uninstall booted com.example.LocalhostTestApp
xcrun simctl install booted build/Build/Products/Debug-iphonesimulator/LocalhostTestApp.app
xcrun simctl launch booted com.example.LocalhostTestApp
```

## Installing Traffic Parrot Root Certificate for HTTPS Testing

To test HTTPS connections with Traffic Parrot, you need to install the Traffic Parrot root certificate on your iOS simulator:

### 1. Locate the Certificate
The root certificate is included in your Traffic Parrot release:
```bash
trafficparrot-x.y.z/certificates/traffic-parrot-root-ca.pem
```

### 2. Install Certificate on iOS Simulator

```bash
xcrun simctl keychain booted add-root-cert /path/to/trafficparrot-x.y.z/certificates/traffic-parrot-root-ca.pem
```

This command directly installs the root certificate into the simulator's keychain and automatically trusts it.

### 3. Verify HTTPS Works
1. Launch the LocalhostTestApp
2. Change the URL to: `https://localhost:8082/` (or your Traffic Parrot HTTPS port)
3. Tap "Send Request"
4. You should now be able to connect via HTTPS without certificate errors

## Usage Notes

1. **On Simulator**: localhost refers to your Mac, so http://localhost:8081 will connect to a server running on your Mac
2. **On Physical Device**: localhost refers to the device itself. Use your Mac's IP address instead (e.g., http://192.168.1.100:8081)
3. **Large Responses**: The app will display up to 10KB of response body. Larger responses are truncated with a message indicating the full size
4. **Logs**: The app uses NSLog for debugging. View logs in Console.app by filtering for "LocalhostTestApp"
5. **HTTPS Testing**: Requires Traffic Parrot root certificate to be installed and trusted (see instructions above)

## Troubleshooting

### Black screen on launch
- Make sure you're using the updated version with proper scene configuration
- Check if the simulator is in dark mode (Settings > Developer > Dark Appearance)

### Can't connect to localhost
- Verify your local server is running on the specified port
- Check that the URL is correct (http not https for local servers)
- Ensure your firewall isn't blocking the connection

### Response body not showing
- Check the response size - very large responses (>10KB) are truncated
- Verify the response is valid UTF-8 text
- Check Console.app logs for debugging information