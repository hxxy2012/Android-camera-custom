# ProShot - Professional Camera App 📷

<div align="center">

![ProShot Logo](https://img.shields.io/badge/ProShot-Professional_Camera-FF9500?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin)
![Min SDK](https://img.shields.io/badge/Min_SDK-24-green?style=for-the-badge)
![Target SDK](https://img.shields.io/badge/Target_SDK-34-blue?style=for-the-badge)

**A professional-grade Android camera application for photography enthusiasts and professional photographers**

</div>

---

## 🎯 Project Overview

**ProShot** is not just another camera app - it's a powerful creation tool designed for photography enthusiasts and professional photographers. It provides DSLR-level manual controls, RAW shooting, AI-powered assistance, creative shooting modes, and professional-grade post-processing capabilities.

### 🎨 Design Philosophy

- **Minimalist Professional Design** - Black theme optimized for photography
- **Gesture-Based Quick Operations** - Intuitive touch controls
- **Real-time Parameter Preview** - See changes instantly
- **Zero Shutter Lag** - Capture the moment precisely
- **Intuitive UI/UX** - Professional yet accessible

---

## ✨ Current Features (Phase 1-3 Implemented)

### 📸 Phase 1: Professional Manual Controls ⭐⭐⭐

#### Manual Exposure Control
- **ISO Control**: 100-6400 (device-dependent) with real-time adjustment
- **Shutter Speed**: Logarithmic control from 1/8000s to 1s
- **White Balance**: Color temperature control (2000K-10000K)
- **Exposure Compensation**: ±3 EV adjustment in 1/3 steps
- **Real-time Preview**: See parameter changes instantly

#### Advanced Focus System
- **AF-S** (Single Autofocus): One-shot autofocus
- **AF-C** (Continuous Autofocus): Tracking autofocus
- **MF** (Manual Focus): Full manual control with distance display

#### Professional Metering
- Evaluative metering (matrix)
- Center-weighted metering
- Spot metering
- Real-time exposure feedback

### 🎨 Phase 2: Composition Aids

#### Grid Overlays
- **Rule of Thirds**: Classic composition grid
- **Golden Ratio**: Phi-based composition guide
- **Diagonal Lines**: Dynamic composition assistance
- **No Grid**: Clean viewfinder

#### Shooting Assistants
- Real-time parameter display
- Professional black theme UI
- Touch-based parameter adjustment
- Visual feedback system

### 📦 Phase 3: RAW Support ⭐⭐⭐

#### RAW Capture
- **DNG Format**: Adobe standard RAW format
- **RAW + JPEG**: Capture both formats simultaneously
- **Full Bit Depth**: 12/14/16-bit color depth
- **Maximum Dynamic Range**: Preserve all image data

#### Image Formats
- RAW Only (DNG)
- JPEG Only (High Quality)
- RAW + JPEG (Professional workflow)

---

## 🏗️ Technical Architecture

### Technology Stack

#### Core Technologies
- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose + Custom Views
- **Camera API**: Camera2 API (low-level control)
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Coroutines**: Kotlin Coroutines for async operations

#### Key Libraries
```gradle
// Core Android
- AndroidX Core KTX
- Lifecycle & ViewModel
- Activity Compose

// Compose
- Compose BOM 2024.01.00
- Material 3
- Material Icons Extended

// Camera
- Camera2 API (manual control)
- CameraX (future extensions)

// Dependency Injection
- Hilt 2.48

// Image Processing
- ExifInterface
- DNG Creator

// AI (Future)
- TensorFlow Lite
- ML Kit

// Utilities
- Timber (logging)
- Coil (image loading)
- DataStore (preferences)
```

### Project Structure

```
app/
├── src/main/
│   ├── java/com/proshot/camera/
│   │   ├── di/                          # Dependency Injection
│   │   │   └── AppModule.kt
│   │   ├── domain/                      # Domain Layer
│   │   │   ├── model/
│   │   │   │   ├── CameraParameter.kt   # Camera parameter types
│   │   │   │   ├── CameraSettings.kt    # App settings
│   │   │   │   └── CameraState.kt       # Camera state
│   │   │   └── repository/
│   │   │       └── CameraRepository.kt  # Repository interface
│   │   ├── data/                        # Data Layer
│   │   │   ├── camera/
│   │   │   │   ├── Camera2Manager.kt    # Camera2 API wrapper
│   │   │   │   ├── ManualCameraController.kt  # Manual controls
│   │   │   │   └── RawCaptureController.kt    # RAW capture
│   │   │   └── repository/
│   │   │       └── CameraRepositoryImpl.kt
│   │   ├── presentation/                # Presentation Layer
│   │   │   ├── camera/
│   │   │   │   ├── CameraScreen.kt      # Main camera screen
│   │   │   │   ├── CameraViewModel.kt   # ViewModel
│   │   │   │   └── components/
│   │   │   │       ├── CameraPreview.kt # Camera preview
│   │   │   │       └── CameraControls.kt # Manual controls UI
│   │   │   ├── theme/                   # App theme
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Theme.kt
│   │   │   │   └── Type.kt
│   │   │   └── MainActivity.kt
│   │   └── ProShotApplication.kt
│   └── res/                             # Resources
│       ├── values/
│       │   ├── colors.xml
│       │   ├── strings.xml
│       │   └── themes.xml
│       └── xml/
│           ├── backup_rules.xml
│           ├── data_extraction_rules.xml
│           └── file_paths.xml
└── build.gradle.kts
```

### Architecture Layers

#### 1. Domain Layer
- **Models**: Pure Kotlin data classes representing camera parameters
- **Repository Interface**: Abstract camera operations
- **Use Cases**: Business logic (future)

#### 2. Data Layer
- **Camera2Manager**: Low-level Camera2 API operations
- **ManualCameraController**: Professional manual control implementation
- **RawCaptureController**: RAW image capture and DNG creation
- **Repository Implementation**: Concrete camera repository

#### 3. Presentation Layer
- **ViewModels**: State management and business logic
- **Composables**: UI components
- **Theme**: Professional dark theme with orange accents

---

## 🎨 UI/UX Design

### Color Palette

```kotlin
// Dark Theme (Primary)
Pure Black:    #000000    // Main background
Dark Gray:     #1C1C1E    // Surface
Medium Gray:   #3A3A3C    // Surface variant

// Accent Colors
Orange:        #FF9500    // Primary accent (focus, selected)
Yellow:        #FFCC00    // Warning, highlights
Green:         #34C759    // Success, confirmation
Red:           #FF3B30    // Error, critical

// Overlays
Semi-transparent Black: #CC000000   // Control backgrounds
Overlay Background:     #DD000000   // Top/bottom bars
```

### Typography

- **Parameter Values**: Roboto Mono (monospace for precision)
- **UI Text**: Roboto Regular
- **Headings**: Roboto Bold
- **Labels**: Roboto Medium

### UI Components

#### Top Info Bar
- Real-time display of ISO, Shutter Speed, WB, EV
- Transparent background overlay
- Orange accent highlights

#### Left Parameter Controls
- Vertical drag sliders for each parameter
- Visual feedback on adjustment
- Current value display
- Range indicators

#### Right Tool Buttons
- Grid toggle
- Histogram toggle
- Level toggle
- Additional tools (future)

#### Bottom Control Bar
- Large circular capture button
- Focus mode selector (AF-S / AF-C / MF)
- Mode switcher (future)

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK 34
- Gradle 8.2+

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/hxxy2012/Android-camera-custom.git
cd Android-camera-custom
```

2. **Open in Android Studio**
- File → Open → Select project directory
- Wait for Gradle sync to complete

3. **Run the app**
- Connect Android device or start emulator
- Click Run (or Shift + F10)
- Grant camera permissions when prompted

### Build APK

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## 📱 Permissions

ProShot requires the following permissions:

### Required
- **CAMERA**: Capture photos and videos
- **WRITE_EXTERNAL_STORAGE**: Save images (API ≤28)
- **READ_MEDIA_IMAGES**: Access saved images (API ≥33)

### Optional
- **ACCESS_FINE_LOCATION**: GPS tagging
- **RECORD_AUDIO**: Video with audio
- **VIBRATE**: Haptic feedback

---

## 🎯 Roadmap

### ✅ Completed (Phase 1-3)
- [x] Camera2 API integration
- [x] Manual exposure controls (ISO, Shutter, WB, EV)
- [x] Professional UI with dark theme
- [x] Focus modes (AF-S, AF-C, MF)
- [x] Grid overlays (Rule of Thirds, Golden Ratio, Diagonal)
- [x] RAW (DNG) capture support
- [x] JPEG + RAW simultaneous capture

### 🚧 In Progress (Phase 4-5)
- [ ] Histogram display (RGB + Luminance)
- [ ] Waveform and Vectorscope
- [ ] Focus peaking
- [ ] Zebra stripes (overexposure warning)
- [ ] Long exposure mode
- [ ] Timelapse mode
- [ ] HDR mode
- [ ] Night mode

### 🔮 Future (Phase 6-9)
- [ ] AI scene recognition
- [ ] AI composition assistant
- [ ] RAW processing engine
- [ ] Professional video mode (4K, 60fps)
- [ ] Slow motion
- [ ] Manual video controls
- [ ] Non-destructive editing
- [ ] Filter library (100+ filters)
- [ ] Learning system
- [ ] Community sharing

---

## 📖 User Guide

### Basic Usage

#### 1. Opening the App
- Launch ProShot
- Grant camera permission when prompted
- Camera preview will start automatically

#### 2. Adjusting Parameters

**ISO**
- Drag up/down on the ISO control (left side)
- Lower values = less noise, need more light
- Higher values = more noise, works in low light

**Shutter Speed**
- Drag up/down on the S control (left side)
- Faster (1/1000) = freeze motion
- Slower (1/30) = motion blur, needs stability

**White Balance**
- Drag up/down on the WB control (left side)
- Lower (2000K) = cooler, bluer tones
- Higher (10000K) = warmer, orange tones

**Exposure Compensation**
- Drag up/down on the EV control (left side)
- Negative values = darker image
- Positive values = brighter image

#### 3. Focus Modes

**AF-S (Single Autofocus)**
- Best for still subjects
- Tap to focus, focus locks until you move

**AF-C (Continuous Autofocus)**
- Best for moving subjects
- Camera continuously adjusts focus

**MF (Manual Focus)**
- Full control over focus distance
- Best for precise focus control

#### 4. Composition Aids

**Grid**
- Tap grid button (right side)
- Cycle through: None → Rule of Thirds → Golden Ratio → Diagonal

**Histogram** (Coming Soon)
- Real-time exposure analysis
- Prevent over/underexposure

**Level** (Coming Soon)
- Ensure camera is level
- Perfect for landscapes and architecture

#### 5. Capturing Images

**JPEG Capture**
- Tap the large circular button
- Image saved to DCIM/ProShot/

**RAW Capture** (Future)
- Enable RAW in settings
- Larger file size, maximum quality
- Best for post-processing

---

## 🔧 Configuration

### Camera Settings

Edit `CameraSettings.kt` to customize defaults:

```kotlin
data class CameraSettings(
    val shootingMode: ShootingMode = ShootingMode.MANUAL,
    val imageFormat: ImageFormat = ImageFormat.JPEG_ONLY,
    val jpegQuality: Int = 95,
    val gridType: GridType = GridType.NONE,
    val showHistogram: Boolean = false,
    val showLevel: Boolean = false,
    val meteringMode: MeteringMode = MeteringMode.MATRIX,
    val gpsTagging: Boolean = false
)
```

---

## 🐛 Known Issues

1. **Preview aspect ratio**: May not match capture aspect ratio on some devices
2. **RAW capture delay**: Slight delay when saving DNG files
3. **Permission handling**: Requires manual permission grant on first launch
4. **Device compatibility**: Full manual control requires HARDWARE_LEVEL_FULL

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add comments for complex logic
- Write clean, readable code

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**ProShot Development Team**

- GitHub: [@hxxy2012](https://github.com/hxxy2012)
- Repository: [Android-camera-custom](https://github.com/hxxy2012/Android-camera-custom)

---

## 🙏 Acknowledgments

- Android Camera2 API documentation
- Jetpack Compose community
- Photography community for feature inspiration
- Open source contributors

---

## 📸 Screenshots

> Screenshots will be added after UI is finalized

---

## 🔗 Resources

- [Android Camera2 API Guide](https://developer.android.com/training/camera2)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [DNG Format Specification](https://helpx.adobe.com/photoshop/digital-negative.html)
- [Photography Basics](https://www.cambridgeincolour.com/tutorials.htm)

---

<div align="center">

**Made with ❤️ for photographers by photographers**

[Report Bug](https://github.com/hxxy2012/Android-camera-custom/issues) ·
[Request Feature](https://github.com/hxxy2012/Android-camera-custom/issues) ·
[Documentation](https://github.com/hxxy2012/Android-camera-custom/wiki)

</div>
