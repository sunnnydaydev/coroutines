# 协程

- 概述
- 协程的创建
- 挂起函数与异步结果
- 协程上下文与调度器
- 补充

# 概述

###### 1、操作系统的多进程

操作系统上每个Application都可以看做一个进程，就拿Windows操作系统为例子：浏览器、酷狗音乐、微信等这些Application都是运行在不同的进程中的。当然还有一些进程没有快捷方式（或者图标），他们运行在后台。

###### 2、那么你知道单核CPU是怎么执行多任务的呢？

操作系统轮流让各个任务交替执行，表面上看，每个任务都是交替执行的，但是，由于CPU的执行速度实在是太快了，我们感觉就像所有任务都在同时执行一样。真正的并行执行多任务只能在多核CPU上实现，但是，由于任务数量远远多于CPU的核心数量，所以，操作系统也会自动把很多任务轮流调度到每个核心上执行。

###### 3、线程

线程是进程中的概念，是进程中任务步骤的细分。单个进程中多个线程之间的切换也是通过操作系统的调度来实现的。

###### 4、协程

协程是微线程，一个线程可以分为多个协程，同样也是轮询执行，这样的好处是减少CPU的资源消耗，一些比较多而且小的事件可以用协程去处理，减少资源的开销。

协程允许在单线程模式下模拟多线程编程的效果,代码执行时的挂起与恢复完全是由编程语言来控制,和操作系统无关。

###### 5、小结

可以发现：多进程、多线程之间的切换依靠操作系统来调度。而协程之间的切换由编程语言设计时决定。具体的调度都不是由使用者决定的。


# 依赖

使用协程库需要单独引入依赖

```groovy
    // 协程核心库
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.1"
    // 协程Android支持库
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1"
```

这里使用了kotlinx-coroutines-core-jvm其实kotlinx-coroutines还提供了其他模块具体功能参考[官方文档](https://github.com/hltj/kotlinx.coroutines-cn/blob/master/README.md#using-in-your-projects)

kotlinx-coroutines-android 是建立在 kotlinx-coroutines-core-jvm 之上的一个专门为 Android 定制的模块，提供了更方便的 API 和功能，以适应 Android 平台的特殊需求。在 Android 开发中，通常你会同时引入这两个库，以充分利用协程在异步编程方面的优势，并与 Android 平台进行良好的集成。

我们还可以根据选择添加一些kts拓展库

```kotlin
    //viewModel对协程扩展封装（viewModelScope）
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0"
    // lifecycle对协程的扩展封装（lifeCycleScope）
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.2.0"
```

若需要添加其他kts库可以查看[官方文档](https://developer.android.google.cn/kotlin/ktx?hl=zh-cn#core)


# 协程的创建

Kotlin中常见的几种创建方式

- GlobalScope
- runBlocking
- MainScope
- CoroutineScope
- viewModelScope（需要单独引入对应ktx扩展）
- lifecycleScope（需要单独引入对应ktx扩展）

需要注意点

- 协程是轻量级的线程。 它们运行在协程上下文中，通过 launch 协程构建器可以在协程上下文中开启一个携程。
- 协程是依赖线程的，所以其生命周期受所属线程的影响。线程执行完毕协程也会关闭。
- 线程提供了Thread.sleep 来阻塞当前线程，协程也提供了相应的挂起函数delay来阻塞当前协程。注意delay这个挂起函数会阻塞当前协成但不会阻塞协成所属的线程。

###### 1、GlobalScope.launch{}

GlobalScope 是一个顶层的 CoroutineScope，它用于启动顶级协程，这些协程的生命周期与整个应用程序的生命周期相同。

GlobalScope 的协程不受限于特定的作用域，因此它们可以在整个应用程序中运行。在使用 GlobalScope 时，需要注意，它创建的协程在整个应用程序的生命周期内都会存在，因此需要小心避免内存泄漏。


```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        GlobalScope.launch {
            println("my-test hello kt Coroutine ！")
        }
        println("my-test main thread！")
    }
}
```
很简单使用GlobalScope的launch方法开启一个协程,这里我们先明白一点GlobalScope.launch开启的协程默认跑在 子线程中、子线程中、子线程中。


###### 2、runBlocking{launch{}}

runBlocking{}也能创建一个协程的上下文环境,它可以保证在表达式内的所有代码和子协程没有全部执行完之前一直阻塞 当前 当前 当前线程.runBlocking函数通常只应该在测试环境下使用,在正式环境中容易产生性能上的问题.

此种方式开启协程，默认的线程调度为当前线程。怎样理解呢？

- 如果runBlocking所处的线程为Main线程则launch所处的线程默认为Main线程

- 如果runBlocking所处的线程为work线程则launch所处的线程默认为Work线程

看个🌰

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        thread {
            runBlocking {
                launch {
                    println("runningBlock2：${Thread.currentThread().name}")
                }
            }
        }
        
        runBlocking {
            launch {
                println("runningBlock1：${Thread.currentThread().name}")
            }
        }
    }
}

