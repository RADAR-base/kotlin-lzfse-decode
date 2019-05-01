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
internal class LZFSEBlockDecoder @Throws(LZFSEDecoderException::class)
constructor(mb: MatchBuffer) : LMDBlockDecoder(mb) {
    private val lValueDecoder: LZFSEValueDecoder = LZFSEValueDecoder(LZFSEConstants.ENCODE_L_STATES)
    private val mValueDecoder: LZFSEValueDecoder = LZFSEValueDecoder(LZFSEConstants.ENCODE_M_STATES)
    private val dValueDecoder: LZFSEValueDecoder = LZFSEValueDecoder(LZFSEConstants.ENCODE_D_STATES)
    private val literalDecoder: LZFSELiteralDecoder = LZFSELiteralDecoder(LZFSEConstants.ENCODE_LITERAL_STATES)

    private val literals = ByteArray(LZFSEConstants.LITERALS_PER_BLOCK + 64)
    private var pos: Int = 0

    private var bb: ByteBuffer = BufferUtil.withCapacity(4096)
    private lateinit var `in`: BitInStream

    private var rawBytes: Int = 0
    private var symbols: Int = 0

    @Throws(LZFSEDecoderException::class, IOException::class)
    fun init(bh: LZFSEBlockHeader, @WillNotClose ch: ReadableByteChannel): LZFSEBlockDecoder {
        lValueDecoder.load(bh.lFreq, L_EXTRA_BITS, L_BASE_VALUE)
                .state(bh.lState)
        mValueDecoder.load(bh.mFreq, M_EXTRA_BITS, M_BASE_VALUE)
                .state(bh.mState)
        dValueDecoder.load(bh.dFreq, D_EXTRA_BITS, D_BASE_VALUE)
                .state(bh.dState)
        literalDecoder.load(bh.literalFreq)
                .state(bh.literalState)
                .nLiteralPayloadBytes(bh.nLiteralPayloadBytes)
                .nLiterals(bh.nLiterals)
                .literalBits(bh.literalBits)
                .decodeInto(ch, literals)

        bb = bb.withCapacity(bh.nLmdPayloadBytes, 32)
        IO.readFully(ch, bb)
        `in` = BitInStream(bb).init(bh.lmdBits)

        rawBytes = bh.nRawBytes
        symbols = bh.nMatches

        pos = 0

        return this
    }

    @Throws(IOException::class)
    override fun literal(): Byte {
        return literals[pos++]
    }

    @Throws(IOException::class)
    override fun literal(b: ByteArray, off: Int, len: Int) {
        System.arraycopy(literals, pos, b, off, len)
        pos += len
    }

    @Throws(LZFSEDecoderException::class)
    override fun lmd(): Boolean {
        return if (symbols > 0) {
            symbols--
            `in`.fill()
            l = lValueDecoder.decode(`in`)
            m = mValueDecoder.decode(`in`)
            d = dValueDecoder.decode(`in`)
            true
        } else {
            false
        }
    }

    override fun toString(): String {
        return "LZFSEBlockDecoder{lValueDecoder=$lValueDecoder, mValueDecoder=$mValueDecoder, dValueDecoder=$dValueDecoder, literalDecoder=$literalDecoder, literals=.length${literals.size}, bb=$bb, in=$`in`}"
    }

    companion object {
        private val L_EXTRA_BITS = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 5, 8)

        private val L_BASE_VALUE = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 20, 28, 60)

        private val M_EXTRA_BITS = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 5, 8, 11)

        private val M_BASE_VALUE = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 24, 56, 312)

        private val D_EXTRA_BITS = byteArrayOf(0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15)

        private val D_BASE_VALUE = intArrayOf(0, 1, 2, 3, 4, 6, 8, 10, 12, 16, 20, 24, 28, 36, 44, 52, 60, 76, 92, 108, 124, 156, 188, 220, 252, 316, 380, 444, 508, 636, 764, 892, 1020, 1276, 1532, 1788, 2044, 2556, 3068, 3580, 4092, 5116, 6140, 7164, 8188, 10236, 12284, 14332, 16380, 20476, 24572, 28668, 32764, 40956, 49148, 57340, 65532, 81916, 98300, 114684, 131068, 163836, 196604, 229372)
    }
}
