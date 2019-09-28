/**
 * @title
 * @Description
 * @author
 * @date 2015年5月26日 下午1:36:02
 * @version V1.0
 */

package com.ihealth.communication.ins;

import android.content.Context;
import android.content.Intent;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.base.protocol.BleCommProtocol;
import com.ihealth.communication.base.protocol.Bp7sBtCommProtocol;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.control.BpProfile;
import com.ihealth.communication.manager.iHealthDevicesIDPS;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.Log;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Private API for Bp series devices that contain Bp7s,Bp550BT
 *
 * @hide
 */
public class A1InsSetforBp7s extends IdentifyIns implements NewDataCallback {
    private static final String TAG = "A1InsSetforBp7s";
    private static final byte deviceType = (byte) 0xa1;
    private Context mContext;
    private BaseCommProtocol btcm;
    private String mAddress;
    private String mType;
    private String hVer;
    private String fVer;
    private String manufacture;
    private String modeNumber;
    private String protocolString;
    private String accessoryName = "";
    public boolean getOfflineData = false;

    private BaseComm mBaseComm;
    private String mUserName;
    /* Product protocol callback */
    private InsCallback mInsCallback;

    /* Communication callback */
    private BaseCommCallback mBaseCommCallback;
    private A1DBtools mA1DBtools;

    /**
     * a constructor for A1InsSetforBp7s.
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
    public A1InsSetforBp7s(Context context, BaseComm com, String userName, String mac, String type, InsCallback insCallback, BaseCommCallback mBaseCommCallback) {
        Log.p(TAG, Log.Level.INFO, "A1InsSetforBp7s_Constructor", userName, mac, type);
        this.mContext = context;
        this.mAddress = mac;
        this.mType = type;
        if (type.equals(iHealthDevicesManager.TYPE_BP7S)) {
            this.btcm = new Bp7sBtCommProtocol(context, mac, type, com, this);
        } else {
            this.btcm = new BleCommProtocol(context, com, mac, deviceType, this);
        }
        offlineList = new ArrayList<>();

        this.mBaseComm = com;
        this.mUserName = userName;
        this.mInsCallback = insCallback;
        this.mBaseCommCallback = mBaseCommCallback;
        this.mA1DBtools = new A1DBtools();
        setInsSetCallbak(insCallback, mac, type, mBaseComm);
    }

    /**
     * Get device IDPS
     *
     * @hide
     */
    public void getIdps() {
        Log.p(TAG, Log.Level.INFO, "getIdps");
        byte[] returnCommand = new byte[2];
        byte commandID = (byte) 0xf1;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(mAddress, returnCommand);
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

    /**
     * @param @param unit:0 mmHg; 1: kPa
     * @return void
     * @Title: setUnit
     * @Description
     * @hide
     */
    public void setUnit(int unit) {
        Log.p(TAG, Log.Level.INFO, "setUnit", unit);
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x26;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        if (unit == 0) {
            returnCommand[2] = 0;
        } else {
            returnCommand[2] = 0x55;
        }
        returnCommand[3] = 0;
        returnCommand[4] = 0;
        btcm.packageData(mAddress, returnCommand);
    }

    // 仅支持单手测量，另一只手的上限和下限设置为0
    public void angleSet(byte leftUpper, byte leftLow, byte rightUpper, byte rightLow) {
        Log.p(TAG, Log.Level.INFO, "angleSet", leftUpper, leftLow, rightUpper, rightLow);
        byte[] returnCommand = new byte[6];
        byte commandID = (byte) 0x29;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = leftUpper;
        returnCommand[3] = leftLow;
        returnCommand[4] = rightUpper;
        returnCommand[5] = rightLow;
        btcm.packageData(mAddress, returnCommand);
    }

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

    private int Memory_Size = 1;

    public void setMemory_Size(int memory_Size) {
        Log.p(TAG, Log.Level.INFO, "setMemory_Size", memory_Size);
        Memory_Size = memory_Size;
    }

    private void getOffLineData() {
        Log.p(TAG, Log.Level.INFO, "getOffLineData");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x46;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) this.Memory_Size;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    public void offlineDataOver() {
        Log.p(TAG, Log.Level.INFO, "offlineDataOver");
        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x47;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) 0x00;
        returnCommand[3] = (byte) 0x00;
        returnCommand[4] = (byte) 0x00;
        btcm.packageData(mAddress, returnCommand);
    }

