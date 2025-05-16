# SnapSafe Security on Android: a technical deep dive

SnapSafe's objective is to protect against three axis of attack: Technical, Legal, Social. This document will walk
through how we attempt to protect from each.

## Legal

SnapSafe uses a PIN to authenticate the user. We do not allow biometric authentication for one important reason: US
Courts have upheld that authorities are within their legal right to coerce a finger print or face scan from you in order
to unlock a device. They must still have probably cause to do so, but the degree to which that protects you is
situational. A PIN however resides in the contents of your mind, and is thus protected by the 5th amendment to the US
Constitution: You can not be forced to give self incriminating information.

## Social

There is a variety of social vectors we protect against.

### Weak PIN

SnapSafe requires a strong PIN, it will dissallow any PIN any that uses a single repeating number, such as "1111". Or a
straight sequence of numbers such as "2345". We also include a black list of PINs that don't fit these patterns, but are
in the top 10 most used PINs, such as "6969"

### Brute Forcing

SnapSafe only allows 10 failed attempts at the PIN. After the 10th attempt, all data is wiped. In order to slow this
down, failed attempts result in an exponential back-off. Slowing an attacker down, and giving you time to think if you
are just misremembering your PIN.

### Accidental Access

The simplest case is someone has your phone, a child perhaps, and they are thumbing through your apps.

- If the app is closed, and they open it, they are presented with the lock screen
- If the app is open, but in the background, the "Secure Window" flag is set, preventing screen shots and causing it to
  only show a white background in the task switcher.
- If the accidental attacker attempts to bring the app into the foreground, SnapSafe will check if it's session has
  expired, if it has, they will be presented with the PIN screen, other wise they will be granted access to the app. We
  did our best.

The easiest way to ensure an accidental attacker does not gain access is simply to close the app, swipe it away in the
Task Switcher. This will require any user to re-enter your PIN before accessing it.

### Coerced Access

It's possible the PIN may be coerced from you even though you are not legally required to divulge it. This could be for
any number of reasons, but in this case, where you have photos inside of SnapSafe that you might suffer retribution for,
a measure of last resort is provided:

#### The Poison Pill

This is an optional feature a user can setup, where they create a second PIN (*unique from the primary SnapSafe PIN*)
known as the Poison Pill PIN (PPP). When this PIN is entered at the PIN Verification screen, the Poison Pill is
activated. All photos will be deleted, all traces of the Poison Pill will be erased, and the PPP will hence forth, be
the "real" PIN for the app. The original PIN is entirely replaced. The attacker will be authenticated with the app and
will have no indication that the Poison Pill was activated.

#### Decoy Photos

Because it may look suspicious that a Secure Camera app has no photos in it, the user can further mark up to 10 photos
as "Decoy Photos". When the Poison Pill is activated these photos will be preserved. These should be photos for which
the user would not suffer any retribution.

##### Marking a photo as a decoy

It's extremely important that when the Poison Pill is activated, it is not apparent that anything out of the ordinary is
happening. Thus we don't have time to decrypt every decoy photo with the normal Data Encryption Key (DEK) and re-encrypt
with the Poison Pill DEK. To solve this, at the time a photo is marked as a Decoy, we re-encrypt the selected photo with
the PP-DEK, and store it off in a separate directory. During Poison Pill activation all that we have to do is delete the
files encrypted with the original DEK, and move the PP-DEK encrypted files into the normal gallery directory.

Because it is not even copying files, just deleting and moving, it is extremely fast.

### Sharing

#### How sharing an encrypted Photo is handled

Sharing is handled using the normal Android Sharing system. A URI is provided to the intended application which
references one of the encrypted photos. When the app being shared to requests the data from the URI, we stream decrypt
the photo and provide the decrypted bytes. No where inside of SnapSafe do the decrypted bytes touch the disk, and they
are only transiently available in memory in their decrypted state.

Once the bytes leave our process however, all bets are off. If you're sharing to another securely written app such as
Signal, you should probably be fine, other wise we can't provide any sort of security guarantee.

#### Meta Data

Besides the pixel data, there is meta data stored in each photo, both in the Jpeg EXIF data, as well as the file name.
By default, all EXIF data inside of the JPEG is scrubbed before sending, and the file name is randomized. This
share-time scrubbing can be disabled by the user if they don't require this level of security.

#### Obfuscating Pixel Data

If you have chosen to share a photo, you are of course choosing to reveal a large amount of information about what ever
the photo was taken of. But that doesn't mean you have to reveal everything. SnapSafe provides a secure-blurring tool to
obscure sensitive parts of the photo. This could be peoples faces, or other identifying items such as tattoos, or
license plates.

On-device face detection is used to automatically find faces that could be blurred. The user can decide which faces to
blur and which to keep. Potentially you don't want to blur every face, so that you can preserve key figures if desired.
The user can also add manual regions that need to be obfuscated.

The secure-blurring process is resistant to de-blurring techniques. It physically destroys much of the pixel data in a
non-deterministic way for a blur region:

