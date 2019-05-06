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
 *
 * @author Ayesha
 */
internal class LZFSEValueDecoder @Throws(LZFSEException::class)
constructor(nStates: Int) {
    private val tans: TANS<ValueEntry> = TANS(Array(nStates) { ValueEntry() })
    private val state: TANS.State = TANS.State()

    fun load(weights: ShortArray, symbolVBits: ByteArray, symbolVBase: IntArray): LZFSEValueDecoder {
        tans.init(weights).foreach { v -> v[symbolVBits] = symbolVBase }
        return this
    }

    fun state(state: Int): LZFSEValueDecoder {
        this.state.value = state
        return this
    }

    fun decode(`in`: BitInStream): Int {
        return tans.transition(state, `in`).readVData(`in`)
    }

    override fun toString(): String {
        return "LZFSEValueDecoder{tans=$tans, state=$state}"
    }

    internal class ValueEntry : TANS.Entry() {
        private var vBits: Int = 0
        private var vBase: Int = 0

        fun readVData(bitIn: BitInStream) = vBase + bitIn.read(vBits)

        operator fun set(symbolVBits: ByteArray, symbolVBase: IntArray): ValueEntry {
            val s = symbol.toUByteInt()
            this.vBase = symbolVBase[s]
            this.vBits = symbolVBits[s].toInt()
            return this
        }
    }
}
