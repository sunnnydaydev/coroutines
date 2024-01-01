package com.example.coroutines

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            test2()
        }
    }


    private suspend fun test3() {
        emptyFlow<Int>().onEmpty {
            println("my-test:onEmpty")
        }.collect()
    }

    private suspend fun test2() {
        flow {
            emit(1)
            throw IllegalArgumentException()
        }.onStart {
            println("my-test: onStart")
        }.retryWhen { _, count ->
            println("my-test: retry:${count}")
            count < 2
        }.catch {
            println("my-test: catch:${it}")
        }.collect {
            println("my-test: collect:${it}")
        }
    }

    private suspend fun test1() {
        val flow = flow {
            emit(1)
        }
        flow.onStart {
            println("my-test: onStart")
        }.onEach {
            println("my-test: onEach:$it")
        }.onCompletion {
            println("my-test: onCompletion")
        }.collect {
            println("my-test: collect:${it}")
        }
    }
}
