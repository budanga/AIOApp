# AIOApp

**AIOApp** is an Android application written entirely in **Kotlin**, designed as a modular **all‑in‑one hub** where multiple utilities and features can coexist inside a single app.

The goal is simple: one app, many tools, clean architecture.

---

## Table of Contents

* [About](#about)
* [Features](#features)
* [Tech Stack](#tech-stack)
* [Project Structure](#project-structure)
* [Getting Started](#getting-started)
* [Requirements](#requirements)
* [Installation](#installation)
* [Roadmap](#roadmap)
* [Contributing](#contributing)
* [License](#license)

---

## About

AIOApp (All‑In‑One App) is a personal and experimental project focused on learning, scaling, and experimenting with Android development using modern tools and best practices.

Rather than creating multiple small apps, AIOApp groups different functionalities into independent modules under a single codebase, allowing easy expansion over time.

---

## Features

* 100% **Kotlin**
* **Jetpack Compose** UI
* Modular and scalable architecture
* Navigation handled via a centralized NavHost
* Clean separation between UI, domain, and data layers
* Designed to grow with new tools and mini‑apps

*Current and planned modules may include:*

* File manager
* Utility tools
* Experimental features
* Personal productivity helpers

---

## Tech Stack

* **Kotlin**
* **Android SDK**
* **Jetpack Compose**
* **Navigation Compose**
* **Gradle (Kotlin DSL compatible)**

---

## Project Structure

```
AIOApp/
├── core/          # Core utilities, navigation, shared logic
└── ui/            # Screens and UI components
    ├── home/
    ├── filemanager/
    └── ...
```

The structure is intentionally flexible to allow new modules without rewriting existing ones.

---

## Getting Started

Follow these steps to run the project locally.

---

## Requirements

* **Android Studio** (latest recommended)
* **Android SDK** (API level according to the project configuration)
* **Kotlin** 1.x
* **Gradle** (handled automatically by Android Studio)

---

## Installation

1. Clone the repository:

```bash
git clone https://github.com/LautaroBudin/AIOApp.git
```

2. Open the project in **Android Studio**

3. Let Gradle sync and download dependencies

4. Run the app on an emulator or physical device

---

## Roadmap

Planned improvements and ideas:

* Expand the file manager module
* Add more independent utilities
* Improve UI consistency and theming
* Introduce persistent storage where needed
* Improve testing coverage

This roadmap is intentionally flexible and may change as the project evolves.

---

## Contributing

Contributions are welcome.

If you want to contribute:

1. Fork the repository
2. Create a new branch:

```bash
git checkout -b feature/your-feature-name
```

3. Commit your changes:

```bash
git commit -m "Describe your changes clearly"
```

4. Push your branch and open a Pull Request

---

## License

This project is currently **unlicensed**.

You are free to explore the code for learning purposes. A license may be added in the future.

---

If you find bugs, have ideas, or want to discuss improvements, feel free to open an issue.
