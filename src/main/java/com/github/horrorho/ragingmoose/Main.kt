package com.github.horrorho.ragingmoose

import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    Files.newInputStream(Paths.get("random.test.lzfse")).use { `in` ->
        val lzfseIn = LZFSEInputStream(`in`)
        Files.newOutputStream(Paths.get("random.test")).use {
            lzfseIn.copyTo(it)
        }
    }
}
