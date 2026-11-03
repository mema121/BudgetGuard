# 💰 BudgetGuard
### Android Personal Finance Management Application

BudgetGuard is an Android-based personal finance management application developed independently using Kotlin and Jetpack Compose.

The application helps users manage income, expenses, and financial transactions while incorporating authentication and PIN-based security features.

## ✨ Key Features

- **User Authentication:** User registration and login using Firebase Authentication.
- **Financial Management:** Record and manage income and expense transactions.
- **Dashboard:** View financial summaries and transaction information.
- **Transaction History:** Review previously recorded financial activities.
- **Financial Insights:** Access financial information through an insights screen.
- **PIN Security:** PIN protection with PBKDF2-HMAC-SHA256 hashing and randomly generated salt.
- **Login Attempt Protection:** Limits repeated incorrect PIN attempts with temporary lockout.
- **Privacy Mode:** Additional privacy controls for financial information.
- **Cloud Database:** Integration with Firebase Cloud Firestore.

## 🛠️ Technologies Used

- **Programming Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Development Environment:** Android Studio
- **Authentication:** Firebase Authentication
- **Database:** Cloud Firestore
- **Security:** PBKDF2-HMAC-SHA256, PIN Authentication

## 🔐 Security Implementation

BudgetGuard incorporates several security mechanisms:

1. PIN hashing using PBKDF2-HMAC-SHA256 with random salt.
2. Temporary account access restrictions after repeated incorrect PIN attempts.
3. Firebase Authentication for user authentication.
4. Privacy Mode to provide additional control over the display of financial information.

These features demonstrate the implementation of application security concepts in Android development.

## 📱 Application Screenshots

Screenshots of the application interface will be added to this section.

## 🚀 Getting Started

1. Clone this repository.
2. Open the project using Android Studio.
3. Configure a Firebase project with Authentication and Cloud Firestore.
4. Add the appropriate `google-services.json` configuration file to the `app/` directory.
5. Configure the necessary Firebase security rules.
6. Sync Gradle and run the application on an Android emulator or device.

**Note:** Firebase configuration is not included in this public repository. A separate Firebase project is required to run the application.

## 👩‍💻 Developer

**Azmisava Sandra Latema**

Cybersecurity Engineering Student (Rekayasa Keamanan Siber)

**Project Type:** Individual Project  
**Year:** 2026

## 📌 Project Purpose

This project was developed to apply Android application development skills and explore security mechanisms for protecting access to personal financial information.
