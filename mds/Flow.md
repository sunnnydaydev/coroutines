# 集合序列Flow

###### 1、集合序列Flow三者有啥区别呢？

其实在kotlin官方文档学习集合这一章节的知识点时我们已经了解到了集合与序列的区别现在再来回顾一下：

在kotlin中集合可以用来表示一组数据，序列也可以表示一组数据，但在对数据的计算操作上二者是不同的：

集合是"所有元素"依次执行每个计算步骤。

序列则是"单个元素"依次执行每个计算步骤。

###### 2、序列与Flow的区别

序列与Flow的区别很简单一个是同步计算，一个是异步计算。

那么为啥Flow是异步计算的呢？ 这里就牵涉到了挂起函数，Flow的下游操作符都是挂起的。

挂起函数是指包含suspend修饰符的函数，它们可以在协程中被调用，并且可以暂停协程的执行而不会阻塞线程。这并不意味着挂起函数一定是异步的。挂起函数可以用于同步的代码。

异步操作是指在不等待任务完成的情况下继续执行后续代码。在协程中，可以使用挂起函数来执行异步操作。这通常涉及到与异步库（如async、await等）一起使用，或者使用特定的挂起函数，如delay，以便在指定的时间后恢复协程的执行。

虽然挂起函数可以用于异步编程，但它们本身并不一定是异步的。挂起函数的异步性质通常是通过与异步库的结合，或者在函数内使用特定的挂起函数（如delay）来实现的。

# 异步流

一个异步数据流，通常包含三部分：

- 上游
- 操作符
- 下游

流有冷热之分：

冷流：下游无消费行为时，上游不会产生数据，只有下游开始消费，上游才从开始产生数据。

热流：无论下游是否有消费行为，上游都会自己产生数据。

# Flow的创建

仅仅创建Flow，是不会执行Flow中的任何代码的，kotlin中有如下几种常见的创建方式:

###### 1、flow

如下创建flow对象，并生产数据

```kotlin
flow {
    listOf("hello", "kotlin", "flow").forEach {
        emit(it)//生产数据
    }
}
```

###### 2、flowOf

如何使用呢？代码如下：

```kotlin
flowOf("hello", "kotlin", "flow")
```

很简单针对flow{}进行了封装

```kotlin
public fun <T> flowOf(vararg elements: T): Flow<T> = flow {
    for (element in elements) {
        emit(element)
    }
}
```

###### 3、asFlow

使用

```kotlin
listOf("hello", "kotlin", "flow").asFlow()
```
还是对flow{}的封装

```kotlin
public fun <T> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        emit(value)
    }
}
```

###### 4、emptyFlow

```kotlin
val emptyFlow = emptyFlow<Int>()
```



# Flow 的操作符

Flow和RxJava一样，用各种操作符撑起了异步数据流框架的半边天，Flow默认为冷流，下游有消费时，才执行生产操作。

因此操作符也被分为两类：中间操作符和末端操作符。中间操作符不会产生消费行为，返回依然为Flow，而末端操作符，会产生消费行为，即触发流的生产。


###### 1、末端操作符

末端操作符都是suspend函数，所以需要运行在协程作用域中。

(1)collect

collect是最常用的末端操作符，用于collect上游产生的数据。相关的api有如下三个：

- collect
- collectIndexed
- collectLatest

```kotlin
val flow = flowOf("hello", "kotlin", "flow")

lifecycleScope.launch {
    flow.collect {
        println("my-test：collected data ->$it")
    }
}
/**
my-test：collected data ->hello
my-test：collected data ->kotlin
my-test：collected data ->flow
 * */

lifecycleScope.launch {
    flow.collectIndexed { index, value ->
        println("my-test：collected data ->$index $value")
    }
}
```

collectLatest 用于collect最新数据，官方文档这样解释

The crucial difference from collect is that when the original flow emits a new value then the action block for the previous value is cancelled.

```kotlin
val flow = flow {
    for (i in 1..3) {
        println("my-test emit $i")
        emit(i)
    }
}
flow
    .collectLatest { value ->
        delay(1)
        println("my-test Collecting $value")
    }
/**
I  my-test emit 1
I  my-test emit 2
I  my-test emit 3
I  my-test Collecting 3
 * */
```

