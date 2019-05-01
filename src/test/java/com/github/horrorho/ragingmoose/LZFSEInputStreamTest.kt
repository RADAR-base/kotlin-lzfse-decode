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
import java.io.*
import java.security.DigestException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.stream.Stream

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
        gen.write(baos)
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
        gen.write(baos)
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

    companion object {
        private val LENGTHS = intArrayOf(
                // bvx-
                7,
                // bvxn
                256, 1023, 2046, 4093,
                // bvx1/ bvx2
                5120, 8191, 16282, 32765, 65532, 131067, 262138, 524281, 1000000)

        private val LZFSE = firstInPath("lzfse", "lzfse.exe").orElse(null)

        @JvmStatic
        fun tcgen(): Stream<EncoderConfig> = Stream.of(
                // Zeros
                EncoderConfig(numberOfWords = 1_000_000, characterSize = 1),
                // Ascending sequence:
//            "m1c1b1d1",
                EncoderConfig(numberOfWords = 1_000_000, characterOffset = 1, characterSize = 1, delay = 1),
                // Descending sequence:
//            "m1c2551b1d1",
                EncoderConfig(numberOfWords = 1_000_000, characterOffset = 255, characterSize = 1, delay = 1),
                // Repeated random string copies:
//            "r10k100",
                EncoderConfig(iterations = 10, numberOfWords = 100_000),
                // Random:
//            "m1v256",
                EncoderConfig(numberOfWords = 1_000_000, vocabularySize = 256),
                // Random 16 character alphabet:
//            "m1v16",
                EncoderConfig(numberOfWords = 1_000_000, vocabularySize = 16),
                // Random bit string from '0' and '1' (text):
//            "m1v2c48b2",
                EncoderConfig(numberOfWords = 1_000_000, vocabularySize = 2, characterOffset = 48, characterSize = 2),
                // Random 16 character alphabet changed every nth:
//            "r10k100v16",
                EncoderConfig(iterations = 10, numberOfWords = 100_000, vocabularySize = 16),
                // 10 copies of numberOfWords size random string:
//            "n100w10000",
                EncoderConfig(numberOfWords = 100, wordSize = 10_000),
                // 10 copies of numberOfWords size random string (text):
//            "n100w10000c48b2",
                EncoderConfig(numberOfWords = 100, wordSize = 10_000, characterOffset = 48, characterSize = 2),
                // 100 copies of numberOfWords size random string:
//            "k1w1000",
                EncoderConfig(numberOfWords = 1_000, wordSize = 1_000),
                // 100 copies of numberOfWords size random string (text):
//            "k1w1000c48b2",
                EncoderConfig(numberOfWords = 1_000, wordSize = 1_000, characterOffset = 48, characterSize = 2),
                // 1000 copies of numberOfWords size random string:
//            "k10w100",
                EncoderConfig(numberOfWords = 10_000, wordSize = 100),
                // 1000 copies of numberOfWords size random string (text):
//            "k10w100c48b2",
                EncoderConfig(numberOfWords = 10_000, wordSize = 100, characterOffset = 48, characterSize = 2),
                // Order 2, 16 word vocabulary:
//            "k500w2v16",
                EncoderConfig(numberOfWords = 500_000, wordSize = 2, vocabularySize = 16),
                // Order 2, 256 word vocabulary:
//            "k500w2v256",
                EncoderConfig(numberOfWords = 500_000, wordSize = 2, vocabularySize = 256),
                // Order 2, 256 word vocabulary changed every nth:
//            "r100k5w2v256",
                EncoderConfig(iterations = 100, numberOfWords = 5_000, wordSize = 2, vocabularySize = 256),
                // Order 3, 256 word vocabulary changed every nth:
//            "r30k5w3v256",
                EncoderConfig(iterations = 30, numberOfWords = 5_000, wordSize = 3, vocabularySize = 256),
                // Order 4, 256 word vocabulary changed every nth:
                EncoderConfig(iterations = 5, numberOfWords = 50_000, wordSize = 4, vocabularySize = 256),
//            "r5k50w4v256",
                // Order 4, 4k word vocabulary:
                EncoderConfig(numberOfWords = 250_000, wordSize = 4, vocabularySize = 4096),
//            "k250v4096w4",
                // Order 8 word vocabulary = {'0', '1'}:
                EncoderConfig(numberOfWords = 100_000, wordSize = 8, vocabularySize = 2, characterOffset = 48, characterSize = 2),
//            "k100v2w8c48b2",
                // Order 8, 16 word vocabulary:
                EncoderConfig(numberOfWords = 100_000, wordSize = 8, vocabularySize = 16),
//            "k100v16w8",
                // Order 8, 256 word vocabulary:
                EncoderConfig(numberOfWords = 100_000, wordSize = 8, vocabularySize = 256),
//            "k100v256w8",
                // Order 8, 4k word vocabulary:
//            "k100v4096w8",
                EncoderConfig(numberOfWords = 100_000, wordSize = 8, vocabularySize = 4096),
                // Order 20, 256 word vocabulary of 20 bit strings (text):
//            "k50w20v256b2c48",
                EncoderConfig(numberOfWords = 50_000, wordSize = 20, vocabularySize = 256, characterSize = 2, characterOffset = 48),
                // Random from alphabet -1, 0, 1
//            "m1c255b3v3",
                EncoderConfig(numberOfWords = 1_000_000, vocabularySize = 3, characterSize = 3, characterOffset = 255),
                // Random from alphabet -1, 0, 1 but delta coded
//            "m1c255b3v3d1",
                EncoderConfig(numberOfWords = 1_000_000, vocabularySize = 3, characterSize = 3, characterOffset = 255, delay = 1),
                // Random from alphabet -1, 0, 1 but delta coded with stride 2
//            "m1c255b3v3d2",
                EncoderConfig(numberOfWords = 1_000_000, vocabularySize = 3, characterSize = 3, characterOffset = 255, delay = 2),
                // Random from alphabet -1, 0, 1 but delta coded with stride 4
//            "m1c255b3v3d4",
                EncoderConfig(numberOfWords = 1_000_000, vocabularySize = 3, characterSize = 3, characterOffset = 255, delay = 4),
                // Random from alphabet -1, 0, 1 but delta coded with stride 1000
//            "m1c255b3v3d1000"
                EncoderConfig(numberOfWords = 1_000_000, vocabularySize = 3, characterSize = 3, characterOffset = 255, delay = 1000))
    }
}
