
package com.ihealth.communication.ins;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.base.protocol.Hs3CommProtocol;
import com.ihealth.communication.cloud.data.DataBaseConstants;
import com.ihealth.communication.cloud.data.DataBaseTools;
import com.ihealth.communication.cloud.data.Data_HS_Result;
import com.ihealth.communication.cloud.data.HS_InAuthor;
import com.ihealth.communication.cloud.data.Make_Data_Util;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.control.Hs3Control;
import com.ihealth.communication.control.HsProfile;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @hide
 */
public class Hs3InsSet implements NewDataCallback {

    private static final String TAG = "Hs3InsSet";

    private Context mContext;
    private byte deviceType = (byte) 0xa6;
    private BaseCommProtocol btcm;
    private static String HS3OFFLINETIME = "HS3OLLINETIME";
    private String deviceMac = "";
    private String mType = "";
    private BaseCommCallback mBaseCommCallback;
    private InsCallback minCallback;
    private String userName;

    public Hs3InsSet(String userName, BaseComm com, Context context, String mac, String type,
                     BaseCommCallback baseCommCallback, InsCallback insCallback) {
        Log.p(TAG, Log.Level.INFO, "Hs3InsSet_Constructor", userName, mac, type);
        btcm = new Hs3CommProtocol(com, this);
        this.mContext = context;
        this.deviceMac = mac;
        this.mType = type;
        this.userName = userName;
        this.mBaseCommCallback = baseCommCallback;
        this.minCallback = insCallback;
        initTimeLong();
    }

    /**
     * create Channel method
     * <ul>
     * <li>
     * Used to create communication channels with HS3.
     * </li>
     * <li>
     * In {@link Hs3Control#init()} method is invoked for the first time.
     * </li>
     * </ul>
     */
    public void createChannel() {
        Log.p(TAG, Log.Level.INFO, "createChannel");
        byte[] returnCommand = new byte[2];
        byte commandID = (byte) 0x23;
        commandIdFlag = commandID & 0xff;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(null, returnCommand);
    }

    private void syncTime(byte year, byte month, byte day, byte hour, byte min, byte sec) {
        byte[] returnCommand = new byte[8];
        byte commandID = (byte) 0x20;
        commandIdFlag = commandID & 0xff;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = year;
        returnCommand[3] = month;
        returnCommand[4] = day;
        returnCommand[5] = hour;
        returnCommand[6] = min;
        returnCommand[7] = sec;
        btcm.packageData(null, returnCommand);
    }

    private long mTimeLong = 0;

    /**
     * Initializes time method
     * <b>Example: </b><br/>
     * 2011-1-2-12-12-12
     */
    private void initTimeLong() {
        @SuppressWarnings("deprecation")
        Date date = new Date(2011 - 1900, 0, 2, 12, 12, 12);
        mTimeLong = date.getTime();
        SharedPreferences timeLongPreferences = mContext.getSharedPreferences(userName + HS3OFFLINETIME,
                Context.MODE_PRIVATE);
        long timeBefore = timeLongPreferences.getLong(deviceMac, 0);

        if (mTimeLong > timeBefore) {
            Editor editor = timeLongPreferences.edit();
            editor.putLong(deviceMac, mTimeLong);
            editor.apply();
        }
    }

    private void setOfflineTime(long offlineTime) {
        SharedPreferences timerLongPreferences = mContext.getSharedPreferences(userName + HS3OFFLINETIME,
                Context.MODE_PRIVATE);
        long offlineBefore = timerLongPreferences.getLong(deviceMac, 0);
        if (offlineTime > offlineBefore) {
            Editor editor = timerLongPreferences.edit();
            editor.putLong(deviceMac, offlineTime);
            editor.apply();
        }
    }

    private void getOfflineTime(String mac) {
        SharedPreferences timerLongPreferences = mContext.getSharedPreferences(userName + HS3OFFLINETIME,
                Context.MODE_PRIVATE);
        this.mTimeLong = timerLongPreferences.getLong(mac, mTimeLong);
    }

    public void getOffLineDataNum() {
        Log.p(TAG, Log.Level.INFO, "getOffLineDataNum");
        // Date date = new Date();
        getOfflineTime(deviceMac);
        Calendar calenda = Calendar.getInstance();
        // calenda.setTime(date);
        // calenda.setTimeZone(TimeZone.getTimeZone("GMT"));
        calenda.setTimeZone(TimeZone.getDefault());
        calenda.setTimeInMillis(mTimeLong);
        int year = calenda.get(Calendar.YEAR) - 2000;
        int month = calenda.get(Calendar.MONTH) + 1;
        int day = calenda.get(Calendar.DAY_OF_MONTH);
        int hour = calenda.get(Calendar.HOUR);
        int am_pm = calenda.get(Calendar.AM_PM);
        int minute = calenda.get(Calendar.MINUTE);
        int second = calenda.get(Calendar.SECOND);
        if (hour == 0) {
            hour = 12;
        }
        hour = makeHour(hour, am_pm);

        byte[] returnCommand = new byte[8];
        byte commandID = (byte) 0x25;

        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;

        returnCommand[2] = (byte) year;
        returnCommand[3] = (byte) month;
        returnCommand[4] = (byte) day;
        returnCommand[5] = (byte) hour;
        returnCommand[6] = (byte) minute;
        returnCommand[7] = (byte) second;

        btcm.packageData(null, returnCommand);
    }

