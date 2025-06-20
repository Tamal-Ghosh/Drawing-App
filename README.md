# 🎨 Drawing App

A simple yet powerful Android drawing application built with **Kotlin**. This app allows users to draw freely on a canvas, adjust brush size and color, undo strokes, set background colors, import images, and save their creations. Designed with a clean, modern **Material Design** UI.

---

## ✨ Features

- 🖌️ **Freehand Drawing** on canvas  
- 📏 **Adjustable Brush Size** using a slider  
- 🎨 **Multiple Preset Colors** and advanced **Color Picker**  
- ↩️ **Undo/Redo** strokes  
- 🖼️ **Set Canvas Background Color**  
- 📂 **Import Images** from the device gallery  
- 💾 **Save Drawings** to device storage (Android 10+ compatible using MediaStore API)  
- 🧑‍🎨 **Modern Material Design UI**
- 🌙 **Dark/Light Theme Toggle** from the navigation drawer  
- 📤 **Share Last Saved Drawing** directly from the navigation drawer

---

## 🛠️ Tech Stack

- **Language:** Kotlin (with some Java)  
- **Framework:** Android SDK  
- **Concurrency:** Kotlin Coroutines for background operations  
- **Color Picker:** [AmbilWarna](https://github.com/yukuku/ambilwarna) library  

---

## 🔐 Permissions

The app requires the following permissions:

- `READ_EXTERNAL_STORAGE` – for importing images  
- `WRITE_EXTERNAL_STORAGE` – for saving drawings *(for Android versions below 10)*  
- `READ_MEDIA_IMAGES` – for Android 13+ image access

> ✅ For Android 10 and above, the app uses **MediaStore API** for saving files without requesting storage permission.

---

## 📦 Setup & Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Tamal-Ghosh/Drawing-App.git
