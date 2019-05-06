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
package org.radarbase.io.lzfse

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

/**
 *
 * @author Ayesha
 */
internal class LZFSELiteralDecoder @Throws(LZFSEException::class)
        constructor(nStates: Int) {
    private val tans: TANS<TANS.Entry> = TANS(Array(nStates) { TANS.Entry() })
    private var bb: ByteBuffer = BufferUtil.withCapacity(4096)

    var state: IntArray = intArrayOf(0, 0, 0, 0)
        set(value) {
            field = value.copyOf()
        }

    var nLiteralPayloadBytes: Int = 0
    var nLiterals: Int = 0
    var literalBits: Int = 0

    // reuse stream to prevent unpredictable memory use.
    private val bitInStream = BitInStream()

    @Throws(LZFSEException::class)
    fun load(weights: ShortArray): LZFSELiteralDecoder {
        tans.init(weights)
        return this
    }

    @Throws(IOException::class, LZFSEException::class)
    fun decodeInto(ch: ReadableByteChannel, literals: ByteArray): LZFSELiteralDecoder {
        bb = bb.withCapacity(nLiteralPayloadBytes, 8)
        ch.readFully(bb)
        bitInStream.init(bb, literalBits)

        // do not use IntRange iterators to prevent unpredictable memory use.
        var i = 0
        while (i < nLiterals) {
            bitInStream.fill()
            tans.transition(state, bitInStream, literals, i)
            i += 4
        }
        return this
    }

    override fun toString(): String {
        return ("LZFSELiteralDecoder{tans=$tans, state=$state, nLiteralPayloadBytes=$nLiteralPayloadBytes, nLiterals=$nLiterals, literalBits=$literalBits}")
    }
}