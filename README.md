# StartupPulse

StartupPulse is an Android application designed to monitor and provide insights into the performance of startups. It leverages Firebase for real-time data and notifications.

## Features

- **Push Notifications:** Receive real-time updates and alerts.
- **Location Services:** Track and monitor location-based data.
- **Audio Recording:** Capture and store audio recordings.

## Permissions

The app requests the following permissions:
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `RECORD_AUDIO`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 28)
- `POST_NOTIFICATIONS`

## Installation

1. Clone the repository.
2. Open the project in Android Studio.
3. Build the project using Gradle:
   ```sh
   ./gradlew build
   ```
