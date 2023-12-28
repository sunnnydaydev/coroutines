package com.example.coroutines

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch(Dispatchers.Main) {
            println("my test lifecycleScope.launch1 - ${Thread.currentThread().name}")
            val json = withContext(Dispatchers.IO) {
                getDataFromBackend()
            }
            println("my test lifecycleScope.launch2 - ${Thread.currentThread().name}")
            // parse json and update ui
        }
    }

    private suspend fun getDataFromBackend(): String {
        println("my test getDataFromBackend - ${Thread.currentThread().name}")
        // 模拟网络请求
        return "i am json"
    }
}

2023-12-28 22:14:48.675 30305-30305 System.out              com.example.coroutines               I  my test lifecycleScope.launch1 - main
2023-12-28 22:14:48.678 30305-30329 System.out              com.example.coroutines               I  my test getDataFromBackend - DefaultDispatcher-worker-2
2023-12-28 22:14:48.678 30305-30305 System.out              com.example.coroutines               I  my test lifecycleScope.launch2 - main
