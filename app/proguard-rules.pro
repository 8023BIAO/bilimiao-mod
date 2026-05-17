# ===== 核心行为 =====
# 禁混淆（保留可读堆栈），允许优化
-dontobfuscate

# ===== 调试信息（崩溃定位必须） =====
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== 应用业务代码 =====
-keep class com.a10miaomiao.** { *; }
-keep class cn.a10miaomiao.** { *; }

# Kotlin data class 关键方法（copy/componentN 等反射使用）
-keepclassmembers class com.a10miaomiao.** {
    synthetic <methods>;
}
-keepclassmembers class cn.a10miaomiao.** {
    synthetic <methods>;
}

# ===== B站 GRPC Protobuf 生成类 =====
-keep class bilibili.** { *; }
-keep class com.bapis.** { *; }

# ===== Kotlin 元数据 =====
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keep class kotlin.Metadata { *; }

# Kotlin 反射
-keep class kotlin.reflect.** { *; }
-keep class kotlin.coroutines.** { *; }

# Kotlin Result / Continuation（协程恢复需要）
-keep class kotlin.Result { *; }
-keep class kotlin.Result$Failure { *; }
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlinx.coroutines.** { *; }

# ===== kotlinx.serialization =====
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
# 保护 @Serializable 类的 $serializer 伴生对象
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-if class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class <1>$serializer {
    *** INSTANCE;
}
-if class * implements kotlinx.serialization.internal.GeneratedSerializer {
    static ** $instance;
}
-keepclassmembers class <1> {
    static <1>$serializer INSTANCE;
}

# ===== Compose =====
-keep class androidx.compose.** { *; }
# 防止 R8 删除 @Composable 函数
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ===== Navigation (反射路由) =====
-keep class cn.a10miaomiao.bilimiao.compose.pages.**.PageConfig { *; }
-keep class * implements androidx.navigation.NavArgs { *; }
# 保留 @Serializable 导航路由对象
-keepclassmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <methods>;
}

# ===== AndroidX Lifecycle =====
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ===== DataStore Preferences =====
-keep class androidx.datastore.preferences.** { *; }

# ===== 网络层 =====
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keep class com.a10miaomiao.bilimiao.comm.network.** { *; }

# ===== 播放器 =====
-keep class com.shuyu.gsyvideoplayer.** { *; }
-keepclassmembers class * extends com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer {
    <init>(...);
}
-keep class androidx.media3.** { *; }

# ===== 图片加载 (Glide) =====
-keep class com.bumptech.glide.** { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ===== DI (Kodein) =====
-keep class org.kodein.di.** { *; }
-keep class org.kodein.type.** { *; }
-keep class * extends org.kodein.type.TypeReference
-keep class * extends org.kodein.type.TypeToken

# ===== UI 库 =====
-keep class com.kongzue.dialogx.** { *; }
-keep class com.mikaelzero.mojito.** { *; }
-keep class splitties.** { *; }
-keep class pbandk.** { *; }

# ===== Material 构造器（Splitties 反射调用） =====
-keepclassmembers class com.google.android.material.** {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
}

# ===== MaterialKolor =====
-keep class com.materialkolor.** { *; }

# ===== PlaybackService（前台服务，系统反射恢复） =====
-keep class com.a10miaomiao.bilimiao.comm.delegate.player.PlaybackService { *; }

# ===== 通用 Android 组件 =====
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

# ===== WebView JS 接口（如有使用） =====
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ===== 静默警告 =====
-dontwarn okhttp3.internal.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn com.google.protobuf.**
-dontwarn pbandk.**
