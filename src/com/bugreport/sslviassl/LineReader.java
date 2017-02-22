package com.bugreport.sslviassl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Reads byte for byte from an InputStream and returns the accumulated data at line breaks.
 * In contrast to BufferedReader, this class does not use any buffer, so we can be sure data
 * beyond the reported data is not consumed from the InputStream.
 */
public class LineReader {
    // Same as old Apache versions
    private static final int MAX_LINE_LENGTH = 8190;
    private InputStream mInput;

    private byte[] mEndingBytes;
    private int mEndingIndex = 0;

    public LineReader(InputStream input, String lineEnding){
        mInput = input;
        mEndingBytes = lineEnding.getBytes();
    }

    /**
     * Create a new LineReader using the default line separator CRLF
     * @param input
     */
    public LineReader(InputStream input){
        mInput = input;
        mEndingBytes = "\r\n".getBytes();
    }

    /**
     * Read the next line from the stream
     * @return
     * @throws IOException
     */
    public String readLine() throws IOException{
        mEndingIndex = 0;
        ByteBuffer lineBuffer = ByteBuffer.allocate(MAX_LINE_LENGTH);

        int b;
        while ((b = mInput.read()) != -1){
            if (b == mEndingBytes[mEndingIndex]){
                mEndingIndex++;

                if (mEndingIndex == mEndingBytes.length){
                    return new String(lineBuffer.array(), 0, lineBuffer.position() - (mEndingBytes.length - 1));
                }
            } else {
                mEndingIndex = 0;
            }

            if (lineBuffer.position() < MAX_LINE_LENGTH){
                lineBuffer.put((byte) b);
            } else {
                throw new IOException("Line is too long");
            }
        }

        if (lineBuffer.position() > 0){
            return new String(lineBuffer.array(), 0, lineBuffer.position());
        }

        return null;
    }
}