1. It resizes the region (*regardless of aspect ratio*) down to an 8x8 grid of pixels. This step does most of the data
   destruction.
2. It randomly replaces 1/3 of the pixels with either black (0x000000FF) or white (0xFFFFFFFF) pixels
3. If it detects two visible eyes for the region, then a black bar overwrites all of the pixels in that row in the 8x8
   grid. This is to destroy data important to facial recognition algorithms such as eye color, or inter-pupillary
   distance.
4. The now distorted region is scaled back up to the original size and aspect ratio and over writes the original pixels.

## Technical

Technical attacks fall into one of two categories: Disk Access, or Memory Access.

### OS Protections

Android has robust protection against both of these at an Operating System level.

#### Disk Access

Android sandboxes each App. Every app runs as it's own Linux UserID, and gets it's own directory with permissions
granted only to that UserID.

#### Memory Access

Android has Security Enhanced Linux running in Enforcing mode. This enforces strict access controls to an apps memory
pages, preventing other apps from gaining access to the run-time memory.

### The Backup Loophole

If your device is unlocked, and hooked up to a computer running the Android Debug Bridge (ADB), then a backup can be
triggered for any app on the device. This will zip up the App's data directory, and export it as an unencrypted zip.
SnapSafe protects against this vector by disallowing backups of any kind.

### System Compromise

If a system is uncompromised, the above OS protections should protect you fully against a data breach. However, if the
OS is compromised, then several other attack vectors become possible. The best way to protect your self against a system
compromise is to use a modern device, from a manufacturer that releases frequent security patches. Having a locked boot
loader, and not rooting your device is also good security practice. If your device is rooted, all security bets are off.
Your security is in your hands at that point.

### Session Timeout

Once a session is authorized (*by the user entering their PIN*) it is valid for the user selected session timeout. (*1,
5, or 10 minutes*). On session authorization a background Service is started which monitors the session, watching for
expiration. A notification is posted to the user informing them that there is currently a valid session. This indicates
that the app is in a vulnerable state, as photos, and most importantly, their encryption key, are current resident in
memory.

While the app is in the foreground, the session will receive "keep alive" pings every 30 seconds which will extend the
session by what ever the session length is. The user can end their session by either closing the app (swiping it away in
the Task Manager) or by tapping the "Close" button on the Session Watcher notification.

This will evict all sensitive data from memory, and is an important bulwark against the key being stolen from memory if
the system is compromised. Any attack must happen during the time window of a valid session, or else there is no key in
memory to steal.

### SnapSafe Protections

SnapSafe is designed to keep you and your data safe even in a system compromise scenario.

#### Disk Access

SnapSafe saves several things to disk:

- Encrypted Photos
- Encrypted thumbnails
- Encryption Key materials (*depending on security type*)
- User Preferences (*partially ciphered*)

**Photos and Thumbnails** are encrypted with an AES/GCM 256 key. This is state of the art protection. As long as the key
is well constructed, we don't believe this can be broken even in determined and well resourced offline attacks.

**User PIN**

The User's PIN is hashed using Argon2. This is a brute force resistant hashing algorithm. When a hardware backed Key
Store is available on device, a PIN key is provisioned in the Key Store, and the PIN hash is encrypted with this before
being stored on disk. If the on-disk hash is captured in the Key Store scenario, they just get an encrypted blob. If it
was captured decrypted in memory and the attacker attempts to brute force it, it is slow to compute (*versus something
like SHA512 which is designed for speed*).

PIN Hash = Argon2(PIN + DeviceInfo)

**User Preferences**

This is the normal unencrypted Android key/value data store. We store preferences like your desired session length, and
sharing options, in plain-text.

For sensitive data such as PIN hashes, we handle this differently depending on whether or not we have a Hardware backed
KeyStore.

#### Software Only:

For these we store 12 bytes of entropy, and use it to XOR encrypt them. The key is sitting on disk right next to them,
so this is not strong security, the point of it is to obfuscate common security materials that automatic scanners might
be looking for.

For instance, the User's PIN is hashed using Argon2, the Argon2 hash format is well known, and a valuable target.
Although it is a brute force resistant hash, we would still prefer it is not found in the first place. Therefore we XOR
encrypt the bCrypt hash to obfuscate it.

#### Hardware Key Store:

A new PIN key is provisioned in the Key Store, and this is used to encrypt the Argon2 hash, which is then saved to disk.
In all but the case of an online memory compromise, this should protect the PIN hash from capture.

**Encryption Key materials**

This depends on the security your devices supports, and the options you selected during vault creation. When
constructing your key we always include two sources of entropy:

**Your PIN:** this is a small source of entry, with 4 digits thats only 10,000 possible combinations, and we restrict
some PINs such as "1111" and "1234" so it's actually a bit less that 10,000. There for longer PINs are clearly better.
But even still, these are not cryptographically large numbers. Importantly, this is the "what you know" portion of the
key.

**Device Identifier:** This is a combination of the Manufacturer name, model name, and device ID (*which resets on each
factory reset*). These strings are concatenated together, and hashed using SHA512. This helps tie the key to this
specific device.

