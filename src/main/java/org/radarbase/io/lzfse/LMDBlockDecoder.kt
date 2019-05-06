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
import kotlin.math.min

/**
 *
 * @author Ayesha
 */
internal abstract class LMDBlockDecoder(private val mb: MatchBuffer) : BlockDecoder {
    var l: Int = 0
    var m: Int = 0
    var d: Int = 0
        set(value) {
            if (value != 0) {
                field = value
            }
        }

    @Throws(IOException::class)
    override fun read(): Int {
        do {
            // Literal
            if (l > 0) {
                l--
                val lit = readLiteral()
                mb.write(lit)
                return lit.toUByteInt()
            }
            // Match
            if (m > 0) {
                m--
                return mb.match(d).toUByteInt()
            }
        } while (lmd())

        return -1
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val endOffset = off + len
        var index = off
        do {
            // Literals
            val numLiterals = min(endOffset - index, l)
            if (numLiterals > 0) {
                readLiteralBytes(b, index, numLiterals)
                mb.write(b, index, numLiterals)

                index += numLiterals
                l -= numLiterals
            }

            // Matches
            val numMatches = min(endOffset - index, m)
            if (numMatches > 0) {
                mb.match(d, b, index, numMatches)
                index += numMatches
                m -= numMatches
            }
        } while (index < endOffset && lmd())

        return index - off
    }

    @Throws(IOException::class)
    internal abstract fun readLiteral(): Byte

    @Throws(IOException::class)
    internal open fun readLiteralBytes(b: ByteArray, off: Int, len: Int) {
        for (i in off until len) {
            b[i] = readLiteral()
        }
    }

    @Throws(IOException::class)
    internal abstract fun lmd(): Boolean
}
