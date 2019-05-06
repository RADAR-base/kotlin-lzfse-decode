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
import kotlin.math.min

/**
 *
 * @author Ayesha
 */
internal class RawBlockDecoder : BlockDecoder {
    private var bb: ByteBuffer = BufferUtil.withCapacity(4096)

    @Throws(IOException::class)
    fun init(header: RawBlockHeader, ch: ReadableByteChannel): RawBlockDecoder {
        bb = bb.withCapacity(header.nRawBytes)
        ch.readFully(bb).rewind()
        return this
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return if (bb.hasRemaining()) bb.getUByteInt() else -1
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val available = min(bb.remaining(), len)
        bb.get(b, off, available)
        return available
    }
}
