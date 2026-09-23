# 📱 ICAI Batch Checker (ICAI Batch Slot Monitor)

[![Release](https://img.shields.io/github/v/release/iambalaji-k/icaibatchcheck?color=brightgreen&label=Version)](https://github.com/iambalaji-k/icaibatchcheck/releases/tag/v2.0.0)
[![Android 14 Ready](https://img.shields.io/badge/Android-14%2B%20Ready-green.svg)](https://developer.android.com/about/versions/14)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-orange.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-brightgreen.svg)](.github/workflows/build.yml)

> **Automated, Real-Time Seat Availability Monitoring & Dual-Alert System for Mandatory ICAI Training Courses.**

---

## 📌 Overview

**ICAI Batch Checker** is a high-performance native Android application engineered for Chartered Accountancy (CA) students across India. It automates the process of monitoring seat availability for mandatory Institute of Chartered Accountants of India (ICAI) courses on the official [ICAI Online Registration Portal](https://www.icaionlineregistration.org/LaunchBatchDetail.aspx):

* **ICITSS** (Information Technology Course & Orientation Course)
* **AICITSS** (Advanced IT Course & Management & Communication Skills - MCS)

Because course slots at ICAI Program Organizing Units (POUs) fill up within minutes of launch, this app continuously polls ICAI servers in the background and sends **instant push notifications** and **Telegram bot alerts** the moment seats become available.

---

## ✨ Key Features

### ⚡ Multi-Target Background Monitoring
* Concurrent background checking across multiple ICAI **Regions** (Southern, Western, Northern, Central, Eastern), **POU Cities**, and **Courses**.
* **Dynamic POU / Center Dropdown**: Centers & cities are fetched live and dynamically from ICAI portal based on the selected region.
* **Unified Course Dropdown Selector**: Clean Material 3 dropdown keeping the configuration form compact and above the fold.
* Powered by a persistent Android **Foreground Service** (`BatchMonitorService`) with user-configurable polling intervals (2m, 5m, 10m, 15m, 30m).

### 🔔 Instant Dual-Alert Engine & Zero Mock Data
* **Android Push Notifications**: High-priority device alerts with custom sound, vibration, and big-text details.
* **Telegram Bot Integration**: Delivers live HTML-formatted alerts directly to your personal Telegram chat or group, complete with instant registration links.
* **Strict Live Error Reporting**: Zero simulated mock data fallbacks. If ICAI servers time out or face errors, the app accurately logs the error in the audit trail without triggering false alarms.

### 🎨 3-Way Theme Switcher (Light, Dark & AMOLED)
* Cyclical single-tap icon toggle in the Top App Bar:
  * **Light Mode**: Crisp, high-contrast daytime layout.
  * **Dark Mode**: Elegant slate dark palette.
  * **AMOLED True Black**: Pure `#000000` pitch black background designed to completely turn off OLED pixels and maximize battery savings.
  * Automatically defaults to your Android system theme and persists your preference.

### 🔋 Battery & Doze-Mode Optimization
* Features CPU `WakeLock` management to wake up the processor silently during scheduled checks when screen is off.
* Android 14+ compatibility guards preventing background launch crashes (`ForegroundServiceStartNotAllowedException`).

### 📊 Modern Material 3 Dashboard
* **Centered KPI Metrics**: Quick-glance cards for *Open Seats* and *Total Batches* with balanced typography.
* **Dedicated Action Banner**: Prominent, one-tap button to launch the official ICAI registration portal in your browser.
* **Real-Time Search & Filter Chips**: Search by city, dates, or venue; filter by *All Batches* or *Open Seats Only (🎉)*.
* **Seat Capacity Progress Bar**: Visual indicator showing open vs. total seats and percentage capacity.

### 💾 Local Cache & Activity Logs
* **Room Database (`AppDatabase`)**: Persists batch records offline.
* **Timestamped Audit Logs**: Keeps a complete log history of every background check event, seat alert, or network status.

---

## 🛠️ Technology Stack

* **Language**: Kotlin 2.0+
* **UI Framework**: Jetpack Compose, Material Design 3
* **Async & Threading**: Kotlin Coroutines & StateFlow
* **Network & Scraping**: OkHttp 4, JSoup (HTML parsing & ASP.NET WebForms postback handling)
* **Database**: Room Database (SQLite)
* **System Services**: Android Foreground Service, Notification Manager, PowerManager WakeLock
* **CI/CD**: GitHub Actions (Cloud APK Build)

---

## 🚀 Getting Started & Installation

### Option A: Install Pre-Built APK (Latest v2.0.0)
1. Go to the [v2.0.0 Release](https://github.com/iambalaji-k/icaibatchcheck/releases/tag/v2.0.0).
2. Download **`icaibatchcheckv2.0.apk`**.
3. Install the APK on your Android device (Android 7.0 / API 24 or higher).

---

## 🤖 Setting Up Telegram Alerts (Optional)

Receive instant slot opening notifications directly on your phone or group via Telegram:

1. **Get Bot Token**:
   * Open Telegram and search for [@BotFather](https://t.me/BotFather).
   * Send `/newbot` and follow instructions to get your **HTTP API Bot Token** (e.g., `123456789:ABCdefGhIJK...`).

2. **Get Chat ID**:
   * Search for [@userinfobot](https://t.me/userinfobot) on Telegram and tap **Start**.
   * Copy your numeric **Id** (e.g., `987654321`).

3. **Configure App**:
   * Open **ICAI Batch Checker** ➔ **Settings** tab ➔ **Telegram Alerts**.
   * Paste your **Bot Token** and **Chat ID**, turn **ON** the switch, and tap **Save Config** & **Test Bot**.

---

## 🔋 Recommended Phone Settings (Xiaomi, Samsung, OnePlus, Vivo)

On smartphones with aggressive battery-management software:

1. **Disable Battery Saver Restrictions**:
   * Go to **Phone Settings ➔ Apps ➔ ICAI Batch Checker ➔ Battery**.
   * Select **Unrestricted** (or *No Restrictions*).
2. **Enable Auto-Start** *(Xiaomi / Vivo / Oppo / Realme)*:
   * Go to **Phone Settings ➔ Apps ➔ Auto-Start** and enable **ICAI Batch Checker**.

---

## 🏗️ Building From Source

### Prerequisites
* **Android Studio** Ladybug or newer
* **JDK 17**
* **Android SDK 36**

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/iambalaji-k/icaibatchcheck.git
   cd icaibatchcheck
   ```
2. Open the project in Android Studio.
3. Sync Gradle and build the APK:
   * Menu: **Build ➔ Build Bundle(s) / APK(s) ➔ Build APK(s)**
   * Output path: `app/build/outputs/apk/debug/app-debug.apk`

---

## ⚠️ Disclaimer & Legal Notice

> **IMPORTANT NOTICE**: Please read this disclaimer carefully before downloading, installing, or using the ICAI Batch Checker application.

### 1. Unofficial Application & Non-Affiliation Statement
* **Independent Community Project**: This application is an independent, community-driven, open-source utility developed solely for personal convenience and educational purposes.
* **No Endorsement or Affiliation**: This software is **NOT** affiliated with, endorsed by, sponsored by, authorized by, maintained by, or in any way officially connected with **The Institute of Chartered Accountants of India (ICAI)**, the Board of Studies (BoS), any of its regional councils (WIRC, SIRC, NIRC, EIRC, CIRC), student associations (WICASA, SICASA, etc.), branch offices, or any of its officers or subsidiaries.
* **Official Portals**: The official ICAI website is accessible at [icai.org](https://www.icai.org/) and the official student registration portal is located at [icaionlineregistration.org](https://www.icaionlineregistration.org/).

### 2. Trademark & Intellectual Property Notice
* All registered trademarks, product names, logos, acronyms, and brand names mentioned within this application or documentation—including but not limited to **ICAI**, **ICITSS**, **AICITSS**, **MCS**, **ITT**, and **Orientation Course**—are the exclusive intellectual property of their respective trademark holders, primarily The Institute of Chartered Accountants of India.
* The use of these names, acronyms, and trademarks is solely for nominative, descriptive, and informational identification purposes to assist students in recognizing relevant training courses. Such use does not imply any affiliation, sponsorship, or endorsement.

### 3. Limitation of Liability & Express Disclaimer of Warranties
* **Provided "AS IS"**: This software is provided on an **"AS IS"** and **"AS AVAILABLE"** basis, without warranty of any kind, express, implied, statutory, or otherwise, including but not limited to warranties of merchantability, fitness for a particular purpose, non-infringement, or availability.
* **No Guarantee of Performance or Notification**: The developer(s), author(s), and maintainer(s) do not guarantee continuous, uninterrupted, or error-free operation of the background monitoring service or notification engine. The developer(s) assume no responsibility or liability for:
  - Third-party web portal outages, structural HTML changes, network timeouts, or rate limiting on ICAI servers.
  - Delayed, dropped, failed, or inaccurate device push notifications or Telegram bot alerts.
  - Missed course registration deadlines, full batch seat allocations, or inability to secure a batch seat.
  - Any direct, indirect, incidental, special, exemplary, punitive, or consequential damages (including loss of data, missed academic sessions, or monetary loss) arising from or in connection with the use or performance of this application.

### 4. Zero Financial Transactions & Non-Commercial Nature
* This application is **100% free and open-source**.
* It does **NOT** collect payments, store banking credentials, handle registration fees, or process course transactions. All registrations and financial transactions must be performed directly by the user on the official ICAI web portal ([icaionlineregistration.org](https://www.icaionlineregistration.org/)).

### 5. User Responsibility & Compliance
* Users are solely responsible for verifying all batch details (dates, timings, venue, seat availability) on the official ICAI portal before making registration or travel decisions.
* Users agree to use this application in compliance with all applicable local laws, regulations, and website terms of service.

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for more information.

---

<p align="center">Made with ❤️ for CA Students in India</p>
