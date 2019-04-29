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
@ParametersAreNonnullByDefault
internal class LZFSEBlockHeader {

    private val bb = ByteBuffer.allocate(V1_SIZE).order(LITTLE_ENDIAN)

    private val literalFreq = ShortArray(LZFSEConstants.ENCODE_LITERAL_SYMBOLS)
    private val lFreq = ShortArray(LZFSEConstants.ENCODE_L_SYMBOLS)
    private val mFreq = ShortArray(LZFSEConstants.ENCODE_M_SYMBOLS)
    private val dFreq = ShortArray(LZFSEConstants.ENCODE_D_SYMBOLS)

    private var nRawBytes: Int = 0
    private var nPayloadBytes: Int = 0
    private var nLiterals: Int = 0
    private var nMatches: Int = 0
    private var nLiteralPayloadBytes: Int = 0
    private var nLmdPayloadBytes: Int = 0

    private var literalBits: Int = 0
    private var lmdBits: Int = 0

    private var lState: Int = 0
    private var mState: Int = 0
    private var dState: Int = 0
    private var literalState0: Int = 0
    private var literalState1: Int = 0
    private var literalState2: Int = 0
    private var literalState3: Int = 0

    @Throws(IOException::class, LZFSEDecoderException::class)
    fun loadV1(@WillNotClose ch: ReadableByteChannel): LZFSEBlockHeader {
        bb.rewind().limit(V1_SIZE)
        IO.readFully(ch, bb).flip()

        nRawBytes(bb.int)
        nPayloadBytes(bb.int)
        nLiterals(bb.int)
        nMatches(bb.int)
        nLiteralPayloadBytes(bb.int)
        nLmdPayloadBytes(bb.int)

        literalBits = bb.int
        literalState0 = bb.short.toInt()
        literalState1 = bb.short.toInt()
        literalState2 = bb.short.toInt()
        literalState3 = bb.short.toInt()

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
        IO.readFully(`in`, bb).flip()

        nRawBytes(bb.int)

        val v0 = bb.long
        val v1 = bb.long
        val v2 = bb.long

        val headerSize = n(v2, 0, 32)
        val nCompressedPayload = headerSize - V2_SIZE - 4

        nLiterals(n(v0, 0, 20))
        nLiteralPayloadBytes(n(v0, 20, 20))
        literalBits = n(v0, 60, 3) - 7
        literalState0 = n(v1, 0, 10)
        literalState1 = n(v1, 10, 10)
        literalState2 = n(v1, 20, 10)
        literalState3 = n(v1, 30, 10)

        nMatches(n(v0, 40, 20))
        nLmdPayloadBytes(n(v1, 40, 20))
        lmdBits = n(v1, 60, 3) - 7
        lState = n(v2, 32, 10)
        mState = n(v2, 42, 10)
        dState = n(v2, 52, 10)

        nPayloadBytes(nLiteralPayloadBytes + nLmdPayloadBytes)

        if (nCompressedPayload == 0) {
            clear(lFreq, mFreq, dFreq, literalFreq)

        } else if (nCompressedPayload > bb.capacity()) {
            throw LZFSEDecoderException()

        } else {
            bb.rewind().limit(nCompressedPayload)
            IO.readFully(`in`, bb).flip()

            initV2Tables(bb, lFreq, mFreq, dFreq, literalFreq)
        }
        return this
    }

    fun literalFreq(): ShortArray {
        return literalFreq
    }

    fun lFreq(): ShortArray {
        return lFreq
    }

    fun mFreq(): ShortArray {
        return mFreq
    }

    fun dFreq(): ShortArray {
        return dFreq
    }

    fun nRawBytes(): Int {
        return nRawBytes
    }

    @Throws(LZFSEDecoderException::class)
    private fun nRawBytes(v: Int) {
        if (v < 0) {
            throw LZFSEDecoderException()
        }
        this.nRawBytes = v
    }

    fun nPayloadBytes(): Int {
        return nPayloadBytes
    }

    @Throws(LZFSEDecoderException::class)
    private fun nPayloadBytes(v: Int) {
        if (v < 0) {
            throw LZFSEDecoderException()
        }
        this.nPayloadBytes = v
    }

    fun nLiterals(): Int {
        return nLiterals
    }

    @Throws(LZFSEDecoderException::class)
    private fun nLiterals(v: Int) {
        if (v < 0) {
            throw LZFSEDecoderException()
        }
        this.nLiterals = v
    }

    fun nMatches(): Int {
        return nMatches
    }

    @Throws(LZFSEDecoderException::class)
    private fun nMatches(v: Int) {
        if (v < 0) {
            throw LZFSEDecoderException()
        }
        this.nMatches = v
    }

    fun nLiteralPayloadBytes(): Int {
        return nLiteralPayloadBytes
    }

    @Throws(LZFSEDecoderException::class)
    private fun nLiteralPayloadBytes(v: Int) {
        if (v < 0) {
            throw LZFSEDecoderException()
        }
        this.nLiteralPayloadBytes = v
    }

