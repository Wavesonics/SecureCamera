# Secure Camera – Privacy Policy
*Effective April 20 2025*

Secure Camera was built with privacy as its first‑class feature.  
Below is the plain‑language summary of what happens to your data when you use the app:

| What we collect | Where it goes | Why we need it |
|-----------------|--------------|----------------|
| **Nothing**—no personal data, usage analytics, or diagnostics | Nowhere (the app has *no* Internet permission) | We don't   |

---

## 1. Data Collection
Secure Camera does **not** collect, store, or transmit any personally identifiable information (PII), usage data, or any other kind of data what so ever. All photos and videos remain solely on your device, unless you explicitly share them.

## 2. Network Access
The app’s manifest deliberately omits `android.permission.INTERNET`. Consequently, Secure Camera is technically incapable of connecting to the Internet, cloud services, or third‑party APIs.

## 3. File Access
Secure Camera operates entirely within its private app storage for saving images you capture. It does **not** request broad file‑system access (e.g., `MANAGE_EXTERNAL_STORAGE`) and never reads files outside its own sandbox.

## 4. Permissions Used
* `android.permission.CAMERA` – required to capture photos and video.
* `android.permission.ACCESS_COARSE_LOCATION` User's can optionally grant this permission if they want obfuscated location data saved with their photos.
* `android.permission.ACCESS_FINE_LOCATION` User's can optionally grant this permission if they want precise location data saved with their photos.

No other runtime permissions are requested.

## 5. Third‑Party Services
None. Secure Camera is 100 % open source and contains no advertising or analytics libraries.

## 6. Changes to This Policy
If Secure Camera’s behavior ever changes in a way that affects privacy, this document will be updated in the source repository and a new release will note those changes.

## 7. Contact
Questions or concerns? Open an issue in the [GitHub repository](https://github.com/Wavesonics/SecureCamera).
