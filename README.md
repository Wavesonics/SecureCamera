# Snap Safe

*The camera that minds its own business.*

Snap Safe is an Android camera app that keeps every pixel—and every byte of data—exactly where it belongs: on **your**
device.

---

## Why Snap Safe?

| We do…                                                          | We never do…                         |
|-----------------------------------------------------------------|--------------------------------------|
| Capture photos ~~& video~~ locally                              | Phone home or talk to servers        |
| Encrypt everything in private storage                           | Slurp analytics or usage stats       |
| Let **you** decide if GPS tags are added (_precision optional_) | Sprinkle ads or trackers in the code |
| Strip out tell‑tale metadata automatically                      | Read files outside our sandbox       |

---

## Key Features

* **Zero‑Leak Design** – The manifest skips `android.permission.INTERNET`; nothing leaves your device.
* **Fully Encrypted** – Shots are written to encrypted, app‑private storage.
* **Metadata Scrub‑A‑Dub** – EXIF and other identifiers are wiped the instant you hit *Share*.
* **PIN‑Locked Gallery** – A separate PIN stands between curious thumbs and your photos.
* **Secure Sharing** – When you *do* share, we hand the file off via Android’s native share sheet—no detours.
* **Granular Location** – Add coarse, fine, or zero location data—your call.
* **100 % Open Source** – Auditable code in plain sight.

### On the Roadmap

* Auto‑blur faces (because sometimes anonymity matters).
* Optical zoom controls & other camera goodies.
* Encrypted video recording.

---

## Contributing

Pull requests are happily accepted.

Start with an issue or draft PR and we can talk it through.

### Automated Publishing

The project uses GitHub Actions to automatically build and publish new releases to Google Play when a tag with the
format `v*` (e.g., `v1.0.0`) is pushed. See the [GitHub Actions workflow documentation](.github/workflows/README.md) for
details on how this works and the required setup.

The project includes a pre-configured [FastLane](https://fastlane.tools/) setup for automating the deployment process.
See the [FastLane documentation](fastlane/README.md) for details on how to use it for manual deployments or to customize
the metadata.

---

## License

Snap Safe is released under the [MIT License](LICENSE). Use it, fork it, improve it—just keep it open.

---

## Privacy

Our full, ultra‑brief Privacy Policy lives in [PRIVACY.md](PRIVACY.md). Spoiler: we collect nothing.
