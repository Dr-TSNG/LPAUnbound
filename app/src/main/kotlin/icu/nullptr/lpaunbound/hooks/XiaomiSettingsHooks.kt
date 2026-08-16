package icu.nullptr.lpaunbound.hooks

import android.util.Log
import icu.nullptr.lpaunbound.TAG
import io.github.libxposed.api.XposedInterface

internal fun installXiaomiSettingsHooks(api: XposedInterface, classLoader: ClassLoader) {
    val targets = listOf(
        "com.android.settings.MiuiUtils" to "isSupportEsimMode",
        "com.android.settings.utils.SettingsFeatures" to "isNeedESIMFeature",
    )

    for ((className, methodName) in targets) {
        runCatching {
            val method = Class.forName(className, false, classLoader)
                .getDeclaredMethod(methodName)
                .apply { isAccessible = true }
            check(method.returnType == Boolean::class.javaPrimitiveType) {
                "$className.$methodName does not return boolean"
            }
            api.hook(method).intercept { true }
            api.log(Log.INFO, TAG, "Enabled Xiaomi Settings gate $className.$methodName")
        }.onFailure {
            api.log(Log.WARN, TAG, "Xiaomi Settings gate unavailable: $className.$methodName", it)
        }
    }
}
