# Add project specific ProGuard rules here.

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==================== Retrofit ====================
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# ==================== OkHttp ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ==================== Gson ====================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data models for Gson serialization
-keep class com.example.newsbite.data.model.** { *; }

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room entities
-keep class com.example.newsbite.data.local.** { *; }

# ==================== Hilt ====================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ==================== Kotlin ====================
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ==================== AndroidX ====================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ==================== Material Components ====================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ==================== Picasso ====================
-dontwarn com.squareup.okhttp.**
-dontwarn com.squareup.picasso.**
-keep class com.squareup.picasso.** { *; }

# ==================== Shimmer ====================
-keep class com.facebook.shimmer.** { *; }

# ==================== WebView ====================
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# ==================== General Android ====================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep ViewBinding classes
-keep class com.example.newsbite.databinding.** { *; }

# Keep BuildConfig
-keep class com.example.newsbite.BuildConfig { *; }
