package com.bugreport.sslviassl.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ForwardThread extends Thread{
    private static final int BUFFER_SIZE = 2048;
    private InputStream mInput;
    private OutputStream mOutput;

    public ForwardThread(InputStream input, OutputStream output){
        mInput = input;
        mOutput = output;
    }

    @Override
    public void run() {
        try {
            int count;
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!isInterrupted() && ((count = mInput.read(buffer)) != -1)) {
                mOutput.write(buffer, 0, count);
                mOutput.flush();
            }

            mOutput.close();
            mInput.close();
        } catch (Exception e){
            try {
                mOutput.close();
            } catch (IOException ignored) {}
            try {
                mInput.close();
            } catch (IOException ignored) {}
        }
    }
}