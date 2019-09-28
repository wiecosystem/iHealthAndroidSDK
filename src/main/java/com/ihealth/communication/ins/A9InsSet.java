
package com.ihealth.communication.ins;

import android.content.Context;
import android.os.SystemClock;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.base.protocol.WifiCommProtocol;
import com.ihealth.communication.cloud.data.DataBaseConstants;
import com.ihealth.communication.cloud.data.DataBaseTools;
import com.ihealth.communication.cloud.data.Data_HS_Result;
import com.ihealth.communication.cloud.data.HS_InAuthor;
import com.ihealth.communication.cloud.data.Make_Data_Util;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.cloud.tools.Method;
import com.ihealth.communication.control.HsProfile;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.Hs5DataUtil;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Private API for body scale HS5.
 * 
 * @hide
 */
public class A9InsSet extends IdentifyIns implements NewDataCallback {
    private static final String TAG = "A9InsSet";
    private String cloud1 = "www.ihealthcloud.net";
    private String cloud2 = "/api/LowerMachine/LowerMachineProcess.htm";
    private static final byte deviceType = (byte) 0xa9;
    private BaseCommProtocol mWifiCommProtocol;
    private ArrayList<String> offlineDataList = new ArrayList<String>();
    private String deviceMac = "";
    private String deviceIp = "";
    /** Product protocol callback */
    private InsCallback mInsCallback;
    private String mType;
    /**
     * Communication callback
     */
    private BaseCommCallback mBaseCommCallback;
    private UserListInHs5 muserListInHs5;
    private int mUserId = 0;
    private Context mContext;
    private String userName;

    private static void printMethodInfo(String methodName, Object... parameters) {
        Log.p(TAG, Log.Level.INFO, methodName, parameters);
    }

    /**
     * a constructor for A9InsSet.
     * 
     * @param context Context
     * @param comm class for communication.
     * @param deviceMac valid Bluetooth address(without colon).
     * @param deviceIp
     * @param insCallback
     * @param baseCommCallback
     * @param type the type of device. eg:{@link iHealthDevicesManager#TYPE_HS5}
     * @param userListInHs5 user's id and position in scale
     */
    public A9InsSet(String userName, Context context, BaseComm comm, String deviceMac, String deviceIp,
            InsCallback insCallback,
            BaseCommCallback baseCommCallback,
            String type, UserListInHs5 userListInHs5) {
        printMethodInfo("A9InsSet_Constructor", userName, deviceMac, deviceIp, type, userListInHs5);
        this.deviceMac = deviceMac;
        this.deviceIp = deviceIp;
        this.mInsCallback = insCallback;
        this.userName = userName;
        this.mBaseCommCallback = baseCommCallback;
        this.mType = type;
        this.muserListInHs5 = userListInHs5;
        this.mContext = context;
        mWifiCommProtocol = new WifiCommProtocol(context, comm, deviceMac, deviceIp, type, baseCommCallback,
                insCallback);
        mWifiCommProtocol.setInsSet(this);
        comm.addCommNotify(deviceMac, mWifiCommProtocol);
        setInsSetCallbak(insCallback, deviceMac, type, comm);
    }

    public void identify() {
        printMethodInfo("identify");
        startTimeout(0xfa, AppsDeviceParameters.Delay_Medium, 0xfb, 0xfd, 0xfe);
        mWifiCommProtocol.packageData(identify(deviceType));
    }

    public void setUserId(int usrId) {
        printMethodInfo("setUserId", usrId);
        this.mUserId = usrId;
    }

    /**
     * ack for state id is 00
     */
    private void ack(byte commandID) {
        if (deviceMac == null || deviceIp == null) {
            return;
        }
        byte[] command = new byte[2];
        command[0] = deviceType;
        command[1] = commandID;
        mWifiCommProtocol.packageData(command);
    }

    /**
     * ack for state id is A0
     */
    private void ackA0(byte commandID) {
        if (deviceMac == null || deviceIp == null) {
            return;
        }
        byte[] command = new byte[2];
        command[0] = deviceType;
        command[1] = commandID;
        mWifiCommProtocol.packageDataAsk(command);
    }

