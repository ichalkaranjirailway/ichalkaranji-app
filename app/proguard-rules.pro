# Add project specific ProGuard rules here.
# Kept minimal since this app has no obfuscation-sensitive code.
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
