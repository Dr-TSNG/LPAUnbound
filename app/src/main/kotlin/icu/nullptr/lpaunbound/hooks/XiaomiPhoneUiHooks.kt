package icu.nullptr.lpaunbound.hooks

import android.util.Log
import icu.nullptr.lpaunbound.TAG
import io.github.libxposed.api.XposedInterface

internal fun installXiaomiPhoneUiHooks(api: XposedInterface, classLoader: ClassLoader) {
    val targets = listOf(
        Triple("com.android.phone.MiuiPhoneUtils", "isEsimCapable", true),
        Triple("com.android.phone.MiuiEsimManagerBase", "isSupportForRegion", true),
        Triple("com.android.phone.MiuiEsimManagerBase", "isRemoveEsimSwitch", false),
    )

    for ((className, methodName, result) in targets) {
        runCatching {
            val method = Class.forName(className, false, classLoader)
                .getDeclaredMethod(methodName)
                .apply { isAccessible = true }
            check(method.returnType == Boolean::class.javaPrimitiveType) {
                "$className.$methodName does not return boolean"
            }
            api.hook(method).intercept { result }
            api.log(Log.INFO, TAG, "Set Xiaomi Phone gate $className.$methodName=$result")
        }.onFailure {
            api.log(Log.WARN, TAG, "Xiaomi Phone gate unavailable: $className.$methodName", it)
        }
    }
}
