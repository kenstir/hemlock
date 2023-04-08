# https://developer.android.com/studio/build/shrink-code
# https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=android
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# https://stackoverflow.com/questions/51860843/how-to-turn-off-only-the-obfuscation-in-android-r8
#-dontshrink
-dontobfuscate
-dontoptimize

# suppress warning
-dontwarn kotlin.jvm.internal.SourceDebugExtension