I  runningBlock1：main
I  runningBlock2：Thread-2
```

###### 3、MainScope

MainScope 是一个特殊的 CoroutineScope，通常用于与 UI 线程相关的协程操作。MainScope 的设计目的是为了简化与 Android 应用程序或者其他 UI 框架集成时的协程管理。

UI 操作需要在主线程上执行，而协程默认在后台线程上运行。为了在协程中执行 UI 操作，可以使用 MainScope 来创建一个与主线程关联的协程作用域。

```kotlin
class MainActivity : AppCompatActivity() {
    private val mainScope = MainScope()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainScope.launch {
            delay(1000*10)
            println("my-test: main scope test")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //取消与主线程关联的写成，避免内存泄漏
        mainScope.cancel()
    }
}
```

这里我们就需要留意几点了：

- MainScope开启协程后协程运行在UI线程，协程内的耗时操作不会阻塞主线程。为啥呢？这里涉及到了协程的Dispatchers，MainScope的Dispatchers是
Dispatchers.Main，协程库专门进行了处理，Dispatchers.Main的协程内可以进行UI操作。
- MainScope使用完毕后记得cancel，这个需要我们手动处理下。



###### 4、CoroutineScope(job)

CoroutineScope也具有协程上下文，使用CoroutineScope的launch创建的协程统统会被job所管理.大大降低协程维护成本.

```kotlin
    val job = Job()
    val scope = CoroutineScope(job) 
    scope.launch {
       // todo 
    }
    job.cancel()
```

此种方式开启协程，默认的线程调度为子线程. 细心的我们会发现MainScope就是对CoroutineScope的封装，写死了线程调度器。记住使用CoroutineScope
也记得在Android组件生命周期结束时cancel。

###### 5、viewModelScope

这个很简单，是ktx对协程的扩展，android中默认帮我们处理了生命周期。默认跑在UI线程。可通过任务调度器手动切换线程

```kotlin
class MainViewModel:ViewModel() {
    fun login() = viewModelScope.launch { 
        delay(1000)
    }
}
```

# 挂起函数与异步结果

挂起函数概念我们可以先不看，目前我们只需知道如下：

delay是我们接触的第一个挂起函数，他的作用是非阻塞当前线程，阻塞当前携程。

Suspend function should be called only from a coroutine or another suspend function

###### 1、挂起函数定义&调用

通过suspend关键字定义的函数就是挂起函数。

```kotlin
fun main() = runBlocking {
    // 计算顺序调用的执行时间
    val time = measureTimeMillis {
        val a = testOne()
        val b = testTwo()
        println("a+b=${a+b}")
    }
    println("耗时：$time ms")
}

suspend fun testOne(): Int {
    delay(1000)
    return 10
}

suspend fun testTwo(): Int {
    delay(1000)
    return 20
}

log:
a+b=30
耗时：2045 ms
```
代码顺序执行，代码延迟时间+代码执行时间是大于2秒的。符合预期~

###### 2、带返回结果的协程async

kt中开启协程有两种方式：launch{} 、async{}二者的不同点在于

- launch{}会返回一个Job并且不附带任何结果值。
- async{}会返回一个Deferred，可以使用Deferred.await()来获取这个异步结果，Deferred也是一个 Job，如果需要我们也可以取消它。

还是上述栗子，假如testOne、testTwo两个函数没有执行顺序，这时可以让二者并发执行，而且使用async可以获取执行结果：

```kotlin
fun main() = runBlocking {
    // 计算并发执行的总时间
    val time = measureTimeMillis {
        val a = async { testOne() } 
        val b = async {  testTwo() }
        println("a+b=${a.await()+b.await()}")
    }
    println("耗时：$time ms")
}

