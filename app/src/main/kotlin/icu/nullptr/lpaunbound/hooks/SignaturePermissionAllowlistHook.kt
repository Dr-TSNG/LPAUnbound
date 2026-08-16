package icu.nullptr.lpaunbound.hooks

import android.annotation.SuppressLint
import android.util.Log
import icu.nullptr.lpaunbound.TAG
import icu.nullptr.lpaunbound.XIAOMI_LPA_PACKAGE
import icu.nullptr.lpaunbound.XIAOMI_LPA_SIGNATURE_PERMISSIONS
import io.github.libxposed.api.XposedInterface

@SuppressLint("PrivateApi")
fun installSignaturePermissionAllowlistHook(
    api: XposedInterface,
    classLoader: ClassLoader,
) {
    runCatching {
        val policyClass = Class.forName(
            "com.android.server.permission.access.permission.AppIdPermissionPolicy",
            false,
            classLoader,
        )
        val method = policyClass.declaredMethods.single {
            it.name == "getSignaturePermissionAllowlistState" &&
                    it.parameterCount == 3 &&
                    it.parameterTypes[2] == String::class.java
        }.apply { isAccessible = true }

        api.hook(method).intercept { chain ->
            val packageState = chain.getArg(1)
            val packageName = packageState.javaClass.methods
                .firstOrNull { it.name == "getPackageName" && it.parameterCount == 0 }
                ?.invoke(packageState) as? String
            val permissionName = chain.getArg(2) as? String
            if (
                packageName == XIAOMI_LPA_PACKAGE &&
                permissionName in XIAOMI_LPA_SIGNATURE_PERMISSIONS
            ) {
                api.log(Log.INFO, TAG, "Allowlisted $permissionName for $packageName")
                true
            } else {
                chain.proceed()
            }
        }
        api.log(Log.INFO, TAG, "Installed Android 15+ signature permission allowlist hook")
    }.onFailure {
        api.log(Log.ERROR, TAG, "Failed to hook the signature permission allowlist", it)
    }
}
