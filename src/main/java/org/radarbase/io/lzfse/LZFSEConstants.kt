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
package org.radarbase.io.lzfse

/**
 *
 * @author Ayesha
 */
internal object LZFSEConstants {
    const val ENCODE_HASH_BITS = 14
    const val ENCODE_HASH_WIDTH = 4
    const val ENCODE_GOOD_MATCH = 40
    const val ENCODE_LZVN_THRESHOLD = 4096

    const val ENDOFSTREAM_BLOCK_MAGIC = 0x24787662
    const val UNCOMPRESSED_BLOCK_MAGIC = 0x2d787662
    const val COMPRESSEDV1_BLOCK_MAGIC = 0x31787662
    const val COMPRESSEDV2_BLOCK_MAGIC = 0x32787662
    const val COMPRESSEDLZVN_BLOCK_MAGIC = 0x6e787662

    const val ENCODE_HASH_VALUES = 1 shl ENCODE_HASH_BITS

    const val ENCODE_L_SYMBOLS = 20
    const val ENCODE_M_SYMBOLS = 20
    const val ENCODE_D_SYMBOLS = 64
    const val ENCODE_LITERAL_SYMBOLS = 256

    const val ENCODE_SYMBOLS = ENCODE_L_SYMBOLS + ENCODE_M_SYMBOLS + ENCODE_D_SYMBOLS + ENCODE_LITERAL_SYMBOLS

    const val ENCODE_L_STATES = 64
    const val ENCODE_M_STATES = 64
    const val ENCODE_D_STATES = 256
    const val ENCODE_LITERAL_STATES = 1024

    const val MATCHES_PER_BLOCK = 10000
    const val LITERALS_PER_BLOCK = 4 * MATCHES_PER_BLOCK

    const val ENCODE_MAX_L_VALUE = 315
    const val ENCODE_MAX_M_VALUE = 2359
    const val ENCODE_MAX_D_VALUE = 262139

    const val MATCH_BUFFER_SIZE = 262144  // must be factor 2
}
