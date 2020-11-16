package ru.example.oem_test

import android.annotation.SuppressLint
import androidx.multidex.MultiDexApplication
import im.dlg.oem_test.BuildConfig
import im.dlg.oem_test.R
import im.dlg.sdk.DialogSdk

// todo This is a copy of DialogXApplication and will be changed later
class OemTestApplication : MultiDexApplication() {

    lateinit var sdk: DialogSdk

    @SuppressLint("DefaultLocale")
    override fun onCreate() {
        super.onCreate()

        sdk = DialogSdk(
                application = this,
                appName = getString(R.string.app_name),
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME,
                fcmProjectId = getString(R.string.gcm_defaultSenderId).toLong(),
                host = TEST_HOST,
                port = TEST_PORT,
                deepLinkActivity = OemTestActivity::class.java,
        )
    }
}
