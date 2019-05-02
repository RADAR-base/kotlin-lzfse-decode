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

import javax.annotation.concurrent.NotThreadSafe

/**
 * tANS - asymmetric numeral systems tabled variant
 *
 * @author Ayesha
 * @param <T>
</T> */
@NotThreadSafe
internal class TANS<T : TANS.Entry>(private val table: Array<T>) {
    private val nZero: Int = Integer.numberOfLeadingZeros(table.size)

    @NotThreadSafe
    internal open class Entry {
        fun readData(bitIn: BitInStream) = nBase + bitIn.read(nBits)

        var nBits: Int = 0
        var nBase: Int = 0
        var symbol: Byte = 0
    }

    @NotThreadSafe
    internal class State {
        var value: Int = 0

        override fun toString(): String {
            return "State{value=$value}"
        }
    }

    fun transition(state: IntArray, `in`: BitInStream, literals: ByteArray, literalOff: Int) {
        var i = 0
        while (i < 4) {
            val entry = table[state[i]]
            literals[literalOff + i] = entry.symbol
            state[i] = entry.readData(`in`)
            i++
        }
    }

    fun transition(state: State, `in`: BitInStream): T {
        return table[state.value].also {
            state.value = it.readData(`in`)
        }
    }

    inline fun foreach(consumer: (T) -> Unit) = table.forEach(consumer)

    @Throws(LZFSEDecoderException::class)
    fun init(weights: ShortArray): TANS<T> {
        if (weights.size > 256) {
            throw LZFSEDecoderException()
        }
        try {
            var t = 0
            weights.forEachIndexed { i, weight ->
                t = fill(i.toByte(), weight.toInt(), t)
            }
            return this
        } catch (ex: ArrayIndexOutOfBoundsException) {
            throw LZFSEDecoderException(ex)
        }
    }

    private fun fill(s: Byte, w: Int, initialT: Int): Int {
        var t = initialT
        val k = Integer.numberOfLeadingZeros(w) - nZero
        val x = (table.size shl 1).ushr(k) - w
        var i = 0
        while (i < w) {
            val e = table[t++]
            e.symbol = s
            if (i < x) {
                e.nBits = k
                e.nBase = (w + i shl k) - table.size
            } else {
                e.nBits = k - 1
                e.nBase = i - x shl k - 1
            }
            i++
        }
        return t
    }

    override fun toString(): String {
        return "TANS{table.length=${table.size}}"
    }
}
