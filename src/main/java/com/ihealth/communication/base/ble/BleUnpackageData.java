package com.ihealth.communication.base.ble;

import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.utils.ByteBufferUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class BleUnpackageData {

//	private static final String TAG = "AndroidBle";

    private NewDataCallback mCommCallback;

    public void addBleCommCallback(NewDataCallback commCallback) {
        this.mCommCallback = commCallback;
    }

    public BleUnpackageData() {
        readBuffer.clear();
    }

    private final Map<Integer, byte[]> readBuffer = new HashMap<Integer, byte[]>();
    private int[] sequenceIdBuffer = null;
    private int mCommandId;

    private static final int NEW_PACKAGEDATA = 101;
    private static final int NEXT_PACKAGEDATA = 102;
    private static final int THIS_PACKAGEDATA = 103;

    private int checkPackageRange(int sequenceId) {
        if (sequenceIdBuffer == null) {
            return NEW_PACKAGEDATA;
        } else {
            for (int seqId : sequenceIdBuffer) {
                if (seqId == sequenceId) {
                    return THIS_PACKAGEDATA;
                }
            }
            return NEXT_PACKAGEDATA;
        }
    }

    private void setSequenceIdBuffer(int allPackageNum, int subPackageNum, int sequenceId) {
        sequenceIdBuffer = null;
        sequenceIdBuffer = new int[allPackageNum];
        sequenceIdBuffer[subPackageNum] = sequenceId;
        int subPackageNumReverse = allPackageNum - subPackageNum - 1;
        for (int i = 0; i <= subPackageNumReverse; i++) {
            int temp = sequenceId - (subPackageNumReverse - i) * 2;
            if (temp < 0) {
                temp = 256 + temp;
            }
            sequenceIdBuffer[i] = temp;
        }

        for (int i = (subPackageNumReverse + 1); i < allPackageNum; i++) {
            int temp = sequenceId + (i - subPackageNumReverse) * 2;
            if (temp > 255) {
                temp = temp - 256;
            }
            sequenceIdBuffer[i] = temp;
        }

    }

    private void finishPackageData() {
        int commandsum = 0;
        for (Entry<Integer, byte[]> entry : readBuffer.entrySet()) {
            commandsum += entry.getValue().length;
        }
        byte[] command = new byte[commandsum];
        int index = 0;
        for (int i = 0; i < sequenceIdBuffer.length; i++) {
            int reqId = sequenceIdBuffer[i];
            byte[] temp = readBuffer.get(reqId);
            for (int j = 0; j < temp.length; j++) {
                command[index] = temp[j];
                index += 1;
            }
        }
        mCommCallback.haveNewData(mCommandId, 0, command);
    }

    private void resetSequenceIdBuffer(int allPackageNum, int subPackageNum, int sequenceId) {
        sequenceIdBuffer = new int[allPackageNum];
        setSequenceIdBuffer(allPackageNum, subPackageNum, sequenceId);
    }

    private void checkSequenceIdBuffer(int allPackageNum, int subPackageNum, int sequenceId, byte[] data) {
        if (readBuffer.get(sequenceId) == null) {
            if ((allPackageNum - 1) == subPackageNum) {
                mCommandId = data[5] & 0xff;
                byte[] temp = ByteBufferUtil.bytesCuttForProductProtocol(6, data);
                readBuffer.put(sequenceId, temp);
            } else {
                byte[] temp = ByteBufferUtil.bytesCuttForProductProtocol(5, data);
                readBuffer.put(sequenceId, temp);
            }
            if (readBuffer.size() == allPackageNum) {
                finishPackageData();
            }
        }
    }

    public synchronized void unPackageData(byte[] data) {
        int stateId = data[2] & 0xff;
        if (stateId == 0) {
            int commandId = data[5] & 0xff;
            byte[] temp = ByteBufferUtil.bytesCuttForProductProtocol(6, data);
            mCommCallback.haveNewData(commandId, 0, temp);
        } else if (stateId == 0xf0) {
            int commandId = data[5] & 0xff;
            byte[] temp = ByteBufferUtil.bytesCuttForProductProtocol(6, data);
            mCommCallback.haveNewData(commandId, 0, temp);
        } else if (stateId < 0xA0) {
            int allPackageNum = (stateId >> 4) + 1;
            int subPackageNum = stateId & 0x0f;
            int sequenceId = data[3] & 0xff;

            switch (checkPackageRange(sequenceId)) {
                case NEXT_PACKAGEDATA:
                    mCommandId = 0;
                    readBuffer.clear();

                case NEW_PACKAGEDATA:
                    resetSequenceIdBuffer(allPackageNum, subPackageNum, sequenceId);

                case THIS_PACKAGEDATA:
                    checkSequenceIdBuffer(allPackageNum, subPackageNum, sequenceId, data);
                    break;

                default:
                    break;
            }

        }

    }
}
