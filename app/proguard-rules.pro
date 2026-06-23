# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** { *** Companion; }
-keepclasseswithmembers class com.example.** { kotlinx.serialization.KSerializer serializer(...); }

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ML Kit Barcode
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Compose
-dontwarn androidx.compose.**

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory { *; }

# Keep WorkManager workers
-keep class * extends androidx.work.CoroutineWorker { *; }

# Keep R8 from stripping generic signatures
-keepattributes Signature, RuntimeException, Exceptions
