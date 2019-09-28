/**
 * @title
 * @Description
 * @author
 * @date 2015年5月26日 下午1:36:02
 * @version V1.0
 */

package com.ihealth.communication.ins;

import android.content.Context;

import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.base.protocol.BleCommProtocol;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.control.ABPMControl;
import com.ihealth.communication.control.BpProfile;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Private API for Bp series devices that contain ABPM
 * 查功能信息、查电量、上传记忆条数、上传记忆数据、查测量时间段、设置测量时间段
 *
 * @hide
 */
public class A1InsSetforABPM extends IdentifyIns implements NewDataCallback {
    private static final String TAG = "A1InsSetforABPM";
    private static final byte deviceType = (byte) 0xa1;
    private Context mContext;
    private BaseCommProtocol btcm;
    private String mAddress;
    private String mType;
    public boolean getOfflineData = false;

    private BaseComm mBaseComm;
    private String mUserName;
    /* Product protocol callback */
    private InsCallback mInsCallback;

    /* Communication callback */
    private BaseCommCallback mBaseCommCallback;
    private A1DBtools mA1DBtools;

    /**
     * a constructor for A1InsSetforABPM.
     *
     * @param context
     * @param com
     * @param userName
     * @param mac
     * @param type
     * @param insCallback
     * @param mBaseCommCallback
     * @hide
     */
    public A1InsSetforABPM(Context context, BaseComm com, String userName, String mac, String type, InsCallback insCallback, BaseCommCallback mBaseCommCallback) {
        Log.p(TAG, Log.Level.INFO, "A1InsSetforABPM_Constructor", userName, mac, type);
        this.mContext = context;
        this.mAddress = mac;
        this.mType = type;

        this.btcm = new BleCommProtocol(context, com, mac, deviceType, this);
        offlineList = new ArrayList<>();

        this.mBaseComm = com;
        this.mUserName = userName;
        this.mInsCallback = insCallback;
        this.mBaseCommCallback = mBaseCommCallback;
        this.mA1DBtools = new A1DBtools();
        setInsSetCallbak(insCallback, mac, type, mBaseComm);
    }

    /**
     * Authentication
     *
     * @hide
     */
    public void identify() {
        Log.p(TAG, Log.Level.INFO, "identify");
        startTimeout(0xfa, AppsDeviceParameters.Delay_Medium, 0xfb, 0xfd, 0xfe);
        btcm.packageData(mAddress, identify(deviceType));
    }

