
package com.ihealth.communication.ins;

import android.content.Context;
import android.content.Intent;

import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.base.protocol.BleCommProtocol;
import com.ihealth.communication.base.protocol.BtCommProtocol;
import com.ihealth.communication.cloud.data.DataBaseConstants;
import com.ihealth.communication.cloud.data.DataBaseTools;
import com.ihealth.communication.cloud.data.Data_HS_Result;
import com.ihealth.communication.cloud.data.HS_InAuthor;
import com.ihealth.communication.cloud.data.Make_Data_Util;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.control.HsProfile;
import com.ihealth.communication.manager.iHealthDevicesIDPS;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @hide
 */
public class A6InsSet extends IdentifyIns implements NewDataCallback, GetBaseCommProtocolCallback {

    private static final String TAG = "A6InsSet";
    private static final byte deviceType = (byte) 0xa6;
    private final BaseComm mBaseComm;
    private BaseCommProtocol btcm;
    private String mAddress;
    private String mType;
    private String hVer;
    private String fVer;
    private String manufacture;
    private String modeNumber;
    private String protocolString;
    private String accessoryName = "";


    private BaseCommCallback mBaseCommCallback;
    private InsCallback mInsCallback;
    private Context mContext;
    private String userName;

    public A6InsSet(String userName, Context context, BaseComm com, String mac, String type,
                    BaseCommCallback baseCommCallback,
                    InsCallback insCallback) {
        Log.p(TAG, Log.Level.INFO, "A6InsSet_Constructor", userName, mac, type);
        this.mAddress = mac;
        this.mType = type;
        mBaseCommCallback = baseCommCallback;
        mInsCallback = insCallback;
        this.mContext = context;
        this.userName = userName;
        if (type.equals(iHealthDevicesManager.TYPE_HS4)) {
            this.btcm = new BleCommProtocol(context, com, mAddress, deviceType, this);
        } else if (type.equals(iHealthDevicesManager.TYPE_HS4S)) {
            this.btcm = new BtCommProtocol(com, this);
        }
        this.mBaseComm = com;
        setInsSetCallbak(insCallback, mac, type, com);
    }

    public void identify() {
        Log.p(TAG, Log.Level.INFO, "identify");
        startTimeout(0xfa, AppsDeviceParameters.Delay_Medium, 0xfb, 0xfd, 0xfe);
        btcm.packageData(mAddress, identify(deviceType));
    }

    public void getIdps() {
        Log.p(TAG, Log.Level.INFO, "getIdps");
        byte[] returnCommand = new byte[2];
        byte commandID = (byte) 0xf1;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(mAddress, returnCommand);
    }

    private int mUnit;
    private int mUserId;

    public void setUnitAndUserId(int unit, int userId) {
        Log.p(TAG, Log.Level.INFO, "setUnitAndUserId", unit, userId);
        if (unit < 1 || unit > 3) {
            notifyParameterError("measureOnline() parameter unit should be in range {1,2,3}.");
        } else {
            this.mUnit = unit;
            this.mUserId = userId;
        }

    }

    private void notifyParameterError(String description) {
        notifyError(HsProfile.ERROR_ID_ILLEGAL_ARGUMENT, description);
    }

