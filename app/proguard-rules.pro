# R8 rules of the release build.
#
# kotlinx.serialization, Retrofit, OkHttp, Room, Hilt and Coil ship their own consumer rules
# (META-INF/com.android.tools/r8). The rules below make the app-specific requirements explicit;
# the release mapping was checked: DTO and route serializers keep `Companion` and `serializer()`.

# --- kotlinx.serialization ---------------------------------------------------------------
# Retrofit's converter and type-safe navigation look serializers up by reflection
# (`serializer(Type)`): keep the generated serializers and the companion accessors.
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, InnerClasses
-keep,includedescriptorclasses class ru.kinopolka.**$$serializer { *; }
-keepclassmembers class ru.kinopolka.** {
    *** Companion;
}
-keepclasseswithmembers class ru.kinopolka.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Serializable objects (navigation graphs and routes without arguments).
-keepclassmembers @kotlinx.serialization.Serializable class ru.kinopolka.** {
    public static ** INSTANCE;
}

# --- Retrofit ----------------------------------------------------------------------------
# Generic return types of the suspend API methods (PagedResponseDto<MediaListItemDto>) must
# survive: the converter needs the full type.
-keepattributes Signature, Exceptions, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface ru.kinopolka.core.network.TmdbApi
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- Stack traces ------------------------------------------------------------------------
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
