# StateFlow与SharedFlow

- StateFlow
- SharedFlow

Flow涉及到三个概念

    producer  ->  Intermediary  -> consumer

    数据提供方      中间可修改数据     数据使用方

# 一、StateFlow

StateFlow是一个可观察数据流，当其状态发生更新时，collect末端操作符变自动收到最新值。当前的状态值可以通过value来读取，若是需要更新其状态并将
数据发送到数据流只需为MutableStateFlow 类的 value 属性分配一个新值即可。举个🌰


```kotlin
/**
 * Create by SunnyDay /12/28 21:36:44
 */
class MainViewModel : ViewModel() {

    private var count = 0

    /**
     * 对内只操作这个类即可
     * */
    private val _uiState: MutableStateFlow<Int> = MutableStateFlow(0)

    /**
     * 对外使用这个类来获取最新数据。
     * */
    val uiState: StateFlow<Int> = _uiState

    /**
     * 初始时通过循环不断更新数据
     * */
    init {
        viewModelScope.launch {
            while (count <= 100) {
                _uiState.value = count
                count += 2
            }
        }
    }

    /**
     * 手动更新数据
     * */
    fun updateValue() {
        viewModelScope.launch {
            for (i in 0..3) {
                _uiState.value = _uiState.value + i
            }
        }
    }
}
```

```kotlin
class MainActivity : AppCompatActivity() {

    private val latestNewsViewModel: LatestNewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * init 中_uiState.value的最后值是100
         * */
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                latestNewsViewModel.uiState.collect {
                    Log.d("MainActivity", "UI层拿到数据:${it}")
                }
            }
        }
        /**
         *activity页面跑起来后点击下按钮：
         *这里_uiState.value在100的基础上求和0,1,2,3 只输出最终的值106
         * */
        findViewById<View>(R.id.btnTest).setOnClickListener {
            latestNewsViewModel.updateValue()
        }
    }
}

/**
D  UI层拿到数据:0
D  UI层拿到数据:100

D  UI层拿到数据:106
 * */
```

可见不会搜集到所有的数据，只更新拿到最新的数据。

负责更新 MutableStateFlow 的类是提供方，从 StateFlow 收集的所有类都是使用方。与使用 flow 构建器构建的冷数据流不同，StateFlow 是热
数据流。从数据流收集数据不会触发任何提供方代码。StateFlow 始终处于活跃状态并存于内存中，而且只有在垃圾回收根中未涉及对它的其他引用时，它才符合垃圾回收条件。

当新使用方开始从数据流中收集数据时，它将接收信息流中的最近一个状态及任何后续状态。如上onClickListener🌰 直接使用上一次的value 100

如果需要更新界面，切勿使用 launch 或 launchIn 扩展函数从界面直接收集数据流。即使 View 不可见，这些函数也会处理事件。此行为可能会导致应用崩溃。
为避免这种情况，请使用 repeatOnLifecycle API（androidx.lifecycle:lifecycle-runtime-ktx:2.4.0）。

可以使用stateIn api 把普通的flow转化为StateFlow

###### 1、StateFlow与LiveData区别

相同点

- 两者都是可观察的数据容器类，并且在应用架构中使用时，两者都遵循相似模式

不同点

- StateFlow 需要将初始状态传递给构造函数，而 LiveData 不需要。
- 当 View 进入 STOPPED 状态时，LiveData.observe() 会自动取消注册使用方，而从 StateFlow 或任何其他数据流收集数据的操作并不会自动停止。如需实现相同的行为，您需要从 Lifecycle.repeatOnLifecycle 块收集数据流。



# 二、SharedFlow


# 参考


[官方文档](https://developer.android.google.cn/kotlin/flow/stateflow-and-sharedflow?hl=ru)



