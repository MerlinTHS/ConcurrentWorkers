package de.ths.concurrent

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlin.random.Random

fun main() = runBlocking {
    launchOrQuit(
        onException = { println("Caught exception: ${it.message}") }
    ) {
        produceAndCollect()
    }

    println("Happy End!")
}

suspend fun CoroutineScope.produceAndCollect() {
    val producer = produceNumbers()

    repeat(3) { id ->
        processNumber(id, producer)
    }

    delay(2000)
    producer.cancel()
}

fun CoroutineScope.produceNumbers() = produce {
    while (isActive) {
        delay(200)
        send(Random.nextInt(0, 5))
    }
}

fun CoroutineScope.processNumber(
    id: Int,
    channel: ReceiveChannel<Int>
) = launch {
    try {
        for (number in channel) {
            if (number == 2) {
                throw IllegalStateException("I don't want a two!")
            }

            println("+ Worker [$id] is processing $number")
        }
    } finally {
        println("- Worker [$id] cleans everything up!")
    }
}

suspend fun launchOrQuit(
    onException: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
): Job = supervisorScope {
    val handler = CoroutineExceptionHandler { _, throwable ->
        onException(throwable)
    }

    launch(handler, block = block)
}