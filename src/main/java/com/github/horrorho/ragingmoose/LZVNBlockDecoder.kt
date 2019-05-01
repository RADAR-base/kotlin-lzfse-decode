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
internal class LZVNBlockDecoder(mb: MatchBuffer) : LMDBlockDecoder(mb) {
    private val tbl = arrayOf<(Int) -> Boolean>(
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::eos, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::nop, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::nop, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::udef, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::udef, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::udef, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::udef, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::udef, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::udef, ::udef, ::udef, ::udef, ::udef, ::udef, ::udef, ::udef,
            ::udef, ::udef, ::udef, ::udef, ::udef, ::udef, ::udef, ::udef,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::medD, ::medD, ::medD, ::medD, ::medD, ::medD, ::medD, ::medD,
            ::medD, ::medD, ::medD, ::medD, ::medD, ::medD, ::medD, ::medD,
            ::medD, ::medD, ::medD, ::medD, ::medD, ::medD, ::medD, ::medD,
            ::medD, ::medD, ::medD, ::medD, ::medD, ::medD, ::medD, ::medD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::smlD, ::preD, ::lrgD,
            ::udef, ::udef, ::udef, ::udef, ::udef, ::udef, ::udef, ::udef,
            ::udef, ::udef, ::udef, ::udef, ::udef, ::udef, ::udef, ::udef,
            ::lrgL, ::smlL, ::smlL, ::smlL, ::smlL, ::smlL, ::smlL, ::smlL,
            ::smlL, ::smlL, ::smlL, ::smlL, ::smlL, ::smlL, ::smlL, ::smlL,
            ::lrgM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM,
            ::smlM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM
    )

    private var bb: ByteBuffer = BufferUtil.withCapacity(4096)
    private var neos = true

    @Throws(IOException::class)
    fun init(header: LZVNBlockHeader, @WillNotClose ch: ReadableByteChannel): LZVNBlockDecoder {
        bb = bb.withCapacity(header.nPayloadBytes)
        ch.readFully(bb).rewind()

        l = 0
        m = 0
        d = -1

        return this
    }

    @Throws(IOException::class)
    override fun lmd(): Boolean {
        if (neos) {
            val opc = bb.get().toInt() and 0xFF
            neos = tbl[opc](opc)
        }
        return neos
    }

    @Throws(IOException::class)
    override fun readLiteral(): Byte {
        return bb.get()
    }

    override fun readLiteralBytes(b: ByteArray, off: Int, len: Int) {
        bb.get(b, off, len)
    }

    private fun smlL(opc: Int): Boolean {
        // 1110LLLL LITERAL
        l = opc and 0x0F
        return true
    }

    private fun lrgL(@Suppress("UNUSED_PARAMETER") opc: Int): Boolean {
        // 11100000 LLLLLLLL LITERAL
        l = (bb.get().toInt() and 0xFF) + 16
        return true
    }

    private fun smlM(opc: Int): Boolean {
        // 1111MMMM
        m = opc and 0xF
        return true
    }

    private fun lrgM(@Suppress("UNUSED_PARAMETER") opc: Int): Boolean {
        // 11110000 MMMMMMMM
        m = (bb.get().toInt() and 0xFF) + 16
        return true
    }

    private fun preD(opc: Int): Boolean {
        // LLMMM110
        l = opc.ushr(6) and 0x03
        m = (opc.ushr(3) and 0x07) + 3
        return true
    }

    private fun smlD(opc: Int): Boolean {
        // LLMMMDDD DDDDDDDD LITERAL
        l = opc.ushr(6) and 0x03
        m = (opc.ushr(3) and 0x07) + 3
        d = opc and 0x07 shl 8 or (bb.get().toInt() and 0xFF)
        return true
    }

    private fun medD(opc: Int): Boolean {
        // 101LLMMM DDDDDDMM DDDDDDDD LITERAL
        val s = bb.short.toInt()
        l = opc.ushr(3) and 0x03
        m = (opc and 0x7 shl 2 or (s and 0x03)) + 3
        d = s.ushr(2)
        return true
    }

    private fun lrgD(opc: Int): Boolean {
        // LLMMM111 DDDDDDDD DDDDDDDD LITERAL
        l = opc.ushr(6) and 0x03
        m = (opc.ushr(3) and 0x07) + 3
        d = bb.short.toInt() and 0xFFFF
        return true
    }

    private fun eos(@Suppress("UNUSED_PARAMETER") opc: Int): Boolean = false

    private fun nop(@Suppress("UNUSED_PARAMETER") opc: Int): Boolean = true

    @Throws(LZFSEDecoderException::class)
    fun udef(@Suppress("UNUSED_PARAMETER") opc: Int): Boolean = throw LZFSEDecoderException()
}
