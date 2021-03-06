# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/ryanbis/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontoptimize
-keepattributes Exceptions
-keep class com.idevicesinc.javautils.** { *; }
-keep class com.idevicesinc.sweetblue.** { *; }
# TODO - Figure out how to obfuscate the P_ApiManager class only. It seems proguard is another victim of gradle 5 changes
#-keep class !com.idevicesinc.sweetblue.api.P_ApiManager { *; }
-dontwarn com.idevicesinc.javautils.**