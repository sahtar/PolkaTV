# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class ru.polkatv.launcher.** { *** Companion; }
-keepclasseswithmembers class ru.polkatv.launcher.** { kotlinx.serialization.KSerializer serializer(...); }
