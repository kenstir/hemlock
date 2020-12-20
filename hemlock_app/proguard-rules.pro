# https://stackoverflow.com/questions/52525155/crashlitycs-reporting-wrong-line-number-after-kotlin-migration
# https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=android
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# https://stackoverflow.com/questions/51860843/how-to-turn-off-only-the-obfuscation-in-android-r8
#-dontshrink
-dontobfuscate
-dontoptimize