    private void getOffLineData() {

        byte[] returnCommand = new byte[5];
        byte commandID = (byte) 0x27;

        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;

        btcm.packageData(null, returnCommand);
    }

    private void ask() {
        byte[] returnCommand = new byte[2];
        byte commandID = (byte) 0xff;
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        btcm.packageData(null, returnCommand);
    }

    private int commandIdFlag = 0;

    @Override
    public void haveNewData(int what, int stateId, byte[] returnData) {
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", String.format("0x%02X", what), stateId, ByteBufferUtil.Bytes2HexString(returnData));
        String hex = Integer.toHexString(what & 0xFF);
        if (hex.length() == 1) {
            hex = '0' + hex;
        }

        switch (what) {
            case (byte) 0x21: { // trans history data
                ask();
                packageJson(returnData);
                break;
            }
            case (byte) 0x22: { // error
                ask();
                int err = returnData[0] & 0xff;
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.ERROR_NUM_HS, err);
                    minCallback.onNotify(deviceMac, mType, HsProfile.ACTION_ERROR_HS, o.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                // errorAnalysis(returnData[0]);
                break;
            }
            case (byte) 0x26: { // 历史数据条数
                ask();
                int num = returnData[0] & 0xff;
                if (num > 0) {
                    getOffLineData();
                } else {
                    try {
                        JSONObject o = new JSONObject();
                        minCallback.onNotify(deviceMac, mType, HsProfile.ACTION_NO_HISTORICALDATA, o.toString());
                    } catch (Exception e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                }
                break;
            }
            case (byte) 0x28: { // receive weight data
                ask();
                int bai, shi, ge, xiao;
                bai = ((returnData[4] & (byte) 0xF0) >>> 4) & (byte) 0x0F;// 9
                shi = (returnData[4] & (byte) 0x0F) & (byte) 0x0F;// 9
                ge = ((returnData[5] & (byte) 0xF0) >>> 4) & (byte) 0x0F;// 10
                xiao = (returnData[5] & (byte) 0x0F) & (byte) 0x0F;// 10
                float weightResult = bai * 100 + shi * 10 + ge + (float) (xiao / 10.0);
                String dataId = PublicMethod.getDataID(deviceMac, weightResult + "", PublicMethod.getTs());
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.DATAID, MD5.md5String(dataId));
                    o.put(HsProfile.WEIGHT_HS, weightResult);
                    minCallback.onNotify(deviceMac, mType, HsProfile.ACTION_ONLINE_RESULT_HS,
                            o.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }

                if (AppsDeviceParameters.isUpLoadData) {
                    // data upload cloud
                    Data_HS_Result hs_Result = Make_Data_Util.makeDataSingleHs(dataId, userName, weightResult, 0,
                            0, 0, 0, 0, 0, mType, deviceMac);
                    DataBaseTools db = new DataBaseTools(mContext);
                    db.addData(DataBaseConstants.TABLE_TB_HSRESULT, hs_Result);
                    // 开启数据上云
                    HS_InAuthor sdk_InAuthor = HS_InAuthor.getInstance();
                    sdk_InAuthor.initAuthor(mContext, userName);
                    sdk_InAuthor.run();
                }

                break;
            }
            case (byte) 0x30: { // request shut down
                ask();
                break;
            }
            case (byte) 0x31: { // repeated data
                ask();
                break;
            }
            case (byte) 0x32: {
                ask();
                syncTime();
                break;
            }
            case (byte) 0x33: {
                ask();
                break;
            }
            case (byte) 0xff: {
                break;
            }
            case 255:
                if (commandIdFlag == 0x20) {
                    mBaseCommCallback.onConnectionStateChange(deviceMac, mType,
                            iHealthDevicesManager.DEVICE_STATE_CONNECTED, 0, null);
                    commandIdFlag = 0;
                    // getOffLineDataNum();
                }
                break;
            default:
                Log.p(TAG, Log.Level.WARN, "Exception", "no method");
                break;
        }
    }

//    /**
//     * analysis err
//     *
//     * @param errorNum
//     * @return
//     */
//    private void errorAnalysis(byte errorNum) {
//        String error = "";
//        switch (errorNum) {
//            case 0x01:
//                error = "Battery in low level";
//                break;
//            case 0x02:
//                error = "over load";
//                break;
//            case 0x03:
//                error = "EEPROM is error";
//                break;
//            case 0x04:
//                error = "basic level too high";
//                break;
//            case 0x05:
//                error = "basic level too low";
//                break;
//            case 0x06:
//                error = "weight is a negative number";
//                break;
//            case 0x07:
//                error = "unstable after 15s since get on the scale";
//                break;
//            default:
//                break;
//        }
//    }

