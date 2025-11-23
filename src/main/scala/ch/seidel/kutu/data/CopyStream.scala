package ch.seidel.kutu.data

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

class CopyStream(override val size: Int) extends ByteArrayOutputStream(size) {
    /**
     * Get an input stream based on the contents of this output stream.
     * Do not use the output stream after calling this method.
     *
     * @return an { @link InputStream}
     */
    def toInputStream = new ByteArrayInputStream(this.buf, 0, this.count)
}