# FastLane Setup for Snap Safe

This directory contains the FastLane configuration for automating the deployment of Snap Safe to Google Play.

## Directory Structure

- `Fastfile`: Defines the deployment lanes for FastLane
- `Appfile`: Contains app-specific configuration (package name, service account key path)
- `metadata/`: Contains app metadata for Google Play
    - `android/`: Platform-specific metadata
        - `en-US/`: English (US) metadata
            - `title.txt`: App title
            - `short_description.txt`: Short app description
            - `full_description.txt`: Full app description
            - `images/`: App screenshots and images
                - `phoneScreenshots/`: Screenshots for phones

## How to Use

### Prerequisites

1. Install FastLane:
   ```bash
   gem install fastlane
   ```

2. Set up a Google Play service account and download the JSON key file
    - Place the key file in the fastlane directory as `play-store-config.json`
    - Make sure the service account has the necessary permissions in the Google Play Console

### Deploying to Google Play

To deploy a new version to Google Play:

1. Update the version information in `gradle/libs.versions.toml`
2. Build the release AAB:
   ```bash
   ./gradlew bundleRelease
   ```
3. Run the deploy lane from the project root directory:
   ```bash
   fastlane deploy
   ```

> **Note**: The Fastfile is configured to look for the AAB file at `app/build/outputs/bundle/release/app-release.aab`
> relative to the project root directory. Make sure you run the `fastlane deploy` command from the project root, not from
> the fastlane directory.

### Automated Deployment

The project uses GitHub Actions to automatically build and deploy new releases to Google Play when a tag with the format
`v*` (e.g., `v1.0.0`) is pushed. See the [GitHub Actions workflow documentation](../.github/workflows/README.md) for
details.

## Customizing Metadata

To update the app metadata:

1. Edit the files in the `metadata/android/en-US/` directory
2. Add screenshots to the `metadata/android/en-US/images/phoneScreenshots/` directory
3. Run the deploy lane to upload the updated metadata to Google Play