    /**
     * set cloud address part one
     */
    public void setCloud1(String cloud1) {
        printMethodInfo("setCloud1", cloud1);
        if (deviceMac == null || deviceIp == null) {
            return;
        }
        byte[] command = new byte[50];
        command[0] = deviceType;
        command[1] = (byte) 0x59;
        byte[] byte_url1 = new byte[48];
        try {
            byte_url1 = cloud1.getBytes("UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        for (int i = 2; i < byte_url1.length + 2; i++) {
            command[i] = byte_url1[i - 2];
        }
        mWifiCommProtocol.packageData(command);
    }

    /**
     * set cloud address part two
     */
    public void setCloud2(String cloud2) {
        printMethodInfo("setCloud2", cloud2);
        if (deviceMac == null || deviceIp == null) {
            return;
        }
        byte[] command = new byte[66];
        command[0] = deviceType;
        command[1] = (byte) 0x5A;
        byte[] byte_url2 = new byte[64];
        try {
            byte_url2 = cloud2.getBytes("UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        for (int i = 2; i < byte_url2.length + 2; i++) {
            command[i] = byte_url2[i - 2];
        }
        mWifiCommProtocol.packageData(command);
    }

    public void createManagementCnn() {
        printMethodInfo("createManagementCnn");
        if (deviceMac == null || deviceIp == null) {
            return;
        }
        byte[] command = new byte[8];
        command[0] = (byte) deviceType;
        command[1] = (byte) 0x31;
        Date date = new Date();
        byte year = (byte) (date.getYear() - 100);
        byte month = (byte) (date.getMonth() + 1);
        byte day = (byte) date.getDate();
        byte hour = (byte) date.getHours();
        byte minute = (byte) date.getMinutes();
        byte second = (byte) date.getSeconds();
        byte[] time = {
                year, month, day, hour, minute, second
        };
        ByteBufferUtil.ByteBufferCopy(time, 0, 6, command, 2);
        mWifiCommProtocol.packageData(command);
    }

    public void WriteUserToScale(int userPstCode, int userId, int age, int height, int isSporter, int gender) {
        printMethodInfo("WriteUserToScale", userPstCode, userId, age, height, isSporter, gender);
        byte[] command = new byte[11];
        command[0] = (byte) deviceType;
        command[1] = (byte) 0x51;
        command[2] = (byte) userPstCode;
        byte userNum[] = ByteBufferUtil.intToByteForuserId(userId);
        command[3] = userNum[0];
        command[4] = userNum[1];
        command[5] = userNum[2];
        command[6] = userNum[3];
        if (age > 100) {
            age = 99;
        } else if (age < 6) {
            age = 7;
        }
        command[7] = (byte) age;
        if (height > 220) {
            height = 219;
        } else if (height <= 80) {
            height = 81;
        }
        command[8] = (byte) height;
        command[9] = (byte) isSporter;
        command[10] = (byte) gender;
        mWifiCommProtocol.packageData(command);
    }

    public void DeleteUserInScale(int userPstCode, int userId) {
        printMethodInfo("DeleteUserInScale", userPstCode, userId);
        byte[] command = new byte[7];
        command[0] = (byte) deviceType;
        command[1] = (byte) 0x52;
        command[2] = (byte) userPstCode;
        byte userNum[] = ByteBufferUtil.intToByteForuserId(userId);
        command[3] = userNum[0];
        command[4] = userNum[1];
        command[5] = userNum[2];
        command[6] = userNum[3];
        mWifiCommProtocol.packageData(command);
    }

    public void updateUserInfo(int userPstCode, int userId, int age, int height, int isSporter, int gender) {
        printMethodInfo("updateUserInfo", userPstCode, userId, age, height, isSporter, gender);
        byte[] command = new byte[11];
        command[0] = (byte) deviceType;
        command[1] = (byte) 0x54;
        command[2] = (byte) userPstCode;
        byte userNum[] = ByteBufferUtil.intToByteForuserId(userId);
        command[3] = userNum[0];
        command[4] = userNum[1];
        command[5] = userNum[2];
        command[6] = userNum[3];
        if (age > 100) {
            age = 99;
        } else if (age < 6) {
            age = 7;
        }
        command[7] = (byte) age;
        if (height > 220) {
            height = 219;
        } else if (height <= 80) {
            height = 81;
        }
        command[8] = (byte) height;
        command[9] = (byte) isSporter;
        command[10] = (byte) gender;
        mWifiCommProtocol.packageData(command);
    }

    public void creatMeasurementCnn(int userPstCode, int userId) {
        printMethodInfo("creatMeasurementCnn", userPstCode, userId);
        byte[] command = new byte[7];
        command[0] = (byte) deviceType;
        command[1] = (byte) 0x32;
        command[2] = (byte) userPstCode;
        byte userNum[] = ByteBufferUtil.intToByteForuserId(userId);
        command[3] = userNum[0];
        command[4] = userNum[1];
        command[5] = userNum[2];
        command[6] = userNum[3];
        mWifiCommProtocol.packageData(command);
    }

    public void creatMemoryCnn(int userPstCode, int userId) {
        printMethodInfo("creatMemoryCnn", userPstCode, userId);
        byte[] command = new byte[13];
        command[0] = deviceType;
        command[1] = (byte) 0x33;
        command[2] = (byte) userPstCode;
        byte userNum[] = ByteBufferUtil.intToByteForuserId(userId);
        command[3] = userNum[0];
        command[4] = userNum[1];
        command[5] = userNum[2];
        command[6] = userNum[3];
        Date date = new Date();
        byte year = (byte) (date.getYear() - 100);
        byte month = (byte) (date.getMonth() + 1);
        byte day = (byte) date.getDate();
        byte hour = (byte) date.getHours();
        byte minute = (byte) date.getMinutes();
        byte second = (byte) date.getSeconds();
        byte[] time = {
                year, month, day, hour, minute, second
        };
        command[7] = time[0];
        command[8] = time[1];
        command[9] = time[2];
        command[10] = time[3];
        command[11] = time[4];
        command[12] = time[5];
        mWifiCommProtocol.packageData(command);
    }

    public void transMemoryPara() {
        printMethodInfo("transMemoryPara");
        byte[] command = new byte[2];
        command[0] = deviceType;
        command[1] = 0x41;
        mWifiCommProtocol.packageData(command);
    }

    public void transMemoryData() {
        printMethodInfo("transMemoryData");
        byte[] command = new byte[2];
        command[0] = deviceType;
        command[1] = 0x42;
        mWifiCommProtocol.packageData(command);
    }

    public void finishCnn() {
        printMethodInfo("finishCnn");
        byte[] command = new byte[2];
        command[0] = deviceType;
        command[1] = 0x39;
        mWifiCommProtocol.packageData(command);
    }

    private enum Command {
        Unknown(0),
        IpAddress_Verifying(0xef),
        Verification_Feedback(0xfb),
        Verification_Success(0xfd),
        Verification_Failed(0xfe),
        SetCloudAddress_Part1(0x59),
        SetCloudAddress_Part2(0x5a),
        CreateUser_Management(0x31),
        AddNewUser(0x51),
        DeleteUser(0x52),
        UpdateUserInfo(0x54),
        MeasureConnection(0x32),
        HistoryConnection(0x33),
        GetMemoryData(0x41),
        GetHistoryData(0x42),
        RealTimeData(0x35),
        StableWeight(0x36),
        ImpedanceWeight(0x37),
        ResultData(0x38),
        Measurement_Stop(0x39),
        Error(0x34);
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
        Command command = Command.parseCommand(what);
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", command, stateId, ByteBufferUtil.Bytes2HexString(returnData));
        // TODO Auto-generated method stub
        byte[] mac = ByteBufferUtil.bytesCutt(returnData.length - 6, returnData.length - 1, returnData);
        String receiveMac = "";
        receiveMac = ByteBufferUtil.Bytes2HexString(mac);

        // WifiDeviceManager.stopUDPSearch = true;
        if (returnData != null) {
            anaCmmData(what, stateId, receiveMac, ByteBufferUtil.bytesCutt(0, returnData.length - 7, returnData));
        }

    }

    private byte[] SCALE_BUSY = {
            (byte) 0x2E, (byte) 0x2E, (byte) 0x2E, (byte) 0x2E,
            (byte) 0x2E, (byte) 0x2E, (byte) 0x2E, (byte) 0x2E, (byte) 0x2E,
            (byte) 0x2E, (byte) 0x2E, (byte) 0x2E, (byte) 0x2E, (byte) 0x2E,
            (byte) 0x2E, (byte) 0x2E
    };

    // private byte[] offlinedatas;

    private void anaCmmData(int what, int stateId, String mac, byte[] returnData) {
        stopTimeout(what);
        Command command = Command.parseCommand(what);
        switch (command) {
            case IpAddress_Verifying://(0xef)
                String tempIP = ByteBufferUtil.Bytes2HexString(returnData);
                if (tempIP.matches(ByteBufferUtil.Bytes2HexString(SCALE_BUSY))) {
                    Log.w(TAG, "Scale is now communicating with i-cloud.");
                } else {
                    Log.i(TAG, "Scale is now communicating with " + tempIP);
                }
                mBaseCommCallback.onConnectionStateChange(deviceMac, mType,
                        iHealthDevicesManager.DEVICE_STATE_DISCONNECTED, 0, null);
                break;
            case Verification_Feedback://(0xfb),
                byte[] req = deciphering(ByteBufferUtil.bufferCut(returnData, 0, 48), "HS5", deviceType);
                startTimeout(0xfc, AppsDeviceParameters.Delay_Medium, 0xfd, 0xfe);
                mWifiCommProtocol.packageData(req);
                break;
            case Verification_Success://(0xfd),
                setCloud1(cloud1);
                break;
            case Verification_Failed://(0xfe),
                mBaseCommCallback.onConnectionStateChange(deviceMac, mType,
                        iHealthDevicesManager.DEVICE_STATE_DISCONNECTED, 0, null);
                break;
            case SetCloudAddress_Part1://(0x59),
                if (returnData[0] == 1) {
                    setCloud2(cloud2);
                } else {
                    Log.w(TAG, "Set cloud address part 1 fail");
                    mBaseCommCallback.onConnectionStateChange(deviceMac, mType,
                            iHealthDevicesManager.DEVICE_STATE_DISCONNECTED, 0, null);
                }
                break;
            case SetCloudAddress_Part2://(0x5a),
                if (returnData[0] == 1) {
                    mBaseCommCallback.onConnectionStateChange(deviceMac, mType,
                            iHealthDevicesManager.DEVICE_STATE_CONNECTED, 0, null);
                    // createManagementCnn();

                } else {
                    Log.w(TAG, "Set cloud address part 2 fail");
                    mBaseCommCallback.onConnectionStateChange(deviceMac, mType,
                            iHealthDevicesManager.DEVICE_STATE_DISCONNECTED, 0, null);
                }
                break;
            case CreateUser_Management://(0x31),
                muserListInHs5.checkUserInHs5(mUserId, ByteBufferUtil.Bytes2HexString(returnData));
                handleManagement();
                break;
            case AddNewUser://(0x51),
                boolean addResuat = false;
                if (returnData[0] == 1) {
                    addResuat = true;
                } else {
                    Log.w(TAG, "Add new user fail");
                    addResuat = false;
                    // disconnectBroad();
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.ADDUSER_RESULT_HS, addResuat);
                    mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_ADDUSER_HS, o.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case DeleteUser://(0x52),
                boolean resualt = false;
                if (returnData[0] == 1) {
                    resualt = true;
                } else {
                    Log.w(TAG, "Delete user fail");
                    // disconnectBroad();
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.DELETEUSER_RESULT_HS, resualt);
                    mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_DELETEUSER_HS, o.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case UpdateUserInfo://(0x54),
                boolean resualt1 = false;
                if (returnData[0] == 1) {
                    resualt1 = true;
                } else {
                    Log.w(TAG, "Update user information fail");
                    // disconnectBroad();
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.UPDATEUSER_RESULT_HS, resualt1);
                    mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_UPDATEUSER_HS, o.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case MeasureConnection://(0x32),
                if (stateId != 0xa0) {
                    Log.w(TAG, "Measure connection fail");
                    // disconnectBroad();
                }
                break;

            case HistoryConnection://(0x33),
                if (returnData[0] == 1) {
                    Log.w(TAG, "The user isn't in scale");
                } else if (returnData[0] == 2) {
                    transMemoryPara();
                }
                break;
            case GetMemoryData://(0x41),
                int memTototal = returnData[0] & 0xff;
                int memSatisfy = returnData[1] & 0xff;
                if (memSatisfy != 0) {
                    transMemoryData();
                } else {
                    Log.w(TAG, "Has no history");
                    mInsCallback
                            .onNotify(deviceMac, mType, HsProfile.ACTION_NO_HISTORICALDATA, new JSONObject().toString());
                    offlineDataList.clear();
                }
                break;
            case GetHistoryData://(0x42),
                byte num = returnData[2];
                for (int i = 0; i < num; i++) {
                    byte[] offlineData = ByteBufferUtil.bytesCutt(4 + i * 16, 4 + i * 16 + 15, returnData);
                    offlineDataList.add(ByteBufferUtil.Bytes2HexString(offlineData));
                }
                if (returnData[3] != (byte) 0x00) {
                    transMemoryData();
                } else { // no data
                    Log.w(TAG, "No history data");
                    handleHistory(offlineDataList);
                    offlineDataList.clear();
                }
                // WifiDeviceManager.stopUDPSearch = false;
                break;
            case RealTimeData://(0x35),
                stopTimeoutTimer();
                TimeoutTimer();
                int weight = (int) (returnData[0] & 0xff) * 256 + (int) (returnData[1] & 0xff);
                if (weight != 0) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put(HsProfile.LIVEDATA_HS,  (float)( weight / 10.0));
                        mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_LIVEDATA_HS, o.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case StableWeight://(0x36),
                ack((byte) 0x36);
                stopTimeoutTimer();
                TimeoutTimer();

                int mWeight = (int) (returnData[0] & 0xff) * 256 + (int) (returnData[1] & 0xff);
                if (mWeight != 0) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put(HsProfile.STABLEDATA_HS, (float) (mWeight / 10.0));
                        mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_STABLEDATA_HS, o.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case ImpedanceWeight://(0x37),
                ack((byte) 0x37);
                stopTimeoutTimer();
                TimeoutTimer();
                int zWeight = (int) (returnData[0] & 0xff) * 256 + (int) (returnData[1] & 0xff);
                if (zWeight != 0) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put(HsProfile.IMPEDANCEDATA_HS, (float)(zWeight / 10.0));
                        mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_IMPEDANCEDATA_HS, o.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case ResultData://(0x38),
                stopTimeoutTimer();
                // WifiDeviceManager.stopUDPSearch = false;
                SystemClock.sleep(200);
                ack((byte) 0x38);
                int[] result = Hs5DataUtil.parseData(returnData);
                handleResult(result);
                break;
            case Measurement_Stop://(0x39),
                // WifiDeviceManager.stopUDPSearch = false;
                stopTimeoutTimer();
                SystemClock.sleep(200);
                ackA0((byte) 0x39);
                // disconnectBroad();
                break;
            case Error://(0x34);
                int err = returnData[0] & 0xff;
                Log.w(TAG, "error=" + err);
                // WifiDeviceManager.stopUDPSearch = false;
                stopTimeoutTimer();
                SystemClock.sleep(200);
                ack((byte) 0x34);
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.ERROR_NUM_HS, err);
                    mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_ERROR_HS, o.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // disconnectBroad();

                break;
            default:
                // WifiDeviceManager.stopUDPSearch = false;
                stopTimeoutTimer();
                // disconnectBroad();

                break;
        }
    }

    Timer timeoutTimer;
    TimerTask timerTask;

    public void TimeoutTimer() {
        printMethodInfo("TimeoutTimer");
        timeoutTimer = new Timer();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                // WifiDeviceManager.stopUDPSearch = false;
                // disconnectBroad();
                stopTimeoutTimer();
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.ERROR_NUM_HS, 700);
                    mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_ERROR_HS, o.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
        timeoutTimer.schedule(timerTask, 4000);
    }

