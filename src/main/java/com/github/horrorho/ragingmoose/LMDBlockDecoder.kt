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

import java.io.IOException
import javax.annotation.concurrent.NotThreadSafe
import kotlin.math.min

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
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
                val lit = literal()
                mb.write(lit)
                return lit.toInt() and 0xFF
            }
            // Match
            if (m > 0) {
                m--
                return mb.match(d).toInt() and 0xFF
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
                literal(b, index, numLiterals)
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
    internal abstract fun literal(): Byte

    @Throws(IOException::class)
    internal open fun literal(b: ByteArray, off: Int, len: Int) {
        for (i in off until len) {
            b[i] = literal()
        }
    }

    @Throws(IOException::class)
    internal abstract fun lmd(): Boolean
}
