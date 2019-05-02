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

import java.lang.Long.toHexString
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import javax.annotation.concurrent.NotThreadSafe


/**
 * Low level bit bb stream.
 *
 * @author Ayesha
 */
@NotThreadSafe
internal class BitInStream {
    // bitCacheSize 63 bit limit avoids unsupported 64 bit shifts/ branch.
    private var bitCache: Long = 0
    private var bitCacheSize: Int = 0
    private lateinit var bb: ByteBuffer

    fun init(bb: ByteBuffer, bits: Int) {
        this.bb = bb
        when {
            bits > 0 -> throw LZFSEDecoderException()
            bits == 0 -> {
                bb.position(bb.position() - 7)
                bitCache = bb.getLong(bb.position() - 1) ushr 8
                bitCacheSize = 56
            }
            else -> {
                bb.position(bb.position() - 8)
                bitCache = bb.getLong(bb.position())
                bitCacheSize = bits + 64
            }
        }
    }

    fun fill() {
        if (bitCacheSize < 56) {
            val nBits = MAXIMUM_CACHE_SIZE - bitCacheSize
            val nBytes = nBits.ushr(3)

            val pos = bb.position() - nBytes
            bb.position(pos)
            bitCache = bb.getLong(pos)
            bitCacheSize += nBytes shl 3
        }
    }

    fun read(n: Int): Int {
        assert(n >= 0)
        if (bitCacheSize < n) {
            throw BufferUnderflowException()
        }
        bitCacheSize -= n
        return (bitCache shr bitCacheSize).toInt() and MASKS[n]
    }

    override fun toString(): String {
        return "BitStream{bb=$bb, bitCache=0x${toHexString(bitCache)}, bitCacheSize=$bitCacheSize}"
    }

    companion object {
        private const val MAXIMUM_CACHE_SIZE = 63 // bits bb long minus sign bit
        private val MASKS = IntArray(32)

        init {
            for (i in 1 until MASKS.size) {
                MASKS[i] = (MASKS[i - 1] shl 1) + 1
            }
        }
    }
}
