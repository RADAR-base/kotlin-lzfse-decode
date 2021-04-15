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
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

/**
 * LZVN block decoder. Decoding table has some precomputed values for the various
 * opcodes.
 */
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
    fun init(header: LZVNBlockHeader, ch: ReadableByteChannel) {
        bb = bb.withCapacity(header.nPayloadBytes)
        ch.readFully(bb).rewind()

        l = 0
        m = 0
        d = -1
    }

    @Throws(IOException::class)
    override fun lmd(): Boolean {
        if (neos) {
            val opc = bb.getUByteInt()
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

    private fun lrgL(@Suppress("UNUSED_PARAMETER") opc: Int): (() -> Unit) = {
        l = bb.getUByteInt() + 16
    }

    private fun smlM(opc: Int): (() -> Unit) {
        val newM = opc and 0xF
        return {
            // 1111MMMM
            m = newM
        }
    }

    private fun lrgM(@Suppress("UNUSED_PARAMETER") opc: Int): (() -> Unit) = {
        // 11110000 MMMMMMMM
        m = bb.getUByteInt() + 16

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
            d = newD or bb.getUByteInt()
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
            d = bb.getUShortInt()
        }
    }

    private fun nop(@Suppress("UNUSED_PARAMETER") opc: Int): (() -> Unit) = {}
    private fun eos(opc: Int): (() -> Unit) = nop(opc)
    private fun udef(@Suppress("UNUSED_PARAMETER") opc: Int): () -> Unit = { throw LZFSEException() }
}
