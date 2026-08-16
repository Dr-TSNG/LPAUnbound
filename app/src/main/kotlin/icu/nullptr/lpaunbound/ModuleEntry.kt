package icu.nullptr.lpaunbound

import android.util.Log
import icu.nullptr.lpaunbound.hooks.installEsimSupportPropertyHook
import icu.nullptr.lpaunbound.hooks.installEuiccPackageManagerHooks
import icu.nullptr.lpaunbound.hooks.installSignaturePermissionAllowlistHook
import icu.nullptr.lpaunbound.hooks.installXiaomiPhoneUiHooks
import icu.nullptr.lpaunbound.hooks.installXiaomiSettingsHooks
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

lateinit var module: ModuleEntry

class ModuleEntry : XposedModule() {
    private lateinit var targetClassLoader: ClassLoader

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        module = this
        installProcessHooks(param.isSystemServer, param.processName)
    }

    override fun onHotReloading(param: XposedModuleInterface.HotReloadingParam): Boolean {
        param.setSavedInstanceState(targetClassLoader)
        return true
    }

    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        param.oldHookHandles.forEach(XposedInterface.HookHandle::unhook)

        val classLoader = requireNotNull(param.savedInstanceState as? ClassLoader) {
            "Target classloader was not restored for ${param.processName}"
        }
        targetClassLoader = classLoader

        installProcessHooks(param.isSystemServer, param.processName)
        installTargetHooks(param.isSystemServer, param.processName, classLoader)
        log(Log.INFO, TAG, "Hot reload completed for ${param.processName}")
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        targetClassLoader = param.classLoader
        installSignaturePermissionAllowlistHook(this, param.classLoader)
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (!param.isFirstPackage) return
        if (param.packageName != PHONE_PACKAGE && param.packageName != SETTINGS_PACKAGE) return

        targetClassLoader = param.classLoader
        installTargetHooks(false, param.packageName, param.classLoader)
    }

    private fun installProcessHooks(isSystemServer: Boolean, processName: String) {
        when {
            isSystemServer -> log(Log.INFO, TAG, "Loaded for system_server")
            processName == PHONE_PACKAGE -> {
                installEsimSupportPropertyHook(this)
                installEuiccPackageManagerHooks(this)
            }

            processName == SETTINGS_PACKAGE -> installEsimSupportPropertyHook(this)
            else -> detach()
        }
    }

    private fun installTargetHooks(
        isSystemServer: Boolean,
        processName: String,
        classLoader: ClassLoader,
    ) {
        when {
            isSystemServer -> installSignaturePermissionAllowlistHook(this, classLoader)
            processName == PHONE_PACKAGE -> installXiaomiPhoneUiHooks(this, classLoader)
            processName == SETTINGS_PACKAGE -> installXiaomiSettingsHooks(this, classLoader)
        }
    }
}
