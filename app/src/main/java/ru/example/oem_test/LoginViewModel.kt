package ru.example.oem_test

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.google.protobuf.StringValue
import im.dlg.grpc.services.AuthenticationGrpc
import im.dlg.grpc.services.AuthenticationOuterClass
import im.dlg.grpc.services.RegistrationGrpc
import im.dlg.grpc.services.RegistrationOuterClass
import im.dlg.sdk.DialogSdk
import im.dlg.sdk.SdkScreen
import io.grpc.Metadata
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.AbstractStub
import io.grpc.stub.MetadataUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.URI
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class LoginViewModel(private val sdk: DialogSdk) : ViewModel() {
    private val compositeDisposable = CompositeDisposable()
    private val state = MutableLiveData<State>(State.Initial).doOnNext { state ->
        if (state is State.Success) {
            sdk.setToken(state.token)
        }
    }
    fun observeState(): LiveData<State> = state

    init {
        sdk.setTokenExpiredListener {
            state.postValue(State.Initial)
        }
    }

    fun onLoginAction(name: String, password: String) {
        state.value = State.Loading

        Single.fromCallable {
            val uri = URI.create("$TEST_HOST:$TEST_PORT")

            val channel = OkHttpChannelBuilder
                    .forTarget(uri.authority)
                    .build()

            val registrationStub = RegistrationGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS)

            val token = registrationStub.registerDevice(
                    RegistrationOuterClass
                            .RequestRegisterDevice
                            .newBuilder()
                            .setAppTitleBytes(ByteString.copyFrom("TestOEM".toByteArray()))
                            .setDeviceTitle(Build.DEVICE)
                            .build()
            )
                    .token

            val authStub = AuthenticationGrpc.newBlockingStub(channel)
                    .withToken(token)
                    .withDeadlineAfter(10, TimeUnit.SECONDS)

            val transactionHash = authStub.startUsernameAuth(
                    AuthenticationOuterClass
                            .RequestStartUsernameAuth.newBuilder()
                            .setUsername(name)
                            .setTimeZone(StringValue.of(TimeZone.getDefault().id))
                            .addAllPreferredLanguages(listOf(Locale.getDefault().toString()))
                            .build()
            )
                    .transactionHash

            authStub.validatePassword(
                    AuthenticationOuterClass
                            .RequestValidatePassword.newBuilder()
                            .setTransactionHash(transactionHash)
                            .setPassword(password)
                            .build()
            )

            val user = authStub.getSelf(Empty.getDefaultInstance())

            return@fromCallable token
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { token -> state.value = State.Success(token) },
                        { e -> state.value = State.Fail("$e") }
                )
                .also(compositeDisposable::add)
    }

    fun onLogoutAction() {
        sdk.logout()
        state.value = State.Initial
    }

    fun openTab(tab: SdkScreen) {
        sdk.openTab(tab)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
        sdk.setTokenExpiredListener(null)
    }

    sealed class State {
        object Initial : State()
        object Loading : State()
        data class Success(val token: String) : State()
        data class Fail(val reason: String) : State()
    }

    private fun <T : AbstractStub<T>> T.withToken(token: String): T {
        val header = Metadata()
        val key = Metadata.Key.of("x-auth-ticket", Metadata.ASCII_STRING_MARSHALLER)
        header.put(key, token)
        return MetadataUtils.attachHeaders(this, header)
    }

    class Factory(private val sdk: DialogSdk) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LoginViewModel(sdk) as T
        }

    }

    private fun <T> MutableLiveData<T>.doOnNext(func: (T) -> Unit): MutableLiveData<T> {
        return value?.let { ListenableMutableLiveData(func, it) } ?: ListenableMutableLiveData(func)
    }

    private class ListenableMutableLiveData<T> : MutableLiveData<T> {
        private val listener: (T) -> Unit

        constructor(listener: (T) -> Unit, value: T) : super(value) {
            this.listener = listener
            listener(value)
        }
        constructor(listener: (T) -> Unit) : super() {
            this.listener = listener
        }

        override fun setValue(value: T) {
            super.setValue(value)
            listener(value)
        }
    }
}