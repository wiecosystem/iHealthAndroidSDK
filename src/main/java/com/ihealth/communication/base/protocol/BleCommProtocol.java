
package com.ihealth.communication.base.protocol;

import android.content.Context;
import android.os.SystemClock;

import com.ihealth.communication.base.ble.BleUnpackageData;
import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.utils.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class BleCommProtocol implements BaseCommProtocol {

    private static final String TAG = "BleCommProtocol";

    private static final byte trasmitHead = (byte) 0xb0;
    private int commandSequenceId = 1;
    private BaseComm mBaseComm;
    private BleUnpackageData mBleUnpackageData;
    private String mAddress;
    private byte mType;
    private Context mContext;

    public BleCommProtocol(Context context, BaseComm com, String mac, byte type, NewDataCallback commCallback) {
        this.mBaseComm = com;
        this.mType = type;
        this.mBleUnpackageData = new BleUnpackageData();
        mBleUnpackageData.addBleCommCallback(commCallback);
        this.mAddress = mac;
        mContext = context;
        mBaseComm.addCommNotify(mac, this);
    }

    private synchronized byte getSequenceId() {
        return (byte) (commandSequenceId & 0xff);
    }

    private synchronized void addSequenceId(int index) {
        this.commandSequenceId = this.commandSequenceId + index;
    }

    private synchronized void setSequenceId(int sequenceId) {
        this.commandSequenceId = sequenceId;
    }

    private Queue<Byte> dataGueue = new LinkedList<Byte>();
    private Queue<byte[]> commandGueue = new LinkedList<byte[]>();

    //jing 20161006 加锁,防止dataGueue在使用的时候被clear,导致的bug:java.lang.NullPointerException: Attempt to invoke virtual method 'byte java.lang.Byte.byteValue()' on a null object reference
    public synchronized void packageData(String mac, byte[] ins) {
        dataGueue.clear();
        for (byte b : ins) {
            dataGueue.offer(b);
        }
        if (dataGueue.size() <= 15) {
            directSend(mac);
        } else {
            splitSend(mac);
        }
    }

    private static final int UNKOWN_SUPPORTCHARACTERWRITE = 2;
    private static final int NOT_SUPPORTCHARACTERWRITE = 3;
    private static final int SUPPORTCHARACTERWRITE = 4;
    private int isSupportOnCharacterWrite = UNKOWN_SUPPORTCHARACTERWRITE;

    private void splitSend(String mac) {
        byte productId = dataGueue.poll();
        int size = dataGueue.size();
        int[] count = splitCount(size);
        int temp1 = ((count.length - 1) << 4);
        int temp2 = (count.length - 1);
        List<byte[]> list = new ArrayList<byte[]>();
        for (int i = 0; i < count.length; i++) {
            int len = count[i];
            byte[] commandtemp = new byte[len + 6];
            commandtemp[0] = trasmitHead;
            commandtemp[1] = (byte) (len + 3);
            commandtemp[2] = (byte) (temp1 + temp2 - i);
            commandtemp[3] = getSequenceId();
            commandtemp[4] = productId;
            for (int j = 0; j < len; j++) {
                commandtemp[5 + j] = dataGueue.poll();
            }
            commandtemp[len + 5] = generateCKS(commandtemp);
            list.add(commandtemp);
            addSequenceId(2);
        }

        for (byte[] bs : list) {
            commandGueue.offer(bs);
        }
        if (!getSendStatus()) {
            setSendStatus(true);
            if (isSupportOnCharacterWrite == UNKOWN_SUPPORTCHARACTERWRITE) {
                sendTimer.schedule(sendTimerTask, 100);
//                Log.v(TAG, "UNKOWN_SUPPORTCHARACTERWRITE");
            } else if (isSupportOnCharacterWrite == NOT_SUPPORTCHARACTERWRITE) {
                sendTimer.schedule(sendTimerTask, 10);
            } else {
                byte[] bs = commandGueue.poll();
                int sequenceId = (bs[3] & 0xff);
                mapTimeoutCommand.put(sequenceId, bs);
                mBaseComm.sendData(mAddress, bs);
            }
        }
        cancelTimerOutTask();
        addTimerOutTask();

    }

    private int[] splitCount(int size) {
        int time = size / 14 + 1;
        int off = size % 14;
        int[] times = new int[time];
        for (int i = 0; i < time - 1; i++) {
            times[i] = 14;
        }
        times[time - 1] = off;
        return times;
    }

    private void directSend(String mac) {
        int len = dataGueue.size() + 2;
        int lenFull = len + 3;
        byte[] commandtemp = new byte[lenFull];
        commandtemp[0] = trasmitHead;
        commandtemp[1] = (byte) len;
        commandtemp[2] = 0x00;
        commandtemp[3] = getSequenceId();
        for (int i = 0; i < (len - 2); i++) {
            commandtemp[4 + i] = dataGueue.poll();
        }
        commandtemp[lenFull - 1] = generateCKS(commandtemp);

        commandGueue.offer(commandtemp);
        addSequenceId(2);
        if (!getSendStatus()) {
            setSendStatus(true);
            if (isSupportOnCharacterWrite == NOT_SUPPORTCHARACTERWRITE) {
                sendTimer.schedule(sendTimerTask, 10);
            } else {
                byte[] command = commandGueue.poll();
                int sequenceId = (command[3] & 0xff);
                mapTimeoutCommand.put(sequenceId, command);
                mBaseComm.sendData(mAddress, command);
            }
        }
        cancelTimerOutTask();
        addTimerOutTask();

    }

    private byte generateCKS(byte[] command) {
        int sum = 0;
        for (int i = 2; i < command.length - 1; i++) {
            sum = sum + command[i];
        }
        return (byte) sum;
    }

    public synchronized void unPackageData(byte[] characteristicChangedValue) {

        if (characteristicChangedValue[0] != (byte) 0xA0) {
            Log.w(TAG, "head byte is not A0");
            return;
        }
        byte len = characteristicChangedValue[1];
        int lenR = characteristicChangedValue.length;
        if (lenR < 6) {
            Log.w(TAG, "command length is not wrong");
            return;
        }
        byte seqID = characteristicChangedValue[3];
        int tempSeqID = 0;
        if (seqID == 0) {
            tempSeqID = 255;
        } else {
            tempSeqID = (seqID & 0xFF) - 1;
        }
        int tempask = tempSeqID;
        if (lenR == 6) {
            mapTimeoutCommand.remove(tempSeqID);
        }
        if (len != lenR - 3) {
            Log.w(TAG, "This is not full command");
            return;
        }

        if (!checkCKS(2, lenR - 2, characteristicChangedValue)) {
            Log.w(TAG, "checksum is wrong");
            return;
        }
        if (characteristicChangedValue.length == 6) {
            return;
        }

        byte stateID = characteristicChangedValue[2];
        int off = stateID & 0x0F;
        //jing 20160907 这强制设置为false，会引发Nexus 6P的bug。 如果此时只有一包正在发送，则缓冲队列其实为null；此时再次发送，就会引发并行发送多包，导致发送失败的bug
//        if(commandGueue.size() == 0){
//            setSendStatus(false);
//        }
        if (characteristicChangedValue[2] != (byte) 0xf0) {
            packageACKlower((byte) (0xA0 + off), (byte) (tempask + 2));
        }
        mBleUnpackageData.unPackageData(characteristicChangedValue);
    }

    //jing 20160908  又发现一个bug，回复确认时，对预期顺序ID的处理错误。。。
    private void packageACKlower(byte stateID, byte sequenceId) {
        int cks = 0;
        byte[] commandSendTemp = new byte[6];
        commandSendTemp[0] = (byte) 0xB0;
        commandSendTemp[1] = (byte) 0x03;
        commandSendTemp[2] = stateID;
        commandSendTemp[3] = sequenceId;
        commandSendTemp[4] = mType;
        cks = commandSendTemp[2] + commandSendTemp[3] + commandSendTemp[4];
        commandSendTemp[5] = (byte) cks;
        commandGueue.offer(commandSendTemp);
        //计算预期顺序ID
        byte currentBag = (byte) (stateID & 0x0f);
        byte expectedId = (byte) ((sequenceId & 0xff) + currentBag * 2);
        setSequenceId(expectedId);
        addSequenceId(2);
        if (!getSendStatus()) {
            setSendStatus(true);
            if (isSupportOnCharacterWrite == NOT_SUPPORTCHARACTERWRITE) {
                sendTimer.schedule(sendTimerTask, 10);
            } else {
                mBaseComm.sendData(mAddress, commandGueue.poll());
            }
        } else {
//            Log.v("AndroidBle", "getSendStatus is true");
        }

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

    @Override
    public void setInsSet(NewDataCallback dataCallback) {
        mBleUnpackageData.addBleCommCallback(dataCallback);
    }

    @Override
    public void packageData(byte[] ins) {

    }

    @Override
    public void packageDataAsk(byte[] returnCommand) {

    }

    private boolean SendStatus = false;

    private void setSendStatus(boolean status) {
        this.SendStatus = status;
    }

    private boolean getSendStatus() {
        return SendStatus;
    }

    @Override
    public void packageDataFinish() {

        if (isSupportOnCharacterWrite == UNKOWN_SUPPORTCHARACTERWRITE) {
            sendTimerTask.cancel();
            sendTimer.cancel();
            isSupportOnCharacterWrite = SUPPORTCHARACTERWRITE;
        }
        Log.v(TAG, "packageDataFinish commandGueue.size():" + commandGueue.size());
        if (commandGueue.size() > 0) {
            byte[] data = commandGueue.poll();
            if (data.length > 6) {
                int sequenceId = (data[3] & 0xff);
                mapTimeoutCommand.put(sequenceId, data);
            }
            mBaseComm.sendData(mAddress, data);
        } else {
            setSendStatus(false);
        }
    }

    @Override
    public void unPackageDataUuid(String uuid, byte[] data) {

    }

    private final Timer sendTimer = new Timer();
    private final TimerTask sendTimerTask = new TimerTask() {
        @Override
        public void run() {
            while (!commandGueue.isEmpty()) {
                byte[] data = commandGueue.poll();

                mBaseComm.sendData(mAddress, data);
                SystemClock.sleep(10);
            }
            setSendStatus(false);
        }
    };

    private Map<Integer, byte[]> mapTimeoutCommand = Collections.synchronizedMap(new LinkedHashMap<Integer, byte[]>());
    private Map<Integer, Integer> mapTimeoutCommandCount = Collections.synchronizedMap(new LinkedHashMap<Integer, Integer>());

    private Timer sendTimeOutTimer = new Timer();
    private TimerTask sendTimeOutTask;

    private void addTimerOutTask() {
        sendTimeOutTask = new TimerTask() {
            @Override
            public void run() {
                repeatSend();
            }
        };
        sendTimeOutTimer.schedule(sendTimeOutTask, 300, 300);
    }

    private void cancelTimerOutTask() {
        if (sendTimeOutTask != null) {
            sendTimeOutTask.cancel();
            sendTimeOutTask = null;
        }
    }

    private void repeatSend() {
        try {
            for (Iterator<Integer> it = mapTimeoutCommand.keySet().iterator(); it.hasNext(); ) {
                Object key = it.next();
                byte[] command = mapTimeoutCommand.get(key);
                if (command != null) {
                    //jing 20160919  三次重发其实没起作用。。。。。。
                    int index = 0;
                    if (mapTimeoutCommandCount.containsKey(key)) {
                        index = mapTimeoutCommandCount.get(key);
                    }
                    if (index < 3) {
                        index += 1;
                        //jing 20160919  历史遗留问题,不知道为什么3次重发的次数没有存起来
                        mapTimeoutCommandCount.put((int) key, index);
                        commandGueue.offer(command);
                    } else {
                        if (mapTimeoutCommandCount.containsKey(key)) {
                            mapTimeoutCommandCount.remove(key);
                        }
                        mapTimeoutCommand.remove(key);

                        //20160731 jing  加包名限制，防止多个App间影响
//                        mContext.sendBroadcast(new Intent(iHealthDevicesManager.MSG_BASECOMMTIMEOUT).putExtra(iHealthDevicesManager.IHEALTH_DEVICE_MAC, mAddress).setPackage(mContext.getPackageName()));
                        cancelTimerOutTask();
//                        mBaseComm.disconnect(mAddress);
//                        break;
                    }
                }
            }
            if (mapTimeoutCommand.size() == 0) {
                cancelTimerOutTask();
            }
            if (!getSendStatus() && commandGueue.size() > 0) {
                setSendStatus(true);
                byte[] data = commandGueue.poll();
                if (data != null) {
                    mBaseComm.sendData(mAddress, data);
                }
            }
        } catch (Exception e) {
            mapTimeoutCommand.clear();
        }
    }

    @Override
    public void destroy() {
        if (mBaseComm != null)
            mBaseComm.removeCommNotify(mAddress);
        mBaseComm = null;
        mBleUnpackageData = null;
        mContext = null;
        if (mapTimeoutCommand != null)
            mapTimeoutCommand.clear();
        mapTimeoutCommand = null;
        if (mapTimeoutCommandCount != null)
            mapTimeoutCommandCount.clear();
        mapTimeoutCommandCount = null;
        if (commandGueue != null)
            commandGueue.clear();
        commandGueue = null;
        if (sendTimeOutTimer != null)
            sendTimeOutTimer.cancel();
        sendTimeOutTimer = null;
        cancelTimerOutTask();
    }

}
