package icu.nullptr.lpaunbound

const val PHONE_PACKAGE = "com.android.phone"
const val SETTINGS_PACKAGE = "com.android.settings"
const val XIAOMI_LPA_PACKAGE = "com.miui.euicc"
const val SUPPORT_ESIM_PROPERTY = "ro.vendor.miui.support_esim"
const val EUICC_SERVICE_ACTION = "android.service.euicc.EuiccService"
const val EUICC_UI_ACTION_PREFIX = "android.service.euicc.action."
const val BIND_EUICC_SERVICE_PERMISSION = "android.permission.BIND_EUICC_SERVICE"
const val WRITE_EMBEDDED_SUBSCRIPTIONS_PERMISSION =
    "android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS"
const val MATCH_SYSTEM_ONLY = 0x00100000L
const val TAG = "LPAUnbound"

val XIAOMI_LPA_SIGNATURE_PERMISSIONS = setOf(
    "android.permission.BIND_EUICC_SERVICE",
    "android.permission.MODIFY_PHONE_STATE",
    "android.permission.READ_PRIVILEGED_PHONE_STATE",
    "android.permission.SECURE_ELEMENT_PRIVILEGED_OPERATION",
    "android.permission.START_ACTIVITIES_FROM_BACKGROUND",
    "android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER",
    "android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS",
    "android.permission.WRITE_SECURE_SETTINGS",
)
