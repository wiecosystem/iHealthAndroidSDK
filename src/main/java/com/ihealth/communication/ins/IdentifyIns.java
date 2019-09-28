
package com.ihealth.communication.ins;

import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.control.UpDeviceControl;
import com.ihealth.communication.control.UpgradeProfile;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.manager.iHealthDevicesUpgradeManager;
import com.ihealth.communication.utils.ByteBufferUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class IdentifyIns {

    private static final String TAG = "IdentifyIns";
    protected byte[] R1 = new byte[16];// 随机数R1——用于认证发给下位机
    protected byte[] R1_stroke = new byte[16];// R1'——用于保存下位机传上来的R1'
    protected byte[] R1_back = new byte[16];// R1——对下位机传上来的R1'进行解密后生成的R1，用于比较随机数R1

    protected byte[] R2_stroke = new byte[16];// R2'——用于保存下位机传上来的R2'
    protected byte[] R2 = new byte[16];// R2——对R2'解密后生成的R2
    protected byte[] deviceID = new byte[16];// 下位机传上来的产品ID
    protected byte[] K = new byte[16];

    GenerateKap mGenerateKap = new GenerateKap();

    //暴露在外的Key+
    private byte[] KeyOut = {
            0x43, 0x68, 0x2F, 0x48,
            0x51, 0x34, 0x4C, 0x7A,
            0x49, 0x74, 0x59, 0x54,
            0x34, 0x32, 0x73, 0x3D
    };

    protected byte[] identify(byte deviceType) {
        byte[] r1 = setR1();
        int len = r1.length + 2;
        byte[] returnCommand = new byte[len];
        byte commandID = (byte) 0xfa;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        for (int i = 2; i < returnCommand.length; i++) {
            returnCommand[i] = r1[i - 2];
        }
        return returnCommand;
    }

    public byte[] getDeviceId() {
        return deviceID;
    }

    protected byte[] deciphering(byte[] returnData, String strType, byte bType) {
        if (returnData.length == 48) {
            // 解析ID R1' 和 R2'
            for (int i = 0; i < 16; i++) {
                deviceID[i] = returnData[i];
                R1_stroke[i] = returnData[i + 16];
                R2_stroke[i] = returnData[i + 32];
            }
        }
        K = XXTEA.encrypt(reverseByteArray(deviceID), getKa(strType));
//        Log.v(TAG, "      k:" + ByteBufferUtil.Bytes2HexString(K));
        R1_back = XXTEA.encrypt(reverseByteArray(R1_stroke), K);
//        Log.v(TAG, "R1_back:" + ByteBufferUtil.Bytes2HexString(R1_back));
        R2 = XXTEA.encrypt(reverseByteArray(R2_stroke), K);
//        Log.v(TAG, "     R2:" + ByteBufferUtil.Bytes2HexString(R2));
        byte[] _R2 = reverseByteArray(R2);
//        Log.v(TAG, "    _R2:" + ByteBufferUtil.Bytes2HexString(_R2));
        return decipheringMessage(_R2, bType);
    }

    public byte[] getKa(String type) {
        byte[] ka = XXTEA.encrypt(swapByteArray(mGenerateKap.getKa(type)), swapByteArray(KeyOut));
//        Log.v(TAG, "value = \n" + ByteBufferUtil.Bytes2HexString(ka));//Ka
        return ka;
    }

    private byte[] decipheringMessage(byte[] r2, byte deviceType) {
        int len = r2.length + 2;
        byte[] returnCommand = new byte[len];
        byte commandID = (byte) 0xFC;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        for (int i = 2; i < returnCommand.length; i++) {
            returnCommand[i] = r2[i - 2];
        }
        return returnCommand;

    }

    protected byte[] reverseByteArray(byte[] data) {
        byte[] result = new byte[16];
        for (int i = 0; i < 4; i++) {
            result[i] = data[3 - i];
            result[i + 4] = data[7 - i];
            result[i + 8] = data[11 - i];
            result[i + 12] = data[15 - i];
        }
        return result;
    }

    protected byte[] setR1() {
        new Random(System.currentTimeMillis()).nextBytes(R1);
        for (int i = 0; i < 16; i++) {
            if (R1[i] < 0)
                R1[i] = (byte) (0 - R1[i]);
        }
//		Log.v(TAG, "R1: " + ByteBufferUtil.Bytes2HexString(R1));
        return reverseByteArray(R1);
    }

    protected byte[] swapByteArray(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (((data[i] & 0x0F) << 4) | ((data[i] & 0xF0) >> 4));
        }
        return result;
    }

    private InsCallback mInsCallback;
    private String mAdress;
    private String mType;
    private BaseComm baseComm;

    protected void setInsSetCallbak(InsCallback insSetCallbak, String mac, String type, BaseComm baseComm) {
        this.mInsCallback = insSetCallbak;
        this.mAdress = mac;
        this.mType = type;
        this.baseComm = baseComm;
    }

    private int[] setCommandId;
    private int setSendCommandId;

    private final Timer commandTimeoutTimer = new Timer();
    private TimerTask commandTimeoutTimerTask;

    protected void startTimeout(int sendCommandId, long delay, int... commandIds) {
        setCommandId = commandIds;
        setSendCommandId = sendCommandId;
        if (commandTimeoutTimerTask != null) {
            commandTimeoutTimerTask.cancel();
        }
        commandTimeoutTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (mInsCallback != null) {
                    if (commandTimeoutTimerTask != null)
                        commandTimeoutTimerTask.cancel();
                    commandTimeoutTimerTask = null;

                    boolean specialCommand = false;

                    //未通过加密认证的设置，直接断开连接
                    if (setSendCommandId >= 0xF0) {
                        specialCommand = true;
                        if (baseComm != null) {
                            baseComm.disconnect(mAdress);
                        }
                    } else if (setSendCommandId >= 0xD0 && setSendCommandId <= 0xD6) {
                        specialCommand = true;
                        //归还comm
                        UpDeviceControl mUpDeviceControl = iHealthDevicesUpgradeManager.getInstance().getUpDeviceControl(mAdress, mType);
                        if (mUpDeviceControl != null) {
                            mUpDeviceControl.returnComm();
                        }
                        //自升级异常，报错
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put(UpgradeProfile.DEVICE_UP_ERROR, 301);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mInsCallback.onNotify(mAdress, mType, UpgradeProfile.ACTION_DEVICE_ERROR, jsonObject.toString());
                    }

                    if (specialCommand == false) {
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put(iHealthDevicesManager.IHEALTH_COMM_TIMEOUT_COMMAND_ID, setSendCommandId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mInsCallback.onNotify(mAdress, mType, iHealthDevicesManager.IHEALTH_COMM_TIMEOUT, jsonObject.toString());
                    }
                }
            }
        };
        commandTimeoutTimer.schedule(commandTimeoutTimerTask, delay);
    }

    protected void stopTimeout(int commandId) {
        if (setCommandId == null)
            return;
        for (int i = 0; i < setCommandId.length; i++) {
            if (setCommandId[i] == commandId) {
                if (commandTimeoutTimerTask != null)
                    commandTimeoutTimerTask.cancel();
                commandTimeoutTimerTask = null;
                return;
            }
        }
    }

}
