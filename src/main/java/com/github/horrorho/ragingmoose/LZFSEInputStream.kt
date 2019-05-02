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
import java.io.InputStream
import java.lang.Integer.toHexString
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@NotThreadSafe
class LZFSEInputStream(private val ch: ReadableByteChannel) : InputStream() {
    private val word = BufferUtil.withCapacity(4)

    private var eos = false

    private val mb: MatchBuffer by lazy { MatchBuffer(LZFSEConstants.MATCH_BUFFER_SIZE) }
    private val lzfseBlockHeader: LZFSEBlockHeader by lazy { LZFSEBlockHeader() }
    private val lzvnBlockHeader: LZVNBlockHeader by lazy { LZVNBlockHeader() }
    private val rawBlockHeader: RawBlockHeader by lazy { RawBlockHeader() }
    private val lzfseBlockDecoder: LZFSEBlockDecoder by lazy { LZFSEBlockDecoder(mb) }
    private val lzvnBlockDecoder: LZVNBlockDecoder by lazy { LZVNBlockDecoder(mb) }
    private val rawBlockDecoder: RawBlockDecoder by lazy { RawBlockDecoder() }

    private var decoder: BlockDecoder? = null

    constructor(`is`: InputStream) : this(Channels.newChannel(`is`))

    override fun available(): Int {
        return if (eos) 0 else 1
    }

    @Throws(IOException::class)
    override fun read(): Int {
        try {
            while (!eos) {
                if (decoder == null) {
                    next()
                } else {
                    val b = decoder!!.read()
                    if (b == -1) {
                        decoder = null
                    } else {
                        return b
                    }
                }
            }
            return -1
        } catch (ex: RuntimeException) {
            throw LZFSEDecoderException(ex)
        }
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len + off > b.size) {
            throw IndexOutOfBoundsException()
        }
        if (len == 0) {
            return 0
        }
        try {
            while (!eos) {
                if (decoder == null) {
                    next()
                } else {
                    val n = decoder!!.read(b, off, len)
                    if (n == 0) {
                        decoder = null
                    }
                    return n
                }
            }
            return -1
        } catch (ex: RuntimeException) {
            throw LZFSEDecoderException(ex)
        }
    }

    @Throws(IOException::class)
    internal operator fun next() {
        when (val magic = magic()) {
            LZFSEConstants.COMPRESSEDV2_BLOCK_MAGIC -> v2Block()
            LZFSEConstants.COMPRESSEDV1_BLOCK_MAGIC -> v1Block()
            LZFSEConstants.COMPRESSEDLZVN_BLOCK_MAGIC -> vnBlock()
            LZFSEConstants.UNCOMPRESSED_BLOCK_MAGIC -> raw()
            LZFSEConstants.ENDOFSTREAM_BLOCK_MAGIC -> eosBlock()
            else -> throw LZFSEDecoderException("bad block: 0x" + toHexString(magic))
        }
    }

    @Throws(IOException::class, LZFSEDecoderException::class)
    internal fun v1Block() {
        lzfseBlockHeader.loadV1(ch)
        decoder = lzfseBlockDecoder.init(lzfseBlockHeader, ch)
    }

    @Throws(IOException::class, LZFSEDecoderException::class)
    internal fun v2Block() {
        lzfseBlockHeader.loadV2(ch)
        decoder = lzfseBlockDecoder.init(lzfseBlockHeader, ch)
    }

    @Throws(IOException::class)
    internal fun vnBlock() {
        lzvnBlockHeader.load(ch)
        decoder = lzvnBlockDecoder.init(lzvnBlockHeader, ch)
    }

    @Throws(IOException::class)
    internal fun raw() {
        rawBlockHeader.load(ch)
        decoder = rawBlockDecoder.init(rawBlockHeader, ch)
    }

    private fun eosBlock() {
        eos = true
        decoder = null
    }

    @Throws(IOException::class)
    internal fun magic(): Int {
        word.rewind()
        ch.readFully(word).rewind()
        return word.int
    }
}
