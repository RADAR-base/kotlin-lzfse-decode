package org.radarbase.io.lzfse

import java.nio.ByteBuffer

fun ByteBuffer.getUByteInt(): Int = get().toInt() and 0xFF
fun ByteBuffer.getUShortInt(): Int = short.toInt() and 0xFFFF

fun Byte.toUByteInt(): Int = this.toInt() and 0xFF