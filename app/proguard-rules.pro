# kotlinx.serialization models
-keep,includedescriptorclasses class com.taurotechnologies.godseye.nativeapp.data.**$$serializer { *; }
-keepclassmembers class com.taurotechnologies.godseye.nativeapp.data.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }

# CesiumMap WebView bridge methods are called by name from JavaScript
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
