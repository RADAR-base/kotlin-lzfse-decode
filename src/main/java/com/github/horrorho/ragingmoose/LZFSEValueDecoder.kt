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

import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
@ParametersAreNonnullByDefault
internal class LZFSEValueDecoder @Throws(LZFSEDecoderException::class)
constructor(nStates: Int) {
    private val tans: TANS<Entry> = TANS(nStates, ::Entry, ::arrayOfNulls)
    private val state: TANS.State = TANS.State()

    @Throws(LZFSEDecoderException::class)
    fun load(weights: ShortArray, symbolVBits: ByteArray, symbolVBase: IntArray): LZFSEValueDecoder {
        tans.init(weights)
                .foreach { _, v -> v[symbolVBits] = symbolVBase }
        return this
    }

    fun state(state: Int): LZFSEValueDecoder {
        this.state.value = state
        return this
    }

    @Throws(LZFSEDecoderException::class)
    fun decode(`in`: BitInStream): Int {
        return tans.transition(state, `in`).let {
            it.vBase + `in`.read(it.vBits).toInt()
        }
    }

    override fun toString(): String {
        return "LZFSEValueDecoder{tans=$tans, state=$state}"
    }

    @NotThreadSafe
    internal class Entry : TANS.Entry() {
        var vBits: Int = 0
        var vBase: Int = 0

        operator fun set(symbolVBits: ByteArray, symbolVBase: IntArray): Entry {
            val s = symbol.toInt() and 0xFF
            this.vBase = symbolVBase[s]
            this.vBits = symbolVBits[s].toInt()
            return this
        }
    }
}
