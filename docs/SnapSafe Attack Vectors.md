# SnapSafe Attack Vectors

##  

## Casual attacker, Unlocked Phone

This could be anything from a child to an authority figure with hard power over you who has your device. Your user files
are decrypted, but still protected by normal Linux permissions, so this attacker will have no access to them.

SnapSafe has several bulwarks to protect you here:

1. If the app is close, opening it will present the attacker with a PIN screen.
    1. Attempts at guessing the PIN are throttled with an exponential back off
    2. A strong PIN is required, no straight sequences or mono-digit PINs
    3. After 10 failed attempts, all data is wiped
    4. Number of failed attempts and current back-off timeout are both persisted over app restart and device restart
2. If the app is open in the background
    1. The app will not show a preview in the Task Switcher
    2. The app has a session timeout, so it's possible that when the attacker attempts to foreground the app, they will
       be required to enter a PIN
    3. If the session is still valid, the attacker will gain full access to the app, but only for the remainder of the
       session. After which they will be required to re-authenticate to continue exploiting it.

## Determined attacker, Unlocked Phone, Unexploited

The attacker has plugged you device into a computer. Their software uses only "normal" means such as ADB to pull data
off of the phone.

SnapSafe protects against this:

1. No data from SnapSafe will be able to be copied off of the device, Nothing is stored in public directories, and
   SnapSafe restricts all forms of Backups, so no normal tools will be allowed to create a backup of SnapSafe's private
   data.

## Determined attacker, Unlocked Phone, Exploited, Filesystem permissions bypassed

The attacker has plugged you device into a computer. Their software succeeds in exploiting vulnerabilities present on
this particular device. If they manage to compromise the OS's permissions and restrictions, they may gain access to
SnapSafe's private files.

SnapSafe protects against this:

1. The PIN is hashed using Argon2, a brute force resistant cryptographically secure hashing algorithm. This is then
   encrypted using a Hardware backed key. What would be captured on disk is a strongly encrypted blob who's key resides
   in the Hardware Key store.
2. All sensitive files are encrypted using the primary Data Encryption Key, a 256 bit AES key.
    1. In the default mode, this DEK is wrapped using a KEK in the Hardware Keystore (TEE or SE), and saved to disk. So
       what would be captured is a strongly encrypted blob.
    2. In the ephemeral mode, this DEK is only ever derived in memory, and is never written to disk

## Determined attacker, Unlocked Phone, Exploited, Memory access bypassed, app is resident in memory

The attacker has plugged you device into a computer. Their software succeeds in exploiting vulnerabilities present on
this particular device. If they manage to compromise the OS's permissions and restrictions, they may gain access to some
or all of SnapSafe's memory.

**Case A: The app is in memory, but the session has expired**

There is no sensitive data in memory. The encryption key, photos, and thumbnails have all been evicted from memory.

**Case B: The app is in memory, with a valid session**

This is the worst case scenario, but we can still provide some protection. The DEK is stored in a Shared Key, where it
is split with an XOR cipher, and the two halves are only brought back together briefly when a crypto op is actually
being executed.