    private void notifyError(int errorID, String description) {
        try {
            JSONObject object = new JSONObject();
            object.put(HsProfile.ERROR_NUM_HS, errorID);
            object.put(HsProfile.ERROR_DESCRIPTION_HS, description);
            mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_ERROR_HS, object.toString());
        } catch (JSONException e) {
            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
        }
    }

    /**
     * @param @param unit :1 kg;2 lb; 3 st
     * @param @param userId
     * @return void
     * @Title: createOnlineLink
     * @Description
     */
    private void createOnlineLink(int unit, int userId) {
        Calendar calenda = Calendar.getInstance();
        calenda.setTimeZone(TimeZone.getDefault());
        int year = calenda.get(Calendar.YEAR) - 2000;
        int month = calenda.get(Calendar.MONTH) + 1;
        int day = calenda.get(Calendar.DAY_OF_MONTH);
        int hour = calenda.get(Calendar.HOUR_OF_DAY);
        int min = calenda.get(Calendar.MINUTE);
        int sed = calenda.get(Calendar.SECOND);
        int yearfull = year + 2000;
        byte[] bYear = ByteBufferUtil.intTo2Byte(yearfull);
        byte[] returnCommand = new byte[14];
        byte commandID = (byte) 0x32;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = bYear[0];
        returnCommand[3] = bYear[1];
        returnCommand[4] = (byte) month;
        returnCommand[5] = (byte) day;
        returnCommand[6] = (byte) hour;
        returnCommand[7] = (byte) min;
        returnCommand[8] = (byte) sed;
        returnCommand[9] = (byte) unit;
        byte[] userIds = ByteBufferUtil.intTo4Byte(userId);
        returnCommand[10] = userIds[0];
        returnCommand[11] = userIds[1];
        returnCommand[12] = userIds[2];
        returnCommand[13] = userIds[3];
        btcm.packageData(mAddress, returnCommand);
    }

    private void createOfflineLink() {
        Calendar calenda = Calendar.getInstance();
        calenda.setTimeZone(TimeZone.getDefault());
        int year = calenda.get(Calendar.YEAR) - 2000;
        int month = calenda.get(Calendar.MONTH) + 1;
        int day = calenda.get(Calendar.DAY_OF_MONTH);
        int hour = calenda.get(Calendar.HOUR_OF_DAY);
        int min = calenda.get(Calendar.MINUTE);
        int sed = calenda.get(Calendar.SECOND);
        int yearfull = year + 2000;
        byte[] bYear = ByteBufferUtil.intTo2Byte(yearfull);
        byte[] returnCommand = new byte[9];
        byte commandID = (byte) 0x33;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) bYear[0];
        returnCommand[3] = (byte) bYear[1];
        returnCommand[4] = (byte) month;
        returnCommand[5] = (byte) day;
        returnCommand[6] = (byte) hour;
        returnCommand[7] = (byte) min;
        returnCommand[8] = (byte) sed;
        btcm.packageData(mAddress, returnCommand);
    }

    public static final int MEASURETYPE_ONLINE = 0x01;
    public static final int MEASURETYPE_OFFLINE = 0x02;
    private static final int STOP_LINK = 0x03;

    private int measureType = 0;

    public void stopLink(int index) {
        Log.p(TAG, Log.Level.INFO, "stopLink", index);
        this.measureType = index;
        byte[] returnCommand = new byte[3];
        byte commandID = (byte) 0x39;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = 0x01;
        btcm.packageData(mAddress, returnCommand);
    }

    private void tansParams() {
        byte[] returnCommand = new byte[2];
        byte commandID = (byte) 0x41;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(mAddress, returnCommand);
    }

    private void tansData() {
        byte[] returnCommand = new byte[2];
        byte commandID = (byte) 0x42;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(mAddress, returnCommand);
    }

    private void allPkgOk(byte commandID) {
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(mAddress, returnCommand);
    }

    private byte[] offlineData;
    private boolean needstop = false;


    //jing 20161004 添加标示位,对HS4S重复测量会先上传上一次的测量结果的bug进行屏蔽
    boolean isMeasureing = false;

    @Override
    public void haveNewData(int what, int stateId, final byte[] returnData) {
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", String.format("0x%02X", what), stateId, ByteBufferUtil.Bytes2HexString(returnData));
        String hex = Integer.toHexString(what & 0xFF);
        if (hex.length() == 1) {
            hex = '0' + hex;
        }
        stopTimeout(what);
        switch (what) {
            case 0xfb:
                byte[] req = deciphering(returnData, mType, deviceType);
                startTimeout(0xfc, AppsDeviceParameters.Delay_Medium, 0xfd, 0xfe);
                btcm.packageData(mAddress, req);
                break;
            case 0xfd:
                mBaseCommCallback.onConnectionStateChange(mAddress, mType,
                        iHealthDevicesManager.DEVICE_STATE_CONNECTED, 0, null);
                break;
            case 0xfe:
                mBaseComm.disconnect();
                break;
            case 0x32:
                int req32 = returnData[0];
                if (req32 == 1) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put(HsProfile.ERROR_NUM_HS, 600);
                        mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_ERROR_HS, o.toString());
                    } catch (Exception e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                } else {
                    TimeoutTimer();
                }
                break;
            case 0x33:
                int req33 = returnData[0];
                if (req33 == 2) {
                    tansParams();
                }
                break;
            case 0x34:
                allPkgOk((byte) 0x34);
                stopTimeoutTimer();
                int error = returnData[0] & 0xff;
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.ERROR_NUM_HS, error);
                    mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_ERROR_HS, o.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }

                break;
            case 0x35:
                isMeasureing = true;
                stopTimeoutTimer();
                double req35 = 0.0;
                req35 = (ByteBufferUtil.byte2ToInt(returnData) / 10.0);
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.LIVEDATA_HS, req35);
                    mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_LIVEDATA_HS, o.toString());

                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                TimeoutTimer();
                break;
            case 0x36:
                allPkgOk((byte) 0x36);
                stopTimeoutTimer();
                needstop = true;
                //特殊处理  防止HS4S bug
                if (isMeasureing == false) {
                    return;
                }
                isMeasureing = false;
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        stopLink(MEASURETYPE_ONLINE);
                        double req36 = 0.0;
                        req36 = (float) (ByteBufferUtil.byte2ToInt(returnData) / 10.0);
                        long TS = PublicMethod.getTs();
                        String dataId = PublicMethod.getDataID(mAddress, req36 + "", TS);

                        try {
                            JSONObject o = new JSONObject();
                            o.put(HsProfile.WEIGHT_HS, req36);
                            o.put(HsProfile.DATAID, MD5.md5String(dataId));
                            mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_ONLINE_RESULT_HS, o.toString());
                        } catch (Exception e) {
                            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                        }
                        // data upload cloud
                        if (AppsDeviceParameters.isUpLoadData) {
                            Data_HS_Result hs_Result = Make_Data_Util.makeDataSingleHs(dataId, userName, (float) req36, 0,
                                    0, 0, 0, 0, 0, mType, mAddress);
                            DataBaseTools db = new DataBaseTools(mContext);
                            db.addData(DataBaseConstants.TABLE_TB_HSRESULT, hs_Result);
                            // 开启数据上云
                            HS_InAuthor sdk_InAuthor = HS_InAuthor.getInstance();
                            sdk_InAuthor.initAuthor(mContext, userName);
                            sdk_InAuthor.run();
                        }
                    }
                };
                timer.schedule(task, 500);

                break;
            case 0x39:
                if (!needstop) {
                    int req39 = returnData[0];
                    if (3 == req39) {
                        if (MEASURETYPE_OFFLINE == measureType) {
                            createOfflineLink();
                        } else if (MEASURETYPE_ONLINE == measureType) {
                            createOnlineLink(mUnit, mUserId);
                        } else if (measureType == STOP_LINK) {

                        }
                    }
                } else {
                    needstop = false;
                }
                break;
            case 0x41:
                int req41 = returnData[1];
                offlineData = null;
                if (req41 != 0) {
                    tansData();
                } else {
                    try {
                        JSONObject o = new JSONObject();
                        mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_NO_HISTORICALDATA, o.toString());

                    } catch (Exception e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                }
                break;
            case 0x42:
                int req42 = returnData[3];
                if (req42 != 0) {
                    tansData();
                    offlineData = ByteBufferUtil.BufferMerger(offlineData, returnData);
                } else {
                    convertOffLineData(offlineData, mType);
                    offlineData = null;
                }
                break;
            case 0xf0:
                allPkgOk((byte) 0xf0);
                packageIDPS(returnData);
                break;
            case 0xff:
                identify();
                break;
        }
    }


    private void convertOffLineData(byte[] offlineData, String type) {
        long currentTime = System.currentTimeMillis() / 1000;
        JSONArray array = new JSONArray();
        JSONObject object = new JSONObject();
        boolean haveData = false;

        try {
            for (int i = 0; (i + 16) <= offlineData.length; i += 16) {
                int year = (int) (offlineData[4 + i] & 0xff) * 256 + (int) (offlineData[5 + i] & 0xff);
                int month = (int) (offlineData[6 + i] & 0xff);
                int day = (int) (offlineData[7 + i] & 0xff);
                int hh = (int) (offlineData[8 + i] & 0xff);
                int mm = (int) (offlineData[9 + i] & 0xff);
                int ss = (int) (offlineData[10 + i] & 0xff);
                float weight = (float) (((float) ((int) (offlineData[11 + i] & 0xff) * 256 + (int) (offlineData[12 + i] & 0xff))) / 10.0);
                long measureTime = ByteBufferUtil.String2TS(year + "-" + month + "-" + day + " " + hh + ":" + mm + ":"
                        + ss);
                if (measureTime <= currentTime) {
                    JSONObject stoneObject = new JSONObject();
                    String dataId = PublicMethod.getDataID(mAddress, weight + "", PublicMethod.getTs());
                    stoneObject.put(HsProfile.DATAID, MD5.md5String(dataId));
                    stoneObject.put(HsProfile.MEASUREMENT_DATE_HS, ByteBufferUtil.TS2String(measureTime));
                    stoneObject.put(HsProfile.WEIGHT_HS, weight);
                    array.put(stoneObject);
                    haveData = true;
                } else {
                }
            }
            if (haveData) {
                haveData = false;
                object.putOpt(HsProfile.HISTORDATA__HS, array);
                mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_HISTORICAL_DATA_HS, object.toString());
            } else {
                mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_NO_HISTORICALDATA, object.toString());
            }

        } catch (Exception e) {
            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
        }
    }


    Timer timeoutTimer;
    TimerTask timerTask;

    private void TimeoutTimer() {
        timeoutTimer = new Timer();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                // WifiDeviceManager.stopUDPSearch = false;
                // disconnectBroad();
                stopTimeoutTimer();
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.ERROR_NUM_HS, 700);
                    mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_ERROR_HS, o.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
            }

        };
        timeoutTimer.schedule(timerTask, 4000);
    }

    private void stopTimeoutTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }


    @Override
    public BaseCommProtocol getBaseCommProtocol() {
        return btcm;
    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {

    }

    public void destroy() {
        Log.p(TAG, Log.Level.INFO, "destroy");
        if (btcm != null)
            btcm.destroy();
        btcm = null;
        mContext = null;
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
                if (mType.equals(iHealthDevicesManager.TYPE_HS4)) {
                    modeNumber = "HS4 11070";
                } else if (mType.equals(iHealthDevicesManager.TYPE_HS4S)) {
                    modeNumber = "HS4S 11070";
                } else {
                    modeNumber = "HS 11070";
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
        }
    }
}