一个常用的场景就是UI数据的更新，当有多个可能导致 UI 更新的事件时，例如网络请求、数据库查询等，使用 collectLatest 可以确保只处理最新的数据，避免因为处理过时的数据而导致 UI 不必要的更新。

(2)转化为集合

- toCollection
- toSet
- toList

很简单的api，这个不用🌰了~

(3)launchIn

在指定的协程作用域中直接执行Flow

```kotlin
 flow.launchIn(lifecycleScope)
```

(4) last&first

获取last&first数据

last、lastOrNull、first、firstOrNull

这里直接看源码算了，不举🌰啦

```kotlin
public suspend fun <T> Flow<T>.last(): T {
    var result: Any? = NULL
    collect {
        result = it
    }
    if (result === NULL) throw NoSuchElementException("Expected at least one element")
    return result as T
}
```

```kotlin
public suspend fun <T> Flow<T>.lastOrNull(): T? {
    var result: T? = null
    collect {
        result = it
    }
    return result
}
```

需要注意一点，当last元素为null，这个方法会抛异常，然而lastOrNull不这样，这也是二者的区别。

first用法与之类似


###### 2、状态操作符

状态操作符不做任何修改，只是在合适的节点返回状态。

(1) onStart 在上游生产数据前调用

(2) onEach 在上游每次emit前调用

(3) onCompletion 在流完成或者取消时调用

(4) catch 对上游中的异常进行捕获

(5) onEmpty 流中未产生任何数据时调用

(6)retry、retryWhen 在发生异常时进行重试，retryWhen中可以拿到异常和当前重试的次数


```kotlin
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
/**
I  my-test: onStart
I  my-test: onEach:1
I  my-test: collect:1
I  my-test: onCompletion
* */
```

很简单onStart首先被调用，onCompletion最后被调用。onEach则是没个元素emit之前被调用。

catch可捕获上游的异常

```kotlin
flow<Int> {
    throw IllegalArgumentException()
}.onCompletion {
    println("my-test: onCompletion:${it}")
}.catch {
    println("my-test: catch:${it}")
}.collect {
    println("my-test: collect:${it}")
}
/**
I  my-test: onCompletion:java.lang.IllegalArgumentException
I  my-test: catch:java.lang.IllegalArgumentException
 * */
```
其实onCompletion也是能捕获异常的，但这里需要注意一点onCompletion要在catch之前调用。

原因是为了保证异常的正确处理。如果catch在onCompletion之前调用，当流中发生异常时，catch会捕获异常并执行相应的处理，然后流程将被视为已完成。这时，onCompletion将不会再执行，因为流程已经结束。

如果onCompletion在catch之前调用，那么即使在流程中发生异常，onCompletion仍然会被执行。这样，你可以确保无论流程是否正常完成或发生异常，都能执行一些清理或收尾工作。


retry lambda 返回值为true代表一直重试，

retryWhen lambda返回值控制重试次数

```kotlin
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

/**

1、emit(1) 后会走onStart，然后collect到数据

I  my-test: onStart
I  my-test: collect:1

2、由于emit(1)后直接抛异常，所以会触发retryWhen回调，注意每次重试条件满足时重新走onStart：

I  my-test: retry:0
I  my-test: onStart
I  my-test: collect:1

I  my-test: retry:1
I  my-test: onStart
I  my-test: collect:1

重试条件不满足，触发异常捕获，跳到catch中
I  my-test: retry:2
I  my-test: catch:java.lang.IllegalArgumentException 
 * */
```

onEmpty的使用就简单了，上游无任何数据emit就会回调这个方法：
```kotlin
emptyFlow<Int>().onEmpty {
    println("my-test:onEmpty")
}.collect()
```

###### 3、Transform操作符

- map
- mapLatest
- mapNotNull
- transform
- transformLatest
- transformWhile

```kotlin
private suspend fun test4() {
    flowOf(1, 2, 3).map {
        it * it
    }.collect{
        println("my-test:collect:$it")
    }
}
```

map用于转化flow的数据，transform与map类似但是又不完全一样，map是一对一的变换，而transform则可以完全控制流的数据，进行过滤、 重组等等操作都可以。
其实map、filter操作都是基于 transform封装的。这里直接简单看下源码算了：

```kotlin
public inline fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R> = transform { value ->
    return@transform emit(transform(value))
}
```

