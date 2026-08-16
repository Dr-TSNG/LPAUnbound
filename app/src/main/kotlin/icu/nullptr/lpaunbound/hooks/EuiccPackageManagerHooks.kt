package icu.nullptr.lpaunbound.hooks

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import icu.nullptr.lpaunbound.BIND_EUICC_SERVICE_PERMISSION
import icu.nullptr.lpaunbound.EUICC_SERVICE_ACTION
import icu.nullptr.lpaunbound.EUICC_UI_ACTION_PREFIX
import icu.nullptr.lpaunbound.MATCH_SYSTEM_ONLY
import icu.nullptr.lpaunbound.TAG
import icu.nullptr.lpaunbound.WRITE_EMBEDDED_SUBSCRIPTIONS_PERMISSION
import icu.nullptr.lpaunbound.XIAOMI_LPA_PACKAGE
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("PrivateApi")
fun installEuiccPackageManagerHooks(api: XposedInterface) {
    val appPackageManager = runCatching {
        Class.forName("android.app.ApplicationPackageManager")
    }.getOrElse {
        api.log(Log.ERROR, TAG, "ApplicationPackageManager is unavailable", it)
        return
    }

    val queryNames = setOf(
        "queryIntentActivities",
        "queryIntentActivitiesAsUser",
        "queryIntentServices",
        "queryIntentServicesAsUser",
    )
    val methods = linkedSetOf<Method>()
    var current: Class<*>? = appPackageManager
    while (current != null && current != Any::class.java) {
        current.declaredMethods
            .asSequence()
            .filter { it.name in queryNames }
            .filterNot { Modifier.isAbstract(it.modifiers) || Modifier.isNative(it.modifiers) }
            .filter { method ->
                method.parameterTypes.any { Intent::class.java.isAssignableFrom(it) }
            }
            .filter { method -> method.parameterTypes.any(::isSupportedFlagType) }
            .forEach(methods::add)
        current = current.superclass
    }

    val loggedQueries = ConcurrentHashMap.newKeySet<String>()
    var installed = 0
    for (method in methods) {
        val intentIndex = method.parameterTypes.indexOfFirst {
            Intent::class.java.isAssignableFrom(it)
        }
        val flagsIndex = method.parameterTypes.indexOfFirst(::isSupportedFlagType)
        if (intentIndex < 0 || flagsIndex < 0) continue

        runCatching {
            method.isAccessible = true
            api.hook(method).intercept { chain ->
                val intent = chain.getArg(intentIndex) as? Intent
                if (!isEuiccQuery(intent)) return@intercept chain.proceed()

                val args = chain.args.toTypedArray()
                val originalFlags = args[flagsIndex] ?: return@intercept chain.proceed()
                val replacementFlags = clearMatchSystemOnly(api, originalFlags)
                    ?: return@intercept chain.proceed()
                if (replacementFlags === originalFlags || replacementFlags == originalFlags) {
                    return@intercept chain.proceed()
                }

                args[flagsIndex] = replacementFlags
                val action = intent?.action.orEmpty()
                val key = "${method.name}:$action"
                if (loggedQueries.add(key)) {
                    api.log(Log.INFO, TAG, "Allowed user-installed LPA candidate for $action")
                }
                chain.proceed(args)
            }
            installed++
        }.onFailure {
            api.log(
                Log.WARN,
                TAG,
                "Failed to hook ${method.declaringClass.name}.${method.name}",
                it
            )
        }
    }

    installZeroPriorityLuiHook(api)
    api.log(Log.INFO, TAG, "Installed $installed eUICC PackageManager query hook(s)")
}

@SuppressLint("PrivateApi")
private fun installZeroPriorityLuiHook(api: XposedInterface) {
    runCatching {
        val connectorClass = Class.forName(
            "com.android.internal.telephony.euicc.EuiccConnector",
        )
        val method = connectorClass.declaredMethods.single {
            it.name == "isValidEuiccComponent" &&
                    it.parameterCount == 2 &&
                    it.returnType == Boolean::class.javaPrimitiveType
        }.apply { isAccessible = true }

        api.hook(method).intercept { chain ->
            val originalResult = chain.proceed() as Boolean
            if (originalResult) return@intercept true

            val packageManager = chain.getArg(0) as? PackageManager
                ?: return@intercept false
            val resolveInfo = chain.getArg(1) as? ResolveInfo
                ?: return@intercept false
            if (!isTrustedXiaomiLui(packageManager, resolveInfo)) {
                return@intercept false
            }

            api.log(
                Log.INFO,
                TAG,
                "Accepted verified Xiaomi LUI with data-app priority ${resolveInfo.priority}",
            )
            true
        }
        api.log(Log.INFO, TAG, "Installed zero-priority Xiaomi LUI compatibility hook")
    }.onFailure {
        api.log(Log.ERROR, TAG, "Failed to hook Xiaomi LUI validation", it)
    }
}

private fun isTrustedXiaomiLui(
    packageManager: PackageManager,
    resolveInfo: ResolveInfo,
): Boolean {
    val componentInfo = resolveInfo.activityInfo ?: resolveInfo.serviceInfo ?: return false
    if (componentInfo.packageName != XIAOMI_LPA_PACKAGE) return false
    if (resolveInfo.filter == null || resolveInfo.priority != 0) return false

    val componentPermission = resolveInfo.activityInfo?.permission
        ?: resolveInfo.serviceInfo?.permission
    if (componentPermission != BIND_EUICC_SERVICE_PERMISSION) return false

    return packageManager.checkPermission(
        WRITE_EMBEDDED_SUBSCRIPTIONS_PERMISSION,
        XIAOMI_LPA_PACKAGE,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun isEuiccQuery(intent: Intent?): Boolean {
    val action = intent?.action ?: return false
    return action == EUICC_SERVICE_ACTION || action.startsWith(EUICC_UI_ACTION_PREFIX)
}

private fun isSupportedFlagType(type: Class<*>): Boolean =
    type == Int::class.javaPrimitiveType ||
            type == Long::class.javaPrimitiveType ||
            type.name == "android.content.pm.PackageManager\$ResolveInfoFlags"

private fun clearMatchSystemOnly(api: XposedInterface, value: Any): Any? = when (value) {
    is Int -> {
        val updated = value and MATCH_SYSTEM_ONLY.toInt().inv()
        if (updated == value) value else updated
    }

    is Long -> {
        val updated = value and MATCH_SYSTEM_ONLY.inv()
        if (updated == value) value else updated
    }

    else -> clearMatchSystemOnlyFromFlagObject(api, value)
}

private fun clearMatchSystemOnlyFromFlagObject(api: XposedInterface, value: Any): Any? =
    runCatching {
        val original = (value.javaClass.getMethod("getValue").invoke(value) as Number).toLong()
        val updated = original and MATCH_SYSTEM_ONLY.inv()
        if (updated == original) return@runCatching value

        value.javaClass
            .getMethod("of", Long::class.javaPrimitiveType)
            .invoke(null, updated)
    }.getOrElse {
        api.log(Log.WARN, TAG, "Unable to rewrite ${value.javaClass.name}", it)
        null
    }
