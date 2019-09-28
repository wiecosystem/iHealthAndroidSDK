
package com.ihealth.communication.base.bt;

import java.util.LinkedList;
import java.util.Queue;

import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.utils.ByteBufferUtil;

import com.ihealth.communication.utils.Log;

public class BtUnpackageData {

    private static final String TAG = "BtUnpackageData";
    private byte[] readBuffer;
    private Queue<Byte> readDataQueue = new LinkedList<Byte>();
    private int quencesequenceID;
    private boolean isHead = false;
    private BaseCommProtocol mBtCommProtocol;

    public void addBtCommProtocol(BaseCommProtocol btCommCallback) {
        this.mBtCommProtocol = btCommCallback;
    }

    public void addReadUsbData(byte[] data, int count) {
        for (int i = 0; i < count; i++) {
            readDataQueue.offer(data[i]);
        }
        isFullCommand();
        isFullCommand();
    }

    private void isFullCommand() {
        int temp;
        byte[] datas = null;
        if (readDataQueue.size() < 6) {
            // Log.w(TAG, "This is not full command");
            return;
        }
        int length = readDataQueue.peek() & 0xff;
        if (160 == length) {
            isHead = true;
            readDataQueue.poll();
        }
        if (!isHead) {
            readDataQueue.poll();
            return;
        }
        temp = readDataQueue.peek() & 0xff;
        int len = temp + 3;
        if (readDataQueue.size() >= temp + 2) {
            datas = new byte[len];
            datas[0] = (byte) 0xA0;
            for (int i = 1; i < len; i++) {
                Byte b = readDataQueue.poll().byteValue();
                if (null != b) {
                    datas[i] = b;
                }
            }

        } else {
            // Log.w(TAG, "This is not full command");
            return;
        }
        if (datas.length <= 3) {
            return;
        }
        temp = (datas[3] & 0xff);
        if (quencesequenceID != temp) {
            quencesequenceID = temp;
        } else {
            // Log.v(TAG, "Duplicate command");
            // return;
        }
        readBuffer = new byte[datas.length];
        for (int i = 0; i < datas.length; i++) {
            readBuffer[i] = datas[i];
        }
        isHead = false;
        mBtCommProtocol.unPackageData(readBuffer);
    }
}
