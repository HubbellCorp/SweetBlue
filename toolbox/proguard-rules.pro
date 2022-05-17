# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/ryanbis/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

-optimizationpasses 5
-keep class org.apache.commons.logging.**                                   { *; }
-keep class org.codehaus.**                                                 { *; }
-keep class com.idevicesinc.sweetblue.**                                    { *; }
-keep class com.idevicesllc.connected.utilities.DotsTextView$JumpingSpan    { *; }
-keep class android.support.v7.widget.SearchView                            { *; }
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,EnclosingMethod


-dontwarn com.idevicesinc.**
-dontwarn javax.xml.stream.events.**
-dontwarn org.codehaus.jackson.**
-dontwarn org.apache.commons.logging.impl.**
-dontwarn org.apache.http.conn.scheme.**