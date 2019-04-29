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
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import java.nio.channels.ReadableByteChannel
import javax.annotation.ParametersAreNonnullByDefault
import javax.annotation.WillNotClose
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
internal class RawBlockDecoder : BlockDecoder {
    private var bb: ByteBuffer = ByteBuffer.allocate(4096)

    @Throws(IOException::class)
    fun init(header: RawBlockHeader, @WillNotClose ch: ReadableByteChannel): RawBlockDecoder {
        initBuffer(header.nRawBytes())
        IO.readFully(ch, bb).rewind()
        return this
    }

    @Throws(IOException::class)
    override fun read(): Int {
        try {
            return if (bb.hasRemaining())
                bb.get().toInt() and 0xFF
            else
                -1
        } catch (ex: BufferUnderflowException) {
            throw LZFSEDecoderException(ex)
        }

    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val available = Math.min(bb.remaining(), len)
        bb.get(b, off, available)
        return available
    }

    fun initBuffer(capacity: Int) {
        if (bb.capacity() < capacity) {
            bb = ByteBuffer.allocate(capacity).order(LITTLE_ENDIAN)
        } else {
            bb.limit(capacity)
        }
        bb.position(0)
    }
}
