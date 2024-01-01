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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            test7()
        }
    }

    private suspend fun test7() {
        val flow = flow {
            emit(1)
            println("my-test: flow:${Thread.currentThread().name}")
        }
        flow.flowOn(Dispatchers.Main)
            .map {
                println("my-test: map:${Thread.currentThread().name}")
                it.toString()
            }.flowOn(Dispatchers.IO)
            .collect {
                println("my-test: collect:${Thread.currentThread().name}")
            }
    }

    private suspend fun test6() {
        val flow1 = flowOf(1, 2)
        val flow2 = flowOf("a", "b", "c")
        flow1.zip(flow2) { num, str ->
            "$num$str"
        }.collect {
            println("my-test:collect:$it")
        }
    }


    private suspend fun test5() {
        flowOf(1, 2, 3).dropWhile {
            it != 2
        }.collect {
            println("my-test:collect:$it")
        }
    }

    private suspend fun test4() {
        flowOf(1, 2, 3).transform {
            emit("it")
        }.collect {
            println("my-test:collect:$it")
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
