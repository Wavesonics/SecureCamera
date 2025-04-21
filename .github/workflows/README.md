# GitHub Actions Workflow for Google Play Publishing

This directory contains GitHub Actions workflow configurations for automating the build and deployment process of the
Secure Camera app to Google Play.

## Workflow: Publish to Play Store

The `publish-to-play-store.yml` workflow automatically builds and publishes the app to Google Play when a new tag with
the format `v*` (e.g., `v1.0.0`) is pushed to the repository.

### Workflow Steps

1. Checkout the code
2. Set up JDK 11
3. Set up Ruby and install Fastlane
4. Decode the Android keystore from a base64-encoded secret
5. Build the release AAB with proper signing
6. Decode the Google Play service account key
7. Deploy to Google Play using Fastlane

### Required Secrets

The following secrets must be configured in your GitHub repository settings:

1. **ENCODED_KEYSTORE**: Base64-encoded Android keystore file
   ```bash
   # Generate using:
   base64 -w 0 keystore.jks > keystore_base64.txt
   ```

2. **KEYSTORE_PASSWORD**: Password for the keystore

3. **KEY_ALIAS**: Alias of the key in the keystore

4. **KEY_PASSWORD**: Password for the key

5. **PLAY_STORE_CONFIG_JSON**: Google Play service account JSON key file content
    - This is used by Fastlane to authenticate with Google Play
    - You need to create a service account in the Google Play Console with the appropriate permissions

### How to Use

1. Set up all the required secrets in your GitHub repository settings
2. When you're ready to release a new version:
    - Update the version information in `gradle/libs.versions.toml`
    - Commit and push the changes
    - Create and push a new tag with the format `v1.0.0` (matching your version)
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. The workflow will automatically trigger and deploy the app to Google Play

### Troubleshooting

If the workflow fails, check the following:

1. Ensure all secrets are correctly configured
2. Verify that the keystore is valid and contains the correct key
3. Make sure the Google Play service account has the necessary permissions
4. Check that the app's version code has been incremented since the last release
