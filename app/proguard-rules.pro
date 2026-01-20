# ProGuard rules for RTSP to Tablet
# @file proguard-rules.pro
# @session SESSION_001
# @created 2026-01-20

# Keep Media3 classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep DataStore
-keep class androidx.datastore.** { *; }
