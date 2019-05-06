/*
 * Copyright 2019 The Hyve and Ayesha
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

import java.lang.Long.toHexString
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer


/**
 * Low level bit bb stream.
 *
 * @author Ayesha
 */
internal class BitInStream {
    // bitCacheSize 63 bit limit avoids unsupported 64 bit shifts/ branch.
    private var bitCache: Long = 0
    private var bitCacheSize: Int = 0
    private lateinit var bb: ByteBuffer

    fun init(bb: ByteBuffer, bits: Int) {
        this.bb = bb
        when {
            bits > 0 -> throw LZFSEException()
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
