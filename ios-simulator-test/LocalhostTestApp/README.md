# Localhost Test App for iOS

This is a simple iOS app designed to test HTTP GET requests to localhost URLs, which is useful for testing against local mock servers.

## Features

- Configurable URL field (default: http://localhost:8081/test)
- Send GET requests to any URL
- Display response status, headers, and body
- Configured to allow HTTP connections to localhost

## Setup Instructions

1. Open Xcode and create a new iOS App project:
   - Product Name: LocalhostTestApp
   - Interface: Storyboard
   - Language: Swift

2. Replace the generated files with the provided files:
   - `ViewController.swift`
   - `Main.storyboard`
   - `Info.plist`
   - `AppDelegate.swift`
   - `SceneDelegate.swift`

3. Build and run the app on a simulator or device

## Usage

1. Enter the URL you want to test (default is http://localhost:8081/test)
2. Tap "Send GET Request"
3. View the response status, headers, and body in the text area

## Important Notes

- The app is configured to allow HTTP (non-HTTPS) connections to localhost and 127.0.0.1
- When testing on a physical device, localhost refers to the device itself, not your development machine
- For testing against a server on your development machine from a physical device, use your machine's IP address instead of localhost

## Troubleshooting

If you're testing on a physical device and need to connect to your development machine:
1. Find your machine's IP address (e.g., 192.168.1.100)
2. Use that IP instead of localhost (e.g., http://192.168.1.100:8081/test)
3. Make sure your firewall allows connections on the port you're using