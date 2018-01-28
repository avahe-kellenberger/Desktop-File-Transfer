package tech.avahe.filetransfer.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.IntFunction;

/**
 * @author Avahe
 */
public class Buffers {

    /**
     * Converts a ByteBuffer to a String.
     * @param buffer The buffer to convert.
     * @return A String created from the ByteBuffer.
     */
    public static String toString(final ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    /**
     * Converts a String to a ByteBuffer.
     * @param string The String to convert.
     * @return A ByteBuffer of the String.
     */
    public static ByteBuffer toBuffer(final String string) {
        return StandardCharsets.UTF_8.encode(string);
    }

    /**
     * Copies the given <code>ByteBuffer</code> into a new buffer, matching the number of bytes used
     * by the original buffer.
     * @param buffer The buffer to copy.
     * @return A copy of the buffer.
     */
    public static ByteBuffer copy(final ByteBuffer buffer) {
        return copyWith(ByteBuffer::allocate, buffer);
    }

    /**
     * Copies the given <code>ByteBuffer</code> into a new buffer, matching the number of bytes used
     * by the original buffer. The allocated bytes in this method are contiguous.
     * @param buffer The buffer to copy.
     * @return A copy of the buffer.
     */
    public static ByteBuffer copyDirect(final ByteBuffer buffer) {
        return copyWith(ByteBuffer::allocateDirect, buffer);
    }

    /**
     * Copies the specified ByteBuffer with the given allocation function.
     * @param allocateFunction The function used to allocate a new ByteBuffer.
     * @param buffer The source ByteBuffer to copy.
     * @return A copy of the buffer.
     */
    private static ByteBuffer copyWith(final IntFunction<ByteBuffer> allocateFunction, final ByteBuffer buffer) {
        final ByteBuffer copiedBuffer = allocateFunction.apply(buffer.limit() - buffer.position());
        final int bufferPosition = buffer.position();
        copiedBuffer.put(buffer);
        copiedBuffer.rewind();
        buffer.position(bufferPosition);
        return copiedBuffer;
    }

}