![Build Status](https://github.com/DarshaunDavis/test-strip-marketplace/actions/workflows/android.yml/badge.svg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

# Test Strip Marketplace

> A classified ads app for buyers, sellers, and wholesalers of diabetic supplies—featuring barcode scanning, location-based matching, sponsored ads, and ratings.

---

## 📝 Table of Contents

- [🔍 Overview](#overview)
- [📷 Screenshots](#screenshots)
- [✨ Features](#features)
- [🛠 Tech Stack](#tech-stack)
- [🚀 Getting Started](#getting-started)
- [📱 Usage](#usage)
- [🏗 Project Structure](#project-structure)
- [📆 Roadmap](#roadmap)
- [🤝 Contributing](#contributing)
- [📄 License](#license)
- [📬 Contact](#contact)

---

## Overview

Test Strip Marketplace is a B2B Android app where users can post and browse paid listings for excess diabetic supplies. The app offers barcode scanning, buyer/seller ratings, profile visibility, geolocation-based matching, and role-specific functionality for Users, Buyers, Wholesalers, and Admins.

## Screenshots

> _Coming soon – UI mockups and screenshots will be added after the UI polish phase._

---

## Features

- **Classified Ads** with repost/delete options
- **Sponsored Listings** pinned above free listings
- **Barcode Scanner** to match products to buyers
- **Buyer & Wholesaler Profiles**
- **User Ratings & Testimonials**
- **Geolocation-Based Matching**
- **In-App Role Management** (Guest, User, Buyer, Wholesaler, Admin)
- **(Optional) In-App Messaging** between users and buyers

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM + Repository Pattern
- **Navigation:** Jetpack Navigation Component
- **Backend:** Firebase Authentication & Firestore
- **Barcode:** ZXing or ML Kit
- **Location:** FusedLocationProviderClient
- **Billing:** Google Play Billing (optional for ad boosts)

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11+
- A Firebase project (Auth + Firestore) set up
- Google Services JSON (`google-services.json`) in `app/`

### Installation
1. **Clone the repo**
   ```bash
   git clone https://github.com/DarshaunDavis/test-strip-marketplace.git
   cd test-strip-marketplace
   ```

2. Open in Android Studio
    - File ▶ Open ▶ select the project root

3. Add Firebase config
    - Place your `google-services.json` in `app/`

4. Build & Run
    - Let Gradle sync, then run on your emulator or device

---

## Usage
1. Launch app and allow location permissions (optional)
2. Register or log in as a User, Buyer, or Wholesaler
3. Post ads or scan a product to find nearby buyers
4. Leave ratings/testimonials based on interactions
5. Pay to promote listings or sponsor your profile

---

## Project Structure
```
app/
├─ src/
│  ├─ main/
│  │  ├─ java/com/lislal/teststripmarketplace/...
│  │  ├─ res/
│  │  ├─ AndroidManifest.xml
│  └─ test/  
└─ build.gradle
```

---

## Roadmap
- 🔄 Post Expiration & Auto-Bump
- 📦 Product Catalog Integration
- 📨 In-App Messaging
- 🎯 Advanced Ad Targeting
- 🎨 Custom White-Label Themes

---

## Contributing
* Fork the repo
* Create a feature branch (`git checkout -b feat/your-feature`)
* Commit your changes (`git commit -m "feat: add …"`)
* Push (`git push origin feat/your-feature`)
* Open a Pull Request

---

## License
This project is licensed under the [MIT License](LICENSE).

---

## Contact
Darshaun Davis – darshaun.davis@gmail.com  
Project Link: https://github.com/DarshaunDavis/test-strip-marketplace
