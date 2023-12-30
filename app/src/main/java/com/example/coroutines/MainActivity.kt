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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

      val flow =  flow {
          listOf("hello", "kotlin", "flow").forEachIndexed{ index, s ->
              if (index==1){
                  delay(1000)
              }else{
                  emit(s)
              }
          }
      }

        lifecycleScope.launch {
            flow {
                emit(1)
                delay(50)
                emit(2)
            }.collectLatest { value ->
                println("my-test Collecting $value")
                delay(100) // Emulate work
                println("my-test $value collected")
            }
        }
    }
}
