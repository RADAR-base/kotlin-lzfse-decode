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

/**
 * tANS - asymmetric numeral systems tabled variant
 *
 * @author Ayesha
 * @param <T>
</T> */
internal class TANS<T : TANS.Entry>(internal val table: Array<T>) {
    private val nZero: Int = Integer.numberOfLeadingZeros(table.size)

    internal open class Entry {
        fun readData(bitIn: BitInStream) = nBase + bitIn.read(nBits)

        var nBits: Int = 0
        var nBase: Int = 0
        var symbol: Byte = 0
    }

    internal class State {
        var value: Int = 0

        override fun toString(): String {
            return "State{value=$value}"
        }
    }

    fun transition(state: IntArray, `in`: BitInStream, literals: ByteArray, literalOff: Int) {
        // do not use IntRange iterators to prevent unpredictable memory use.
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

    @Throws(LZFSEException::class)
    fun init(weights: ShortArray): TANS<T> {
        if (weights.size > 256) {
            throw LZFSEException()
        }
        try {
            var t = 0
            weights.forEachIndexed { i, weight ->
                t = fill(i.toByte(), weight.toInt(), t)
            }
            return this
        } catch (ex: ArrayIndexOutOfBoundsException) {
            throw LZFSEException(ex)
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
