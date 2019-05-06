# kotlin-lzfse-decode
[ ![Download](https://api.bintray.com/packages/radar-base/org.radarbase/lzfse-decode/images/download.svg) ](https://bintray.com/radar-base/org.radarbase/lzfse-decode/_latestVersion)

A Kotlin [LZFSE](https://github.com/lzfse/lzfse) capable decoder. This code base is a Kotlin adaptation of the [RagingMoose](https://github.com/horrorho/RagingMoose) Java LZFSE decoder, with some small optimizations applied. That codebase has been designed from the ground up as and barring constants/ tables and a few core routines, has little in the way of resemblance to the source material.

## Should I use it?

The raison d'être of kotlin-lzfse-decode is it's ease of integration into Java projects without the use of external dependencies/ interfacing.

For command line usage or increased performance on large datasets, I would suggest using the reference [lzfse](https://github.com/lzfse/lzfse) compressor.

## Usage

Create an instance of [LZFSEInputStream](https://github.com/RADAR-base/kotlin-lzfse-decode/blob/master/src/main/java/org/radarbase/io/lzfse/LZFSEInputStream.kt) and consume/ close as an [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html).

The constructor accepts a [ReadableByteChannel](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/ReadableByteChannel.html) or an InputStream.

A simple example that decodes and prints the contents of an LZFSE compressed text archive. [LZFSEException](https://github.com/RADAR-base/kotlin-lzfse-decode/blob/master/src/main/java/org/radarbase/io/lzfse/LZFSEException.kt)s signify errors in the underlying data format.

Below is a Kotlin example:

```kotlin
try {
    Files.newByteChannel(Paths.get("my.lzfse.compressed.text.file")).use { channel ->
        val data = LZFSEInputStream(channel).use { it.readAllBytes() }
        println(data.toString(Charsets.UTF_8))
    }
} catch (ex: LZFSEException) {
    println("Bad LZFSE archive: $path")
} catch (ex: IOException) {
    println("IOException: $ex")
}
```

And here is a Java example:

```java
Path path = Paths.get("my.lzfse.compressed.text.file"); // your LZFSE compressed text file here

byte[] buffer = new byte[4096];
try (LZFSEInputStream is = new LZFSEInputStream(Files.newByteChannel(path));
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
    int nRead;
    while ((nRead = is.read(buffer, 0, buffer.length)) != -1) {
        baos.write(buffer, 0, numRead);
    }
    System.out.println(baos.toString("UTF-8"));

} catch (LZFSEException ex) {
    System.err.println("Bad LZFSE archive: " + path);
} catch (IOException ex) {
    System.err.println("IOException: " + ex.toString());
}
```

## License

The current license is the Apache License 2.0 as is contained in the LICENSE file. This code is re-licensed from MIT license as quoted:

> Copyright 2017 Ayesha.
>
> Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