    fun nLmdPayloadBytes(): Int {
        return nLmdPayloadBytes
    }

    @Throws(LZFSEDecoderException::class)
    private fun nLmdPayloadBytes(v: Int) {
        if (v < 0) {
            throw LZFSEDecoderException()
        }
        this.nLmdPayloadBytes = v
    }

    fun literalBits(): Int {
        return literalBits
    }

    fun lmdBits(): Int {
        return lmdBits
    }

    fun lState(): Int {
        return lState
    }

    fun mState(): Int {
        return mState
    }

    fun dState(): Int {
        return dState
    }

    fun literalState0(): Int {
        return literalState0
    }

    fun literalState1(): Int {
        return literalState1
    }

    fun literalState2(): Int {
        return literalState2
    }

    fun literalState3(): Int {
        return literalState3
    }

    override fun toString(): String {
        return ("LZFSEBlockHeader{"
                + "literalFreq=" + literalFreq.size
                + ", lFreq=" + lFreq.size
                + ", mFreq=" + mFreq.size
                + ", dFreq=" + dFreq.size
                + ", nRawBytes=" + nRawBytes
                + ", nPayloadBytes=" + nPayloadBytes
                + ", nLiterals=" + nLiterals
                + ", nMatches=" + nMatches
                + ", nLiteralPayloadBytes=" + nLiteralPayloadBytes
                + ", nLmdPayloadBytes=" + nLmdPayloadBytes
                + ", literalBits=" + literalBits
                + ", lmdBits=" + lmdBits
                + ", lState=" + lState
                + ", mState=" + mState
                + ", dState=" + dState
                + ", literalState0=" + literalState0
                + ", literalState1=" + literalState1
                + ", literalState2=" + literalState2
                + ", literalState3=" + literalState3
                + '}'.toString())
    }

    companion object {

        @Throws(LZFSEDecoderException::class)
        fun initV1Tables(bb: ByteBuffer, vararg tables: ShortArray) {
            try {
                var i = 0
                var j = 0
                var k = 0
                val n = count(*tables)
                while (i < n) {
                    tables[j][k] = bb.short
                    if (++k == tables[j].size) {
                        j++
                        k = 0
                    }
                    i++
                }
            } catch (ex: BufferUnderflowException) {
                throw LZFSEDecoderException(ex)
            }

        }

        @Throws(LZFSEDecoderException::class)
        fun initV2Tables(bb: ByteBuffer, vararg tables: ShortArray) {
            var accum = 0
            var accumNBits = 0
            var i = 0
            var j = 0
            var k = 0
            val n = count(*tables)
            while (i < n) {
                while (bb.hasRemaining() && accumNBits + 8 <= 32) {
                    accum = accum or (bb.get().toInt() and 0xFF shl accumNBits)
                    accumNBits += 8
                }

                val nbits = nBits(accum)
                if (nbits > accumNBits) {
                    throw LZFSEDecoderException()
                }

                tables[j][k] = value(accum, nbits).toShort()
                if (++k == tables[j].size) {
                    j++
                    k = 0
                }

                accum = accum ushr nbits
                accumNBits -= nbits
                i++
            }
            if (accumNBits >= 8 || bb.hasRemaining()) {
                throw LZFSEDecoderException()
            }
        }

        fun nBits(bits: Int): Int {
            return FREQ_NBITS_TABLE[bits and 0x1F].toInt()
        }

        private val FREQ_NBITS_TABLE = byteArrayOf(2, 3, 2, 5, 2, 3, 2, 8, 2, 3, 2, 5, 2, 3, 2, 14, 2, 3, 2, 5, 2, 3, 2, 8, 2, 3, 2, 5, 2, 3, 2, 14)

        fun value(bits: Int, nBits: Int): Int {
            if (nBits == 8) {
                return 8 + (bits.ushr(4) and 0x000F)
            }
            return if (nBits == 14) {
                24 + (bits.ushr(4) and 0x03FF)
            } else FREQ_VALUE_TABLE[bits and 0x1F].toInt()
        }

        private val FREQ_VALUE_TABLE = byteArrayOf(0, 2, 1, 4, 0, 3, 1, -1, 0, 2, 1, 5, 0, 3, 1, -1, 0, 2, 1, 6, 0, 3, 1, -1, 0, 2, 1, 7, 0, 3, 1, -1)

        fun count(vararg tables: ShortArray): Int {
            var n = 0
            for (table in tables) {
                n += table.size
            }
            return n
        }

        fun clear(vararg tables: ShortArray) {
            for (table in tables) {
                clear(table)
            }
        }

        fun clear(table: ShortArray) {
            for (i in table.indices) {
                table[i] = 0
            }
        }

        fun n(v: Long, offset: Int, nBits: Int): Int {
            return (v.ushr(offset) and (1L shl nBits) - 1L).toInt()
        }

        private const val V1_SIZE = 48 + LZFSEConstants.ENCODE_SYMBOLS * 2

        private const val V2_SIZE = 28
    }
}
