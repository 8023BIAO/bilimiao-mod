# ===== 核心行为 =====
# 禁混淆（保留可读堆栈），允许优化
-dontobfuscate

# ===== 应用业务代码 =====
-keep class com.a10miaomiao.** { *; }
-keep class cn.a10miaomiao.** { *; }

# ===== 框架/第三方库 =====
# Kotlin 元数据（序列化/Compose 反射必须）
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keep class kotlin.Metadata { *; }

# AndroidX
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class kotlinx.coroutines.** { *; }

# 网络
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }

# 播放器
-keep class com.shuyu.gsyvideoplayer.** { *; }

# 图片加载
-keep class com.bumptech.glide.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# DI (Kodein) — 必须保留匿名TypeReference子类及泛型签名
-keep class org.kodein.di.** { *; }
-keep class org.kodein.type.** { *; }
# 保留Kotlin reified内联生成的匿名TypeReference子类
-keep class * extends org.kodein.type.TypeReference
-keep class * extends org.kodein.type.TypeToken
# 保留泛型签名（对TypeToken/TypeReference至关重要）
-keepattributes Signature

# UI 库
-keep class com.kongzue.dialogx.** { *; }
-keep class com.mikaelzero.mojito.** { *; }
-keep class splitties.** { *; }
-keep class pbandk.** { *; }

# Material 构造器（Splitties 反射调用）
-keepclassmembers class com.google.android.material.** {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
}

# 导航路由（反射使用）
-keep class cn.a10miaomiao.bilimiao.compose.pages.**.PageConfig { *; }
-keep class * implements androidx.navigation.NavArgs { *; }

# ===== 通用保护 =====
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== 静默警告 =====
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**