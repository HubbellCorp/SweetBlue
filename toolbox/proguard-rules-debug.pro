# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/ryanbis/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate
-keep class org.apache.commons.logging.**                                   { *; }
-keep class org.codehaus.**                                                 { *; }
-keep class com.idevicesinc.sweetblue.**                                    { *; }
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,EnclosingMethod

-dontwarn com.idevicesinc.javautils.**
-dontwarn javax.xml.stream.events.**
-dontwarn org.codehaus.jackson.**
-dontwarn org.apache.commons.logging.impl.**
-dontwarn org.apache.http.conn.scheme.**
