
package com.ihealth.communication.ins;

import android.content.Context;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.base.protocol.BleCommProtocol;
import com.ihealth.communication.cloud.data.DataBaseConstants;
import com.ihealth.communication.cloud.data.DataBaseTools;
import com.ihealth.communication.cloud.data.Data_PO_Result;
import com.ihealth.communication.cloud.data.Make_Data_Util;
import com.ihealth.communication.cloud.data.PO_InAuthor;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.control.HsProfile;
import com.ihealth.communication.control.Po3Control;
import com.ihealth.communication.control.PoProfile;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.Log;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @ClassName: AcInsSet
 * @Description: TODO
 * @hide
 */
public class AcInsSet extends IdentifyIns implements NewDataCallback, GetBaseCommProtocolCallback {

    private static final String TAG = "AcInsSet";
    private static final byte deviceType = (byte) 0xac;

    private String mAddress;
    private String mType;
    private BleCommProtocol blecm;
    private BaseCommCallback mBaseCommCallback;
    private InsCallback mInsCallback;
    private Context mContext;
    private String accessoryFirm;
    private String userName;

    public AcInsSet(String username, Context context, BaseComm com, String mac, String accessoryFirm, String type,
                    BaseCommCallback baseCommCallback,
                    InsCallback insCallback) {
        Log.p(TAG, Log.Level.INFO, "AcInsSet", username, mac, accessoryFirm, type);
        this.userName = username;
        this.mAddress = mac;
        this.mType = type;
        mBaseCommCallback = baseCommCallback;
        mInsCallback = insCallback;
        mContext = context;
        this.accessoryFirm = accessoryFirm;
        blecm = new BleCommProtocol(context, com, mAddress, deviceType, this);
        loadSyncTime();
        setInsSetCallbak(insCallback, mac, type, com);
    }

    private void loadSyncTime() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                POMethod.setSyncTime(mContext, mAddress);
            }
        }.start();
    }

    public void identify() {
        Log.p(TAG, Log.Level.INFO, "identify");
        startTimeout(0xfa, AppsDeviceParameters.Delay_Medium, 0xfb, 0xfd, 0xfe);
        blecm.packageData(mAddress, identify(deviceType));
    }

    /**
     * back to factory setting
     */
