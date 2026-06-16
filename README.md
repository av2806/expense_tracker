# Expense Tracker 💸

A privacy-focused Android application that automatically tracks expenses by parsing bank SMS alerts. Built with modern Android development practices, the app eliminates manual expense entry while keeping all financial data stored locally on your device.

---

## 🚀 Features

* **Automatic SMS Parsing** – Detects and logs transactions from bank SMS alerts.
* **Privacy-First** – All data is stored locally using Room Database.
* **Smart Categorization** – Categorizes transactions based on merchant keywords.
* **Modern UI** – Built entirely with Jetpack Compose.
* **Manual Entry** – Add expenses that are not received through SMS.
* **Real-Time Updates** – UI updates automatically using Kotlin Flow.

---

## 🛠 Tech Stack

| Component        | Technology        |
| ---------------- | ----------------- |
| Language         | Kotlin            |
| UI               | Jetpack Compose   |
| Architecture     | MVVM              |
| Database         | Room              |
| Async Processing | Coroutines & Flow |
| SMS Handling     | BroadcastReceiver |

---

## 🏗 Architecture

The application follows the **MVVM** architecture pattern.

### Data Flow

```text
Bank SMS
   ↓
SmsReceiver
   ↓
SMS Parser
   ↓
Repository
   ↓
Room Database
   ↓
ViewModel
   ↓
Compose UI
```

---

## ⚙️ Getting Started

### Prerequisites

* Android Studio (Koala or newer)
* JDK 17+
* Android Device or Emulator (API 24+)

### Installation

1. Clone the repository:

```bash
git clone https://github.com/av2806/expense_tracker.git
cd expense_tracker
```

2. Open the project in Android Studio.

3. Sync Gradle and run the application.

---

## 📱 Permissions

The app requires the following permissions for automatic expense tracking:

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
```

> All SMS processing happens locally on the device. No data is uploaded or shared.

---

## 📋 Roadmap

* [ ] Monthly spending analytics
* [ ] CSV export
* [ ] Dark mode
* [ ] Custom categories
* [ ] Budget tracking
* [ ] Multi-bank SMS support
