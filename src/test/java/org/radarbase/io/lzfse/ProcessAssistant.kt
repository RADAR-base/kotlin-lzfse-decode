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

import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
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
        Thread { ProcessAssistant.ProcessInputStream.pipe(`is`, p.outputStream, Consumer { pis.error(it) }) }
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
                .filter(ProcessAssistant::isInPath)
                .findFirst()
    }

    private fun isInPath(cmd: String): Boolean {
        return Pattern.compile(Pattern.quote(File.pathSeparator))
                .splitAsStream(System.getenv("PATH"))
                .map { Paths.get(it, cmd) }
                .anyMatch { Files.exists(it) }
    }
}
