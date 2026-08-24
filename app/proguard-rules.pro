# ============================================================
# ForceStop - ProGuard / R8 规则
# 目标：开启 minify + shrinkResources 后仍能正常运行
# ============================================================

# ---- 基础属性保留 ----
-keepattributes Signature
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Kotlin ----
# 保留 Kotlin 元数据（Compose / 序列化等依赖）
-keep class kotlin.Metadata { *; }
-keep class org.jetbrains.annotations.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ---- Kotlin 协程 ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---- Jetpack Compose ----
# Compose 编译器生成代码需保留，否则运行期崩溃 / 界面空白
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keep class com.android.tools.r8.** { *; }
-dontwarn android.support.**
-dontwarn androidx.compose.**

# Composable 函数与带有 @Composable / @Stable / @Immutable 注解的成员
-keep @androidx.compose.runtime.Stable class *
-keep @androidx.compose.runtime.Immutable class *
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ---- ViewModel / Lifecycle ----
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ---- Activity Compose / Compose UI 内部 ----
-keep class androidx.activity.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }

# ---- 项目自身（按需保留，避免混淆入口类导致反射/序列化问题）----
-keep class com.wzy020.forcestop.** { *; }
-dontwarn com.wzy020.forcestop.**

# ---- 通用兜底 ----
-keep class * implements android.os.Parcelable { *; }
-keep class * implements java.io.Serializable { *; }