    private void allPkgOk(byte commandID) {
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(mAddress, returnCommand);
    }

    private byte[] activitys;

    @Override
    public void haveNewData(int what, int stateId, byte[] returnData) {
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", String.format("0x%02X", what), stateId, ByteBufferUtil.Bytes2HexString(returnData));
        stopTimeout(what);
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
            case 0x20:
                int batteryLevel = returnData[0] & 0xff;
                if (!((batteryLevel > 0) && (batteryLevel <= 100))) {
                    /* if the battery beyond 100, set battery to 100. */
                    batteryLevel = 100;
                }
                try {
                    jsonObject.put(BpProfile.BATTERY_BP, batteryLevel);
                    mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_BATTERY_BP, jsonObject.toString());
                } catch (JSONException e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }

                break;
            case 0x21:
                //记忆组别
                if (returnData != null) {
                    //
                    boolean upAirMeasureFlg;    //上气、下气测量标志位
                    boolean armMeasureFlg;  //腕式、臂式
                    boolean haveAngleSensorFlg; //是否带角度
                    boolean haveOfflineFlg; //是否带离线测量功能
                    boolean haveHSDFlg; //是否有HSD
                    boolean haveAngleSetFlg;    //是否带手腕角度设置
                    boolean mutableUploadFlg;   //一次是否上传多组记忆
                    boolean selfUpdateFlg;  //是否支持自升级

                    if ((returnData[1] & 0x01) == 1) {
                        upAirMeasureFlg = true; //上气
                    } else {
                        upAirMeasureFlg = false; //下气
                    }

                    if ((returnData[1] & 0x02) == 1) {
                        armMeasureFlg = true;   //臂式
                    } else {
                        armMeasureFlg = true;   //臂式
                    }

                    if ((returnData[1] & 0x04) == 1) {
                        haveAngleSensorFlg = true;  //带角度测量
                    } else {
                        haveAngleSensorFlg = false;
                    }

                    if ((returnData[1] & 0x10) == 1) {
                        haveOfflineFlg = true;  //带离线测量功能
                    } else {
                        haveOfflineFlg = false; //不带
                    }

                    if ((returnData[1] & 0x40) == 1) {
                        haveHSDFlg = true;  //有HSD
                    } else {
                        haveHSDFlg = false; //没有
                    }

                    if ((returnData[6] & 0x01) == 1) {
                        haveAngleSetFlg = true;
                    } else {
                        haveAngleSetFlg = false;
                    }

                    if ((returnData[6] & 0x02) == 1) {
                        mutableUploadFlg = true;
                    } else {
                        mutableUploadFlg = false;
                    }

                    if ((returnData[6] & 0x04) == 1) {
                        selfUpdateFlg = true;
                    } else {
                        selfUpdateFlg = false;
                    }
                    //记忆组别
                    int memorySize = returnData[2] & 0xff;
                    try {
                        jsonObject.put(BpProfile.FUNCTION_IS_UPAIR_MEASURE, upAirMeasureFlg);
                        jsonObject.put(BpProfile.FUNCTION_IS_ARM_MEASURE, armMeasureFlg);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_ANGLE_SENSOR, haveAngleSensorFlg);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_OFFLINE, haveOfflineFlg);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_HSD, haveHSDFlg);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_ANGLE_SETTING, haveAngleSetFlg);
                        jsonObject.put(BpProfile.FUNCTION_IS_MULTI_UPLOAD, mutableUploadFlg);
                        jsonObject.put(BpProfile.FUNCTION_HAVE_SELF_UPDATE, selfUpdateFlg);
                        jsonObject.put(BpProfile.MEMORY_GROUP_BP, memorySize);
                        mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_FUNCTION_INFORMATION_BP, jsonObject.toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                }

                break;

            case 0x26:
                mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_SET_UNIT_SUCCESS_BP, null);
                break;

            case 0x40:
                int num = returnData[1] & 0xff;
                if (num == 0 && getOfflineData) {            //获取数据
                    try {
                        jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_MAC, mAddress);
                        jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
                        mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_HISTORICAL_DATA_BP, new JSONObject().toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                } else if (num != 0 && getOfflineData) {    //获取数据
                    getOffLineData();
                } else if (!getOfflineData) {            //获取数量
                    try {
                        jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_MAC, mAddress);
                        jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
                        jsonObject.put(BpProfile.HISTORICAL_NUM_BP, num);
                        mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_HISTORICAL_NUM_BP, jsonObject.toString());
                    } catch (JSONException e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                }
                break;
            case 0x46:
                // cancelRepeat();
                activitys = ByteBufferUtil.BufferMerger(activitys,
                        ByteBufferUtil.bytesCutt(2, returnData.length - 1, returnData));
                if (returnData[1] == 0) {
                    if (mType.equals(iHealthDevicesManager.TYPE_BP7S) || mType.equals(iHealthDevicesManager.TYPE_550BT))
                        offlineDataOver();
                    if (activitys != null) {
                        convertOffline(activitys);
                        activitys = null;
                    }
                } else {
                    getOffLineData();
                }
                break;
            case 0x47:
                mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_HISTORICAL_OVER_BP, null);
                break;
            case 0x29:
                mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_SET_ANGLE_SUCCESS_BP, null);
                break;
            case 0x38:
                int errorNum = (int) (returnData[0] & 0xff);
                try {
                    jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
                    jsonObject.put(BpProfile.ERROR_NUM_BP, errorNum);
                    mInsCallback.onNotify(mAddress, mType, BpProfile.ACTION_ERROR_BP, jsonObject.toString());
                } catch (JSONException e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                break;
            case 0xf0:
                // cancelRepeat();
                allPkgOk((byte) 0xf0);
                packageIDPS(returnData);
                break;
            case 0xff:
                identify();
                break;
            default:
                Log.p(TAG, Log.Level.WARN, "Exception", "no method");
                break;
        }
    }

    private void convertOffline(byte[] datas) {
        JSONObject eachData;
        JSONObject jsonObject = new JSONObject();
        JSONArray eachArray = new JSONArray();

        offlineList.clear();
        //查最新协议
        int num = (datas.length) / 10;
        int index = 0;
        for (int i = 0; i < num; i++) {
            String str = "";
            int temp = 0;
            for (int j = 0; j < 10; j++) {
                if (j == 0) {
                    if (datas[j + index] < 0) {
                        temp = 1;
                    } else {
                        temp = 0;
                    }
                    str += (datas[j + index] & 0x7f) + ",";
                } else if (j == 1) {
                    str += (datas[j + index] & 0x7f) + ",";
                } else {
                    str += (datas[j + index] & 0xff) + ",";
                }
            }
            str += temp;
            index += 10;
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
            int IsArr = Integer.parseInt(offlineList.get(i).split(",")[10]);// 0齐，1不齐
            byte data0 = (byte) Integer.parseInt(offlineList.get(i).split(",")[0]);
            byte data1 = (byte) Integer.parseInt(offlineList.get(i).split(",")[1]);
            int HSD = 0;
            if ((data1 & 0x80) != 0) {
                HSD = 1;
            }
            byte data2 = (byte) Integer.parseInt(offlineList.get(i).split(",")[2]);
            int hands = (data2 & 0xc0) >> 6;
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
                eachData.put(BpProfile.MEASUREMENT_STRAT_ANGLE_BP, angle);
                eachData.put(BpProfile.MEASUREMENT_ANGLE_CHANGE_BP, angleChange);
                eachData.put(BpProfile.MEASUREMENT_HAND_BP, hands);
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

    //获取字符串的结束位置  以数字0为结束符
    private int getLength(byte[] buffer, int index) {
        int length = 0;
        for (int i = index; i < buffer.length; i++) {
            if (buffer[i] == 0) {
                break;
            }
            length++;
        }
        return length;
    }

    private void packageIDPS(byte[] returnData) {
        // com.jiuan.BPV20
        int length = getLength(returnData, 0);
        byte[] protocolStringBs = new byte[length];
        // BP Monitor
        length = getLength(returnData, 16);
        byte[] accessorynameBs = new byte[length];
        // 1.0.2
        length = 3;
        byte[] fwVerBs = new byte[length];
        // 1.0.1
        length = 3;
        byte[] hwVerBs = new byte[length];
        // iHealth
        length = getLength(returnData, 38);
        byte[] manufactureBs = new byte[length];
        // BP5 110700
        length = getLength(returnData, 54);
        byte[] modeNumberBs = new byte[length];
        try {
            for (int i = 0; i < protocolStringBs.length; i++) {
                protocolStringBs[i] = returnData[i + 0];
            }
            for (int i = 0; i < accessorynameBs.length; i++) {
                accessorynameBs[i] = returnData[i + 16];
            }
            for (int i = 0; i < fwVerBs.length; i++) {
                fwVerBs[i] = (byte) (returnData[i + 32] + 0x30);
            }
            for (int i = 0; i < hwVerBs.length; i++) {
                hwVerBs[i] = (byte) (returnData[i + 35] + 0x30);
            }
            for (int i = 0; i < manufactureBs.length; i++) {
                manufactureBs[i] = returnData[i + 38];
            }
            for (int i = 0; i < modeNumberBs.length; i++) {
                modeNumberBs[i] = returnData[i + 54];
            }
            protocolString = new String(protocolStringBs, "UTF-8");
            accessoryName = new String(accessorynameBs, "UTF-8");
            fVer = new String(fwVerBs, "UTF-8");
            hVer = new String(hwVerBs, "UTF-8");
            if (modeNumberBs[0] == 0x42) {
                modeNumber = new String(modeNumberBs, "UTF-8");
            } else {
                if (mType.equals(iHealthDevicesManager.TYPE_BP7S)) {
                    modeNumber = "BP7S 11070";
                } else {
                    modeNumber = "BP 11070";
                }
            }
            manufacture = new String(manufactureBs, "UTF-8");
            Intent intent = new Intent(iHealthDevicesIDPS.MSG_IHEALTH_DEVICE_IDPS);
            intent.putExtra(iHealthDevicesIDPS.PROTOCOLSTRING, protocolString);
            intent.putExtra(iHealthDevicesIDPS.ACCESSORYNAME, accessoryName);
            intent.putExtra(iHealthDevicesIDPS.FIRMWAREVERSION, fVer);
            intent.putExtra(iHealthDevicesIDPS.HARDWAREVERSION, hVer);
            intent.putExtra(iHealthDevicesIDPS.MODENUMBER, modeNumber);
            intent.putExtra(iHealthDevicesIDPS.MANUFACTURER, manufacture);
            intent.putExtra(iHealthDevicesIDPS.SERIALNUMBER, mAddress);
            intent.putExtra(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
            //20160731 jing  加包名限制，防止多个App间影响
            intent.setPackage(mContext.getPackageName());

            mContext.sendBroadcast(intent);

        } catch (UnsupportedEncodingException e) {
            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
        } catch (Exception e1) {
            Log.p(TAG, Log.Level.WARN, "Exception", e1.getMessage());
        }
    }
}
