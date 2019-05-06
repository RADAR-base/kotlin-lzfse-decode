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
