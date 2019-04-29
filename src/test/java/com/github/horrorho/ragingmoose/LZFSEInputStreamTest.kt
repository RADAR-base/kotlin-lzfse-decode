/*
 * The MIT License
 *
 * Copyright 2017 Ayesha.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.ragingmoose

import com.github.horrorho.ragingmoose.ProcessAssistant.firstInPath
import com.github.horrorho.ragingmoose.ProcessAssistant.newPipedInputStream
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.DigestException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 *
 * @author Ayesha
 */
class LZFSEInputStreamTest {
    /**
     * Test using resource data.
     *
     *
     * Format: SHA-256 digest | LZFSE encoded data
     *
     *
     * lzfse.test: contains bvx-, bvx1, bvx2, bvxn encrypted random words (text)
     *
     * @throws IOException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.DigestException
     */
    @Test
    @Throws(IOException::class, NoSuchAlgorithmException::class, DigestException::class)
    fun defaultTest() {
        val `is` = this.javaClass.classLoader.getResourceAsStream("lzfse.test")
        assertNotNull(`is`, "lzfse.test")

        `is`?.let {
            val digest = ByteArray(32)
            `is`.read(digest)

            val baos = ByteArrayOutputStream()
            LZFSEInputStream(`is`).use { it.copyTo(baos) }

            val md = MessageDigest.getInstance("SHA-256")
            val _digest = md.digest(baos.toByteArray())

            assertArrayEquals(digest, _digest, "SHA-256")
        }
    }

    /**
     * Tcgen data into LZFSE external compressor into RagingMoose decompressor.
     *
     * @param arg
     * @throws IOException
     */
    @ParameterizedTest
    @MethodSource("tcgen")
    @Throws(IOException::class)
    fun tcgenTest(encoderConfig: EncoderConfig) {
        assumeTrue(LZFSE != null, "lzfse")

        val encode = ProcessBuilder(LZFSE, "-encode")

        // Generate compression test data.
        val baos = ByteArrayOutputStream()
        val gen = EncoderGenerator(encoderConfig)
        gen.generate(baos)
        val bs = baos.toByteArray()

        // Test data into LZFSE external compressor into RagingMoose decompressor.
        for (len in LENGTHS) {
            val head = bs.copyOf(len)

            baos.reset()
            LZFSEInputStream(newPipedInputStream(encode, ByteArrayInputStream(head))).use { it.copyTo(baos) }
            val _head = baos.toByteArray()

            assertArrayEquals(head, _head, "$encoderConfig:$len")
        }
    }

    /**
     * Tcgen data into LZFSE external compressor into LZFSE external decompressor
     *
     * @param arg
     * @throws IOException
     */
    @ParameterizedTest
    @MethodSource("tcgen")
    @Throws(IOException::class)
    fun tcgenTestExt(encoderConfig: EncoderConfig) {
        assumeTrue(LZFSE != null, "lzfse")

        val encode = ProcessBuilder(LZFSE, "-encode")
        val decode = ProcessBuilder(LZFSE, "-decode")

        // Generate compression test data.
        val baos = ByteArrayOutputStream()
        val gen = EncoderGenerator(encoderConfig)
        gen.generate(baos)
        val bs = baos.toByteArray()

        // Test data into LZFSE external compressor into RagingMoose decompressor.
        for (len in LENGTHS) {
            val head = bs.copyOf(len)

            baos.reset()
            val encodeStream = newPipedInputStream(encode, ByteArrayInputStream(head))
            newPipedInputStream(decode, encodeStream).use { it.copyTo(baos) }
            val _head = baos.toByteArray()

            assertArrayEquals(head, _head, "$encoderConfig:$len")
        }
    }

