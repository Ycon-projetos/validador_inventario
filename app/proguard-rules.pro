# Room — manter entidades e DAOs
-keep class com.ycon.validadorinventario.data.entity.** { *; }
-keep class com.ycon.validadorinventario.data.dao.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
