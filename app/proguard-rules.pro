# R8 rules of the release build.
#
# Retrofit, OkHttp, Gson, Room, Hilt, Glide, Paging and WorkManager ship their own consumer rules
# (META-INF/com.android.tools/r8). The rules below make the app-specific requirements explicit.

# --- Gson --------------------------------------------------------------------------------
# DTOs are Java records read by reflection: their names may be obfuscated, but the canonical
# constructor and the record components with @SerializedName must stay. Records also need the
# Record attribute to be recognised as records at runtime.
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault, InnerClasses, Record
-keep class ru.kinopolka.core.network.model.** {
    <init>(...);
    <fields>;
}
# Generic types such as PagedResponseDto<MediaListItemDto> are read through TypeToken.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# --- Retrofit ----------------------------------------------------------------------------
# The converter needs the full generic return types of the API methods.
-keepattributes Exceptions, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface ru.kinopolka.core.network.TmdbApi
-keep,allowobfuscation,allowshrinking class retrofit2.Call

# --- Views -------------------------------------------------------------------------------
# Custom views and fragments are created from XML and the navigation graph by name.
-keep class ru.kinopolka.core.ui.AspectRatioFrameLayout { <init>(...); }

# --- Stack traces ------------------------------------------------------------------------
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
