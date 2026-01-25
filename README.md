# MCFactor: Next-Gen 2FA for Minecraft Servers
![MCFactor Banner](https://cdn.modrinth.com/data/cached_images/c4857bbc14abf3431bb12b084414ebe166dd7d3f.png)
**Secure your players. Protect your server.**

**MCFactor** is a comprehensive Two-Factor Authentication (2FA) solution that pairs your Minecraft server with a dedicated, modern mobile application (iOS & Android). It is designed to prevent unauthorized logins—whether from hackers or just siblings sharing a computer.

Unlike traditional map-based Captchas or text-based passwords, MCFactor uses **SHA3-256 encryption** and **time-based tokens** synced with a mobile device to ensure top-tier security without ruining the gameplay experience.

### 📱 The Companion App

This plugin requires players to use the **MCFactor** app.

* **iOS / Android Supported**
* **Modern UI:** Dark/Light mode.
* **Secure:** Tokens refresh every 3 minutes.

![App Screens](https://cdn.modrinth.com/data/cached_images/46c3ccf9085ae0fef8a419de3e7014cb26de7c5b.png)

---

## ✨ Features

* **Folia & Paper & Spigot support:** Fully supports modern server software, including asynchronous processing on **Folia**.
* **BEDROCK Support (Geyser):** Bedrock players are also supported.
* **Code Auth:** The player enters a 6-digit dynamic code from the app into the game chat.
* **App Auth (Seamless):** If enabled, the server opens a local REST API port. Players simply tap "Authenticate" on their phone, and the server logs them in automatically!
* **Login sessions are frozen:** Players cannot move, break blocks, or interact until authenticated.


* **Global Language Support:** 11 languages including English, Japanese, Spanish, Chinese, and Hindi.

---

## 🛠️ Install

1. **Install:** Drop the `MCFactor-x.x.x-SNAPSHOT.jar` into your plugins folder.
2. **Auto-Setup:** On the first launch, the plugin automatically connects to the backend API to retrieve a unique **Server ID** and **Secret Key**. No manual database setup is required!

---

## 📚 Wiki
https://github.com/yamak493/MCFactor-Plugin/wiki

---

## ⚙️ Configuration

This plugin is mostly self-setting up.:

* **`auth-method`**: Choose between `code` (manual entry) or `app` (push-only).
* **`port`**: Define the port for the REST API if using App Auth.
* **`language`**: Set the default server language.

*Note: Player data (UUID links) is stored locally in `link.json`.*

---

## 📜 Commands & Permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/2fa <AppKey>` | Link your account to the mobile app. | `mcfactor.use` |
| `/2fa` | Unlink your current device. | `mcfactor.use` |
| `/2fa disconnect <player>` | (Admin) Force unlink a specific player. | `mcfactor.admin` |
| `/2fa help` | Show the help menu. | `mcfactor.use` |

---

## 🤝 License
* This plugin is open source.
* You're welcome to use the REST API we host in your own apps (although we're not sure if it will be useful).
* The companion application is ARR (All rights reserved).

*The companion application source code is not publicly available.*

---

## 📥 Download app

* Android: [Install]()
* iOS: [Install]()

*Note: The app displays ads.*