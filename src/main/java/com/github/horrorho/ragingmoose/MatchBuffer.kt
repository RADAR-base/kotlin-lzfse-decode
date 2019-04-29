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
internal class MatchBuffer(size: Int) {

    private val buf: ByteArray = ByteArray(size)
    private val mod: Int = size - 1
    private var p: Int = 0

    init {
        if (size and mod != 0) {
            throw IllegalArgumentException("size not a power of 2: $size")
        }
    }

    fun write(b: Byte) {
        buf[p] = b
        p++
        p = p and mod
    }

    fun match(d: Int): Byte {
        val b = buf[p - d and mod]
        write(b)
        return b
    }

    override fun toString(): String {
        return "MatchBuffer{buf.length=${buf.size}, mod=$mod, p=$p}"
    }
}
