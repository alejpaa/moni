# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keep,includedescriptorclasses class <1>$$serializer {
    *;
}

-keep @kotlinx.serialization.Serializable class com.moni.app.data.** { *; }

# supabase-kt & Ktor / OkHttp
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.github.jan.supabase.**
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn kotlinx.atomicfu.**
-dontwarn org.slf4j.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.**
-dontwarn javax.naming.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn com.sun.jdi.**
-dontwarn android.security.NetworkSecurityPolicy
