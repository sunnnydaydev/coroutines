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

- GlobalScope.launch{}
- runBlocking{launch{}}
- CoroutineScope(job).launch{}
- MainScope().launch {  }

需要注意点

- 协程是轻量级的线程。 它们运行在协程上下文中，通过 launch 协程构建器可以在协程上下文中开启一个携程。
- 协程是依赖线程的，所以其生命周期受所属线程的影响。线程执行完毕协程也会关闭。
- 线程提供了Thread.sleep 来阻塞当前线程，协程也提供了相应的挂起函数delay来阻塞当前协程。注意delay这个挂起函数会阻塞当前协成但不会阻塞协成所属的线程。

###### 1、GlobalScope.launch{}

GlobalScope.launch{}会创建一个协程作用域，并开启一个协程。

```kotlin
fun main() {

    GlobalScope.launch {
        println("hello kt Coroutine ！")
    }

    println("abs.main thread！")
    //Thread.sleep(500)
}
```

如上代码，通过GlobalScope.launch{}创建了协程域并开启一个协程。注意GlobalScope内的协程为后台协程。可以理解为java的守护线程。如何理解呢？就是通过GlobalScope.launch{}创建携程时会运行在子线程中的，这个子线程可能是已经被创建的，也可能是新创建的。但是这个子线程有个特点就是类似Java的守护线程。所以在接在主线程直接通过GlobalScope.launch{}创建的协程都是运行在类似java的守护线程中的。

运行上述程序就会发现只打印 abs.main thread！当你让主线程休眠时这个后台协程就会执行，如上把注释去除再运行即可。


接下来看看GlobalScope.launch协程与所属的线程的关系：

```kotlin
fun abs.main() {
    
    GlobalScope.launch {
        println("协程1-当前所属线程：${Thread.currentThread().name}")

        launch {
            println("协程2-当前所属线程：${Thread.currentThread().name}")
        }

        launch {
            println("协程3-当前所属线程：${Thread.currentThread().name}")
        }
        launch {
            println("协程4-当前所属线程：${Thread.currentThread().name}")
        }

        GlobalScope.launch {
            println("协程5-当前所属线程：${Thread.currentThread().name}")
        }
    }
    
    Thread.sleep(3000)
}

log：第一次运行
协程1-当前所属线程：DefaultDispatcher-worker-1
协程2-当前所属线程：DefaultDispatcher-worker-2
协程3-当前所属线程：DefaultDispatcher-worker-2
协程4-当前所属线程：DefaultDispatcher-worker-2
协程5-当前所属线程：DefaultDispatcher-worker-2

log：第二次运行
协程1-当前所属线程：DefaultDispatcher-worker-1
协程2-当前所属线程：DefaultDispatcher-worker-2
协程3-当前所属线程：DefaultDispatcher-worker-2
协程5-当前所属线程：DefaultDispatcher-worker-3
协程4-当前所属线程：DefaultDispatcher-worker-2
```

- 通过GlobalScope.launch {} 方式开启协程后，会先创建一个子线程，并运行在子线程中。
- 后续开启的协程要么运行在新的子线程中，要么运行在已存在的子线程中。这个受编程语言的调度。具有不确定性。
- 换句话来说每通过GlobalScope.launch {} 方式开启的携程都会运行在子线程中，但是运行的子线程具有不确定性，有可能是已经存在的子线程（其他协程创建的），有可能先创建个子线程再运行在子线程中。这个具有不确定性，受变成语言调度控制。

总结:GlobalScope.launch {}方式开启的协程默认运行在子线程中。

###### 2、runBlocking{launch{}}

runBlocking{launch{}}也能创建一个协程的上下文环境,它可以保证在表达式内的所有代码和子协程没有全部执行完之前一直阻塞当前线程.runBlocking函数通常只应该在测试环境下使用,在正式环境中容易产生性能上的问题.

```kotlin
fun abs.main() {
    runBlocking {
        println("currentThread：${Thread.currentThread().name}")

        launch {
            println("协程1-当前所属线程：${Thread.currentThread().name}")
        }
        launch {
            println("协程2-当前所属线程：${Thread.currentThread().name}")
        }
        launch {
            println("协程3-当前所属线程：${Thread.currentThread().name}")
        }
    }
}
log：
协程1-当前所属线程：abs.main
协程2-当前所属线程：abs.main
协程3-当前所属线程：abs.main
```
此种方式开启协程，默认的线程调度为当前线程。怎样理解呢？

- 如果runBlocking所处的线程为Main线程则launch所处的线程为Main线程

- 如果runBlocking所处的线程为work线程则launch所处的线程为Work线程

runBlocking{} 阻塞主线程验证
```kotlin
fun abs.main() {
    // 1、代码开始执行runBlocking，然后阻塞主线程
    runBlocking {
         launch {
            println("task which need long time !")
                 delay(1000*5)
        }
        println("i am quit ！")
    }
    // 2、runBlocking{} 内的代码执行完毕，开始继续往下执行，也即执行这里
    println("im abs.main") 
}

log:
i am quit ！
task which need long time !
im abs.main // 这个延迟了5s才被输出
```

