package com.github.horrorho.ragingmoose

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun main(vararg args: String) {
    val repetitions = if (args.isEmpty()) 1 else args[0].toInt()
    repeat(repetitions) {
        val start = System.nanoTime()
        val buffer = ByteArray(65536)
        Files.newByteChannel(Paths.get("radar-questionnaire.tar.lzfse")).use { `in` ->
            LZFSEInputStream(`in`).use {
                do {
                    val nRead = it.read(buffer, 0, buffer.size)
                } while (nRead != -1)
            }
        }
        val diff = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) / 1000.0
        println("Time taken: $diff s")
    }
}
