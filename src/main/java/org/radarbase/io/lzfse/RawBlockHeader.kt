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
import java.nio.channels.ReadableByteChannel

/**
 *
 * @author Ayesha
 */
internal class RawBlockHeader {
    private val bb = BufferUtil.withCapacity(4)

    internal var nRawBytes: Int = 0
        private set

    @Throws(IOException::class)
    fun load(ch: ReadableByteChannel) {
        bb.rewind()
        ch.readFully(bb).flip()

        nRawBytes = bb.int
    }

    override fun toString() = "RawBlockHeader{nRawBytes=$nRawBytes}"
}
