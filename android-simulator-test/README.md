# Localhost Test App for Android

This is a simple Android app designed to test HTTP/HTTPS GET requests to localhost URLs, which is useful for testing against local mock servers and APIs.

## Prerequisites

- **Android Studio**: Download and install from [developer.android.com](https://developer.android.com/studio)
- **Android SDK**: Automatically installed with Android Studio
- **Java Development Kit (JDK)**: Android Studio includes this

## Setup Instructions

1. Navigate to the `android-simulator-test` directory:
   ```bash
   cd android-simulator-test
   ```

2. Build the project:
   ```bash
   ./gradlew build
   ```

3. Create and start Android emulator:
   
   ```bash
   ~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.0 -writable-system &
   ```
   
4. Install the app:
   ```bash
   ./gradlew installDebug
   ```

5. Start the app from command line:
   ```bash
   ~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.localhosttestapp/.MainActivity
   ```
   
   Alternatively, launch manually from your device's app drawer (appears as "Localhost Test App")

## Usage

1. Enter the URL you want to test (default is http://localhost:8081/test)
2. Tap "Send GET Request"
3. View the response status, headers, and body in the scrollable text area

## Testing with Local HTTPS

The app supports HTTPS connections, including self-signed certificates commonly used in local development. Here are several ways to test HTTPS locally.

### Traffic Parrot HTTPS Setup

If you're using Traffic Parrot for API mocking with HTTPS, you'll need to install Traffic Parrot's root CA certificate:

   ```bash
   # Step 1: Generate the certificate hash
   HASH=$(openssl x509 -inform PEM -subject_hash_old -in /path/to/trafficparrot-x.y.z/certificates/traffic-parrot-root-ca.pem | head -1)
   
   # Step 2: Copy certificate with hash name and .0 extension
   cp /path/to/trafficparrot-x.y.z/certificates/traffic-parrot-root-ca.pem ${HASH}.0
   
   # Step 3: Push certificate to Android device
   ~/Library/Android/sdk/platform-tools/adb push ${HASH}.0 /sdcard/
   ```
   
* Open the emulator
* Import the certificate `Settings -> Security & privacy -> More Security -> Encryption and credentials -> Install a certificate`
* Test HTTPS with Traffic Parrot, open the app and point it at `https://10.0.2.2:8082/test`

## Important Notes

### Network Configuration
- The app is configured to allow HTTP (non-HTTPS) connections to localhost via `android:usesCleartextTraffic="true"`
- HTTPS connections work with both valid and self-signed certificates
- The app includes proper internet permissions in the manifest

### HTTPS Certificate Handling
- The app accepts self-signed certificates for testing purposes
- Production apps should implement proper certificate validation
- Certificate errors will be shown in the response if they occur

