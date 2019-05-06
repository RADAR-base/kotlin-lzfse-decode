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