    public void stopTimeoutTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }

    // fake disconnecting
    private void disconnectBroad() {
        mBaseCommCallback.onConnectionStateChange(deviceMac, mType,
                iHealthDevicesManager.DEVICE_STATE_DISCONNECTED, 0, null);
        // wifiDeviceManager.HS5ConnecttingMap.remove(mHs5Control.getWifiIDPSData().getDeviceMac());
        //
        // wifiDeviceManager.HS5ConnectedWifiMap.remove(mHs5Control.getWifiIDPSData().getDeviceMac());
        //
    }

    private void handleManagement() {
        int info = 0;
        int emptyPosition = muserListInHs5.getFristFreeInScale();
        if (emptyPosition == 0) {
            emptyPosition = -1;
        } else {
            emptyPosition = emptyPosition - 1;
        }
        if (muserListInHs5.getUserInList() == 0) {
            Log.w(TAG, "the user isn't in scale");
            if (muserListInHs5.getFristFreeInScale() == 0) {
                info = 3;
                Log.w(TAG, " the user scale is full need delete");

            } else {
                info = 2;
                Log.w(TAG, "the scale has empty position");
            }
        } else {
            info = 1;
            Log.w(TAG, "the user is in scale");
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(HsProfile.USERINFO_IN_HS, info);
            if (muserListInHs5.getUserInList() == 0) {
                jsonObject.put(HsProfile.USERPOSITION_HS, -1);
            } else {
                jsonObject.put(HsProfile.USERPOSITION_HS, muserListInHs5.getUserInList() - 1);
            }
            jsonObject.put(HsProfile.EMPTYPOSITION_HS, emptyPosition);
            JSONArray jsonArra = new JSONArray();
            for (int i = 0; i < muserListInHs5.getStates().length; i++) {
                jsonArra.put(muserListInHs5.getStates()[i]);
            }
            jsonObject.put(HsProfile.STATUS_HS, jsonArra);
            mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_MANAGEMENT_HS, jsonObject.toString());

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

    }

    private void handleHistory(ArrayList<String> offlinedatalist) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (String offline : offlinedatalist) {
            int[] data = new int[13];
            data = Hs5DataUtil.parseData(ByteBufferUtil.hexStringToByte(offline));
            String str = Hs5DataUtil.getDateStr(data);
            long measureTime = ByteBufferUtil.BirthdayToLong(str);
            long currentTime = Method.getTS();
            if (measureTime <= currentTime) {
                String dataId=PublicMethod.getDataID(deviceMac,(float)( data[6] / 10.0)+"",measureTime);

                JSONObject jsonObject2 = new JSONObject();
                try {
                    jsonObject2.put(HsProfile.DATAID,dataId);
                    jsonObject2.put(HsProfile.MEASUREMENT_DATE_HS, str);
                    jsonObject2.put(HsProfile.WEIGHT_HS,  (float)( data[6] / 10.0));
                    jsonObject2.put(HsProfile.FAT_HS,  (float) (data[7] / 10.0));
                    jsonObject2.put(HsProfile.WATER_HS,  (float) (data[8] / 10.0));
                    jsonObject2.put(HsProfile.MUSCLE_HS, (float) (data[9] / 10.0));
                    jsonObject2.put(HsProfile.SKELETON_HS, (float) (data[10] / 10.0));
                    jsonObject2.put(HsProfile.FATELEVEL_HS, data[11]);
                    jsonObject2.put(HsProfile.DCI_HS, data[12]);
                    jsonArray.put(jsonObject2);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }

            } else {
                Log.w(TAG, "it is future time");
            }

        }
        if (jsonArray.length() > 0) {
            try {
                jsonObject.put(HsProfile.HISTORDATA__HS, jsonArray);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_HISTORICAL_DATA_HS, jsonObject.toString());
        } else {
            mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_NO_HISTORICALDATA, jsonObject.toString());
        }

    }

    private void handleResult(int[] data) {
        // weightR, fat, water, muscle, skeleton, vFatLevel, DCI
        JSONObject jsonObject = new JSONObject();
        float weightR = (float) (data[6] / 10.0);
        float fat = (float) (data[7] / 10.0);
        float water =  (float) (data[8] / 10.0);
        float muscle =  (float) (data[9] / 10.0);
        float skeleton = (float) (data[10] / 10.0);
        int vFatLevel = data[11];
        int DCI = data[12];
        String dataId=PublicMethod.getDataID(deviceMac,weightR+"",PublicMethod.getTs());

        try {
            jsonObject.put(HsProfile.DATAID, MD5.md5String(dataId));
            jsonObject.put(HsProfile.WEIGHT_HS, weightR);
            jsonObject.put(HsProfile.FAT_HS,  fat);
            jsonObject.put(HsProfile.WATER_HS,  water);
            jsonObject.put(HsProfile.MUSCLE_HS,  muscle);
            jsonObject.put(HsProfile.SKELETON_HS,  skeleton);
            jsonObject.put(HsProfile.FATELEVEL_HS, vFatLevel);
            jsonObject.put(HsProfile.DCI_HS, DCI);
            mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_ONLINE_RESULT_HS, jsonObject.toString());
            if (AppsDeviceParameters.isUpLoadData) {
                // 将结果上云 weightR, fat, water, muscle, skeleton, vFatLevel, DCI

                Data_HS_Result hs_Result = Make_Data_Util.makeDataSingleHs(dataId,userName, weightR, fat,
                        water, muscle, skeleton, vFatLevel, DCI, iHealthDevicesManager.TYPE_HS5,
                        deviceMac);
                DataBaseTools db = new DataBaseTools(mContext);
                db.addData(DataBaseConstants.TABLE_TB_HSRESULT, hs_Result);
                // 开启数据上云
                HS_InAuthor sdk_InAuthor = HS_InAuthor.getInstance();
                sdk_InAuthor.initAuthor(mContext, userName);
                sdk_InAuthor.run();
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {

    }
}
