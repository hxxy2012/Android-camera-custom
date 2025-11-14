# ProShot ProGuard Rules

# Keep camera classes
-keep class androidx.camera.** { *; }
-keep class android.hardware.camera2.** { *; }

# Keep TensorFlow Lite
-keep class org.tensorflow.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
