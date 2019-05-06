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

import kotlin.math.min

/**
 * Buffer for matches. Implemented as a ring buffer.
 * @param size number of bytes in the buffer, must be a power of 2.
 */
internal class MatchBuffer(size: Int) {
    private val buf: ByteArray = ByteArray(size)
    private val mod: Int = size - 1
    private var p: Int = 0

    init {
        if (size and mod != 0) {
            throw IllegalArgumentException("size not a power of 2: $size")
        }
    }

    /** Write single byte to buffer. */
    fun write(b: Byte) {
        buf[p] = b
        p = (p + 1) and mod
    }

    /** Write byte array to buffer, wrapping around if necessary. */
    fun write(b: ByteArray, off: Int, len: Int) {
        // if the number of bytes is larger than the buffer, discard the first
        // number of bytes
        val writeLen = min(len, buf.size)
        val writeOffset = off + len - writeLen
        p = (p + len - writeLen) and mod

        copyRing(b, writeOffset, buf, p, writeLen)
        p = (p + writeLen) and mod
    }

    fun match(d: Int): Byte {
        val b = buf[(p - d) and mod]
        buf[p] = b
        p = (p + 1) and mod
        return b
    }

    /**
     * Read a match and write it back with delay d.
     * If len is larger than d, this means that after d bytes, multiple copies of the source array
     * are written back.
     */
    tailrec fun match(d: Int, b: ByteArray, off: Int, len: Int) {
        if (len <= d) {
            matchUnsafe(d, b, off, len)
        } else {
            matchUnsafe(d, b, off, d)
            match(d, b, off + d, len - d)
        }
    }

    /** Read a match and write it back with delay d. Delay d must not be larger than len. */
    private fun matchUnsafe(d: Int, b: ByteArray, off: Int, len: Int) {
        val srcOff = (p - d) and mod
        copyRing(buf, srcOff, buf, p, len)
        copyRing(buf, srcOff, b, off, len)
        p = (p + len) and mod
    }

    /** Copy source buffer to destination, wrapping around if the end of the buffer is encountered. */
    private fun copyRing(src: ByteArray, srcOff: Int, dest: ByteArray, destOff: Int, len: Int) {
        val srcLen = src.size - srcOff
        val destLen = dest.size - destOff

        val initialLen = minOf(srcLen, destLen, len)

        System.arraycopy(src, srcOff, dest, destOff, initialLen)

        when (initialLen) {
            len -> return
            srcLen -> {
                if (destLen >= len || destLen == srcLen) {
                    System.arraycopy(src, 0, dest, destOff + initialLen, len - initialLen)
                } else  {
                    System.arraycopy(src, 0, dest, destOff + initialLen, destLen - initialLen)
                    System.arraycopy(src, destLen - initialLen, dest, 0, len - destLen)
                }
            }
            destLen -> {
                if (srcLen >= len || srcLen == destLen) {
                    System.arraycopy(src, srcOff + initialLen, dest, 0, len - initialLen)
                } else  {
                    System.arraycopy(src, srcOff + initialLen, dest, 0, srcLen - initialLen)
                    System.arraycopy(src, 0, dest, srcLen - initialLen, len - srcLen)
                }
            }
        }
    }

    override fun toString(): String {
        return "MatchBuffer{buf.length=${buf.size}, mod=$mod, p=$p}"
    }
}
