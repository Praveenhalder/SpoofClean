# ⚡ Spoof Clean

**A tiny performance boost app for iQOO / vivo devices.**
Unlock full CPU & GPU performance in your games and apps with a single tap.

![Size](https://img.shields.io/badge/size-26KB-brightgreen)
![Platform](https://img.shields.io/badge/platform-Android-blue)
![Devices](https://img.shields.io/badge/devices-iQOO%20%7C%20vivo-orange)
![License](https://img.shields.io/badge/license-GPLv3-blue)

---

## ✨ Features

- 🚀 **No complicated setup**: install, grant two permissions, done
- 🔔 **Quick toggle**: turn Spoof Mode on/off straight from the notification control center
- 🪶 **Super lite**: the entire app is only **26 KB**
- 🔓 **Open source**: read the code, modify it, and add your own features

<img width="1260" height="792" alt="IMG_20261005_231127" src="https://github.com/user-attachments/assets/4cc0151d-32d0-4ea9-96fe-ec78ecfdf7ba" />

---

## 🧠 How It Works

vivo and iQOO devices unlock full CPU and GPU performance **only for benchmark apps**. Everything else runs under thermal and performance limits.

Spoof Clean uses the **same package name as AnTuTu**, which tricks the device into treating it as a benchmark app and unlocking full throttle while Spoof Mode is on.

---

## 📲 How to Use

1. **Install** the APK
2. **Open the app once** to enable the required permissions:
   - Notification
   - Display over other apps (draw overlay)
3. **Open the game or app** you want to boost
4. **Pull down the control center** and add the **Spoof Mode** tile if it isn't already there
5. **Tap Spoof Mode** and enjoy! 🎮

---

## 📥 Download

Grab the latest APK from the [Releases](../../releases) page.

---

## 🛠️ Build From Source

```bash
git clone https://github.com/Praveenhalder/SpoofClean.git
cd spoof-clean
./gradlew assembleRelease
```

The APK will be generated in `app/build/outputs/apk/release/`.

---

## ⚠️ Disclaimer

- Running at full performance increases **heat and battery drain**. Keep an eye on your device temperature during long sessions.
- Turn Spoof Mode **off** when you don't need it.
- This project is **not affiliated with** AnTuTu, vivo, or iQOO. All trademarks belong to their respective owners.
- Use at your own risk. Results may vary by device model and software version.

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repo
2. Create your branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m "Add my feature"`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

Bug reports and feature ideas are welcome in [Issues](../../issues).

---

## 📄 License

Released under the [GNU General Public License v3.0](LICENSE). You're free to use, modify, and share this project, but any modified versions you distribute must also be open source under the same license.

---

<p align="center">If this helped you, consider giving the repo a ⭐</p>
