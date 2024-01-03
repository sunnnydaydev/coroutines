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

SharedFlow用订阅者的相关话术称呼更加合适，它可以将已发送过的数据发送给新的订阅者并且具有高的配置性。

先来看一个🌰

```kotlin
class MainViewModel : ViewModel() {
    
    private val _event: MutableSharedFlow<Int> = MutableSharedFlow()

    val event: SharedFlow<Int> = _event

    init {
        viewModelScope.launch {
            //SharedFlow 无emit方法，MutableSharedFlow有
            _event.emit(1)
            _event.emit(2)
            _event.emit(3)
        }
    }
}
```

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 每个collect调用就是一个新的订阅者
                latestNewsViewModel.event.collect {
                    Log.d("my-test", "观察者1拿到数据:${it}")
                }

                latestNewsViewModel.event.collect {
                    Log.d("my-test", "观察者2拿到数据:${it}")
                }

            }
        }
    }
```

###### 1、订阅者collect

如上代码，我们run之后就会碰到第一个问题，看下log打印

D  观察者1拿到数据:1

D  观察者1拿到数据:2

D  观察者1拿到数据:3

可以发现观察者2未打印log，不是说好的每个订阅者都能订阅到数据的么？

这个与MutableSharedFlow的实现类SharedFlowImpl有关，在他的collect方法中会不断的接收值，会不断的挂起当前协程，不断的恢复。
因此会只走当前协程中首个collect方法，要想其他的collect方法也执行只需要新开协程即可。

```kotlin
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    latestNewsViewModel.event.collect {
                        Log.d("my-test", "观察者1拿到数据:${it}")
                    }
                }

                launch {
                    latestNewsViewModel.event.collect {
                        Log.d("my-test", "观察者2拿到数据:${it}")
                    }
                }
            }
        }
```

###### 2、replay参数

SharedFlow 默认无粘性的，后面的订阅者默认收不到之前已emit的数据，想要接受就需要replay参数控制。看个🌰：

```kotlin
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {


                launch {
                    latestNewsViewModel.event.collect {
                        Log.d("my-test", "观察者1拿到数据:${it}")
                    }
                }

                launch {
                    latestNewsViewModel.event.collect {
                        Log.d("my-test", "观察者2拿到数据:${it}")
                    }
                }

                findViewById<View>(R.id.btnTest).setOnClickListener {
                    Log.d("my-test", "add 观察者3")
                    launch {
                        latestNewsViewModel.event.collect {
                            Log.d("my-test", "观察者3拿到数据:${it}")
                        }
                    }
                }

            }
        }

/***
D  观察者1拿到数据:1
D  观察者2拿到数据:1
D  观察者2拿到数据:2
D  观察者1拿到数据:2
D  观察者1拿到数据:3
D  观察者2拿到数据:3
D  add 观察者3
 * */
```
通过log我们可以发现，点击事件后添加了新的观察者3，但是这个观察者未收到之前的数据，要想让观察着收到之前的数据就要通过replay参数来设置

replay 为0 代表不重放，也就是没有粘性
replay 为1 代表重放最新的一个数据，后来的接收器能接受1个最新数据。
replay 为2 代表重放最新的两个数据，后来的接收器能接受2个最新数据。

```kotlin
class MainViewModel : ViewModel() {

    // 这里传参1后，再次操作上述的步骤点击事件触发后便会打印新的log->
    // D  观察者3拿到数据:3
    private val _event: MutableSharedFlow<Int> = MutableSharedFlow(1)

    val event: SharedFlow<Int> = _event

    init {
        viewModelScope.launch {
            _event.emit(1)
            _event.emit(2)
            _event.emit(3)
        }
    }
}
```

###### 3、extraBufferCapacity

```kotlin
public fun <T> MutableSharedFlow(
    replay: Int = 0,
    extraBufferCapacity: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): MutableSharedFlow<T> {
    require(replay >= 0) { "replay cannot be negative, but was $replay" }
    require(extraBufferCapacity >= 0) { "extraBufferCapacity cannot be negative, but was $extraBufferCapacity" }
    require(replay > 0 || extraBufferCapacity > 0 || onBufferOverflow == BufferOverflow.SUSPEND) {
        "replay or extraBufferCapacity must be positive with non-default onBufferOverflow strategy $onBufferOverflow"
    }
    val bufferCapacity0 = replay + extraBufferCapacity
    val bufferCapacity = if (bufferCapacity0 < 0) Int.MAX_VALUE else bufferCapacity0 // coerce to MAX_VALUE on overflow
    return SharedFlowImpl(replay, bufferCapacity, onBufferOverflow)
}
```

这里吧源码粘过来方便理解，extraBufferCapacity见名知意"额外的数据缓冲池"

Flow 存在发送过快，消费太慢的情况，这种情况下，就需要使用缓存池，把未消费的数据存下来。缓冲池大小从源码可以看出：

bufferCapacity0 = replay + extraBufferCapacity

bufferCapacity0为负数时则缓冲池大小为Int.MAX_VALUE

###### 4、onBufferOverflow

当指定了有限的缓冲容量，并且超过了这个容量时onBufferOverflow 缓冲策略就起作用啦，这个有三种策略：

- BufferOverflow.SUSPEND ： 超过就挂起，默认实现
- BufferOverflow.DROP_OLDEST : 丢弃最老的数据
- BufferOverflow.DROP_LATEST : 丢弃最新的数据


###### 5、SharedFlow使用场景

发生订阅时，需要将过去已经更新的 n 个值，同步给新的订阅者

###### 6、tryEmit与emit区别

当 MutableSharedFlow 中缓存数据量超过阈值时，emit 方法和 tryEmit 方法的处理方式会有不同：

- emit 方法：当缓存策略为 BufferOverflow.SUSPEND 时，emit 方法会挂起，直到有新的缓存空间。
- tryEmit 方法：tryEmit 会返回一个 Boolean 值，true 代表传递成功，false 代表会产生一个回调，让这次数据发射挂起，直到有新的缓存空间。

###### 7、shareIn

将冷流转化为SharedFlow，官方🌰

```kotlin
class NewsRemoteDataSource(
    private val externalScope: CoroutineScope,
) {
    val latestNews: Flow<List<ArticleHeadline>> = flow {
        
    }.shareIn(
        externalScope,
        replay = 1,
        started = SharingStarted.WhileSubscribed() // 启动政策
    )
}

```

started提供了三种策略：

- SharingStarted.WhileSubscribed()：存在订阅者时，将使上游提供方保持活跃状态。
- SharingStarted.Eagerly：立即启动提供方。
- SharingStarted.Lazily：在第一个订阅者出现后开始共享数据，并使数据流永远保持活跃状态。


# 参考


[官方文档](https://developer.android.google.cn/kotlin/flow/stateflow-and-sharedflow?hl=ru)



