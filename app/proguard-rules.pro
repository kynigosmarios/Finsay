# Finsay R8 / ProGuard keep rules

# Preserve line numbers for Crashlytics / Play Console stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations and generic signatures needed by Firestore, Moshi, Retrofit, Room
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep all Firestore & Room data model classes
-keep class com.example.data.model.** { *; }
-keep class com.example.data.entity.** { *; }

# Keep classes annotated with @Keep or Firestore @IgnoreExtraProperties
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
    @com.google.firebase.firestore.PropertyName *;
}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }

# Moshi JSON adapters & reflection
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Jetpack Credential Manager & Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ML Kit OCR & Barcode Scanning
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
