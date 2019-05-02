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
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import javax.annotation.WillNotClose
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
internal class LZVNBlockDecoder(mb: MatchBuffer) : LMDBlockDecoder(mb) {
    private val tbl: Array<() -> Unit> = arrayOf(
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
            ::smlM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM, ::smlM)
            .mapIndexed { i, f -> f(i) }
            .toTypedArray()

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
            val opc = bb.getUByte()
            tbl[opc]()
            neos = opc != 6
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

    private fun smlL(opc: Int): (() -> Unit) {
        val newL = opc and 0x0F
        return {
            // 1110LLLL LITERAL
            l = newL
        }
    }

    private fun lrgL(opc: Int): (() -> Unit) = {
        l = bb.getUByte() + 16
    }

    private fun smlM(opc: Int): (() -> Unit) {
        val newM = opc and 0xF
        return {
            // 1111MMMM
            m = newM
        }
    }

    private fun lrgM(opc: Int): (() -> Unit) = {
        // 11110000 MMMMMMMM
        m = bb.getUByte() + 16

    }

    private fun preD(opc: Int): (() -> Unit) {
        val newL = opc.ushr(6) and 0x03
        val newM = (opc.ushr(3) and 0x07) + 3
        return {
            // LLMMM110
            l = newL
            m = newM
        }
    }

    private fun smlD(opc: Int): (() -> Unit) {
        val newL = opc.ushr(6) and 0x03
        val newM = (opc.ushr(3) and 0x07) + 3
        val newD = opc and 0x07 shl 8
        return {
            // LLMMMDDD DDDDDDDD LITERAL
            l = newL
            m = newM
            d = newD or bb.getUByte()
        }
    }

    private fun medD(opc: Int): (() -> Unit) {
        val newL = opc.ushr(3) and 0x03
        val newM = opc and 0x7 shl 2
        return {
            // 101LLMMM DDDDDDMM DDDDDDDD LITERAL
            val s = bb.short.toInt()
            l = newL
            m = (newM or (s and 0x03)) + 3
            d = s.ushr(2)
        }
    }

    private fun lrgD(opc: Int): (() -> Unit) {
        val newL = opc.ushr(6) and 0x03
        val newM = (opc.ushr(3) and 0x07) + 3
        return {
            // LLMMM111 DDDDDDDD DDDDDDDD LITERAL
            l = newL
            m = newM
            d = bb.getUShort()
        }
    }

    private fun nop(opc: Int): (() -> Unit) = {}
    private fun eos(opc: Int): (() -> Unit) = nop(opc)
    private fun udef(opc: Int): () -> Unit = { throw LZFSEDecoderException() }
}

fun ByteBuffer.getUByte(): Int = get().toInt() and 0xFF
fun ByteBuffer.getUShort(): Int = short.toInt() and 0xFFFF
