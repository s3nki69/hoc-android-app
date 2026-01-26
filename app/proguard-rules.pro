# Add project specific ProGuard rules here.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class hu.hoc.app.**$$serializer { *; }
-keepclassmembers class hu.hoc.app.** {
    *** Companion;
}
-keepclasseswithmembers class hu.hoc.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