很简单，map参数接受一个高阶函数transform，函数的参数类型为T，返回值类型为R，然后使用transform方法进行转化。

我们使用时直接传递一个lambda即可，然后lambda最后一行的表达式充当返回值即可。接着继续看下transform方法源码：

```kotlin
public inline fun <T, R> Flow<T>.transform(
    @BuilderInference crossinline transform: suspend FlowCollector<R>.(value: T) -> Unit
): Flow<R> = flow { // Note: safe flow is used here, because collector is exposed to transform on each operation
    collect { value ->
        // kludge, without it Unit will be returned and TCE won't kick in, KT-28938
        return@collect transform(value)
    }
}
```
这里注意了transform方法的的参数也是高阶函数，只不过函数是FlowCollector<R>的拓展函数。返回值类型也为Flow<R>

因此我们需要在高阶函数中emit(R)即可。

###### 4、筛选操作符

- filter
- filterInstance
- filterNot
- filterNotNull

```kotlin
flowOf(1, 2, 3).filter {
    it % 2 != 0
}.collect {
    println("my-test:collect:$it")
}
/**
my-test:collect:1
my-test:collect:3
* */
```

- drop(n) 丢弃前n个元素
- dropWhile 根据lambda丢弃定元素(第一个触发条件的drop)
- take(n) 取前n个元素
- takeWhile:根据lambda取特定元素(第一个触发条件的take)

```kotlin
flowOf(1, 2, 3).takeWhile {
    it != 2
}.collect {
    println("my-test:collect:$it")
}
//my-test:collect:1
```

- distinctUntilChangedBy：去重操作符，可以按照指定类型的参数进行去重
- debounce：操作符用于防抖，指定时间内的值只接收最新的一个
- sample：操作符与debounce操作符有点像，但是却限制了一个周期性时间，sample操作符获取的是一个周期内的最新的数据，可以理解为debounce操作符增加了周期的限制


###### 5、组合操作符

- combine
- merge
- zip

```kotlin
val numbers = (1..3).asFlow()
val letters = flowOf("A", "B", "C")
val combined = numbers.combine(letters) { a, b -> "$a$b" }
combined.collect { value -> println(value) }

/**
1A
2A
2B
3B
3C
 * */
```
numbers和letters是两个不同的流。使用combine函数，我们将它们组合成一个新的流combined，并且通过提供的lambda表达式将它们的值合并在一起。在这个例子中，新流combined会在numbers或letters中的任一流发射新值时触发，并使用最新的值调用提供的lambda表达式。

这个貌似不好理解我们可以看这个例子：

```kotlin
val flow1 = flowOf(1, 2)
val flow2 = flowOf("a", "b", "c")
flow1.combine(flow2){ num, str ->
    "$num$str"
}.collect{
    println("my-test:collect:$it")
}

/**
my-test:collect:1a
my-test:collect:2a
my-test:collect:2b
my-test:collect:2c
 * */
```
可以发现，当两个Flow数量不同时，始终由Flow1开始，用其最新的元素，与Flow2的最新的元素进行组合，形成新的元素。


merge操作符用于将多个流合并，类似集合的展平

```kotlin
val flow1 = flowOf(1, 2)
val flow2 = flowOf("a", "b", "c")
listOf(flow1,flow2).merge().collect{
    println("my-test:collect:$it")
}
/***
my-test:collect:1
my-test:collect:2
my-test:collect:a
my-test:collect:b
my-test:collect:c
 * */
```
zip操作符会分别从两个流中取值，当一个流中的数据取完，zip过程就完成了

```kotlin
val flow1 = flowOf(1, 2)
val flow2 = flowOf("a", "b", "c")
flow1.zip(flow2){ num, str ->
    "$num$str"
}.collect{
    println("my-test:collect:$it")
}
/***
my-test:collect:1a
my-test:collect:2b
 * */
```
# Flow 的线程切换

# Flow 的取消

# Flow 的阻塞模型

###### 1、同步阻塞

###### 2、异步阻塞


# The end

[再谈协程之第三者Flow基础档案](https://mp.weixin.qq.com/s/hpLTj8SiirGvw2hsPLL-1g)

[Android flow doc](https://developer.android.google.cn/kotlin/flow?hl=zh_cn)

[kotlin flow doc](https://legacy.kotlincn.net/docs/reference/coroutines/flow.html)




