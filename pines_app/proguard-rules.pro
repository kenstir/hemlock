## Ktor and kotlinx.serialization rules in preparation for R8 Full Mode
# 1. Protect generic signatures for Ktor and Serialization
-keepattributes Signature, *Annotation*, InnerClasses
# 2. Protect kotlinx.serialization metadata
-keepclassmembers class * { *** Companion; }
-keepnames class kotlinx.serialization.internal.GeneratedSerializer { *; }
# 3. Ktor Client - prevent stripping of the CIO/OkHttp engines
-keep class io.ktor.client.engine.** { *; }

# https://developer.android.com/studio/build/shrink-code
# https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=android
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# https://stackoverflow.com/questions/51860843/how-to-turn-off-only-the-obfuscation-in-android-r8
#-dontshrink
-dontobfuscate
#-dontoptimize

# suppress warning
-dontwarn kotlin.jvm.internal.SourceDebugExtension