###### 3、CoroutineScope(job).launch{}

CoroutineScope对象也具有协程上下文，使用CoroutineScope对象的launch创建的协程统统会被job所管理.大大降低协程维护成本.

```kotlin
    val job = Job()
    val scope = CoroutineScope(job) 
    scope.launch {
        println("当前Thread：${Thread.currentThread().name}") 
    }
```

此种方式开启携程，默认的线程调度为子线程.


###### 4、小结

- 掌握哪些对象可调用launch{}
- 掌握不同对象通过launch开启携程后，知道协程所属的线程。
- 了解其他的对象如Android的MainScope等也可以调用launch。


# 挂起函数与异步结果

挂起函数概念我们可以先不看，初次上来一看可能懵逼，不理解，，，，目前我们只需知道如下：

- delay是我们接触的第一个挂起函数，他的作用是非阻塞当前线程，阻塞当前携程。
- Suspend function should be called only from a coroutine or another suspend function 翻译过来很简单，挂起函数只能被协程作用域或者其他的挂起函数调用~

###### 1、挂起函数定义&调用

```kotlin
fun abs.main() = runBlocking {
    // 计算顺序调用的执行时间
    val time = measureTimeMillis {
        val a = testOne()
        val b = testTwo()
        println("a+b=${a+b}")
    }
    println("耗时：$time ms")
}

/**
 * 通过suspend 关键字定义的函数就是挂起函数。
 * */
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

这里需要阐明一点了开启协程有两种方式：

- launch
- async

async 类似于 launch。它启动了一个单独的协程，不同之处在于 launch 返回一个 Job 并且不附带任何结果值，而 async 返回一个 Deferred ， 这代表了一个将会在稍后提供结果， 可以使用await() 在一个延期的值上得到它的最终结果，Deferred 也是一个 Job，所以如果需要的话，你可以取消它。

还是上述栗子，假如testOne、testTwo两个函数没有执行顺序，这时可以让二者并发执行，而且使用async可以获取执行结果：

```kotlin
fun abs.main() = runBlocking {

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

刚接触，这里有人可能会出现疑惑：launch也是开启了协程，也是异步的，为啥还要设计async呢？

注意async的设计是有返回结果的，可以异步计算，而launch无返回结果，不可进行异步计算。


###### 3、async的惰性启动

如果不想async{}之后就立即启动协程可以通过将 start 参数设置为 CoroutineStart.LAZY 而变为惰性的。 这样只有结果通过 await 获取的时候协程才会启动，或者在 Job 的 start 函数调用的时候。

```kotlin
fun abs.main() = runBlocking {

    val test1 = async (start = CoroutineStart.LAZY){
        println("1当前线程：${Thread.currentThread().name}")
        1+1
    }
    
    val test2 = async (start = CoroutineStart.LAZY) {
        println("2当前线程：${Thread.currentThread().name}")
        1+1
    }
    
    println("结果：${test1.await()}") // 调用await时才启动
    println("结果：${test2.await()}") // 调用await时才启动

}

log:

1当前线程：abs.main
结果：2
2当前线程：abs.main
结果：2

```
其实你会发现上述的pln注释之后是async 是不会有结果打印的，证明CoroutineStart.LAZY时async未执行。


# 协程上下文与调度器

###### 1、调度器与线程

协程上下文包含一个协程调度器 （ CoroutineDispatcher）它确定了相关的协程在哪个线程或哪些线程上执行。协程调度器可以将协程限制在一个特定的线程执行，或将它分派到一个线程池，亦或是让它不受限地运行。
所有的协程构建器诸如 launch 和 async 接收一个可选的 CoroutineDispatcher 参数，它可以被用来显式的为一个新协程或其它上下文元素指定一个调度器。

```kotlin
@ObsoleteCoroutinesApi
fun abs.main() = runBlocking<Unit> {
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
- Default 默认的调度器。祥见前面的几种开启方式，使用的就是默认调度器。
- Main：协程运行在Main线程
- Unconfined 是一个特殊的调度器且似乎也运行在 abs.main 线程中，但实际上， 它是一种不同的机制，这会在后文中讲到。
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
fun abs.main() = runBlocking<Unit> {

    launch(Dispatchers.Unconfined+CoroutineName("aaa")) {

    }

}
```


# 补充

###### 1、withContext

withContext可以理解为async函数的简化版,它是一个挂起函数,返回结果是withContext函数体内最后一行代码.相当于val result = async{a+b}.await()

```kotlin
fun abs.main() = runBlocking {
    val result = withContext(Dispatchers.Default) {
        1+1
    }
    println(result)
}
```
withContext还是挺nice的：

- 任意切线程
- lambda带返回结果

