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

import com.github.horrorho.ragingmoose.TANS.Entry
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import javax.annotation.WillNotClose
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
internal class LZFSELiteralDecoder @Throws(LZFSEDecoderException::class)
        constructor(nStates: Int) {

    private val tans: TANS<Entry> = TANS(Array(nStates) { Entry() })
    private var state: IntArray = intArrayOf(0, 0, 0, 0)

    private var bb: ByteBuffer = BufferUtil.withCapacity(4096)

    private var nLiteralPayloadBytes: Int = 0
    private var nLiterals: Int = 0
    private var literalBits: Int = 0

    @Throws(LZFSEDecoderException::class)
    fun load(weights: ShortArray): LZFSELiteralDecoder {
        tans.init(weights)
        return this
    }

    fun state(state: IntArray): LZFSELiteralDecoder {
        this.state = state.copyOf()
        return this
    }

    fun nLiteralPayloadBytes(nLiteralPayloadBytes: Int): LZFSELiteralDecoder {
        this.nLiteralPayloadBytes = nLiteralPayloadBytes
        return this
    }

    fun nLiterals(nLiterals: Int): LZFSELiteralDecoder {
        this.nLiterals = nLiterals
        return this
    }

    fun literalBits(literalBits: Int): LZFSELiteralDecoder {
        this.literalBits = literalBits
        return this
    }

    @Throws(IOException::class, LZFSEDecoderException::class)
    fun decodeInto(@WillNotClose ch: ReadableByteChannel, literals: ByteArray): LZFSELiteralDecoder {
        bb = bb.withCapacity(nLiteralPayloadBytes, 8)
        IO.readFully(ch, bb)
        val `in` = BitInStream(bb).init(literalBits)

        var i = 0
        while (i < nLiterals) {
            `in`.fill()
            literals[i + 0] = tans.transition(0, state, `in`).symbol
            literals[i + 1] = tans.transition(1, state, `in`).symbol
            literals[i + 2] = tans.transition(2, state, `in`).symbol
            literals[i + 3] = tans.transition(3, state, `in`).symbol
            i += 4
        }
        return this
    }

    override fun toString(): String {
        return ("LZFSELiteralDecoder{tans=$tans, state=$state, nLiteralPayloadBytes=$nLiteralPayloadBytes, nLiterals=$nLiterals, literalBits=$literalBits}")
    }
}