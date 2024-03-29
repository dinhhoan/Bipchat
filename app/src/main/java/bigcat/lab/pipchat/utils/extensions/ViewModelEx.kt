// Package set to androidx.lifecycle so we can have access to package private methods

@file:Suppress("PackageDirectoryMismatch")

package androidx.lifecycle

import bigcat.lab.pipchat.data.error.ErrorModel
import bizverse.lab.healthylifestyle.utils.extensions.FlowResult
import bizverse.lab.healthylifestyle.utils.extensions.LoadingAware
import bizverse.lab.healthylifestyle.utils.extensions.ViewErrorAware
import bizverse.lab.healthylifestyle.utils.extensions.onError
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * @author mvn-toan.nguyen2 on 8/23/22
 **/

private const val ERROR_FLOW_KEY = "androidx.lifecycle.ErrorFlow"
private const val LOADING_FLOW_KEY = "androidx.lifecycle.LoadingFlow"

fun <T> T.sendViewError(errorModel: ErrorModel) where T : ViewErrorAware, T : ViewModel {
    viewModelScope.launch {
        getErrorMutableSharedFlow().emit(errorModel)
    }
}

suspend fun <T> T.emitErrorModel(errorModel: ErrorModel) where T : ViewErrorAware, T : ViewModel {
    getErrorMutableSharedFlow().emit(errorModel)
}

val <T> T.viewErrorFlow: SharedFlow<ErrorModel> where T : ViewErrorAware, T : ViewModel
    get() {
        return getErrorMutableSharedFlow()
    }

val <T> T.loadingFlow: StateFlow<Boolean> where T : LoadingAware, T : ViewModel
    get() {
        return loadingMutableStateFlow
    }

var <T> T.isLoading: Boolean where T : LoadingAware, T : ViewModel
    get() {
        return loadingMutableStateFlow.value
    }
    set(value) {
        loadingMutableStateFlow.tryEmit(value)
    }

private val <T> T.loadingMutableStateFlow: MutableStateFlow<Boolean> where T : LoadingAware, T : ViewModel
    get() {
        val flow: MutableStateFlow<Boolean>? = getTag(LOADING_FLOW_KEY)
        return flow ?: setTagIfAbsent(LOADING_FLOW_KEY, MutableStateFlow(false))
    }

private fun <T> T.getErrorMutableSharedFlow(): MutableSharedFlow<ErrorModel> where T : ViewErrorAware, T : ViewModel {

    val flow: MutableSharedFlow<ErrorModel>? = getTag(ERROR_FLOW_KEY)
    return flow ?: setTagIfAbsent(ERROR_FLOW_KEY, MutableSharedFlow())
}

fun <F, T> Flow<FlowResult<F>>.bindLoading(t: T): Flow<FlowResult<F>> where T : LoadingAware, T : ViewModel {
    return this
        .onStart {
            t.loadingMutableStateFlow.value = true
        }
        .onCompletion {
            t.loadingMutableStateFlow.value = false
        }
}

fun <F, T> Flow<FlowResult<F>>.bindError(t: T): Flow<FlowResult<F>> where T : ViewErrorAware, T : ViewModel {
    return this
        .onError(normalAction = {
            t.emitErrorModel(it)
        })
}

fun <F, T> Flow<FlowResult<F>>.bindCommonError(t: T): Flow<FlowResult<F>> where T : ViewErrorAware, T : ViewModel {
    return this
        .onError(commonAction = {
            t.emitErrorModel(it)
        })
}
