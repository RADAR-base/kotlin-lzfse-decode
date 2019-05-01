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

import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.channels.ReadableByteChannel
import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.WillNotClose
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
internal class LZFSEBlockHeader {
    private val bb = BufferUtil.withCapacity(V1_SIZE)

    internal val literalFreq = ShortArray(LZFSEConstants.ENCODE_LITERAL_SYMBOLS)
    internal val lFreq = ShortArray(LZFSEConstants.ENCODE_L_SYMBOLS)
    internal val mFreq = ShortArray(LZFSEConstants.ENCODE_M_SYMBOLS)
    internal val dFreq = ShortArray(LZFSEConstants.ENCODE_D_SYMBOLS)

    internal var nRawBytes: Int = 0
        @Throws(LZFSEDecoderException::class)
        private set(value) {
            if (value < 0) {
                throw LZFSEDecoderException()
            }
            field = value
        }

    private var nPayloadBytes: Int = 0
        @Throws(LZFSEDecoderException::class)
        set(value) {
            if (value < 0) {
                throw LZFSEDecoderException()
            }
            field = value
        }

    internal var nLiterals: Int = 0
        @Throws(LZFSEDecoderException::class)
        private set(value) {
            if (value < 0) {
                throw LZFSEDecoderException()
            }
            field = value
        }

    internal var nMatches: Int = 0
        @Throws(LZFSEDecoderException::class)
        private set(value) {
            if (value < 0) {
                throw LZFSEDecoderException()
            }
            field = value
        }

    internal var nLiteralPayloadBytes: Int = 0
        @Throws(LZFSEDecoderException::class)
        private set(value) {
            if (value < 0) {
                throw LZFSEDecoderException()
            }
            field = value
        }

    internal var nLmdPayloadBytes: Int = 0
        @Throws(LZFSEDecoderException::class)
        private set(value) {
            if (value < 0) {
                throw LZFSEDecoderException()
            }
            field = value
        }

    internal var literalBits: Int = 0
    internal var lmdBits: Int = 0

    internal var lState: Int = 0
    internal var mState: Int = 0
    internal var dState: Int = 0
    internal var literalState = intArrayOf(0, 0, 0, 0)

    @Throws(IOException::class, LZFSEDecoderException::class)
    fun loadV1(@WillNotClose ch: ReadableByteChannel): LZFSEBlockHeader {
        bb.rewind().limit(V1_SIZE)
        ch.readFully(bb).flip()

        nRawBytes = bb.int
        nPayloadBytes = bb.int
        nLiterals = bb.int
        nMatches = bb.int
        nLiteralPayloadBytes = bb.int
        nLmdPayloadBytes = bb.int

        literalBits = bb.int
        literalState[0] = bb.short.toInt()
        literalState[1] = bb.short.toInt()
        literalState[2] = bb.short.toInt()
        literalState[3] = bb.short.toInt()

        lmdBits = bb.int
        lState = bb.short.toInt()
        mState = bb.short.toInt()
        dState = bb.short.toInt()

        initV1Tables(bb, lFreq, mFreq, dFreq, literalFreq)

        return this
    }

    @Throws(IOException::class, LZFSEDecoderException::class)
    fun loadV2(@WillNotClose `in`: ReadableByteChannel): LZFSEBlockHeader {
        bb.rewind().limit(V2_SIZE)
        `in`.readFully(bb).flip()

        nRawBytes = bb.int

        val v0 = bb.long
        val v1 = bb.long
        val v2 = bb.long

        nLiterals = n(v0, 0, 20)
        nLiteralPayloadBytes = n(v0, 20, 20)
        literalBits = n(v0, 60, 3) - 7
        literalState[0] = n(v1, 0, 10)
        literalState[1] = n(v1, 10, 10)
        literalState[2] = n(v1, 20, 10)
        literalState[3] = n(v1, 30, 10)

        nMatches = n(v0, 40, 20)
        nLmdPayloadBytes = n(v1, 40, 20)
        lmdBits = n(v1, 60, 3) - 7
        lState = n(v2, 32, 10)
        mState = n(v2, 42, 10)
        dState = n(v2, 52, 10)

        nPayloadBytes = nLiteralPayloadBytes + nLmdPayloadBytes

        val headerSize = n(v2, 0, 32)
        val nCompressedPayload = headerSize - V2_SIZE - 4

        when {
            nCompressedPayload == 0 -> clear(lFreq, mFreq, dFreq, literalFreq)
            nCompressedPayload > bb.capacity() -> throw LZFSEDecoderException()
            else -> {
                bb.rewind().limit(nCompressedPayload)
                `in`.readFully(bb).flip()

                initV2Tables(bb, lFreq, mFreq, dFreq, literalFreq)
            }
        }
        return this
    }

