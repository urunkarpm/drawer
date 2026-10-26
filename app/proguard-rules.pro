# Drawer Launcher ProGuard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}

# Strip debug and verbose logs in release build to prevent main-thread I/O jank
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Keep Kotlin coroutines & reflection internals safe for full R8 optimization
-dontwarn kotlinx.coroutines.**
