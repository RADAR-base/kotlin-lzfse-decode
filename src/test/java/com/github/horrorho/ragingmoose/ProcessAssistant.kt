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

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Arrays
import java.util.Optional
import java.util.function.Consumer
import java.util.regex.Pattern
import javax.annotation.WillClose
import javax.annotation.WillNotClose
import javax.annotation.concurrent.Immutable
import javax.annotation.concurrent.NotThreadSafe

/**
 *
 * @author Ayesha
 */
@Immutable
object ProcessAssistant {
    @NotThreadSafe
    internal class ProcessInputStream constructor(process: Process) : InputStream() {
        private val `is`: InputStream = process.inputStream
        private var process: Process? = process

        @Volatile
        private var error: IOException? = null

        @Throws(IOException::class)
        override fun read(): Int {
            chk()
            return `is`.read()
        }

        @Throws(IOException::class)
        override fun read(b: ByteArray): Int {
            chk()
            return `is`.read(b)
        }

        @Throws(IOException::class)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            chk()
            return `is`.read(b, off, len)
        }

        @Throws(IOException::class)
        override fun available(): Int {
            chk()
            return `is`.available()
        }

        @Throws(IOException::class)
        fun chk() {
            error?.let {
                error = null
                throw it
            }
        }

        fun error(ex: IOException) {
            if (error == null) {
                error = ex
            } else {
                error!!.addSuppressed(ex)
            }
        }

        @Throws(IOException::class)
        override fun close() {
            process?.let { process ->
                try {
                    if (process.isAlive) {
                        process.destroyForcibly()
                    } else {
                        if (process.exitValue() != 0) {
                            error(processError(process))
                        }
                    }
                    error?.let {
                        throw it
                    }
                } finally {
                    this.process = null
                    `is`.close()
                }
            }
        }

        companion object {

            fun pipe(@WillNotClose `is`: InputStream, @WillClose os: OutputStream, error: Consumer<IOException>) {
                try {
                    `is`.copyTo(os)
                } catch (ex: IOException) {
                    error.accept(ex)
                } finally {
                    try {
                        os.close()
                    } catch (ex: IOException) {
                        error.accept(ex)
                    }

                }
            }

            @Throws(IOException::class)
            fun string(@WillNotClose `is`: InputStream): String {
                ByteArrayOutputStream().use { baos ->
                    `is`.copyTo(baos)
                    return baos.toString("UTF-8")
                }
            }

            fun processError(process: Process): IOException {
                try {
                    process.errorStream.use { `is` ->
                        return IOException(string(`is`))
                    }
                } catch (ex: IOException) {
                    return ex
                }
            }
        }
    }

    @Throws(IOException::class)
    fun newPipedInputStream(pb: ProcessBuilder, @WillNotClose `is`: InputStream): InputStream {
        val p = pb.start()
        val pis = ProcessInputStream(p)
        Thread { ProcessInputStream.pipe(`is`, p.outputStream, Consumer { pis.error(it) }) }
                .start()
        return pis
    }

    @Throws(IOException::class)
    fun newInputStream(pb: ProcessBuilder): InputStream {
        val p = pb.start()
        return ProcessInputStream(p)
    }

    fun firstInPath(vararg cmd: String): Optional<String> {
        return Arrays.stream(cmd)
                .filter(::isInPath)
                .findFirst()
    }

    private fun isInPath(cmd: String): Boolean {
        return Pattern.compile(Pattern.quote(File.pathSeparator))
                .splitAsStream(System.getenv("PATH"))
                .map { Paths.get(it, cmd) }
                .anyMatch { Files.exists(it) }
    }
}
