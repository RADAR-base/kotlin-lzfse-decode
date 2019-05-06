package org.radarbase.io.lzfse

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun main() {
    repeat(100) {
        val start = System.nanoTime()

        Files.newByteChannel(Paths.get("radar-questionnaire.tar.lzfse")).use { channel ->
            LZFSEInputStream(channel).use {
                it.readAllBytes().toString(Charsets.UTF_8)
            }
        }

        val diff = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) / 1000.0
        println("Time taken: $diff s")
    }
}