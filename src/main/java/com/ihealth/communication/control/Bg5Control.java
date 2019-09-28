package com.ihealth.communication.control;

import android.content.Context;

import com.ihealth.androidbg.audio.CrcCheck;
import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.ins.Bg5InsSet;
import com.ihealth.communication.ins.InsCallback;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.Log;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Public API for the BG5
 * <p> The class provides methods to control BG5 device.
 * You need to call the device method, and then call the connection method
 * <p> If you want to connect a BG5 device, you need to call{@link iHealthDevicesManager#startDiscovery} to discovery a new BG5 device,
 * and then call{@link iHealthDevicesManager#connectDevice}to connect BG5 device.
 */
public class Bg5Control implements IBGDelegate {

    private static final String TAG = "Bg5Control";

    private Bg5InsSet mBg5InsSet;
    private BaseComm mComm;
    private long bottleId = 0;
    private String mac = "";
    private String type = "";
    private InsCallback mInsCallback = null;
    private boolean mNeedReadEE;
    private static final long OneCodeBottleID = (long) 0xFF << 24 | 0xFF << 16 | 0xFF << 8 | 0xFF;
    private static final long CtlCodeBottleID = (long) 0xFF << 24 | 0xFF << 16 | 0xFF << 8 | 0xFE;

    /**
     * a constructor for Bg5Contorl.
     *
     * @param com               class for communication.
     * @param context           Context.
     * @param mac               valid Bluetooth address(without colon).
     * @param type              valid Bluetooth name.
     * @param needRead          flag to get the version of Bg device
     * @param mBaseCommCallback communication callback
     * @param insCallback       Bg series protocol callback.
     * @hide
     */
    public Bg5Control(String userName, BaseComm com, Context context, String mac, String type, boolean needRead, BaseCommCallback mBaseCommCallback, InsCallback insCallback) {

        Log.p(TAG, Log.Level.INFO, "Bg5Control", userName, com, context, mac, type, mBaseCommCallback, insCallback);
        this.mComm = com;
        this.mBg5InsSet = new Bg5InsSet(userName, com, context, mac, type, mBaseCommCallback, insCallback);
        this.mac = mac;
        this.type = type;
        this.mNeedReadEE = needRead;
        this.mInsCallback = insCallback;
    }

    /**
     * Set time to Bg5 device.
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After set time successfully,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_SET_TIME}, the keys of message will show in the KeyList of action.</li>
     * </ul>
     */
    @Override
    public void setTime() {
        mBg5InsSet.setTime();
    }

    /**
     * Set unit to Bg5 device.
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After set time successfully,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_SET_UNIT}, the keys of message will show in the KeyList of action.</li>
     * </ul>
     *
     * @param type the unit flag,1:mmol/L 2:mg/dL<br/>
     *             <b>Range:</b> 1, 2
     */
    @Override
    public void setUnit(int type) {
        if ((type > 0) && (type < 3)) {
            mBg5InsSet.setUnit(type);
        } else {

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(Bg5Profile.ERROR_NUM_BG, 400);
                jsonObject.put("description", "setUnit(int type) parameter type should be in the range [1, 2].");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mInsCallback.onNotify(mac, this.type, Bg5Profile.ACTION_ERROR_BG, jsonObject.toString());
        }
    }

    /**
     * Get the Bg5 battery status
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_BATTERY_BG}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     */
    @Override
    public void getBattery() {
        mBg5InsSet.getBattery();
    }

    /**
     * Start On-line measurement
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_START_MEASURE}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     *
     * @param type 1:test with blood; 2: test with control liquid <br/>
     *             <b>Range:</b> 1, 2
     */
    @Override
    public void startMeasure(int type) {
        if ((type > 0) && (type < 3)) {
            mBg5InsSet.startMeasure(type);
        } else {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(Bg5Profile.ERROR_NUM_BG, 400);
                jsonObject.put("description", "startMeasure(int type) parameter type should be in the range [1, 2].");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mInsCallback.onNotify(mac, this.type, Bg5Profile.ACTION_ERROR_BG, jsonObject.toString());

        }

    }

    /**
     * @param stripCodeOFScan
     * @return
     * @hide
     */
    private long getBottleIdFromQRCode(String stripCodeOFScan) {
        long bottleID = 0;
        if (!isValidCode(stripCodeOFScan)) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(Bg5Profile.ERROR_NUM_BG, 7);
                mInsCallback.onNotify(mac, type, Bg5Profile.ACTION_ERROR_BG, jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return 0;
        }
        if (PublicMethod.isOneCode(stripCodeOFScan)) {
            bottleID = OneCodeBottleID;
        } else if (PublicMethod.isCtlCode(stripCodeOFScan)) {
            bottleID = CtlCodeBottleID;
        } else {
            byte[] buffer = hexStringToByte(stripCodeOFScan);
            if (buffer != null && buffer.length == 30) {
                bottleID = (int) ((buffer[26] & 0xFF) * 256 * 256
                        * 256 + (buffer[27] & 0xFF) * 256 * 256
                        + (buffer[24] & 0xFF) * 256 + (buffer[25] & 0xFF));
            }
        }
        return bottleID;
    }

    /**
     * Set bottle Info to current Bg device
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_SET_BOTTLE_MESSAGE_SUCCESS}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     *
     * @param QRCode QRCode of strip bottle
     */
    @Override
    public void setBottleMessage(String QRCode) {
        bottleId = getBottleIdFromQRCode(QRCode);
        if (bottleId != 0) {
            mBg5InsSet.setBottleId(bottleId, QRCode, (byte) 0, "", true);
        }
    }

    synchronized private static boolean isValidCode(String QRCode) {
        if (PublicMethod.isOneCode(QRCode)) {
            return true;
        }
        if (PublicMethod.isCtlCode(QRCode)) {
            return true;
        }
        if (QRCode == null || QRCode.length() != 60) {
            return false;
        }
        byte[] qrcodes = hexStringToByte(QRCode);
        if (qrcodes.length == 30) {
            CrcCheck ccQR = new CrcCheck(hexByteToInt(qrcodes, 28));
            int chechsumFAC = ccQR.getCRCValue();
            if (qrcodes[28] == (byte) ((chechsumFAC & 0Xff00) >> 8) && qrcodes[29] == (byte) (chechsumFAC & 0X00ff)) {
                return true;
            }
        }
        return false;
    }

    private static int[] hexByteToInt(byte[] bt, int len) {
        int[] in = new int[len];
        for (int i = 0; i < len; i++) {
            in[i] = (int) bt[i];
            if (in[i] < 0) {
                in[i] = 256 + in[i];
            }
        }
        return in;
    }

    /**
     * Set bottle info to Bg5 device
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_SET_BOTTLE_MESSAGE_SUCCESS}, the keys of message will show in {@linkplain Bg5Profile#ACTION_SET_BOTTLE_MESSAGE_SUCCESS KeyList of the action}.
     * </li>
     * </ul>
     *
     * @param QRCode   QRCode of strip bottle
     * @param stripNum set strip num (stripNum >0 && stripNum <= 255)
     * @param overDate set expire time (format as "2016-03-01")
     */
    @Override
    public void setBottleMessage(String QRCode, int stripNum, String overDate) {
        bottleId = getBottleIdFromQRCode(QRCode);
        if (bottleId != 0) {
            mBg5InsSet.setBottleId(bottleId, QRCode, stripNum, overDate, false);
        }
    }

    /**
     * Get bottle info of current Bg device
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_GET_CODEINFO}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     **/
    @Override
    public void getBottleMessage() {
        mBg5InsSet.getCode();
    }

    /**
     * Get historical data
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_HISTORICAL_DATA_BG}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     **/
    @Override
    public void getOfflineData() {
        mBg5InsSet.readEENum();
    }

    /**
     * Delete historical data
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_DELETE_HISTORICAL_DATA}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     **/
    @Override
    public void deleteOfflineData() {
        mBg5InsSet.deleteEE();
    }

    /**
     * keep in connection
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_KEEP_LINK}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     **/
    @Override
    public void holdLink() {
        mBg5InsSet.linkHold();
    }

    /**
     * Send bottleId to current Bg device
     *
     * @param bottleId
     */
    @Override
    public void setBottleId(long bottleId) {
        mBg5InsSet.setBottleId(bottleId, "", (byte) 0, "", true);
    }

    /**
     * Get bottleId from current Bg5 device
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * </ul>
     * <ul>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * </ul>
     * <ul>
     * <li>The action of the callback is {@link Bg5Profile#ACTION_GET_BOTTLEID}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     **/
    @Override
    public void getBottleId() {
        mBg5InsSet.getBottleId();
    }

    /**
     * Parse bottle Info of the QRCode
     *
     * @param QRCode the QRCode of scan
     * @return jsonString like {"bottleInfo":[{"bottleId":"18882266","overDate":"2015-06-26","stripNum":"25"}]}
     */
    public static String getBottleInfoFromQR(String QRCode) {
        if (PublicMethod.isOneCode(QRCode)) {
            String[] bottleInfo = new String[3];
            bottleInfo[0] = "" + OneCodeBottleID;
            bottleInfo[1] = "2099-12-16";
            bottleInfo[2] = "" + 255;
            String arrName[] = new String[]{"bottleId", "overDate", "stripNum"};
            String jsonData = changeStringToJson("bottleInfo", bottleInfo, arrName);
            return jsonData;
        }
        if (isValidCode(QRCode)) {
            byte buffer[] = hexStringToByte(QRCode);
            if (buffer != null && buffer.length == 30) {
                long bottleIDFromErweima = (long) (buffer[26] & 0xFF) * 256 * 256 * 256
                        + (long) (buffer[27] & 0xFF) * 256 * 256
                        + (long) (buffer[24] & 0xFF) * 256
                        + (long) (buffer[25] & 0xFF);

                int year = (buffer[24] & 0xFE) >> 1;
                int month = (buffer[24] & 0x01) * 8 + ((buffer[25] & 0xE0) >> 5);
                int day = buffer[25] & 0x1F;
                int barCount;
                if (year == 15 && month == 1 && day == 15) {
                    barCount = 10;
                } else {
                    barCount = (int) (buffer[22] & 0xFF);
                    // 试条数据不超过25
                    if (barCount > 25) {
                        barCount = 25;
                    }
                }
                String overData = "20" + year + "-"
                        + ((month > 10) ? month : "0" + month) + "-"
                        + ((day > 10) ? day : "0" + day);
                //处理非法日期
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    overData = simpleDateFormat.format(simpleDateFormat.parse(overData));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String[] bottleInfo = new String[3];
                bottleInfo[0] = bottleIDFromErweima + "";
                bottleInfo[1] = overData;
                bottleInfo[2] = barCount + "";
                String arrName[] = new String[]{"bottleId", "overDate", "stripNum"};
                String jsonData = changeStringToJson("bottleInfo", bottleInfo, arrName);
                return jsonData;
            } else {
                return new JSONObject().toString();
            }
        } else {
            return new JSONObject().toString();
        }
    }

    /**
     * @param hex
     * @return
     */
    private static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    /**
     * @param cr
     * @return
     */
    private static byte toByte(char cr) {
        byte b = (byte) "0123456789ABCDEF".indexOf(cr);
        return b;
    }

    /**
     * @param objName
     * @param arr
     * @param arrName
     * @return
     * @hide
     */
    private static String changeStringToJson(String objName, String[] arr, String[] arrName) {
        String[] arrJson = arr;
        try {
            JSONObject object = new JSONObject();
            JSONArray array = new JSONArray();
            JSONObject bottleObject = new JSONObject();
            for (int i = 0; i < arrJson.length; i++) {

                bottleObject.put(arrName[i], arr[i]);

            }
            array.put(bottleObject);
            object.put(objName, array);
            return object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @hide
     */
    @Override
    public void init() {
        Log.e(mac, "mNeedReadEE = " + mNeedReadEE);
        if (mNeedReadEE) {
            mBg5InsSet.getBtEE();
        } else {
            mBg5InsSet.identify();
        }
    }

    /**
     * @hide
     */
    public void readEE() {
        byte[] commR = hexStringToByte("0D0A2B495353435F525F4545503A3031386130360D0A");
        mComm.sendData(mac, commR);
    }

    /*
     *disconnect the Bg5
     */
    @Override
    public void disconnect() {
        mComm.disconnect();
    }

    public String codeStripStrAnalysis(String qr) {
        JSONObject json = new JSONObject();
        if (isValidCode(qr)) {
            byte[] data = ByteBufferUtil.hexStringToByte(qr);
            if ((qr != null) && (data.length == 30)) {
                try {
                    int stripnum;
                    int bottleId = (data[26] & 0xFF) * 256 * 256 * 256 + (data[27] & 0xFF) * 256 * 256 + (data[24] & 0xFF) * 256 + (data[25] & 0xFF);
                    int year = (data[24] & 0xFE) >> 1;
                    int month = (data[24] & 0x1) * 8 + ((data[25] & 0xE0) >> 5);
                    int day = data[25] & 0x1F;
                    if ((year == 15) && (month == 1) && (day == 15)) {
                        stripnum = 10;
                    } else {
                        stripnum = data[22] & 0xFF;
                        if (stripnum > 25)
                            stripnum = 25;
                    }
                    String date = String.valueOf(year + 2000) + "-" + String.valueOf(month) + "-" + String.valueOf(day);
                    json.put("bottleid", bottleId);
                    json.put("stripnum", stripnum);
                    json.put("duedate", date);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return json.toString();
    }

    @Override
    public void destroy() {
        if (mBg5InsSet != null)
            mBg5InsSet.destroy();
        mBg5InsSet = null;
        mComm = null;
    }

}
