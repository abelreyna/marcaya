# Reglas R8 / ProGuard — Registro de Tiempo de Personal
# Autor: Juan Abel Reyna Gonzales

# Modelos que Firestore serializa por reflexión
-keep class com.juanreyna.registrotiempo.data.remote.dto.** { *; }
-keepclassmembers class com.juanreyna.registrotiempo.data.remote.dto.** { <init>(); }

# Firebase / Google Play Services
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Credential Manager
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

# Eliminar logs en producción
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