//    public void a1Ins() {
//        Log.p(TAG, Log.Level.INFO, "a1Ins");
//        byte commandID = (byte) 0xA1;
//        byte[] returnCommand = new byte[2];
//        returnCommand[0] = deviceType;
//        returnCommand[1] = commandID;
//        blecm.packageData(mAddress, returnCommand);
//    }

    /**
     * sync time
     */
    public void a2Ins() {
        Log.p(TAG, Log.Level.INFO, "a2Ins");
        Calendar calenda = Calendar.getInstance();
        calenda.setTimeZone(TimeZone.getDefault());
        Integer year = calenda.get(Calendar.YEAR) - 2000;
        Integer month = calenda.get(Calendar.MONTH) + 1;
        Integer day = calenda.get(Calendar.DAY_OF_MONTH);
        Integer hour = calenda.get(Calendar.HOUR_OF_DAY);
        Integer min = calenda.get(Calendar.MINUTE);
        Integer sed = calenda.get(Calendar.SECOND);
        byte bYear = year.byteValue();
        byte bMonth = month.byteValue();
        byte bDay = day.byteValue();
        byte bHour = hour.byteValue();
        byte bMin = min.byteValue();
        byte bSed = sed.byteValue();
        byte commandID = (byte) 0xa2;
        byte[] returnCommand = new byte[8];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = bYear;
        returnCommand[3] = bMonth;
        returnCommand[4] = bDay;
        returnCommand[5] = bHour;
        returnCommand[6] = bMin;
        returnCommand[7] = bSed;
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * return user id
     */
//    public void a3Ins() {
//        Log.p(TAG, Log.Level.INFO, "a3Ins");
//        byte commandID = (byte) 0xA3;
//        byte[] returnCommand = new byte[2];
//        returnCommand[0] = deviceType;
//        returnCommand[1] = commandID;
//        blecm.packageData(mAddress, returnCommand);
//    }

    /**
     * real time data
     */
    private void a5Ins() {
        byte commandID = (byte) 0xA5;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * a8 reply
     */
    private void a8Ins() {
        byte commandID = (byte) 0xA8;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * history data
     */
    public void a9Ins() {
        Log.p(TAG, Log.Level.INFO, "a9Ins");
        byte commandID = (byte) 0xA9;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

//    public void acIns() {
//        Log.p(TAG, Log.Level.INFO, "acIns");
//        byte commandID = (byte) 0xAC;
//        byte[] returnCommand = new byte[2];
//        returnCommand[0] = deviceType;
//        returnCommand[1] = commandID;
//        blecm.packageData(mAddress, returnCommand);
//    }

//    public void adIns() {
//        Log.p(TAG, Log.Level.INFO, "adIns");
//        byte commandID = (byte) 0xAD;
//        byte[] returnCommand = new byte[2];
//        returnCommand[0] = deviceType;
//        returnCommand[1] = commandID;
//        blecm.packageData(mAddress, returnCommand);
//    }

    private void aaIns() {
        Log.p(TAG, Log.Level.INFO, "aaIns");
        byte commandID = (byte) 0xAA;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get battery
     */
    public void c1Ins() {
        Log.p(TAG, Log.Level.INFO, "c1Ins");
        byte commandID = (byte) 0xC1;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

    private void allPkgOk(byte commandID) {
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

    private List<byte[]> listHistory = new ArrayList<>();
    private byte[] historys;

    private int offNum = 0;

    public int getOffNum() {
        return offNum;
    }

    private boolean isRealTimeData = false;
    private byte[] result; //上次数据的缓存
    private static ArrayList<String> realLineWave = new ArrayList<>(); //数据库以A加点拼接字符串

    @Override
    public void haveNewData(int what, int stateId, byte[] command) {
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", what, stateId, ByteBufferUtil.Bytes2HexString(command));
        String hex = Integer.toHexString(what & 0xFF);
        if (hex.length() == 1) {
            hex = '0' + hex;
        }
        stopTimeout(what);
        switch (what) {
            case 0xfb:
                byte[] req = deciphering(command, mType, deviceType);
                startTimeout(0xfc, AppsDeviceParameters.Delay_Medium, 0xfd, 0xfe);
                blecm.packageData(mAddress, req);
                break;
            case 0xfd:
                mBaseCommCallback.onConnectionStateChange(mAddress, mType,
                        iHealthDevicesManager.DEVICE_STATE_CONNECTED, 0, null);
                break;
            case 0xa1:
                break;
            case 0xa2:
                a5Ins();
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        POMethod.syncPotime(mContext, mAddress);
                    }
                }.start();
                // c1Ins();
                break;
            case 0xa5:
                break;
            case 0xa6:
                allPkgOk((byte) 0xa6);
                break;
            case 0xa7:
                try {
                    int[] resultData = POMethod.getOnlineData(command);
                    for (int i = 0; i < 3; i++) {
                        realLineWave.add("A" + result[5 + i]);
                    }
                    JSONObject o = POMethod.changeData2JSON(resultData);
                    assert o != null;
                    mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_LIVEDA_PO, o.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                result = command;
                isRealTimeData = true;
                msgPo3ResultData(result);
                break;
            case 0xa8:
                a8Ins();
                if (isRealTimeData) {
                    isRealTimeData = false;
                    cancelResualtTimer();
                    int[] resultData = POMethod.getOnlineData(result);
                    String dataId = PublicMethod.getDataID(mAddress, resultData[0] + "", PublicMethod.getTs());
                    try {
                        JSONObject o = POMethod.changeData2JSON(resultData);
                        assert o != null;
                        o.put(PoProfile.DATAID, MD5.md5String(dataId));
                        mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_RESULTDATA_PO, o.toString());
                    } catch (Exception e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                    //upload data
                    if (AppsDeviceParameters.isUpLoadData) {
                        String wave = "";
                        if (realLineWave.size() >= 90) {
                            for (int i = realLineWave.size() - 90; i < realLineWave.size(); i++) {
                                wave = wave + realLineWave.get(i);
                            }
                        } else {
                            for (int i = 0; i < realLineWave.size(); i++) {
                                wave = wave + realLineWave.get(i);
                            }
                        }
                        realLineWave.clear();
                        if (wave.length() > 2) {
                            wave = wave.substring(1);
                        }
                        // data upload cloud
                        Data_PO_Result po_Result = Make_Data_Util.makeDataSinglePo(dataId, userName, resultData[1],
                                resultData[0],
                                resultData[2], mAddress, wave);
                        DataBaseTools db = new DataBaseTools(mContext);
                        db.addData(DataBaseConstants.TABLE_TB_PO, po_Result);
                        // 开启数据上云
                        PO_InAuthor sdk_InAuthor = PO_InAuthor.getInstance();
                        sdk_InAuthor.initAuthor(mContext, userName);
                        sdk_InAuthor.run();
                    }

                }
                break;
            case 0xa9:
                offNum = command[0];
                if (offNum == 0) {
                    try {
                        JSONObject o = new JSONObject();
                        mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_NO_OFFLINEDATA_PO, o.toString());
                    } catch (Exception e) {
                        Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                    }
                } else {
                    aaIns();
                }
                break;
            case 0xaa:
                break;
            case 0xab:
                allPkgOk((byte) 0xab);
                historys = ByteBufferUtil.BufferMerger(null, command);
                break;
            case 0xac:
                allPkgOk((byte) 0xac);
                byte[] temphistorys = ByteBufferUtil.BufferMerger(historys, command);
                listHistory.add(temphistorys);
                historys = null;
                break;
            case 0xad:
                allPkgOk((byte) 0xad);
                try {
                    Object object = new Object();
                    String messageString = POMethod.changeOfflineData2JSON(mContext, mAddress, accessoryFirm,
                            listHistory);
                    if (messageString == null) {
                        mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_NO_OFFLINEDATA_PO, object.toString());
                    } else {
                        mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_OFFLINEDATA_PO, messageString);
                    }
                } catch (JSONException e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                listHistory.clear();
                a5Ins();
                break;
            case 0xc1:
                int battery = command[0];
                try {
                    JSONObject o = new JSONObject();
                    o.put(PoProfile.BATTERY_PO, battery);
                    mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_BATTERY_PO, o.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }

                // a9Ins();
                break;
            default:
                break;
        }
    }

    private Timer timeoutTimer;
    private TimerTask timeoutTask;

    public void TimeoutTimer() {
        Log.p(TAG, Log.Level.INFO, "TimeoutTimer");
        stopTimeoutTimer();

        timeoutTimer = new Timer();
        timeoutTask = new TimerTask() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                // WifiDeviceManager.stopUDPSearch = false;
                // disconnectBroad();
                stopTimeoutTimer();
                try {
                    JSONObject o = new JSONObject();
                    o.put(HsProfile.ERROR_NUM_HS, 700);
                    mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_ERROR_PO, o.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
            }

        };
        timeoutTimer.schedule(timeoutTask, 4000);
    }

    private void stopTimeoutTimer() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }


    private Timer timer;
    private TimerTask timerTask;

    private void msgPo3ResultData(final byte[] data) {
        cancelResualtTimer();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                cancelResualtTimer();

                //jing 20160726 添加设备断开的特殊处理，不返回结果
                Po3Control po3Control = iHealthDevicesManager.getInstance().getPo3Control(mAddress);
                if (po3Control == null) {
                    return;
                }
                try {
                    int[] resultData = POMethod.getOnlineData(data);
                    JSONObject o = POMethod.changeData2JSON(resultData);
                    String dataId = PublicMethod.getDataID(mAddress, resultData[0] + "", PublicMethod.getTs());
                    assert o != null;
                    o.put(PoProfile.DATAID, MD5.md5String(dataId));
                    mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_RESULTDATA_PO, o.toString());
                    //upload
                    if (AppsDeviceParameters.isUpLoadData) {
                        String wave = "";
                        if (realLineWave.size() >= 90) {
                            for (int i = realLineWave.size() - 90; i < realLineWave.size(); i++) {
                                wave = wave + realLineWave.get(i);
                            }
                        } else {
                            for (int i = 0; i < realLineWave.size(); i++) {
                                wave = wave + realLineWave.get(i);
                            }
                        }
                        realLineWave.clear();
                        if (wave.length() > 2) {
                            wave = wave.substring(1);
                        }
                        // data upload cloud
                        Data_PO_Result po_Result = Make_Data_Util.makeDataSinglePo(dataId, userName, resultData[1],
                                resultData[0],
                                resultData[2], mAddress, wave);
                        DataBaseTools db = new DataBaseTools(mContext);
                        db.addData(DataBaseConstants.TABLE_TB_PO, po_Result);
                        // 开启数据上云
                        PO_InAuthor sdk_InAuthor = PO_InAuthor.getInstance();
                        sdk_InAuthor.initAuthor(mContext, userName);
                        sdk_InAuthor.run();
                    }

                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
            }
        };
        timer.schedule(timerTask, 1200);
    }

    private void cancelResualtTimer() {
        if (timerTask != null)
            timerTask.cancel();
        if (timer != null)
            timer.cancel();
        timer = null;
        timerTask = null;
    }

    @Override
    public BaseCommProtocol getBaseCommProtocol() {
        return blecm;
    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {

    }

    public void destroy() {
        Log.p(TAG, Log.Level.INFO, "destroy");
        mBaseCommCallback = null;
        mInsCallback = null;
        mContext = null;
        if (blecm != null)
            blecm.destroy();
        blecm = null;
    }
}
