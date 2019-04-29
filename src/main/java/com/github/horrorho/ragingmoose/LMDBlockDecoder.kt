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
import java.util.Objects
import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
@ParametersAreNonnullByDefault
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
        try {
            do {
                // Literal
                if (l > 0) {
                    l--
                    val b = literal()
                    mb.write(b)
                    return b.toInt() and 0xFF
                }
                // Match
                if (m > 0) {
                    m--
                    return mb.match(d).toInt() and 0xFF
                }
            } while (lmd())

            return -1

        } catch (ex: IllegalArgumentException) {
            throw LZFSEDecoderException(ex)
        }

    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        try {
            val to = off + len
            var o = off
            do {
                // Literals
                val ls = Math.min(to - o, l)
                run {
                    val n = o + ls
                    while (o < n) {
                        val _b = literal()
                        mb.write(_b)
                        b[o] = _b
                        o++
                    }
                }
                l -= ls
                // Matches
                val ms = Math.min(to - o, m)
                val n = o + ms
                while (o < n) {
                    b[o] = mb.match(d)
                    o++
                }
                m -= ms
            } while (to - o > 0 && lmd())

            return o - off
        } catch (ex: IllegalArgumentException) {
            throw LZFSEDecoderException(ex)
        }

    }

    @Throws(IOException::class)
    internal abstract fun literal(): Byte

    @Throws(IOException::class)
    internal abstract fun lmd(): Boolean
}
