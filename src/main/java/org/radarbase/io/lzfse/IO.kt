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

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ReadableByteChannel

internal fun ReadableByteChannel.readFully(bb: ByteBuffer): ByteBuffer {
    while (bb.hasRemaining()) {
        if (read(bb) == -1) {
            throw EOFException()
        }
    }
    return bb
}

internal object BufferUtil {
    fun withCapacity(capacity: Int): ByteBuffer {
        return ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)
    }
}

internal fun ByteBuffer.withCapacity(capacity: Int, atPosition: Int = 0): ByteBuffer {
    val totalCapacity = capacity + atPosition
    return if (capacity() < totalCapacity) {
        BufferUtil.withCapacity(totalCapacity)
    } else {
        limit(totalCapacity)
    }.position(atPosition)
}
