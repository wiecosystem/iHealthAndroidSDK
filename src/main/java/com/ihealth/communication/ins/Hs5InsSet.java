
package com.ihealth.communication.ins;

import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.bt.BtCommThread;
import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BtCommProtocol;
import com.ihealth.communication.control.HsProfile;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class Hs5InsSet implements NewDataCallback {

    private static final String TAG = "Hs5InsSet";

    public static final String MSG_SETTING_WIFI = "com.msg.setting.wifi";
    public static final String MSG_SET_WIFI_SUCCESS = "com.msg.wifi.success";
    public static final String MSG_SET_WIFI_FAIL = "com.msg.wifi.fail";
    public static final String MSG_NOT_FOUND = "com.msg.not.found";

    private BtCommProtocol btcom;
    private BaseCommCallback mBaseCommCallback;
    private InsCallback mInsCallback;
    private String deviceMac = "";
    private String mType = "";

    private static void printMethodInfo(String methodName, Object... parameters) {
        Log.p(TAG, Log.Level.INFO, methodName, parameters);
    }
    public Hs5InsSet(String mac, String type, BaseComm com, BaseCommCallback baseCommCallback, InsCallback insCallback) {
        printMethodInfo("Hs5InsSet_Constructor", mac, type);
        this.btcom = new BtCommProtocol(com, this);
        mInsCallback = insCallback;
        mBaseCommCallback = baseCommCallback;
        this.deviceMac = mac;
        this.mType = type;

    }

    public void setWifi(String ssid, int type, String pw) {
        printMethodInfo("setWifi", ssid, type, pw);
        byte[] pwd = new byte[32];
        byte[] pwd1 = pw.getBytes();
        System.arraycopy(pwd1, 0, pwd, 0, pwd1.length);
        int lenPwd = pwd.length;
        byte[] ssidbyte = ssid.getBytes();
        int lenSsid = ssidbyte.length;
        byte[] commandWifi = new byte[34 + lenPwd];
        commandWifi[0] = (byte) 0x00;
        commandWifi[1] = (byte) type;
        for (int i = 0; i < lenSsid; i++) {
            commandWifi[2 + i] = ssidbyte[i];
        }
        for (int i = lenSsid + 2; i < 34; i++) {
            commandWifi[i] = (byte) 0x00;
        }
        for (int i = 34; i < lenPwd + 34; i++) {
            commandWifi[i] = pwd[i - 34];
        }
        btcom.packageDataWithoutProtocol(commandWifi);
        checkState();
    }

    private void check() {
        byte[] stateByte = {
                (byte) 0xA9, (byte) 0xE9
        };
        btcom.packageData(null, stateByte);
    }

    private Timer timer;
    private TimerTask btTask;

    public void checkState() {
        printMethodInfo("checkState");

        timer = new Timer();
        btTask = new TimerTask() {
            @Override
            public void run() {
                if (!BtCommThread.ioException) {
                    check();
                } else {
                    cancelTimer();
                    if (state == 1) {// 正在设置wifi中且断开了连接 为设置wifi失败
                        try {
                            JSONObject o = new JSONObject();
                            mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_SETWIFI_FAIL,
                                    o.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        timer.schedule(btTask, delay, period);
    }

    public void cancelTimer() {
        printMethodInfo("cancelTimer");
        if (timer != null)
            timer.cancel();
        if (btTask != null)
            btTask.cancel();
    }

    private long delay = 500;
    private long period = 1000;
    private int state;

    /**
     * check the state of setting wifi: 0 check fail；1 setting ；2 success ；3 fail ；4: command wrong
     */

    @Override
    public void haveNewData(int what, int stateId, byte[] command) {
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", String.format("0x%02X", what), stateId, ByteBufferUtil.Bytes2HexString(command));
        switch (what) {
            case 0xe0:
                state = command[0];
                Log.d(TAG, "haveNewData:" + state);
                switch (state) {
                    case 1:
                        mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_SETTINGWIFI, null);
                        break;
                    case 2:
                        cancelTimer();
                        mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_SETWIFI_SUCCESS, null);
                        break;
                    case 3:
                        cancelTimer();
                        mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_SETWIFI_FAIL, null);
                        break;
                    default:
                        cancelTimer();
                        mInsCallback.onNotify(deviceMac, mType, HsProfile.ACTION_SETWIFI_UNKNOW, null);
                        break;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {

    }

    public void destroy(){

    }
}
