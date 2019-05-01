package com.github.horrorho.ragingmoose

import java.io.InputStream

class ForwardedInputStream(origin: Sequence<ByteArray>) : InputStream() {
    private val originIterator = origin.iterator()
    private var buffer: ByteArray? = null
    private var offset: Int = 0

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return nextBuffer()?.let {
            val copyLen = Math.min(it.size - offset, len)
            System.arraycopy(it, offset, b, off, copyLen)
            offset += copyLen
            if (offset == it.size) {
                buffer = null
            }
            copyLen
        } ?: -1
    }

    override fun read(): Int {
        return nextBuffer()?.let {
            offset += 1
            if (offset == it.size) {
                buffer = null
            }
            it[offset - 1].toInt() and 0xFF
        } ?: -1
    }

    private fun nextBuffer(): ByteArray? {
        if (buffer == null && originIterator.hasNext()) {
            buffer = originIterator.next()
            offset = 0
        }
        return buffer
    }

    override fun available(): Int {
        return if (buffer != null || originIterator.hasNext()) 1 else 0
    }
}