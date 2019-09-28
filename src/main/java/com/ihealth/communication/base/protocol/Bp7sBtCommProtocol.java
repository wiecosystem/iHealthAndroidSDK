
package com.ihealth.communication.base.protocol;

import android.content.Context;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.DataNotify;
import com.ihealth.communication.base.comm.DataNotifyImpl;
import com.ihealth.communication.base.comm.NewDataCallback;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class Bp7sBtCommProtocol implements BaseCommProtocol {

    private static final String TAG = "BtCommProtocol >>>";
    private BaseComm comm;
    private byte trasmitHead = (byte) 0xb0;
    private DataNotify btNotify;
    private int sendSequenceId = 1;
    private Context mContext;
    private String mac;
    private String type;

    public Bp7sBtCommProtocol(Context context, String mac, String type, BaseComm com, NewDataCallback dataCallBack) {
        this.comm = com;
        mContext = context;
        this.mac = mac;
        this.type = type;
        com.addCommNotify(this);
        btNotify = new DataNotifyImpl();
        btNotify.attach(dataCallBack);
    }

    private int commandSequenceID = 1;
    private ConcurrentHashMap<Integer, byte[]> commandCountMap = new ConcurrentHashMap<Integer, byte[]>();

    public void packageDataAsk(byte[] command) {
        int len = command.length + 2;
        int lenFull = len + 3;
        byte[] commandtemp = new byte[lenFull];
        commandtemp[0] = trasmitHead;
        commandtemp[1] = (byte) len;
        commandtemp[2] = (byte) 0xa0;
        commandtemp[3] = (byte) commandSequenceID;
        for (int i = 0; i < command.length; i++) {
            commandtemp[4 + i] = command[i];
        }
        commandtemp[lenFull - 1] = generateCKS(commandtemp);
        comm.sendData(null, commandtemp);
        addSeqID();
    }

    private Timer timer;
    private TimerTask task;

    int i;

    private void repeatSendTimer() {
        cancelTimer();
        timer = new Timer();
        task = new TimerTask() {

            @Override
            public void run() {
                repeatSend();
                i++;

                if (i >= 3) {
                    Log.w(TAG, "repeatSendTimer() -- failed");
                    cancelTimer();
//                    Intent mIntent = new Intent(A1InsSetforBp7s.MSG_BP7S_NO_RESPONDERROR);
//                    mIntent.putExtra(DeviceManager.MSG_TYPE, type);
//                    mIntent.putExtra(DeviceManager.MSG_MAC, mac);
//                    mContext.sendBroadcast(mIntent);

                }
            }
        };
        timer.schedule(task, 500, 500);
    }

    private void repeatSend() {
        try {
            for (Iterator<Integer> it = commandCountMap.keySet().iterator(); it.hasNext();) {
                Object key = it.next();
                if (commandCountMap.get(key) != null) {
                    comm.sendData(null, commandCountMap.get(key));
                }
            }
        } catch (Exception e) {
            commandCountMap.clear();
        }
    }

    private void cancelTimer() {
        i = 0;
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    byte priorSeqID = -1;
    byte[] priorReceiveData = null;

    public void unPackageData(byte[] commandReceive) {
        // 判断是不是下位机发过来的指令
        if (commandReceive[0] != (byte) 0xA0) {
            Log.w(TAG, "head byte is not A0");
            return;
        }
        int len = commandReceive[1] & 0xff;
        int lenR = commandReceive.length;

        if (lenR == 6) {
            Log.w(TAG, "lenR == 6");
            return;
        }
        // 获得当前下位机发过来的顺序ID
        byte seqID = commandReceive[3];
        int tempSeqID = 0;
        if (seqID == 0) {
            tempSeqID = 255;
        } else {
            tempSeqID = (seqID & 0xFF) - 1;
        }


        commandCountMap.remove(tempSeqID);
        priorSeqID = seqID;
        priorReceiveData = commandReceive;
        if (tempSeqID == sendSequenceId) {
            cancelTimer();
        }
        setSeqID(seqID);
        if (len != lenR - 3) {
            Log.w(TAG, "This is not full command");
            return;
        }
        // byte stateID = commandReceive[2];
        if (!checkCKS(2, lenR - 2, commandReceive)) {
            Log.w(TAG, "checksum is wrong");
            return;
        }

        byte[] command = bytesCutt(6, lenR - 2, commandReceive);
        // 顺序ID +1 为以后发指令做准备
        addSeqID();
        btNotify.haveNewData(commandReceive[5] & 0xff, commandReceive[2] & 0xff, command);
    }

    private boolean checkCKS(int start, int end, byte[] data) {
        int cks = data[end + 1] & 0xFF;
        int sum = 0;
        for (int i = start; i < end + 1; i++) {
            sum = sum + data[i] & 0xFF;
        }
        if (sum == cks) {
            return true;
        } else {
            return false;
        }
    }

    private byte generateCKS(byte[] command) {
        int sum = 0;
        for (int i = 2; i < command.length - 1; i++) {
            sum = sum + command[i];
        }
        return (byte) sum;
    }

    private void addSeqID() {
        if (commandSequenceID == 255) {
            commandSequenceID = 0;
        } else {
            commandSequenceID += 1;
        }
    }

    private void setSeqID(int seqID) {
        commandSequenceID = seqID;
    }

    private byte[] bytesCutt(int start, int stop, byte[] data) {
        int len = stop - start + 1;
        byte[] dataR = new byte[len];
        for (int i = 0; i < dataR.length; i++) {
            dataR[i] = data[start + i];
        }
        return dataR;
    }

    // private Timer timer;
    // private TimerTask task;
    // private void repeatSendTimer() {
    // timer = new Timer();
    // task = new TimerTask() {
    // @Override
    // public void run() {
    // int i = 0;
    // do {
    // repeatSend();
    // i++;
    // SystemClock.sleep(500);
    // } while (i<2);
    // }
    // };
    // timer.schedule(task, 500);
    // }

    // private void cancelRepeatSendTimer(){
    // if(task != null){
    // task.cancel();
    // timer.cancel();
    // }
    // }

    // private void repeatSend() {
    // for (Iterator<Integer> it = commandCountMap.keySet().iterator();it.hasNext();){
    // Object key = it.next();
    // if(commandCountMap.get(key) != null){
    // comm.sendData(commandCountMap.get(key));
    // }
    // }
    // }

    @Override
    public void packageData(String mac, byte[] command) {
        // cancelRepeatSendTimer();
        commandCountMap.clear();
        int len = command.length + 2;
        int lenFull = len + 3;

        byte[] commandtemp = new byte[lenFull];
        commandtemp[0] = trasmitHead;
        commandtemp[1] = (byte) len;
        commandtemp[2] = (byte) 0x00;
        commandtemp[3] = (byte) commandSequenceID;
        for (int i = 0; i < command.length; i++) {
            commandtemp[4 + i] = command[i];
        }
        commandtemp[lenFull - 1] = generateCKS(commandtemp);
        comm.sendData(null, commandtemp);
        commandCountMap.put((commandSequenceID & 0xFF), commandtemp);
        sendSequenceId = commandSequenceID & 0xFF;
        addSeqID();
        addSeqID();
        repeatSendTimer();
    }

    @Override
    public void setInsSet(NewDataCallback dataCallback) {
        // TODO Auto-generated method stub

    }

    public void packageDataWithoutProtocol(byte[] command) {
        comm.sendData(null, command);
    }

    /*
     * (non-Javadoc)
     * @see com.ihealth.communication.ins.BaseCommProtocol#packageData(byte[])
     */
    @Override
    public void packageData(byte[] ins) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unPackageDataUuid(String uuid, byte[] data) {
        
    }

    @Override
    public void packageDataFinish() {

    }

    @Override
    public void destroy() {

    }
}