    override fun toString(): String {
        return ("LZFSEBlockHeader{literalFreq=${literalFreq.size}, lFreq=${lFreq.size}, mFreq=${mFreq.size}, dFreq=${dFreq.size}, nRawBytes=$nRawBytes, nPayloadBytes=$nPayloadBytes, nLiterals=$nLiterals, nMatches=$nMatches, nLiteralPayloadBytes=$nLiteralPayloadBytes, nLmdPayloadBytes=$nLmdPayloadBytes, literalBits=$literalBits, lmdBits=$lmdBits, lState=$lState, mState=$mState, dState=$dState, literalState=$literalState")
    }

    companion object {

        @Throws(LZFSEDecoderException::class)
        fun initV1Tables(bb: ByteBuffer, vararg tables: ShortArray) {
            tables.forEach { table ->
                table.indices.forEach { i ->
                    table[i] = bb.short
                }
            }
        }

        @Throws(LZFSEDecoderException::class)
        fun initV2Tables(bb: ByteBuffer, vararg tables: ShortArray) {
            var accum = 0
            var accumNBits = 0
            tables.forEach { table ->
                table.replace {
                    while (bb.hasRemaining() && accumNBits + 8 <= 32) {
                        accum = accum or (bb.get().toInt() and 0xFF shl accumNBits)
                        accumNBits += 8
                    }

                    val nbits = FREQ_NBITS_TABLE[accum and 0x1F]
                    if (nbits > accumNBits) {
                        throw LZFSEDecoderException()
                    }

                    value(accum, nbits)
                            .also {
                                accum = accum ushr nbits
                                accumNBits -= nbits
                            }
                }
            }
            if (accumNBits >= 8 || bb.hasRemaining()) {
                throw LZFSEDecoderException()
            }
        }

        private val FREQ_NBITS_TABLE = intArrayOf(2, 3, 2, 5, 2, 3, 2, 8, 2, 3, 2, 5, 2, 3, 2, 14, 2, 3, 2, 5, 2, 3, 2, 8, 2, 3, 2, 5, 2, 3, 2, 14)

        private fun value(bits: Int, nBits: Int): Short {
            return when (nBits) {
                8 -> (8 + (bits.ushr(4) and 0x000F)).toShort()
                14 -> (24 + (bits.ushr(4) and 0x03FF)).toShort()
                else -> FREQ_VALUE_TABLE[bits and 0x1F]
            }
        }

        private val FREQ_VALUE_TABLE = shortArrayOf(0, 2, 1, 4, 0, 3, 1, -1, 0, 2, 1, 5, 0, 3, 1, -1, 0, 2, 1, 6, 0, 3, 1, -1, 0, 2, 1, 7, 0, 3, 1, -1)

        private fun clear(vararg tables: ShortArray) {
            tables.forEach { it.fill(0) }
        }

        private fun n(v: Long, offset: Int, nBits: Int): Int {
            return (v.ushr(offset) and (1L shl nBits) - 1L).toInt()
        }

        private const val V1_SIZE = 48 + LZFSEConstants.ENCODE_SYMBOLS * 2

        private const val V2_SIZE = 28
    }
}

inline fun IntArray.mapInPlace(transform: (Int) -> Int) {
    forEachIndexed { i, v ->
        this[i] = transform(v)
    }
}

inline fun ShortArray.replace(transform: () -> Short) {
    indices.forEach { i ->
        this[i] = transform()
    }
}