    private void syncTime() {
        Calendar calenda = Calendar.getInstance();
        calenda.setTimeZone(TimeZone.getDefault());
        // calenda.setTimeZone(TimeZone.getTimeZone("GMT"));
        int year = calenda.get(Calendar.YEAR) - 2000;
        int month = calenda.get(Calendar.MONTH) + 1;
        int day = calenda.get(Calendar.DAY_OF_MONTH);
        int hour = calenda.get(Calendar.HOUR);
        int am_pm = calenda.get(Calendar.AM_PM);
        int minute = calenda.get(Calendar.MINUTE);
        int second = calenda.get(Calendar.SECOND);
        hour = makeHour(hour, am_pm);
        syncTime((byte) year, (byte) month, (byte) day, (byte) hour, (byte) minute, (byte) second);
    }

    private int makeHour(int hour, int am_pm) {
        return (hour + am_pm * 32);
    }

    /*
     * conver the data to int[] result = new int[] { year, month, day, hour, minute, second,
     * weight};
     */
    private int[] makeData(byte[] data) {
        int year = ((data[0] & 0xfc) >> 2) + 2000;
        int month = (data[0] & 0x03) << 2
                | (data[1] & 0xc0) >> 6;
        int day = (data[1] & 0x3e) >> 1;
        int h;
        int hh1 = data[1] & 0x01;
        int hh2 = (data[2] & 0xf0) >> 4;
        int mm = (data[2] & 0x0f) << 2
                | ((data[3] & 0xc0) >> 6);
        int ss = data[3] & 0x3f;
        if (hh2 == 12) {
            h = hh1 * 12;
        } else {
            h = hh1 * 12 + hh2;
        }
        int low = data[5] & 0x0f;
        int high = (data[5] & 0xf0) >> 4;
        int low_ = data[4] & 0x0f;
        int high_ = (data[4] & 0xf0) >> 4;
        int weight = high_ * 1000 + low_ * 100 + high * 10 + low;
        return new int[]{
                year, month, day, h, mm, ss, weight
        };
    }

    private String packageJson(byte[] datas) {
        String json = "";
        int num = datas.length / 7;
        long hs3OfflineTime = 0;
        long currentTime = System.currentTimeMillis() / 1000; // ms
        try {
            JSONObject o = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            for (int j = 0; j < num; j++) {
                byte[] data = ByteBufferUtil.bytesCutt(j * 7, 7 * j + 6, datas);
                if (data != null && data.length == 7) {
                    int[] result = makeData(data);
                    String time = (result[0] < 10 ? ("0" + result[0]) : result[0]) + "-"
                            + (result[1] < 10 ? ("0" + result[1]) : result[1]) + "-"
                            + (result[2] < 10 ? ("0" + result[2]) : result[2]) + " "
                            + (result[3] < 10 ? ("0" + result[3]) : result[3]) + ":"
                            + (result[4] < 10 ? ("0" + result[4]) : result[4]) + ":"
                            + (result[5] < 10 ? ("0" + result[5]) : result[5]);
                    long measureTime = ByteBufferUtil.String2TS(time);
                    if (measureTime < currentTime) {
                        // record the nearest time of history data
                        if (measureTime * 1000 > mTimeLong) {
                            if (hs3OfflineTime < measureTime * 1000) {
                                hs3OfflineTime = measureTime * 1000;
                            }
                            String dataId = PublicMethod.getDataID(deviceMac, (float) (result[6] / 10.0) + "", measureTime);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(HsProfile.DATAID, MD5.md5String(dataId));
                            jsonObject.put(HsProfile.MEASUREMENT_DATE_HS, ByteBufferUtil.TS2String(measureTime));
                            jsonObject.put(HsProfile.WEIGHT_HS, (float) (result[6] / 10.0));
                            jsonArray.put(jsonObject);

                        } else {
                        }

                    } else {
                    }
                }
            }
            if (jsonArray.length() > 0) {
                setOfflineTime(hs3OfflineTime);
                o.put(HsProfile.HISTORDATA__HS, jsonArray);
                minCallback.onNotify(deviceMac, mType, HsProfile.ACTION_HISTORICAL_DATA_HS, o.toString());
            } else {
                minCallback.onNotify(deviceMac, mType, HsProfile.ACTION_NO_HISTORICALDATA, o.toString());
            }

        } catch (Exception e) {
            Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
        }
        return json;
    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {

    }

    public void destroy() {
        Log.p(TAG, Log.Level.INFO, "destroy");
    }
}
