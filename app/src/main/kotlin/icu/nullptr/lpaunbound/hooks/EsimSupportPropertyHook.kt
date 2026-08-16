package icu.nullptr.lpaunbound.hooks

import android.util.Log
import icu.nullptr.lpaunbound.SUPPORT_ESIM_PROPERTY
import icu.nullptr.lpaunbound.TAG
import io.github.libxposed.api.XposedInterface

fun installEsimSupportPropertyHook(api: XposedInterface) {
    runCatching {
        val method = Class.forName("android.os.SystemProperties")
            .getDeclaredMethod(
                "getBoolean",
                String::class.java,
                Boolean::class.javaPrimitiveType,
            )
            .apply { isAccessible = true }

        api.hook(method).intercept { chain ->
            if (chain.getArg(0) == SUPPORT_ESIM_PROPERTY) true else chain.proceed()
        }
        api.log(Log.INFO, TAG, "Installed Xiaomi eSIM property hook")
    }.onFailure {
        api.log(Log.ERROR, TAG, "Failed to hook Xiaomi eSIM property", it)
    }
}