suspend fun testOne(): Int {
    delay(1000)
    return 10
}

suspend fun testTwo(): Int {
    delay(1000)
    return 20
}

log:
a+b=30
耗时：1032 ms
```

可见带返回结果的async很容易实现异步计算~

刚接触，这里有人可能会出现疑惑：launch也是开启了协程，也是异步的，为啥还要设计async呢？ 这里需要明白这点：

async的设计是有返回结果的，可以异步计算，而launch无返回结果，不可进行异步计算。


###### 3、async的惰性启动

如果不想async{}之后就立即启动协程可以通过将 start 参数设置为 CoroutineStart.LAZY 而变为惰性的。

这样只有结果通过 await 获取的时候协程才会启动，或者在 Job的 start 函数调用的时候。

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {

            val def = async(start = CoroutineStart.LAZY) {
                println("my-test: async")
                1
            }

            delay(1000*10)
            val result = def.await()
            println("my-test: result:$result")
        }
    }
}

```

我们会发现程序跑起来10s后两个log会依次打印。在此之前无log打印


# 协程上下文与调度器

###### 1、调度器与线程

协程上下文包含一个协程调度器 （ CoroutineDispatcher）它确定了相关的协程在哪个线程或哪些线程上执行。协程调度器可以将协程限制在一个特定的线程执行，或将它分派到一个线程池，亦或是让它不受限地运行。
所有的协程构建器诸如 launch 和 async 接收一个可选的 CoroutineDispatcher 参数，它可以被用来显式的为一个新协程或其它上下文元素指定一个调度器。

```kotlin
@ObsoleteCoroutinesApi
fun main() = runBlocking<Unit> {
     // 默认运行在当前线程   
     launch{

     }
    //主动指定运行在IO线程
    launch (Dispatchers.IO){ 
       
    }
    // 主动指定运行在主线程    
    async(Dispatchers.Main) {
      
    }
}
```

Dispatchers中定义了如下：

- Default 默认的调度器。此调度程序经过了专门优化，适合在主线程之外执行占用大量 CPU 资源的工作。用例示例包括对列表排序和解析 JSON
- Main：协程运行在Main线程
- Unconfined 是一个特殊的调度器且似乎也运行在main 线程中，但实际上， 它是一种不同的机制，这会在后文中讲到。
- IO 协程运行在IO线程

###### 2、子协程

当一个协程在其它协程中启动的时候，它将通过 CoroutineScope.coroutineContext 来承袭上下文，并且这个新协程的 Job 将会成为父协程作业的子作业。当一个父协程被取消的时候，所有它的子协程也会被递归的取消。

注意：当使用 GlobalScope 来启动一个协程时，则新协程的作业没有父作业。 因此它与这个启动的作用域无关且独立运作。

###### 3、协程命名

launch{}  或者async{} 传参CoroutineName("xxx") 即可

```kotlin
val v1 = async(CoroutineName("xxx")) {}
```

注意有时我们需要在协程上下文中定义多个元素。我们可以使用 + 操作符来实现。 比如说，我们可以显式指定一个调度器来启动协程并且同时显式指定一个命名:

```kotlin
fun main() = runBlocking<Unit> {

    launch(Dispatchers.Unconfined+CoroutineName("aaa")) {

    }

}
```

# 功能强大的withContext

withContext可以理解为async函数的简化版,它是一个挂起函数,返回结果是withContext函数体内最后一行。代码相当于val result = async{a+b}.await()

```kotlin
fun main() = runBlocking {
    val result = withContext(Dispatchers.Default) {
        1+1
    }
    println(result)
}
```
withContext的任意切换线程、消除了回调还是很nice的

```kotlin
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
```
I  my test lifecycleScope.launch1 - main
I  my test getDataFromBackend - DefaultDispatcher-worker-2
I  my test lifecycleScope.launch2 - main

如图以同步的方式写异步操作十分nice~  getDataFromBackend是一个挂起函数，这跑在work线程中withContext块以外的代码都是跑在主线程的。

# The End 

[Kotlin coroutines doc](https://legacy.kotlincn.net/docs/reference/coroutines/coroutines-guide.html)

[Kotlin coroutines in Android](https://developer.android.google.cn/kotlin/coroutines?hl=zh_cn)
