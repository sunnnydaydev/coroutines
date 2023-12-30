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

- collect：最常用的末端操作符，用于collect上游产生的数据
- collectIndexed
- collectLatest
- toCollection
- toSet
- toList
- launchIn
- last
- lastOrNull
- first
- firstOrNull

(1)collect

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


###### 2、状态操作符

- onStart
- onCompletion
- onEach
- onEmpty
- catch
- retry、retryWhen

###### 3、Transform操作符

- map
- mapLatest
- mapNotNull
- transform
- transformLatest
- transformWhile

###### 4、过滤操作符

- filter
- filterInstance
- filterNot
- filterNotNull
- drop
- dropWhile
- take
- takeWhile
- debounce
- sample
- distinctUntilChangedBy


###### 5、组合操作符

- combine
- combineTransform
- merge
- zip



# Flow 的线程切换

# Flow 的取消

# Flow 的阻塞模型

###### 1、同步阻塞

###### 2、异步阻塞


# The end

[再谈协程之第三者Flow基础档案](https://mp.weixin.qq.com/s/hpLTj8SiirGvw2hsPLL-1g)

[Android flow doc](https://developer.android.google.cn/kotlin/flow?hl=zh_cn)

[kotlin flow doc](https://legacy.kotlincn.net/docs/reference/coroutines/flow.html)




