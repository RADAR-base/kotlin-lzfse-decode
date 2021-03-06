/*
 * Copyright 2019 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.radarbase.io.lzfse

import java.io.OutputStream
import kotlin.random.Random

class EncoderGenerator(val cfg: EncoderConfig) {
    // V be in 1..B^W, W > 0,
    // B in 1..256, and C in 0..255.

    fun write(output: OutputStream) {
        generate().forEach { output.write(it) }
    }

    fun generate(): Sequence<ByteArray> {
        val random = Random.Default

        return generateSequence {
            val vocab = generateVocab(random)

            generateWords(vocab, random).take(cfg.numberOfWords)
        }.take(cfg.iterations).flatMap { it }
    }

    private fun generateWord(random: Random) = ByteArray(cfg.wordSize).apply {
        indices.forEach { i -> this[i] = (random.nextInt(0, cfg.characterSize) + cfg.characterOffset).toByte() }
    }

    private fun generateVocab(random: Random) = generateSequence { generateWord(random) }
            .distinct()
            .take(cfg.vocabularySize)
            .toList()

    private fun generateWords(vocab: List<ByteArray>, random: Random): Sequence<ByteArray> {
        var k = 0
        val buf = ByteArray(cfg.delay)

        return generateSequence {
            val s = vocab[random.nextInt(0, cfg.vocabularySize)]
            if (cfg.delay > 0) {
                s.map { v ->
                    buf[k] = (buf[k] + v).toByte()
                    k = (k + 1).rem(cfg.delay)
                    buf[k]
                }.toByteArray()
            } else {
                s
            }
        }
    }
}

data class EncoderConfig(
        val iterations: Int = 1,
        val numberOfWords: Int = 1,
        val wordSize: Int = 1,
        val characterSize: Int = 256,
        val characterOffset: Int = 0,
        val vocabularySize: Int = 1,
        val delay: Int = 0) {
    init {
        if (iterations < 1) throw IllegalArgumentException("Need to repeat iterations")
        if (wordSize < 1) throw IllegalArgumentException("Need word size >= 1")
        if (characterSize > 256 || characterSize < 1) throw IllegalArgumentException("Character size should be 1 <= characterSize <= 256")
        if (characterOffset > 255 || characterOffset < 0) throw IllegalArgumentException("Character offset should be 0 <= characterSize <= 255")
    }
}
