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
import java.io.InputStream
import java.lang.Integer.toHexString
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

/**
 * Input stream that decodes an LZFSE encoded stream. On encoding errors, a LZFSEDecoderException is
 * thrown.
 */
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

    /** Reports 1 if there is data available, 0 if the end of the stream has been reached. */
    override fun available(): Int {
        return if (eos) 0 else 1
    }

    @Throws(IOException::class)
    override fun read(): Int {
        try {
            while (!eos) {
                decoder?.let {
                    val b = it.read()
                    if (b == -1) {
                        decoder = null
                    } else {
                        return@read b
                    }
                } ?: next()
            }
            return -1
        } catch (ex: RuntimeException) {
            throw LZFSEException(ex)
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
                decoder?.let {
                    val n = it.read(b, off, len)
                    if (n == 0) {
                        decoder = null
                    }
                    return@read n
                } ?: next()
            }
            return -1
        } catch (ex: RuntimeException) {
            throw LZFSEException(ex)
        }
    }

    private fun next() {
        decoder = when (val magic = magic()) {
            LZFSEConstants.COMPRESSEDV2_BLOCK_MAGIC -> {
                lzfseBlockHeader.loadV2(ch)
                lzfseBlockDecoder.init(lzfseBlockHeader, ch)
            }
            LZFSEConstants.COMPRESSEDV1_BLOCK_MAGIC -> {
                lzfseBlockHeader.loadV1(ch)
                lzfseBlockDecoder.init(lzfseBlockHeader, ch)
            }
            LZFSEConstants.COMPRESSEDLZVN_BLOCK_MAGIC -> {
                lzvnBlockHeader.load(ch)
                lzvnBlockDecoder.init(lzvnBlockHeader, ch)
            }
            LZFSEConstants.UNCOMPRESSED_BLOCK_MAGIC -> {
                rawBlockHeader.load(ch)
                rawBlockDecoder.init(rawBlockHeader, ch)
            }
            LZFSEConstants.ENDOFSTREAM_BLOCK_MAGIC -> {
                eos = true
                null
            }
            else -> throw LZFSEException("bad block: 0x" + toHexString(magic))
        }
    }

    private fun magic(): Int {
        word.rewind()
        ch.readFully(word).rewind()
        return word.int
    }
}
