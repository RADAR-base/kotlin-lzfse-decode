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

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

/**
 *
 * @author Ayesha
 */
internal class LZFSEBlockDecoder(mb: MatchBuffer) : LMDBlockDecoder(mb) {
    private val lValueDecoder: LZFSEValueDecoder = LZFSEValueDecoder(LZFSEConstants.ENCODE_L_STATES)
    private val mValueDecoder: LZFSEValueDecoder = LZFSEValueDecoder(LZFSEConstants.ENCODE_M_STATES)
    private val dValueDecoder: LZFSEValueDecoder = LZFSEValueDecoder(LZFSEConstants.ENCODE_D_STATES)
    private val literalDecoder: LZFSELiteralDecoder = LZFSELiteralDecoder(LZFSEConstants.ENCODE_LITERAL_STATES)
    private val bitInStream = BitInStream()

    private val literals = ByteArray(LZFSEConstants.LITERALS_PER_BLOCK + 64)
    private var pos: Int = 0

    private var bb: ByteBuffer = BufferUtil.withCapacity(4096)

    private var rawBytes: Int = 0
    private var symbols: Int = 0

    fun init(bh: LZFSEBlockHeader, ch: ReadableByteChannel): LZFSEBlockDecoder {
        lValueDecoder.load(bh.lFreq, L_EXTRA_BITS, L_BASE_VALUE)
                .state(bh.lState)
        mValueDecoder.load(bh.mFreq, M_EXTRA_BITS, M_BASE_VALUE)
                .state(bh.mState)
        dValueDecoder.load(bh.dFreq, D_EXTRA_BITS, D_BASE_VALUE)
                .state(bh.dState)
        literalDecoder.load(bh.literalFreq).apply {
            state = bh.literalState
            nLiteralPayloadBytes = bh.nLiteralPayloadBytes
            nLiterals = bh.nLiterals
            literalBits = bh.literalBits
        }.decodeInto(ch, literals)

        bb = bb.withCapacity(bh.nLmdPayloadBytes, 32)
        ch.readFully(bb)
        bitInStream.init(bb, bh.lmdBits)

        rawBytes = bh.nRawBytes
        symbols = bh.nMatches

        pos = 0

        return this
    }

    override fun readLiteral(): Byte {
        return literals[pos++]
    }

    override fun readLiteralBytes(b: ByteArray, off: Int, len: Int) {
        System.arraycopy(literals, pos, b, off, len)
        pos += len
    }

    override fun lmd(): Boolean {
        return if (symbols > 0) {
            symbols--
            bitInStream.fill()
            l = lValueDecoder.decode(bitInStream)
            m = mValueDecoder.decode(bitInStream)
            d = dValueDecoder.decode(bitInStream)
            true
        } else {
            false
        }
    }

    override fun toString(): String {
        return "LZFSEBlockDecoder{lValueDecoder=$lValueDecoder, mValueDecoder=$mValueDecoder, dValueDecoder=$dValueDecoder, literalDecoder=$literalDecoder, literals=.length${literals.size}, bb=$bb, bitInStream=$bitInStream}"
    }

    companion object {
        private val L_EXTRA_BITS = byteArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                2, 3, 5, 8)

        private val L_BASE_VALUE = intArrayOf(
                0, 1, 2, 3, 4, 5, 6, 7,
                8, 9, 10, 11, 12, 13, 14, 15,
                16, 20, 28, 60)

        private val M_EXTRA_BITS = byteArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                3, 5, 8, 11)

        private val M_BASE_VALUE = intArrayOf(
                0, 1, 2, 3, 4, 5, 6, 7,
                8, 9, 10, 11, 12, 13, 14, 15,
                16, 24, 56, 312)

        private val D_EXTRA_BITS = byteArrayOf(
                0, 0, 0, 0, 1, 1, 1, 1,
                2, 2, 2, 2, 3, 3, 3, 3,
                4, 4, 4, 4, 5, 5, 5, 5,
                6, 6, 6, 6, 7, 7, 7, 7,
                8, 8, 8, 8, 9, 9, 9, 9,
                10, 10, 10, 10, 11, 11, 11, 11,
                12, 12, 12, 12, 13, 13, 13, 13,
                14, 14, 14, 14, 15, 15, 15, 15)

        private val D_BASE_VALUE = intArrayOf(
                0, 1, 2, 3, 4, 6, 8, 10,
                12, 16, 20, 24, 28, 36, 44, 52,
                60, 76, 92, 108, 124, 156, 188, 220,
                252, 316, 380, 444, 508, 636, 764, 892,
                1020, 1276, 1532, 1788, 2044, 2556, 3068, 3580,
                4092, 5116, 6140, 7164, 8188, 10236, 12284, 14332,
                16380, 20476, 24572, 28668, 32764, 40956, 49148, 57340,
                65532, 81916, 98300, 114684, 131068, 163836, 196604, 229372)
    }
}
