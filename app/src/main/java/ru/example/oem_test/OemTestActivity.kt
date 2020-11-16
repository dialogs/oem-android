package ru.example.oem_test

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import im.dlg.oem_test.R
import kotlinx.android.synthetic.main.activity_main.vBottomBar

open class OemTestActivity : AppCompatActivity() {

    private val sdk by lazy(LazyThreadSafetyMode.NONE) { (application as OemTestApplication).sdk }

    private val dialogScreen = Screen("Dialog") {
        sdk.getHostFragment()
    }

    private val defaultScreen = Screen("Default") {
        DefaultFragment()
    }

    private val currentTabFragment: Fragment?
        get() = supportFragmentManager.fragments.firstOrNull { !it.isHidden && (sdk.isHostFragment(it) || it is DefaultFragment) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        vBottomBar.inflateMenu(R.menu.navigation_menu)

        vBottomBar.setOnNavigationItemSelectedListener {
            val tab = when (it.itemId) {
                R.id.menu_item_dialogs -> dialogScreen
                R.id.menu_item_default -> defaultScreen
                else -> defaultScreen
            }

            selectTab(tab)

            true
        }

        if (savedInstanceState == null) {
            selectTab(defaultScreen)
        }

        sdk.setUnreadCounterListener { unreadCount ->
            vBottomBar.getOrCreateBadge(R.id.menu_item_dialogs).apply {
                maxCharacterCount = 3
                verticalOffset = 2.dp().toInt()
                horizontalOffset = 2.dp().toInt()
                number = unreadCount
            }
        }

        sdk.deepLinksResolver.onCreate(this, ::selectDialogsTab)
    }

    private fun selectDialogsTab()  {
        vBottomBar.selectedItemId = R.id.menu_item_dialogs
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        sdk.deepLinksResolver.onNewIntent(intent, ::selectDialogsTab)
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        sdk.deepLinksResolver.onActivityStarted(intent, options, ::selectDialogsTab) {
            super.startActivity(intent, options)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sdk.deepLinksResolver.onDestroy()
    }

    override fun onBackPressed() {
        val currentFragment = currentTabFragment
        if (currentFragment == null || !sdk.onBackPressed(currentFragment)) super.onBackPressed()
    }

    private fun selectTab(tab: Screen) {
        val newFragment = supportFragmentManager.findFragmentByTag(tab.tag)

        if (isCurrentTabTheSameFragment(tab)) return

        val transaction = supportFragmentManager.beginTransaction()

        if (newFragment == null) transaction.add(R.id.contentFragment, tab.fragmentGetter(), tab.tag)

        currentTabFragment?.let {
            transaction.hide(it)
            transaction.setMaxLifecycle(it, Lifecycle.State.STARTED)
        }
        newFragment?.let {
            transaction.show(it)
            transaction.setMaxLifecycle(it, Lifecycle.State.RESUMED)
        }

        transaction.commitNow()
    }

    private fun isCurrentTabTheSameFragment(tab: Screen): Boolean {
        val currentFragment = currentTabFragment
        val newFragment = supportFragmentManager.findFragmentByTag(tab.tag)

        return currentFragment != null && newFragment != null && currentFragment == newFragment
    }

    class Screen(val tag: String, val fragmentGetter: () -> Fragment)

    private fun Number.dp() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), resources.displayMetrics)
}