**Data Key Salt:** This is an optional source of Entropy uses in some of our keys (*as described below*). It is 32 Bytes
of entropy from a Secure Random source.

**Argon2 Salt:** The salt used to Argon2 hash the PIN, is also used as the PBKF2 algorithm salt

- **Weak Security**
    - Your device does not support an form of Hardware Backed encryption keys.
    - In this case we require your PIN to have a minimum size of 6 digits (*instead of 4*) because here, your PIN is the
      main source of entropy during PIN creation. 6 is still not very much, a longer PIN is much better in this case.
      But it must be understood that with this security level, we can not guarantee that an attacker, given enough time,
      will not be able to crack this encryption.
    - Key = PBKF2(PIN + DeviceID, bSalt)
    - Ephemeral Key: This key is derived in memory, and is never written to disk.
- **Normal Security**
    - Your device supports a Trusted Execution Environment (TEE) for storing encryption keys and executing some
      cryptographic operations. A KEK (*Key Encryption Key*) is provisioned inside of the TEE.
    - **Key Wrapping:** This is the default mode, a Data Encryption Key is constructed in memory, then passed into the
      TEE, and encrypted with the KEK. This "Wrapped Key" is then written to disk. When the user authenticates with
      their PIN, the Wrapped key is read from disk, passed back the TEE, and finally the unwrapped Data key is resident
      in main memory.
    - **Ephemeral Key:** An optional level of security this mode never writes the fully derived key to disk. In this
      mode the dSalt is passed into the TEE, and encrypted with the KEK, and written to disk. Although this is the main
      source of entry, it is still not the full key. When the user authenticates we read the dSalt from disk, decrypt it
      with the KEK in the TEE, and finally construct the full key in memory. The key derivation function is quite slow (
      *by design of course*) so the trade off is that it now takes multiple seconds to authenticate.
    - Key = PBKF2(PIN + DeviceID + dSalt, bSalt)
- **Strong Security**
    - Your device supports a Secure Element (SE), this is the strongest security possible. Functionally it is similar to
      a TEE, but it is a physically separate chip from your main SOC. It is thus resistant to side channel attacks. It
      is also designed to be resistant to physical "chip-off" attacks where your device has been deconstructed and the
      chips put in an attack harness. In this case a SE is designed to detect this and self destruct the data inside.
    - Other than the benefits the SE adds on it's own, the rest of our security is identical to the **Normal
      Security&#32;**level.
    - Key = PBKF2(PIN + DeviceID + dSalt, bSalt)

**Photos**

On disk photos and thumbnails are encrypted using a standard Initialization Vector (IV) + AES/GCM 256. The IV is unique
to each photo, and is concatenated at the start of each file. So decryption first reads out the IV, then uses that and
the key to decrypt the rest of the file.

File = [IV + Encrypted Photo Data]

##### In-memory

What ever the security level, once the key is derived, it resides in main memory. A compromised Operation System may
result in a partial or full memory dump being possible.

**Photos:**

We keep at most 3 full photos in memory, if you are viewing a particular photo, it is that photo, and the photo on
either side of it in the gallery. We also keep potentially dozens of thumbnails in memory to improve Gallery
performance. These are all fully decrypted bitmaps. Any dump of this memory would result in easily viewable images.

We clear these from memory when your session expires, or when the system sends us a "trim" request, but that's about it.
We need them in memory in order to be able to operate.

**Encryption Key:**

Once an encryption key is derived, we immediately store it into a "Sharded Key". The purpose of this is to attempt to
fool automatic memory scanners. But this only makes things harder, not impossible. They already have fully compromised
your OS and are dumping your memory, there's only so much we can do.

First we create a randomly sized array to store the first half of the key. It is always larger than the required key
size, but not exactly the length of an AES 256 key. It is filled with entropy. Then we make a second large allocation,
attempting to split up the two key halve allocations in memory and prevent them from residing next to each other
spatially. This is the JVM, so we don't have a lot of control here, but we're making a best effort. Then we allocate the
array for the second key half. It is again randomly sized so that it doesn't look immediately like an AES key.

We then XOR the real key against the first array, and store the result in the second array. Finally we de-allocate the "
Spacer" buffer.

The original key is then thrown away.

If the attacker only gets a partial memory dump, there is a possibility that only one of the two key halves will be
included, which would be worthless. If they get a full dump and both key halves, they are still disguised in their
randomly sized arrays, and they wouldn't "look" like an AES key statistically because they have been XOR'd. The hope is
this would trick automatic scanners.

The two halves are only brought together momentarily when an actual Crypto-op is needed to be preformed.

When a Sharded Key is evicted from memory, the arrays are zero'd out before release. This is actually redundant because
at an OS Level, Android always zeros memory during allocation. But you never know.

**Time-Window**

All of these sensitive bits of data in memory are evicted as soon as possible. When the user's authenticated session
expires, we evict the photos. thumbnails. and encryption keys. So any attack must execute within the time window that
the data is actually resident in memory.