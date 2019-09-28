package com.ihealth.communication.ins;

import android.content.Context;

import com.ihealth.communication.base.ble.BleConfig;
import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BleCommContinueProtocol;
import com.ihealth.communication.control.BtmProfile;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * BtmInsSet
 * <p>
 * Created by apple on 1/13/16.
 */
public class BtmInsSet implements NewDataCallback {
    private static final String TAG = "BtmInsSet";
    private BleCommContinueProtocol mbleCommContinueProtocol;
    private Context mContext;
    private BaseComm mComm;
    private String mAddress;
    private String mType;
    private InsCallback mInsCallback;

    public BtmInsSet(Context context, BaseComm com, String userName, String mac, String type, InsCallback insCallback, BaseCommCallback baseCommCallback) {
        Log.p(TAG, Log.Level.INFO, "BtmInsSet_Constructor", userName, mac, type);
        this.mContext = context;
        this.mComm = com;
        this.mAddress = mac;
        this.mType = type;
        this.mInsCallback = insCallback;
        this.mbleCommContinueProtocol = new BleCommContinueProtocol(com, mac, this);
    }

    public void getBattery() {
        Log.p(TAG, Log.Level.INFO, "getBattery");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        int year = calendar.get(Calendar.YEAR) - 2000;
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);

        byte[] returnCommand = new byte[8];
        returnCommand[0] = (byte) 0xd0;
        returnCommand[1] = (byte) 0xa0;
        returnCommand[2] = (byte) year;
        returnCommand[3] = (byte) month;
        returnCommand[4] = (byte) day;
        returnCommand[5] = (byte) hour;
        returnCommand[6] = (byte) min;
        returnCommand[7] = generateCKS(returnCommand);
        mComm.sendData(mAddress, returnCommand);
    }

    public void setStandbyTime(int hour, int minute, int second) {
        Log.p(TAG, Log.Level.INFO, "setStandbyTime", hour, minute, second);
        byte[] returnCommand = new byte[6];
        returnCommand[0] = (byte) 0xd0;
        returnCommand[1] = (byte) 0xa1;
        returnCommand[2] = (byte) hour;
        returnCommand[3] = (byte) minute;
        returnCommand[4] = (byte) second;
        returnCommand[5] = generateCKS(returnCommand);
        mComm.sendData(mAddress, returnCommand);

    }

    public void setTemperatureUnit(byte unit) {
        Log.p(TAG, Log.Level.INFO, "setTemperatureUnit", unit);
        byte[] returnCommand = new byte[4];
        returnCommand[0] = (byte) 0xd0;
        returnCommand[1] = (byte) 0xa2;
        returnCommand[2] = unit;
        returnCommand[3] = generateCKS(returnCommand);
        mComm.sendData(mAddress, returnCommand);

    }

    public void setMeasuringTarget(byte target) {
        Log.p(TAG, Log.Level.INFO, "setMeasuringTarget", target);
        byte[] returnCommand = new byte[4];
        returnCommand[0] = (byte) 0xd0;
        returnCommand[1] = (byte) 0xa3;
        returnCommand[2] = target;
        returnCommand[3] = generateCKS(returnCommand);
        mComm.sendData(mAddress, returnCommand);

    }

    public void setOfflineTarget(byte target) {
        Log.p(TAG, Log.Level.INFO, "setOfflineTarget", target);
        byte[] returnCommand = new byte[4];
        returnCommand[0] = (byte) 0xd0;
        returnCommand[1] = (byte) 0xa4;
        returnCommand[2] = target;
        returnCommand[3] = generateCKS(returnCommand);
        mComm.sendData(mAddress, returnCommand);

    }

    public void getMemoryData() {
        Log.p(TAG, Log.Level.INFO, "getMemoryData");
        byte[] returnCommand = new byte[3];
        returnCommand[0] = (byte) 0xd0;
        returnCommand[1] = (byte) 0xa5;
        returnCommand[2] = generateCKS(returnCommand);
        mComm.sendData(mAddress, returnCommand);

    }

    private void sendErrorData() {
        byte[] returnCommand = new byte[3];
        returnCommand[0] = (byte) 0xd0;
        returnCommand[1] = (byte) 0xa6;
        returnCommand[2] = generateCKS(returnCommand);
        mComm.sendData(mAddress, returnCommand);

    }

    private void sendConfirmData() {
        byte[] returnCommand = new byte[3];
        returnCommand[0] = (byte) 0xd0;
        returnCommand[1] = (byte) 0xa7;
        returnCommand[2] = generateCKS(returnCommand);
        mComm.sendData(mAddress, returnCommand);

    }

    @Override
    public void haveNewData(int what, int stateId, byte[] command) {
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", what, stateId, ByteBufferUtil.Bytes2HexString(command));
    }

    private int count;
    private int currentCount;
    private JSONArray memoryArray = new JSONArray();

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", uuid, ByteBufferUtil.Bytes2HexString(command));
        if (uuid.equals(BleConfig.UUID_BTM_READ_DATA_CHARACTERISTIC)) {
            //判断命令头
            if ((command[0] & 0xFF) != 0xD0) {
                sendErrorData();
                return;
            }
            int commandId = command[1] & 0xff;
            switch (commandId) {
                case 0xB0:
                    //发送确认指令
                    sendConfirmData();
                    boolean power = ((command[2] & 0xff) == 0xC4);
                    String battery = "Normal voltage";
                    if (power) {
                        battery = "Low Voltage";
                    }
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put(BtmProfile.BTM_BATTERY, battery);
                        mInsCallback.onNotify(mAddress, mType, BtmProfile.ACTION_BTM_BATTERY, jsonObject.toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                    break;
                case 0xB1:
                    //发送确认指令
                    sendConfirmData();
                    try {
                        //TODO 解析温度数据
                        //4e04
                        //0100 1110 0000 0100
                        JSONObject data = new JSONObject();
                        if (("" + (byte) ((command[2] >> 7) & 0x1) + (byte) ((command[2] >> 6) & 0x1)).equals("01")) {
                            //人体
                            data.put(BtmProfile.BTM_TEMPERATURE_TARGET, "body");
                        } else {
                            //物体
                            data.put(BtmProfile.BTM_TEMPERATURE_TARGET, "object");
                        }
                        float temp = (float) (((((command[2] & 0xFF) << 8) + (command[3] & 0xFF)) & 0x0FFF) / 100.0);
                        data.put(BtmProfile.BTM_TEMPERATURE, temp);
                        mInsCallback.onNotify(mAddress, mType, BtmProfile.ACTION_BTM_MEASURE, data.toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                    break;
                case 0xB2:
                    count = command[2] & 0xff;
                    memoryArray = null;
                    memoryArray = new JSONArray();
                    currentCount = 0;
                    break;
                case 0xB3:
                    currentCount += 1;
                    if (currentCount < count) {
                        try {
                            memoryArray.put(decodeTemperature(command));
                        } catch (Exception e) {
                            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                        }
                    } else {
                        //发送确认指令
                        sendConfirmData();
                        try {
                            JSONObject memory = new JSONObject();
                            memory.put(BtmProfile.MEMORY_COUNT, count);
                            memory.put(BtmProfile.BTM_TEMPERATURE_ARRAY, memoryArray);
                            mInsCallback.onNotify(mAddress, mType, BtmProfile.ACTION_BTM_MEMORY, memory.toString());
                        } catch (Exception e) {
                            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                        }
                    }
                    break;
                case 0xB4:
                    //发送确认指令
                    sendConfirmData();
                    break;
                case 0xB6:
                    //TODO 重发
                    break;
                case 0xB7:
                    Log.i(TAG, ByteBufferUtil.Bytes2HexString(command));
                    mInsCallback.onNotify(mAddress, mType, BtmProfile.ACTION_BTM_CALLBACK, "收到响应");
                    break;
            }
        }
    }

    /**
     * 把byte转为字符串的bit
     */
    private String byteToBit(byte b) {
        return ""
                + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
                + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
                + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                + (byte) ((b >> 1) & 0x1) + (byte) ((b) & 0x1);
    }

    /**
     * This method decode temperature value received from Health Thermometer device
     */
    private JSONObject decodeTemperature(byte[] command) throws Exception {
        //非法数据
        JSONObject jsonObject = new JSONObject();
        if (command[7] == 0 && command[8] == 0) {
            return jsonObject;
        }
        int year = (command[2] & 0xff) + 2000;
        int month = command[3] & 0xff;
        int day = command[4] & 0xff;
        int hour = command[5] & 0xff;
        int min = command[6] & 0xff;
        String time = year + "-" + month + "-" + day + " " + hour + ":" + min;
        if (("" + (byte) ((command[7] >> 7) & 0x1) + (byte) ((command[7] >> 6) & 0x1)).equals("01")) {
            //人体
            jsonObject.put(BtmProfile.BTM_TEMPERATURE_TARGET, "body");
        } else {
            //物体
            jsonObject.put(BtmProfile.BTM_TEMPERATURE_TARGET, "object");
        }
        float temp = (float) (((((command[7] & 0xFF) << 8) + (command[8] & 0xFF)) & 0x0FFF) / 100.0);

        jsonObject.put(BtmProfile.BTM_MEASURE_TIME, time);
        jsonObject.put(BtmProfile.BTM_TEMPERATURE, temp);

        return jsonObject;
    }


    private byte generateCKS(byte[] command) {
        int sum = 0;
        for (int i = 0; i < command.length; i++) {
            sum = sum + command[i];
        }
        return (byte) sum;
    }

    public void destroy() {
        Log.p(TAG, Log.Level.INFO, "destroy");
        mComm = null;
        mInsCallback = null;
        mContext = null;
        if (mbleCommContinueProtocol != null)
            mbleCommContinueProtocol.destroy();
        mbleCommContinueProtocol = null;
    }

}
