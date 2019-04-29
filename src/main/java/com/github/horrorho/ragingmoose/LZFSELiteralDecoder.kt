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
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.channels.ReadableByteChannel
import java.util.function.Supplier
import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.WillNotClose
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
internal class LZFSELiteralDecoder @Throws(LZFSEDecoderException::class)
        constructor(nStates: Int) {

    private val tans: TANS<Entry> = TANS(nStates, ::Entry, ::arrayOfNulls)
    private val state0: TANS.State = TANS.State()
    private val state1: TANS.State = TANS.State()
    private val state2: TANS.State = TANS.State()
    private val state3: TANS.State = TANS.State()

    private var bb: ByteBuffer = ByteBuffer.wrap(ByteArray(0))

    private var nLiteralPayloadBytes: Int = 0
    private var nLiterals: Int = 0
    private var literalBits: Int = 0

    @Throws(LZFSEDecoderException::class)
    fun load(weights: ShortArray): LZFSELiteralDecoder {
        tans.init(weights)
        return this
    }

    fun state(state0: Int, state1: Int, state2: Int, state3: Int): LZFSELiteralDecoder {
        this.state0.value = state0
        this.state1.value = state1
        this.state2.value = state2
        this.state3.value = state3
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
        initBuffer()
        IO.readFully(ch, bb)
        val `in` = BitInStream(bb)
                .init(literalBits)

        var i = 0
        while (i < nLiterals) {
            `in`.fill()
            literals[i + 0] = tans.transition(state0, `in`).symbol
            literals[i + 1] = tans.transition(state1, `in`).symbol
            literals[i + 2] = tans.transition(state2, `in`).symbol
            literals[i + 3] = tans.transition(state3, `in`).symbol
            i += 4
        }
        return this
    }

    private fun initBuffer() {
        val capacity = 8 + nLiteralPayloadBytes
        if (bb.capacity() < capacity) {
            bb = ByteBuffer.allocate(capacity).order(LITTLE_ENDIAN)
        } else {
            bb.limit(capacity)
        }
        bb.position(8)
    }

    override fun toString(): String {
        return ("LZFSELiteralDecoder{tans=$tans, state0=$state0, state1=$state1, state2=$state2, state3=$state3, nLiteralPayloadBytes=$nLiteralPayloadBytes, nLiterals=$nLiterals, literalBits=$literalBits}")
    }
}
