# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\tools\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Trying to prevent the Play Store warning
# "This App Bundle contains Java/Kotlin code, which might be obfuscated."
# https://stackoverflow.com/questions/52525155/crashlitycs-reporting-wrong-line-number-after-kotlin-migration
#-keepattributes SourceFile,LineNumberTable

# To disable R8 features:
# https://stackoverflow.com/questions/51860843/how-to-turn-off-only-the-obfuscation-in-android-r8
#-dontshrink
-dontobfuscate
-dontoptimize