    // Based on 'Compression test file generator' with tcgen
    // Matt Mahoney
    // https://encode.ru/threads/1306-Compression-test-file-generator
    // http://mattmahoney.net/
    internal fun tcgen(): Array<EncoderConfig> = arrayOf(
            // Zeros
            EncoderConfig(n = 1_000_000, b = 1),
            // Ascending sequence:
//            "m1c1b1d1",
            EncoderConfig(n = 1_000_000, c = 1, b = 1, d = 1),
            // Descending sequence:
//            "m1c2551b1d1",
            EncoderConfig(n = 1_000_000, c = 255, b = 1, d = 1),
            // Repeated random string copies:
//            "r10k100",
            EncoderConfig(r = 10, n = 100_000),
            // Random:
//            "m1v256",
            EncoderConfig(n = 1_000_000, v = 256),
            // Random 16 character alphabet:
//            "m1v16",
            EncoderConfig(n = 1_000_000, v = 16),
            // Random bit string from '0' and '1' (text):
//            "m1v2c48b2",
            EncoderConfig(n = 1_000_000, v = 2, c = 48, b = 2),
            // Random 16 character alphabet changed every nth:
//            "r10k100v16",
            EncoderConfig(r = 10, n = 100_000, v = 16),
            // 10 copies of n size random string:
//            "n100w10000",
            EncoderConfig(n = 100, w = 10_000),
            // 10 copies of n size random string (text):
//            "n100w10000c48b2",
            EncoderConfig(n = 100, w = 10_000, c = 48, b = 2),
            // 100 copies of n size random string:
//            "k1w1000",
            EncoderConfig(n = 1_000, w = 1_000),
            // 100 copies of n size random string (text):
//            "k1w1000c48b2",
            EncoderConfig(n = 1_000, w = 1_000, c = 48, b = 2),
            // 1000 copies of n size random string:
//            "k10w100",
            EncoderConfig(n = 10_000, w = 100),
            // 1000 copies of n size random string (text):
//            "k10w100c48b2",
            EncoderConfig(n = 10_000, w = 100, c = 48, b = 2),
            // Order 2, 16 word vocabulary:
//            "k500w2v16",
            EncoderConfig(n = 500_000, w = 2, v = 16),
            // Order 2, 256 word vocabulary:
//            "k500w2v256",
            EncoderConfig(n = 500_000, w = 2, v = 256),
            // Order 2, 256 word vocabulary changed every nth:
//            "r100k5w2v256",
            EncoderConfig(r = 100, n = 5_000, w = 2, v = 256),
            // Order 3, 256 word vocabulary changed every nth:
//            "r30k5w3v256",
            EncoderConfig(r = 30, n = 5_000, w = 3, v = 256),
            // Order 4, 256 word vocabulary changed every nth:
            EncoderConfig(r = 5, n = 50_000, w = 4, v = 256),
//            "r5k50w4v256",
            // Order 4, 4k word vocabulary:
            EncoderConfig(n = 250_000, w = 4, v = 4096),
//            "k250v4096w4",
            // Order 8 word vocabulary = {'0', '1'}:
            EncoderConfig(n = 100_000, w = 8, v = 2, c = 48, b = 2),
//            "k100v2w8c48b2",
            // Order 8, 16 word vocabulary:
            EncoderConfig(n = 100_000, w = 8, v = 16),
//            "k100v16w8",
            // Order 8, 256 word vocabulary:
            EncoderConfig(n = 100_000, w = 8, v = 256),
//            "k100v256w8",
            // Order 8, 4k word vocabulary:
//            "k100v4096w8",
            EncoderConfig(n = 100_000, w = 8, v = 4096),
            // Order 20, 256 word vocabulary of 20 bit strings (text):
//            "k50w20v256b2c48",
            EncoderConfig(n = 50_000, w = 20, v = 256, b = 2, c = 48),
            // Random from alphabet -1, 0, 1
//            "m1c255b3v3",
            EncoderConfig(n = 1_000_000, v = 3, b = 3, c = 255),
            // Random from alphabet -1, 0, 1 but delta coded
//            "m1c255b3v3d1",
            EncoderConfig(n = 1_000_000, v = 3, b = 3, c = 255, d = 1),
            // Random from alphabet -1, 0, 1 but delta coded with stride 2
//            "m1c255b3v3d2",
            EncoderConfig(n = 1_000_000, v = 3, b = 3, c = 255, d = 2),
            // Random from alphabet -1, 0, 1 but delta coded with stride 4
//            "m1c255b3v3d4",
            EncoderConfig(n = 1_000_000, v = 3, b = 3, c = 255, d = 4),
            // Random from alphabet -1, 0, 1 but delta coded with stride 1000
//            "m1c255b3v3d1000"
            EncoderConfig(n = 1_000_000, v = 3, b = 3, c = 255, d = 1000))

    companion object {
        private val LENGTHS = intArrayOf(
                // bvx-
                7,
                // bvxn
                256, 1023, 2046, 4093,
                // bvx1/ bvx2
                5120, 8191, 16282, 32765, 65532, 131067, 262138, 524281, 1000000)

        private val LZFSE = firstInPath("lzfse", "lzfse.exe").orElse(null)
    }
}
