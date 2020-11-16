package ru.example.oem_test

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import im.dlg.oem_test.BuildConfig
import im.dlg.oem_test.R
import im.dlg.sdk.SdkScreen
import kotlinx.android.synthetic.main.default_fragment.vCallTab
import kotlinx.android.synthetic.main.default_fragment.vDialogTab
import kotlinx.android.synthetic.main.default_fragment.vLog
import kotlinx.android.synthetic.main.default_fragment.vLogin
import kotlinx.android.synthetic.main.default_fragment.vLogout
import kotlinx.android.synthetic.main.default_fragment.vName
import kotlinx.android.synthetic.main.default_fragment.vPassword
import ru.example.oem_test.LoginViewModel.State

class DefaultFragment : Fragment() {
    private val sdk by lazy(LazyThreadSafetyMode.NONE) { (requireActivity().application as OemTestApplication).sdk }
    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.default_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vLogin.setOnClickListener { onAction() }
        vLogout.setOnClickListener { viewModel.onLogoutAction() }
        vCallTab.setOnClickListener { viewModel.openTab(SdkScreen.CONTACTS) }
        vDialogTab.setOnClickListener { viewModel.openTab(SdkScreen.DIALOGS) }
        vPassword.setOnEditorActionListener { _, _, _ -> onAction(); false }
    }

    private fun onAction() {
        hideKeyboard()
        viewModel.onLoginAction(
                name = vName.text.toString(),
                password = vPassword.text.toString()
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this, LoginViewModel.Factory(sdk)).get(LoginViewModel::class.java)

        viewModel.observeState().observe(viewLifecycleOwner) { state ->
            when (state) {
                State.Initial -> {
                    vName.setText(BuildConfig.TEST_USER)
                    vPassword.setText(BuildConfig.TEST_PASSWORD)
                    vLogin.isEnabled = true
                    vLogout.isEnabled = true
                    vLog.text = "$TEST_HOST:$TEST_PORT"
                }

                State.Loading -> {
                    vLogin.isEnabled = false
                    vLogout.isEnabled = false
                    vLog.text = "Loading..."
                }

                is State.Success -> {
                    vLogin.isEnabled = true
                    vLogout.isEnabled = true
                    Log.d("Token", state.token)
                    vLog.text = "Token: ${state.token}"

                    sdk.setToken(state.token)
                }

                is State.Fail -> {
                    vLogin.isEnabled = true
                    vLogout.isEnabled = true
                    vLog.text = "Error: ${state.reason}"
                }
            }
        }
    }

    private fun hideKeyboard() {
        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.let { inputMethodManager ->
            val token = activity?.currentFocus?.windowToken ?: activity?.window?.decorView?.windowToken
            inputMethodManager.hideSoftInputFromWindow(token, 0)
        }
    }
}
