package com.github.horrorho.ragingmoose

import java.io.OutputStream
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

class EncoderGenerator(val cfg: EncoderConfig) {
    // V be in 1..B^W, W > 0,
    // B in 1..256, and C in 0..255.

    fun generate(output: OutputStream) {
        val random = Random.Default

        for (i in 0 until cfg.r) {
            val vocab = generateVocab(random)
            generateOutput(vocab, output, random)
        }
    }

    private fun generateVocab(random: Random): List<ByteArray> {
        //        for i = 0..V-1 (initialize vocabulary)
        //      do
        //        for j = 0..W-1
        //          dict[i][j] = random(B) + C (mod 256)
        //      while dict[i] = dict[j] for some j < i

        val vocab = ArrayList<ByteArray>(cfg.v)

        for (i in 0 until cfg.v) {
            do {
                val arr = ByteArray(cfg.w)
                vocab[i] = arr
                for (j in 0 until cfg.w) {
                    arr[j] = random.nextInt(0, cfg.b).toByte()
                }
            } while (i > 0 && vocab[i] in vocab.subList(0, i))
        }

        return vocab
    }

    private fun generateOutput(vocab: List<ByteArray>, output: OutputStream, random: Random) {
        var k = 0
        val buf = ByteArray(cfg.d)

        for (i in 0 until cfg.n) {
            val s = vocab[random.nextInt(0, cfg.v)]
            if (cfg.d > 0) {
                for (j in 0 until cfg.w) {
                    buf[k] = ((buf[k] + s[j]).rem(256)).toByte()
                    k = (k + 1).rem(cfg.d)
                    output.write(buf[k].toInt())
                }
            } else {
                output.write(s)
            }
        }
    }
}

data class EncoderConfig(
        val r: Int = 1,
        val n: Int = 1,
        val w: Int = 1,
        val b: Int = 256,
        val c: Int = 0,
        val v: Int = 1,
        val d: Int = 0)