    /**
     * Get battery of the current Bp device.
     *
     * @hide
     */
    public void getBatteryLevel() {
        Log.p(TAG, Log.Level.INFO, "getBatteryLevel");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x20;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) 0x00;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    /**
     * 下位机功能信息:产品类型、命令ID、年月日时分秒
     */
    public void getFunctionInfo() {
        Log.p(TAG, Log.Level.INFO, "getFunctionInfo");
        Calendar calenda = Calendar.getInstance();
        calenda.setTimeZone(TimeZone.getDefault());
        int year = calenda.get(Calendar.YEAR) - 2000;
        int month = calenda.get(Calendar.MONTH) + 1;
        int day = calenda.get(Calendar.DAY_OF_MONTH);
        int hour = calenda.get(Calendar.HOUR_OF_DAY);
        int min = calenda.get(Calendar.MINUTE);
        int sed = calenda.get(Calendar.SECOND);

        byte[] returnCommand = new byte[8];
        byte commandID = (byte) 0x21;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) year;
        returnCommand[3] = (byte) month;
        returnCommand[4] = (byte) day;
        returnCommand[5] = (byte) hour;
        returnCommand[6] = (byte) min;
        returnCommand[7] = (byte) sed;
        btcm.packageData(mAddress, returnCommand);
    }

    public void setDisplayUnit(int unit) {
        Log.p(TAG, Log.Level.INFO, "getFunctionInfo", unit);
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x26;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        if (unit == ABPMControl.UNIT_KPA) {
            returnCommand[2] = (byte) 0x55;
        } else if (unit == ABPMControl.UNIT_MMHG) {
            returnCommand[2] = (byte) 0x00;
        }
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    public void setMeasureTime(int length, boolean isMedicine, int[]... times) {
        Log.p(TAG, Log.Level.INFO, "setMeasureTime", length, isMedicine, times);
        byte[] returnCommand = new byte[20];
        byte commandID = (byte) 0x27;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) length;
        if (isMedicine) {
            returnCommand[3] = (byte) 0x02;
        } else {
            returnCommand[3] = (byte) 0x00;
        }

        int[][] data = new int[4][4];
        if (times.length == 1) {
            data[1] = new int[4];
            data[2] = new int[4];
            data[3] = new int[4];
        } else if (times.length == 2) {
            data[2] = new int[4];
            data[3] = new int[4];
        } else {
            data[3] = new int[4];
        }

        for (int i = 0; i < data.length; i++) {
            int[] time = data[i];
            for (int aTime : time) {
                if (aTime == 0) {
                    returnCommand[4 + i * 4] = (byte) 0xff;
                    returnCommand[5 + i * 4] = (byte) 0xff;
                    returnCommand[6 + i * 4] = (byte) 0xff;
                    returnCommand[7 + i * 4] = (byte) 0xff;
                } else {
                    returnCommand[4 + i * 4] = (byte) time[0];
                    returnCommand[5 + i * 4] = (byte) time[1];
                    returnCommand[6 + i * 4] = (byte) time[2];
                    returnCommand[7 + i * 4] = (byte) time[3];

                }
            }
        }
        btcm.packageData(mAddress, returnCommand);
    }

    /**
     * 查询测量时间段
     */
    public void getMeasureTime() {
        Log.p(TAG, Log.Level.INFO, "getMeasureTime");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x28;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) 0x00;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    public void setAlarm(int[]... args) {
        Log.p(TAG, Log.Level.INFO, "setAlarm", Arrays.toString(args));
        byte[] returnCommand = new byte[3 + args.length * 6];
        byte commandID = (byte) 0x29;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) 0x00;
        for (int i = 0; i < args.length; i++) {
            int[] arg = args[i];
            returnCommand[3 + i * 6] = (byte) arg[0];
            returnCommand[4 + i * 6] = (byte) arg[1];
            byte[] high = intToBytes2(arg[2]);
            returnCommand[5 + i * 6] = high[0];
            returnCommand[6 + i * 6] = high[1];
            byte[] low = intToBytes2(arg[3]);
            returnCommand[7 + i * 6] = low[0];
            returnCommand[8 + i * 6] = low[1];
        }
        btcm.packageData(mAddress, returnCommand);
    }

    public void getAlarmSetting() {
        Log.p(TAG, Log.Level.INFO, "getAlarmSetting");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x2a;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) 0x00;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    public void getAlarmType() {
        Log.p(TAG, Log.Level.INFO, "getAlarmType");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x2b;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) 0x00;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    /**
     * 查询记忆条数
     * 单组机型默认记忆组别为 1
     */
    public void getOffLineDataNum() {
        Log.p(TAG, Log.Level.INFO, "getOffLineDataNum");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x40;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) this.Memory_Size;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    private void getOffLineData() {
        Log.p(TAG, Log.Level.INFO, "getOffLineData");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x41;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) this.Memory_Size;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    public void deleteAllMemory() {
        Log.p(TAG, Log.Level.INFO, "deleteAllMemory");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x43;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) this.Memory_Size;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    /**
     * 将int数值转换为占2个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。
     */
    private static byte[] intToBytes2(int value) {
        byte[] src = new byte[2];
        src[0] = (byte) ((value >> 8) & 0xFF);
        src[1] = (byte) (value & 0xFF);
        return src;
    }

    private int Memory_Size = 1;

    public void setMemory_Size(int memory_Size) {
        Log.p(TAG, Log.Level.INFO, "setMemory_Size", memory_Size);
        Memory_Size = memory_Size;
    }


    private void allPkgOk(byte commandID) {
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(mAddress, returnCommand);
    }


    private byte[] activitys;

    private enum Command {
        Unknown(0),
        Verification_Feedback(0xfb),
        Verification_Success(0xfd),
        Verification_Failed(0xfe),
        Get_BatteryLevel(0x20),
        Get_FunctionInfo(0x21),
        Set_MeasureTime(0x27),
        Get_MeasureTime(0x28),
        Get_AlarmSetting(0x2A),
        Get_AlarmType(0x2B),
        Get_OffLineDataNum(0x40),
        Get_OffLineData(0x41),
        DeleteAllMemory_Finish(0x44),
        DeleteAllMemory_Confirm(0x45);
        int what;

        Command(int what) {
            this.what = what;
        }

        static Command parseCommand(int what) {
            for (Command command : values()) {
                if (command.what == what) {
                    return command;
                }
            }
            return Unknown;
        }

        @Override
        public String toString() {
            return String.format("%s(0x%02X)", name(), what);
        }
    }

    @Override
    public void haveNewData(int what, int stateId, byte[] returnData) {
        stopTimeout(what);
        Command command = Command.parseCommand(what);
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", command, stateId, ByteBufferUtil.Bytes2HexString(returnData));
        JSONObject jsonObject = new JSONObject();
        switch (what) {
            case 0xfb:
                byte[] req = deciphering(returnData, mType, deviceType);
                startTimeout(0xfc, AppsDeviceParameters.Delay_Medium, 0xfd, 0xfe);
                btcm.packageData(mAddress, req);
                break;
            case 0xfd:
                this.mBaseCommCallback.onConnectionStateChange(mAddress, mType, iHealthDevicesManager.DEVICE_STATE_CONNECTED, 0, null);
                break;
            case 0xfe:
                mBaseComm.disconnect();
                break;
            case 0x20:  //获取电量
                int batteryLevel = returnData[0] & 0xff;
                if (!((batteryLevel > 0) && (batteryLevel <= 100))) {
                    /* if the battery beyond 100, set battery to 100. */
                    batteryLevel = 100;
                }
                try {
                    jsonObject.put(BpProfile.BATTERY_BP, batteryLevel);
                    jsonObject.put(BpProfile.VOLTAGE_BP, returnData[1] & 0xff);
                    jsonObject.put(BpProfile.REMAINING_TIMES_BP, returnData[2] & 0xff);
                    mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_BATTERY_BP, jsonObject.toString());
                } catch (JSONException e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }

                break;
            case 0x21:
                //记忆组别
                if (returnData != null) {
                    int state = returnData[0] & 0xff;
                    boolean upAirMeasureFlg;    //上气、下气测量标志位 bit0
                    boolean armMeasureFlg;  //腕式、臂式 bit1
                    boolean haveAngleSensorFlg; //是否带角度 bit2
                    boolean havePowerOffFunc;   //是否带关机功能 bit3
                    boolean haveOfflineFlg; //是否带离线测量功能 bit4
                    boolean allowDeleteOffline; //是否允许删除记忆数据 bit5
                    boolean haveHSDFlg; //是否有HSD bit6
                    boolean automaticCycleMeasure;  //自动循环测量 还是单次测量 bit7

                    //bit0
                    if ((returnData[1] & 0x01) == 1) {
                        upAirMeasureFlg = true; //上气
                    } else {
                        upAirMeasureFlg = false; //下气
                    }
                    //bit1
                    if ((returnData[1] & 0x02) == 1) {
                        armMeasureFlg = true;   //臂式
                    } else {
                        armMeasureFlg = false;   //腕式
                    }
                    //bit2
                    if ((returnData[1] & 0x04) == 1) {
                        haveAngleSensorFlg = true;  //带角度测量
                    } else {
                        haveAngleSensorFlg = false;
                    }
                    //bit3
                    if ((returnData[1] & 0x08) == 1) {
                        havePowerOffFunc = true;
                    } else {
                        havePowerOffFunc = false;
                    }
                    //bit4
                    if ((returnData[1] & 0x10) == 1) {
                        haveOfflineFlg = true;  //带离线测量功能
                    } else {
                        haveOfflineFlg = false; //不带
                    }
                    //bit5
                    if ((returnData[1] & 0x20) == 1) {
                        allowDeleteOffline = true;
                    } else {
                        allowDeleteOffline = false;
                    }
                    //bit6
                    if ((returnData[1] & 0x40) == 1) {
                        haveHSDFlg = true;  //有HSD
                    } else {
                        haveHSDFlg = false; //没有
                    }
                    //bit7
                    if ((returnData[1] & 0x80) == 1) {
                        automaticCycleMeasure = true;
                    } else {
                        automaticCycleMeasure = false;
                    }
                    //记忆组别
                    int memorySize = returnData[2] & 0xff;
                    //组最大记忆容量
                    int maxMemoryCapacity = returnData[3] & 0xff;

                    boolean haveShowUnitMeasure;            //有无显示单位设置 bit4
                    int showUnit;
                    //bit4
                    if ((returnData[4] & 0x10) == 1) {
                        haveShowUnitMeasure = true;
                    } else {
                        haveShowUnitMeasure = false;
                    }
                    //bit5
                    showUnit = returnData[4] & 0x20;    //1为kPa,0为mmHg

                    boolean onlyUpdateMemory;               //一次上传一组还是多组记忆 bit1
                    boolean isAutoUpdate;                   //是否支持自升级 bit2
                    boolean haveActivityDetection;          //有无活动量检测 bit3
                    boolean haveAlarmSetting;               //有无报警设置 bit4

                    //bit1
                    if ((returnData[6] & 0x02) == 1) {
                        onlyUpdateMemory = true;   //多组
                    } else {
                        onlyUpdateMemory = false;   //一组
                    }
                    //bit2
                    if ((returnData[6] & 0x04) == 1) {
                        isAutoUpdate = true;  //支持
                    } else {
                        isAutoUpdate = false;   //不支持
                    }
                    //bit3
                    if ((returnData[6] & 0x08) == 1) {
                        haveActivityDetection = true;
                    } else {
                        haveActivityDetection = false;
                    }
                    //bit4
                    if ((returnData[6] & 0x10) == 1) {
                        haveAlarmSetting = true;  //带离线测量功能
                    } else {
                        haveAlarmSetting = false; //不带
                    }

                    try {
                        jsonObject.put(BpProfile.FUNCTION_OPERATING_STATE, state);
                        jsonObject.put(BpProfile.FUNCTION_IS_UPAIR_MEASURE, upAirMeasureFlg);
                        jsonObject.put(BpProfile.FUNCTION_IS_ARM_MEASURE, armMeasureFlg);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_ANGLE_SENSOR, haveAngleSensorFlg);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_POWEROFF, havePowerOffFunc);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_OFFLINE, haveOfflineFlg);
                        jsonObject.put(BpProfile.FUNCTION_ALLOW_DELETE_OFFLINEDATA, allowDeleteOffline);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_HSD, haveHSDFlg);
                        jsonObject.put(BpProfile.FUNCTION_IS_AUTOCYCLE_MEASURE, automaticCycleMeasure);
                        jsonObject.put(BpProfile.MEMORY_GROUP_BP, memorySize);
                        jsonObject.put(BpProfile.FUNCTION_MAX_MEMORY_CAPACITY, maxMemoryCapacity);
                        //添加
                        jsonObject.put(BpProfile.FUNCTION_HAVE_SHOW_UNIT_SETTING, haveShowUnitMeasure);
                        jsonObject.put(BpProfile.FUNCTION_SHOW_UNIT, showUnit);
                        jsonObject.put(BpProfile.FUNCTION_ONLY_UPDATE_MEMORY, onlyUpdateMemory);
                        jsonObject.put(BpProfile.FUNCTION_IS_AUTO_UPDATE, isAutoUpdate);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_ACTIVITY_DETECTION, haveActivityDetection);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_ALARM_SETTING, haveAlarmSetting);

                        mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_FUNCTION_INFORMATION_BP, jsonObject.toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                }

                break;
            case 0x27:
                break;
            case 0x28:
                if (returnData != null) {
                    boolean isCompletedMeasurement;    //测量过程是否完成 bit0
                    boolean isMedicine;                 //是否服药 bit1
                    //bit0
                    if ((returnData[0] & 0x01) == 1) {
                        isCompletedMeasurement = true;
                    } else {
                        isCompletedMeasurement = false;
                    }
                    //bit1
                    if ((returnData[0] & 0x02) == 1) {
                        isMedicine = true;
                    } else {
                        isMedicine = false;
                    }

                    int measureTime = returnData[1] & 0xff;

                    int morningHour = returnData[2] & 0xff;
                    int morningMin = returnData[3] & 0xff;
                    int morningInterval = returnData[4] & 0xff;
                    boolean morningVibrationSwitch;
                    boolean morningSoundSwitch;
                    //bit0
                    if ((returnData[5] & 0x01) == 1) {
                        morningVibrationSwitch = true;
                    } else {
                        morningVibrationSwitch = false;
                    }
                    //bit1
                    if ((returnData[5] & 0x02) == 1) {
                        morningSoundSwitch = true;
                    } else {
                        morningSoundSwitch = false;
                    }
                    JSONArray morningSetting = new JSONArray();
                    morningSetting.put(morningHour);
                    morningSetting.put(morningMin);
                    morningSetting.put(morningInterval);
                    morningSetting.put(morningVibrationSwitch);
                    morningSetting.put(morningSoundSwitch);

                    int nightHour = returnData[6] & 0xff;
                    int nightMin = returnData[7] & 0xff;
                    int nightInterval = returnData[8] & 0xff;
                    boolean nightVibrationSwitch;
                    boolean nightSoundSwitch;
                    //bit0
                    if ((returnData[9] & 0x01) == 1) {
                        nightVibrationSwitch = true;
                    } else {
                        nightVibrationSwitch = false;
                    }
                    //bit1
                    if ((returnData[9] & 0x02) == 1) {
                        nightSoundSwitch = true;
                    } else {
                        nightSoundSwitch = false;
                    }
                    JSONArray nightSetting = new JSONArray();
                    nightSetting.put(nightHour);
                    nightSetting.put(nightMin);
                    nightSetting.put(nightInterval);
                    nightSetting.put(nightVibrationSwitch);
                    nightSetting.put(nightSoundSwitch);

                    int noonRestHour = returnData[10] & 0xff;
                    int noonRestMin = returnData[11] & 0xff;
                    int noonRestInterval = returnData[12] & 0xff;
                    boolean noonRestVibrationSwitch;
                    boolean noonRestSoundSwitch;
                    //bit0
                    if ((returnData[13] & 0x01) == 1) {
                        noonRestVibrationSwitch = true;
                    } else {
                        noonRestVibrationSwitch = false;
                    }
                    //bit1
                    if ((returnData[13] & 0x02) == 1) {
                        noonRestSoundSwitch = true;
                    } else {
                        noonRestSoundSwitch = false;
                    }
                    JSONArray noonRestSetting = new JSONArray();
                    noonRestSetting.put(noonRestHour);
                    noonRestSetting.put(noonRestMin);
                    noonRestSetting.put(noonRestInterval);
                    noonRestSetting.put(noonRestVibrationSwitch);
                    noonRestSetting.put(noonRestSoundSwitch);

                    int noonGetupHour = returnData[14] & 0xff;
                    int noonGetupMin = returnData[15] & 0xff;
                    int noonGetupInterval = returnData[16] & 0xff;
                    boolean noonGetupVibrationSwitch;
                    boolean noonGetupSoundSwitch;
                    //bit0
                    if ((returnData[17] & 0x01) == 1) {
                        noonGetupVibrationSwitch = true;
                    } else {
                        noonGetupVibrationSwitch = false;
                    }
                    //bit1
                    if ((returnData[17] & 0x02) == 1) {
                        noonGetupSoundSwitch = true;
                    } else {
                        noonGetupSoundSwitch = false;
                    }
                    JSONArray noonGetupSetting = new JSONArray();
                    noonGetupSetting.put(noonGetupHour);
                    noonGetupSetting.put(noonGetupMin);
                    noonGetupSetting.put(noonGetupInterval);
                    noonGetupSetting.put(noonGetupVibrationSwitch);
                    noonGetupSetting.put(noonGetupSoundSwitch);

                    try {
                        jsonObject.put(BpProfile.MEASURE_IS_COMPLETE_BP, isCompletedMeasurement);
                        jsonObject.put(BpProfile.MEASURE_IS_MEDICINE_BP, isMedicine);
                        jsonObject.put(BpProfile.GET_MEASURE_TIME_BP, measureTime);
                        jsonObject.put(BpProfile.GET_FIRST_TIME_BP, morningSetting);
                        jsonObject.put(BpProfile.GET_SECOND_TIME_BP, nightSetting);
                        jsonObject.put(BpProfile.GET_THIRD_TIME_BP, noonRestSetting);
                        jsonObject.put(BpProfile.GET_FORTH_TIME_BP, noonGetupSetting);

                        mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_GET_CYCLE_MEASURE, jsonObject.toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                }
                break;
            case 0x2a:
                if (returnData != null) {
                    activitys = ByteBufferUtil.BufferMerger(activitys, ByteBufferUtil.bytesCutt(1, returnData.length - 1, returnData));
                    if (returnData[0] == 0) {   //数据分包顺序号
                        if (activitys != null) {
                            convertAlarmSetting(activitys);
                            activitys = null;
                        }
                    } else {
                        getAlarmSetting();
                    }
                }
                break;
            case 0x2b:
                if (returnData != null) {
                    boolean isHighAlarm;    //是否支持高压生理报警 bit0
                    boolean isLowAlarm;  //是否支持低压生理报警 bit1
                    boolean isAverage; //是否支持平均压生理报警 bit2
                    boolean isHeartRate;   //是否支持心率生理报警 bit3
                    boolean isExcess; //是否支持血压超量程报警 bit4
                    boolean isOther; //是否支持其他技术报警 bit5

                    //bit0
                    if ((returnData[0] & 0x01) == 1) {
                        isHighAlarm = true;
                    } else {
                        isHighAlarm = false;
                    }
                    //bit1
                    if ((returnData[0] & 0x02) == 1) {
                        isLowAlarm = true;
                    } else {
                        isLowAlarm = false;
                    }
                    //bit2
                    if ((returnData[0] & 0x04) == 1) {
                        isAverage = true;
                    } else {
                        isAverage = false;
                    }
                    //bit3
                    if ((returnData[0] & 0x08) == 1) {
                        isHeartRate = true;
                    } else {
                        isHeartRate = false;
                    }
                    //bit4
                    if ((returnData[0] & 0x10) == 1) {
                        isExcess = true;
                    } else {
                        isExcess = false;
                    }
                    //bit5
                    if ((returnData[0] & 0x20) == 1) {
                        isOther = true;
                    } else {
                        isOther = false;
                    }

                    try {
                        jsonObject.put(BpProfile.ALARM_TYPE_HIGH_BP, isHighAlarm);
                        jsonObject.put(BpProfile.ALARM_TYPE_LOW_BP, isLowAlarm);
                        jsonObject.put(BpProfile.ALARM_TYPE_AVERAGE_BP, isAverage);
                        jsonObject.put(BpProfile.ALARM_TYPE_HEART_RATE_BP, isHeartRate);
                        jsonObject.put(BpProfile.ALARM_TYPE_EXCESS_BP, isExcess);
                        jsonObject.put(BpProfile.ALARM_TYPE_OTHER_BP, isOther);

                        mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_ALARM_TYPE_BP, jsonObject.toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                }
                break;

            case 0x40:
                int num = returnData[1] & 0xff;
                if (num == 0 && getOfflineData) {            //获取数据
                    mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_HISTORICAL_DATA_BP, new JSONObject().toString());
                } else if (num != 0 && getOfflineData) {    //获取数据
                    getOffLineData();
                } else if (!getOfflineData) {            //获取数量
                    try {
                        jsonObject.put(BpProfile.HISTORICAL_NUM_BP, num);
                        mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_HISTORICAL_NUM_BP, jsonObject.toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                }
                break;
            case 0x41:
                // cancelRepeat();
                activitys = ByteBufferUtil.BufferMerger(activitys,
                        ByteBufferUtil.bytesCutt(2, returnData.length - 1, returnData));
                if (returnData[1] == 0) {   //数据分包顺序号
                    if (activitys != null) {
                        convertOffline(activitys);
                        activitys = null;
                    }
                } else {
                    getOffLineData();
                }
                break;

            case 0x38:
                int errorNum = returnData[0] & 0xff;
                try {
                    jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
                    jsonObject.put(BpProfile.ERROR_NUM_BP, errorNum);
                    mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_ERROR_BP, jsonObject.toString());
                } catch (JSONException e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                break;
            case 0xff:
                identify();
                break;
            default:
                Log.p(TAG, Log.Level.WARN, "Exception", "no method");
                break;
        }
    }

    private void convertAlarmSetting(byte[] activities) {
        JSONObject eachData;
        JSONObject jsonObject = new JSONObject();
        JSONArray eachArray = new JSONArray();

        int num = (activities.length) / 6;
        int index = 0;
        for (int i = 0; i < num; i++) {
            eachData = new JSONObject();
            int type = activities[i + index];
            boolean isSoundAlarm;
            boolean isVisualAlarm;
            boolean isVibrationAlarm;
            //bit0
            if ((activities[i + index + 1] & 0x01) == 1) {
                isSoundAlarm = true;
            } else {
                isSoundAlarm = false;
            }
            //bit1
            if ((activities[i + index + 1] & 0x02) == 1) {
                isVisualAlarm = true;
            } else {
                isVisualAlarm = false;
            }
            //bit2
            if ((activities[i + index + 1] & 0x04) == 1) {
                isVibrationAlarm = true;
            } else {
                isVibrationAlarm = false;
            }
            int high = bytesToInt(new byte[]{activities[i + index + 2], activities[i + index + 3]});
            int low = bytesToInt(new byte[]{activities[i + index + 4], activities[i + index + 5]});
            try {
                eachData.put(BpProfile.ALARM_TYPE_BP, type);
                eachData.put(BpProfile.IS_SOUND_ALARM_BP, isSoundAlarm);
                eachData.put(BpProfile.IS_VISUAL_ALARM_BP, isVisualAlarm);
                eachData.put(BpProfile.IS_VIBRATION_ALARM_BP, isVibrationAlarm);
                eachData.put(BpProfile.ALARM_HIGH_BP, high);
                eachData.put(BpProfile.ALARM_LOW_BP, low);
                eachArray.put(eachData);
            } catch (JSONException e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            index += 5;
        }
        try {
            jsonObject.putOpt(BpProfile.ALARM_SETTING_BP, eachArray);
            mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_ALARM_SETTING_BP, jsonObject.toString());
        } catch (JSONException e) {
            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
        }
    }

    /**
     * 基于位移的 byte[]转化成int
     *
     * @param bytes bytes
     * @return int  number
     */

    private static int bytesToInt(byte[] bytes) {
        return bytes[1] & 0xFF |
                (bytes[0] & 0xFF) << 8;
    }

    private void convertOffline(byte[] datas) {
        JSONObject eachData;
        JSONObject jsonObject = new JSONObject();
        JSONArray eachArray = new JSONArray();

        offlineList.clear();
        int num = (datas.length) / 9;
        int index = 0;
        for (int i = 0; i < num; i++) {
            String str = "";
            //AHR
            int temp = 0;
            //HSD
            int temp2 = 0;
            //角度信息 协议里没有
            int angInfo = 0;
            for (int j = 0; j < 9; j++) {
                if (j == 0) {
                    if (datas[j + index] < 0) {
                        temp = 1;
                    } else {
                        temp = 0;
                    }
                    str += (datas[j + index] & 0x7f) + ","; //年
                } else if (j == 1) {
                    if (datas[j + index] < 0) {
                        temp2 = 1;
                    } else {
                        temp2 = 0;
                    }
                    str += (datas[j + index] & 0x7f) + ","; //月
                } else if (j == 2) {                        //日
                    angInfo = angInfo | ((datas[j + index] & 0xE0) >> 5);
                    str += (datas[j + index] & 0xff) + ",";
                } else if (j == 3) {                        //时
                    angInfo = angInfo | ((datas[j + index] & 0xE0) >> 2);
                    str += (datas[j + index] & 0xff) + ",";
                } else if (j == 4) {                        //分
                    angInfo = angInfo | ((datas[j + index] & 0xC0));
                    str += (datas[j + index] & 0xff) + ",";
                } else {
                    str += (datas[j + index] & 0xff) + ",";
                }
            }
            str += temp;
            str += "," + temp2 + ",";
            str += angInfo;
            index += 9;
            offlineList.add(str);
        }
        for (int i = 0; i < offlineList.size(); i++) {
            // byte[] datas = ByteBufferUtil.hexStringToByte(result.get(i));
            // int Sys = datas[5] & 0xff + datas[6] & 0xff;
            eachData = new JSONObject();

            int SYS = Integer.parseInt(offlineList.get(i).split(",")[5])
                    + Integer.parseInt(offlineList.get(i).split(",")[6]);
            int DIA = Integer.parseInt(offlineList.get(i).split(",")[6]);
            int Pulse = Integer.parseInt(offlineList.get(i).split(",")[7]);
            int activityAmount = Integer.parseInt(offlineList.get(i).split(",")[8]);   //活动量
            int IsArr = Integer.parseInt(offlineList.get(i).split(",")[9]);// 0齐，1不齐 AHR
            int HSD = Integer.parseInt(offlineList.get(i).split(",")[10]); //HSD
            int angleInfo = Integer.parseInt(offlineList.get(i).split(",")[11]); //角度信息
            byte data0 = (byte) Integer.parseInt(offlineList.get(i).split(",")[0]);
            byte data1 = (byte) Integer.parseInt(offlineList.get(i).split(",")[1]);
            byte data2 = (byte) Integer.parseInt(offlineList.get(i).split(",")[2]);
            int hands = (data2 & 0xc0) >> 6;
            //??
            int angle = (byte) Integer.parseInt(offlineList.get(i).split(",")[8]);
            int angleChange = (byte) Integer.parseInt(offlineList.get(i).split(",")[9]);

            String str_offlineDate = Integer.parseInt("20" + offlineList.get(i).split(",")[0]) + "-"
                    + (data1 & 0x7f) + "-"
                    + (data2 & 0x3f) + " "
                    + Integer.parseInt(offlineList.get(i).split(",")[3]) + ":"
                    + Integer.parseInt(offlineList.get(i).split(",")[4]) + ":00";
            long ts = System.currentTimeMillis() / 1000;
            try {
                eachData.put(BpProfile.MEASUREMENT_DATE_BP, str_offlineDate);
                eachData.put(BpProfile.HIGH_BLOOD_PRESSURE_BP, SYS);
                eachData.put(BpProfile.LOW_BLOOD_PRESSURE_BP, DIA);
                eachData.put(BpProfile.PULSEWAVE_BP, Pulse);
                eachData.put(BpProfile.MEASUREMENT_AHR_BP, IsArr);
                eachData.put(BpProfile.MEASUREMENT_HSD_BP, HSD);
                eachData.put(BpProfile.ANGLE_BP, angleInfo);             //AV23协议 添加
                eachData.put(BpProfile.ACTIVITY_AMOUNT_BP, activityAmount);        //AV23协议 添加

                eachData.put(BpProfile.DATAID, MD5.md5String(PublicMethod.getBPDataID(mAddress, Pulse + "", ts)));
                eachArray.put(eachData);
            } catch (JSONException e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            mA1DBtools.save(mContext, mUserName, mAddress, mType, SYS, DIA, Pulse, ts);
        }
        try {
            jsonObject.putOpt(BpProfile.HISTORICAL_DATA_BP, eachArray);
            mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_HISTORICAL_DATA_BP, jsonObject.toString());
        } catch (JSONException e) {
            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
        }
    }

    private ArrayList<String> offlineList;

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {

    }

    public void destroy() {
        Log.p(TAG, Log.Level.INFO, "destroy");
        if (btcm != null)
            btcm.destroy();
        btcm = null;
        mContext = null;
        mBaseComm = null;
    }
}
