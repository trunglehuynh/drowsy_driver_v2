package com.galaxylab.drowsydriver.Utility

//import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.launchAndLogException(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {

    val wrapperBlock: suspend CoroutineScope.() -> Unit = {
        try {
            block()
        } catch (e: Exception) {
            logException(e)
            Timber.e(e)
        }
    }

    val job = launch(context, start, wrapperBlock)
    job.invokeOnCompletion { logException(it) }

    return job
}

@ExperimentalCoroutinesApi
fun <T> CoroutineScope.asyncAndLogExc(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {

    val deferred = async(context, start, block)

    deferred.invokeOnCompletion { logException(it) }

    return deferred


}

private fun logException(it: Throwable?) {
    it?.let {
        try {
//            FirebaseCrashlytics.getInstance().log(it.message.toString())
            it.printStackTrace()
            Timber.e(it)
            Timber.e("invokeOnCompletion message:${it.message} ")
        } catch (e: IllegalStateException) {
            Timber.e(e)
        }

    }
}