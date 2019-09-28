
package com.ihealth.communication.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

import com.ihealth.communication.base.ble.AndroidBle;
import com.ihealth.communication.base.ble.BleComm;
import com.ihealth.communication.base.ble.BleConfig;
import com.ihealth.communication.base.ble.BleConfig.BleUuid;
import com.ihealth.communication.base.ble.BtleCallback;
import com.ihealth.communication.base.bt.BtCommThread;
import com.ihealth.communication.base.bt.BtCommThreadEE;
import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.usb.FtdiUsb;
import com.ihealth.communication.base.usb.Pl2303Usb;
import com.ihealth.communication.base.wifi.UdpSearchCallback;
import com.ihealth.communication.base.wifi.WifiCommThread;
import com.ihealth.communication.clientmanager.iHealthDeviceClientMap;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.cloudmanager.iHealthDeviceCloudManager;
import com.ihealth.communication.control.ABPMControl;
import com.ihealth.communication.control.AbiControl;
import com.ihealth.communication.control.Am3Control;
import com.ihealth.communication.control.Am3sControl;
import com.ihealth.communication.control.Am4Control;
import com.ihealth.communication.control.BPControl;
import com.ihealth.communication.control.Bg5Control;
import com.ihealth.communication.control.Bg5lControl;
import com.ihealth.communication.control.BgControl;
import com.ihealth.communication.control.Bp3lControl;
import com.ihealth.communication.control.Bp3mControl;
import com.ihealth.communication.control.Bp550BTControl;
import com.ihealth.communication.control.Bp5Control;
import com.ihealth.communication.control.Bp723Control;
import com.ihealth.communication.control.Bp7Control;
import com.ihealth.communication.control.Bp7sControl;
import com.ihealth.communication.control.Bp926Control;
import com.ihealth.communication.control.BsControl;
import com.ihealth.communication.control.BtmControl;
import com.ihealth.communication.control.DeviceControl;
import com.ihealth.communication.control.Hs3Control;
import com.ihealth.communication.control.Hs4Control;
import com.ihealth.communication.control.Hs4sControl;
import com.ihealth.communication.control.Hs5Control;
import com.ihealth.communication.control.Hs5ControlForBt;
import com.ihealth.communication.control.HsControl;
import com.ihealth.communication.control.Po3Control;
import com.ihealth.communication.control.PoControl;
import com.ihealth.communication.ins.InsCallback;
import com.ihealth.communication.privatecontrol.AbiControlSubManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.IDPS;
import com.ihealth.communication.utils.Log;
import com.ihealth.communication.commandcache.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h1>iHealth SDK document</h1>
 * <p/>
 * <h3>1. the need to introduce the file and related configuration</h3>
 * <p/>
 * Need to introduce the development kit iHealthLibrary.jar. Which are ABI BP3, BP5, BP7, BG1, BG5,
 * BP, HS3, Android4.0 and HS5 support AM3 and its version; AM3S, HS4, and PO3 support Android4.3
 * and the above version and Android4.4 Samsung brand mobile phone.
 * <p/>
 * <img src="{@docRoot}/ihealthdemo.png"></img>
 * <p/>
 * <h3>2. standard operating procedures</h3>
 * <p/>
 * <h5>(1) Initialization iHealth SDK.</h5>
 * {@see iHealthDevicesManager#init(Context)}
 * <p/>
 * <h5>(2) Verify iHealth device uses-permission.</h5>
 * {@seeiHealthDevicesManager#sdkUserInAuthor(Context, String, String, String, int)}
 * <p/>
 * <h5>(3) Register callback, and get a callback ID.</h5>
 * {@seeiHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
 * <p/>
 * <h5>(4) Add callback filter.</h5>
 * {@see iHealthDevicesManager#addCallbackFilterForAddress(int, String...)}
 * {@see iHealthDevicesManager#addCallbackFilterForDeviceType(int, String...)}
 * <p/>
 * <h5>(5) Discovery a iHealth device.</h5>
 * {@see iHealthDevicesManager#startDiscovery(int)}
 * <p/>
 * <h5>(6) Connection a iHealth device.</h5>
 * {@see iHealthDevicesManager#connectDevice(String, String)}
 * <p/>
 * <h5>(7) Get iHealth device controller.</h5>
 * <p/>
 * {@see iHealthDevicesManager#getAm3Control(String)} <br>
 * {@see iHealthDevicesManager#getAm3sControl(String)} <br>
 * {@see iHealthDevicesManager#getBp3lControl(String)} <br>
 * {@see iHealthDevicesManager#getBp3mControl(String)} <br>
 * {@see iHealthDevicesManager#getBp5Control(String)} <br>
 * {@see iHealthDevicesManager#getBp7Control(String)} <br>
 * {@see iHealthDevicesManager#getHs4Control(String)} <br>
 * {@see iHealthDevicesManager#getHs4sControl(String)} <br>
 * {@see iHealthDevicesManager#getHs5Control(String)} <br>
 * {@see iHealthDevicesManager#getHs5ControlForBt(String)} <br>
 * {@see iHealthDevicesManager#getPo3Control(String)} <br>
 */
public class iHealthDevicesManager {

    private static final String TAG = "Runtime_iHealthDM";
    public final static String IHEALTH_DEVICE_MAC = "mac";
    public final static String IHEALTH_DEVICE_TYPE = "type";
    public final static String IHEALTH_DEVICE_NAME = "name";
    public final static String IHEALTH_COMM_TIMEOUT = "communicate_timeout";
    public final static String IHEALTH_COMM_TIMEOUT_COMMAND_ID = "communicate_timeout_id";

    public final static String IHEALTH_MSG_BG5_EE = "com.ihealth.msg.btdevicemanager.bt.bg5.ee";
    public static final String IHEALTH_MSG_BG5_EE_EXTRA = "com.ihealth.msg.btdevicemanager.bt.bg5.extra";

    public final static String MSG_BASECOMMTIMEOUT = "ihealth_base_comm_timeout";

    private BtCommThreadEE btCommThreadee = null;

    private long discoveryType = 0;
    private long discoveryTypeForBle = 0;

    /**
     * Indicates the Am3 is allowed to be searched.
     */
    public final static long DISCOVERY_AM3 = 1 << 0;

    /**
     * Indicates the Am3s is allowed to be searched.
     */
    public final static long DISCOVERY_AM3S = 1 << 1;

    /**
     * Indicates the Am4 is allowed to be searched.
     */
    public final static long DISCOVERY_AM4 = 1 << 2;

    /**
     * Indicates the Po3 is allowed to be searched.
     */
    public final static long DISCOVERY_PO3 = 1 << 3;

    /**
     * Indicates the Hs4 is allowed to be searched.
     */
    public final static long DISCOVERY_HS4 = 1 << 4;

    /**
     * Indicates the Bp3l is allowed to be searched.
     */
    public final static long DISCOVERY_BP3L = 1 << 5;

    /**
     * Indicates the Bp3l is allowed to be searched.
     */
    public final static long DISCOVERY_BTM = 1 << 6;

    /**
     * Indicates the Bp550BT is allowed to be searched.
     */
    public final static long DISCOVERY_BP550BT = 1 << 7;

    /**
     * Indicates the Bp926 is allowed to be searched.
     */
    public final static long DISCOVERY_KD926 = 1 << 8;

    /**
     * Indicates the Bp723 is allowed to be searched.
     */
    public final static long DISCOVERY_KD723 = 1 << 9;

    /**
     * Indicates the ABPM is allowed to be searched.
     */
    public final static long DISCOVERY_ABPM = 1 << 10;

    /**
     * Indicates the continua bp is allowed to be searched.
     */
    public final static long DISCOVERY_CBP = 1 << 11;
    /**
     * Indicates the Bg5L is allowed to be searched.
     */
    public final static long DISCOVERY_BG5l = 1 << 12;
    /**
     * Indicates the continua bg is allowed to be searched.
     */
    public final static long DISCOVERY_CBG = 1 << 13;
    /**
     * Indicates the continua po is allowed to be searched.
     */
    public final static long DISCOVERY_CPO = 1 << 13;
    /**
     * Indicates the continua hs is allowed to be searched.
     */
    public final static long DISCOVERY_CHS = 1 << 13;
    /**
     * Indicates the continua body composition scale is allowed to be searched.
     */
    public final static long DISCOVERY_CBS = 1 << 13;

    /**
     * Indicates the Bp3m is allowed to be searched.
     */
    public final static long DISCOVERY_BP3M = 1 << 23;
    /**
     * Indicates the Bp7s is allowed to be searched.
     */
    public final static long DISCOVERY_BP7S = 1 << 24;
    /**
     * Indicates the Bp5 is allowed to be searched.
     */
    public final static long DISCOVERY_BP5 = 1 << 25;

    /**
     * Indicates the Bp7 is allowed to be searched.
     */
    public final static long DISCOVERY_BP7 = 1 << 26;

    /**
     * Indicates the Hs3 is allowed to be searched.
     */
    public final static long DISCOVERY_HS3 = 1 << 27;
    /**
     * Indicates the Hs4s is allowed to be searched.
     */
    public final static long DISCOVERY_HS4S = 1 << 28;

    /**
     * Indicates the Hs5 is allowed to be searched for bluetooth.
     */
    public final static long DISCOVERY_HS5_BT = 1 << 29;

    /**
     * Indicates the Hs5 is allowed to be searched for wifi.
     */
    public final static long DISCOVERY_HS5_WIFI = 1 << 30;

    /**
     * Indicates the Bg5 is allowed to be searched.
     */
    public final static long DISCOVERY_BG5 = (long) 1 << 32;

    /**
     * Indicates the Am3 is allowed to be searched
     */
    public static final String TYPE_BP3M = "BP3M";

    /**
     * Indicates the Am3 is allowed to be searched
     */
    public static final String TYPE_BP3L = "BP3L";

    /**
     * Indicates the Am3 is allowed to be searched
     */
    public static final String TYPE_BP5 = "BP5";

    /**
     * Indicates the Am3 is allowed to be searched
     */
    public static final String TYPE_BP7 = "BP7";

    /**
     * Indicates the Bp7s is allowed to be searched
     */
    public static final String TYPE_BP7S = "BP7S";

    /**
     * Indicates the 550bt is allowed to be searched
     */
    public static final String TYPE_550BT = "550bt";

    /**
     * Indicates the 926 is allowed to be searched
     */
    public static final String TYPE_KD926 = "KD-926";

    /**
     * Indicates the 723 is allowed to be searched
     */
    public static final String TYPE_KD723 = "KD-723";

    /**
     * Indicates the ABPM is allowed to be searched
     */
    public static final String TYPE_ABPM = "BPW3";

    /**
     * Indicates that is device type of Bg1.
     */
    public static final String TYPE_BG1 = "BG1";

    /**
     * Indicates that is device type of Bg5.
     */
    public static final String TYPE_BG5 = "BG5";

    /**
     * Indicates that is device type of Hs3.
     */
    public static final String TYPE_HS3 = "HS3";

    /**
     * Indicates that is device type of Hs4.
     */
    public static final String TYPE_HS4 = "HS4";

    /**
     * Indicates that is device type of Hs4s.
     */
    public static final String TYPE_HS4S = "HS4S";

    /**
     * Indicates that is device type of Hs5.
     */
    public static final String TYPE_HS5 = "HS5";

    /**
     * Indicates that is device type of Hs6.
     */
    public static final String TYPE_HS6 = "HS6";

    /**
     * Indicates that is device type of Am3.
     */
    public static final String TYPE_AM3 = "AM3";

    /**
     * Indicates that is device type of Am3s.
     */
    public static final String TYPE_AM3S = "AM3S";

    /**
     * Indicates that is device type of Am4.
     */
    public static final String TYPE_AM4 = "AM4";

    /**
     * Indicates that is device type of Po3.
     */
    public static final String TYPE_PO3 = "PO3";

    /**
     * Indicates that is device type of Hs5 for Bluetooth.
     */
    public static final String TYPE_HS5_BT = "HS5BT";

    /**
     * Indicates that is device type of BT.
     */
    public static final String TYPE_BTM = "BTM";

    /**
     * Indicates that is device type of Continua BP.
     */
    public static final String TYPE_CBP = "CBP";
    /**
     * Indicates that is device type of Bg5L.
     */
    public static final String TYPE_BG5l = "BG5L";
    /**
     * Indicates that is device type of Continua BG.
     */
    public static final String TYPE_CBG = "CBG";
    /**
     * Indicates that is device type of Continua PO.
     */
    public static final String TYPE_CPO = "CPO";
    /**
     * Indicates that is device type of Continua HS.
     */
    public static final String TYPE_CHS = "CHS";
    /**
     * Indicates that is device type of Continua Body Composition Scale.
     */
    public static final String TYPE_CBS = "CBS";


    private static final String NAME_AM3 = "Activity Monitor";
    private static final String NAME_AM3S = "AM3S";
    private static final String NAME_AM4 = "AM4";
    private static final String NAME_PO3 = "Pulse Oximeter";
    private static final String NAME_HS3 = "iHealth HS3";
    private static final String NAME_HS4 = "Body Scale";
    private static final String NAME_HS5 = "iHealth HS5";
    private static final String NAME_HS4S = "HS4S";
    private static final String NAME_BG5 = "BG5";
    private static final String NAME_550BT = "550";
    private static final String NAME_KD926 = "926";
    private static final String NAME_KD723 = "723";
    private static final String NAME_ABPM = "BP Monitor";
    private static final String NAME_BTM = "FDTH";
    private static final String NAME_BG5l = "BG5";

    private Context mContext;
    private BluetoothAdapter bluetoothAdapter;

    private static class SingletonHolder {
        static final iHealthDevicesManager INSTANCE = new iHealthDevicesManager();
    }

    /**
     * Get the iHealthDevicesManager instance.
     *
     * @return iHealthDevicesManager instance
     */
    public static iHealthDevicesManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private iHealthDevicesManager() {

    }

    private Map<String, Bp3mControl> mapBp3mControl = new ConcurrentHashMap<String, Bp3mControl>();
    private Map<String, Bp3lControl> mapBp3lControl = new ConcurrentHashMap<String, Bp3lControl>();
    private Map<String, Bp5Control> mapBp5Control = new ConcurrentHashMap<String, Bp5Control>();
    private Map<String, Bp7Control> mapBp7Control = new ConcurrentHashMap<String, Bp7Control>();
    private Map<String, Bp7sControl> mapBp7sControl = new ConcurrentHashMap<String, Bp7sControl>();
    private Map<String, Bp550BTControl> mapBp550BTControl = new ConcurrentHashMap<String, Bp550BTControl>();
    private Map<String, Bp926Control> mapBp926Control = new ConcurrentHashMap<String, Bp926Control>();
    private Map<String, Bp723Control> mapBp723Control = new ConcurrentHashMap<String, Bp723Control>();
    private Map<String, ABPMControl> mapABPMControl = new ConcurrentHashMap<String, ABPMControl>();

    private Map<String, Hs3Control> mapHs3Control = new ConcurrentHashMap<String, Hs3Control>();
    private Map<String, Hs4Control> mapHs4Control = new ConcurrentHashMap<String, Hs4Control>();
    private Map<String, Hs4sControl> mapHs4sControl = new ConcurrentHashMap<String, Hs4sControl>();
    private Map<String, Hs5ControlForBt> mapHs5ControlForBt = new ConcurrentHashMap<String, Hs5ControlForBt>();
    private Map<String, Am3Control> mapAm3Control = new ConcurrentHashMap<String, Am3Control>();
    private Map<String, Am3sControl> mapAm3sControl = new ConcurrentHashMap<String, Am3sControl>();
    private Map<String, Am4Control> mapAm4Control = new ConcurrentHashMap<String, Am4Control>();
    private Map<String, Po3Control> mapPo3Control = new ConcurrentHashMap<String, Po3Control>();
    private Map<String, Bg5lControl> mapBG5lControl = new ConcurrentHashMap<String, Bg5lControl>();

    private Map<String, Hs5Control> HS5WifiMap = new ConcurrentHashMap<String, Hs5Control>(); // 提供界面的
    private Map<String, Hs5Control> HS5ConnectedWifiMap = new ConcurrentHashMap<String, Hs5Control>(); // 假断开
    private Map<String, String> HS5ConnecttingMap = new ConcurrentHashMap<String, String>();
    private Map<String, Integer> HS5CountMap = new ConcurrentHashMap<String, Integer>(); // 5次扫描不到断开连接

    private Map<String, Bg5Control> mapBg5Control = new ConcurrentHashMap<String, Bg5Control>();
    private Map<String, String> BTConnecttingMap = new ConcurrentHashMap<String, String>();

    private Map<String, BtmControl> mapBtmControl = new ConcurrentHashMap<String, BtmControl>();
    private Map<String, BPControl> mapBpControl = new ConcurrentHashMap<>();
    private Map<String, BgControl> mapBgControl = new ConcurrentHashMap<>();
    private Map<String, PoControl> mapPoControl = new ConcurrentHashMap<>();
    private Map<String, HsControl> mapHsControl = new ConcurrentHashMap<>();
    private Map<String, BsControl> mapBsControl = new ConcurrentHashMap<>();


    public Hs5Control getHs5Control(String mac) {
        return HS5WifiMap.get(mac);
    }

    public int getHs5Number() {
        return HS5WifiMap.size();
    }

    private Map<String, ScanDevice> scanBlueDevicesMap = new ConcurrentHashMap<String, ScanDevice>();// 扫描到的蓝牙设备map
    private Map<String, String> mapMacAndType = new ConcurrentHashMap<String, String>();//存储设备MAC和Type的对应关系


    public Map<String, CommandCacheControl> commandCacheControlMap = new ConcurrentHashMap<>();

    public void init(Context mContext) {
        Log.p(TAG, Log.Level.INFO, "init");
        Log.w(TAG,"iHealthLibrary Version:" + AppsDeviceParameters.APP_VERSION);
        mapBg5Control.clear();

        scanBlueDevicesMap.clear();
        mapBp3mControl.clear();
        mapBp3lControl.clear();
        mapBp5Control.clear();
        mapBp7Control.clear();
        mapBp7sControl.clear();
        mapBp550BTControl.clear();
        mapBp926Control.clear();
        mapBp723Control.clear();
        mapABPMControl.clear();
        mapHs3Control.clear();
        mapHs4Control.clear();
        mapHs4sControl.clear();
        mapHs5ControlForBt.clear();
        mapAm3Control.clear();
        mapAm3sControl.clear();
        mapAm4Control.clear();
        mapPo3Control.clear();
        mapBtmControl.clear();
        mapBpControl.clear();
        mapBG5lControl.clear();
        mapBgControl.clear();
        mapHsControl.clear();
        mapPoControl.clear();
        mapHsControl.clear();
        mapBsControl.clear();

        BTConnecttingMap.clear();
//        mapMacAndType.clear();

        this.mContext = mContext;
        // this.mUserid = userid;
        bleInit();
        // hs5wifi
        HS5WifiMap.clear();
        HS5ConnectedWifiMap.clear();
        HS5ConnecttingMap.clear();
        HS5CountMap.clear();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initReceiver();
        // wifi
        wifiInit(mContext, mBaseCommCallback, mInsCallback);
        //upgrade
        iHealthDevicesUpgradeManager.getInstance().init(mContext, mInsCallback);

        //jing 20160727
        String packageName = mContext.getPackageName();
        if (packageName.length() >= 12) {
            String tempPackageName = getOurPackageName(packageName.substring(0, 12));
            if (tempPackageName.equals("af7043a8b34a1bf4ebe9949bdf94f73870")) {
                AppsDeviceParameters.isUpLoadData = false;
            } else {
                AppsDeviceParameters.isUpLoadData = true;
            }
        }
        if (AppsDeviceParameters.isUpLoadData == true) {
            String ourPackageName = getOurPackageName(packageName);
            if (ourPackageName.equals("cebc91e451a413cc66d5253aacb8640bbc") || ourPackageName.equals("ad9541cd84371aa9a98dfbc200cd25fa95") || ourPackageName.equals("e67478cfd3493889c5c9eb80a26501bd74") || ourPackageName.equals("b4552034f0f4bb02cdcd89afe0628a2155")) {
                AppsDeviceParameters.isUpLoadData = false;
            } else if (ourPackageName.equals("c067e30bc78239171b25095164ebd0c267")) {
                try {
                    String validateDateStr = "2018-09-30 12:00:00";
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date invalidateDate = dateFormat.parse(validateDateStr);
                    Date tempDate = new Date();
                    if (tempDate.before(invalidateDate)) {
                        AppsDeviceParameters.isUpLoadData = false;
                    } else {
                        AppsDeviceParameters.isUpLoadData = true;
                    }
                } catch (ParseException exception) {
                    Log.w(TAG, "ParseException:" + exception);
                    AppsDeviceParameters.isUpLoadData = true;
                }
            } else {
                AppsDeviceParameters.isUpLoadData = true;
            }
        }


        mNotifyThread = new NotifyThread();
        mScanThread = new ScanThread();
        mScanFinishThread = new ScanFinishThread();
        mConnectionThread = new ConnectionThread();
        AbiControlSubManager.getInstance().init();
        //DISCOVERY_BP7S 以下的都是BLE
        discoveryTypeForBle = DISCOVERY_BP3M;

    }

    private String getOurPackageName(String tempPackageName) {
        byte[] secrets;
        try {
            tempPackageName = tempPackageName.substring(0, tempPackageName.length() - 1) + "@#$";
            secrets = MessageDigest.getInstance("MD5").digest(tempPackageName.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e1) {
            throw new RuntimeException("Huh, getPackageName should be supported?", e1);
        } catch (UnsupportedEncodingException e2) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e2);
        }
        StringBuilder stringBuilder = new StringBuilder(secrets.length * 2);
        byte[] temps = secrets;
        int i = temps.length;
        for (int j = 0; j < i; ++j) {
            int k = temps[j];
            if ((k & 0xFF) < 16)
                stringBuilder.append("0");
            stringBuilder.append(Integer.toHexString(k & 0xFF));
        }
        stringBuilder.append(Integer.toHexString(temps[1] & 0xFF));
        return stringBuilder.toString();
    }

    public void destroy() {
        Log.p(TAG, Log.Level.INFO, "destroy");
        try {
            AbiControlSubManager.getInstance().destory();
            synchronized (mIHealthDeviceClientMap) {
                mIHealthDeviceClientMap.clear();
            }

            disconnenctAllConnectDevice();
            //jing 20160728 关闭扫描
            if (bluetoothAdapter != null) {
                bluetoothAdapter.cancelDiscovery();
            }
            if (mBleComm != null) {
                mBleComm.scan(false);
            }
//          此处存在隐患，注册的广播没有注销
            mContext.unregisterReceiver(bluetoothReceiver);

            //注销相关通讯类
//            mBleComm = null;
//            mNotifyThread = null;
//            mScanThread = null;
//            mScanFinishThread = null;
//            mConnectionThread = null;
            mapIdps.clear();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "destroy() -- e: " + e);
        }

    }

    private void disconnenctAllConnectDevice() {
        //断开所有已连接设备
        for (String mac : mapMacAndType.keySet()) {
            String deviceType = mapMacAndType.get(mac);
            if (deviceType.equals(TYPE_BP5)) {
                Bp5Control bp5Control = mapBp5Control.get(mac);
                if (bp5Control != null)
                    bp5Control.disconnect();
            } else if (deviceType.equals(TYPE_BP7)) {
                Bp7Control bp7Control = mapBp7Control.get(mac);
                if (bp7Control != null)
                    bp7Control.disconnect();
            } else if (deviceType.equals(TYPE_BP7S)) {
                Bp7sControl bp7sControl = mapBp7sControl.get(mac);
                if (bp7sControl != null)
                    bp7sControl.disconnect();
            } else if (deviceType.equals(TYPE_BG5)) {
                Bg5Control bg5Control = mapBg5Control.get(mac);
                if (bg5Control != null)
                    bg5Control.disconnect();
            } else if (deviceType.equals(TYPE_HS3)) {
                Hs3Control hs3Control = mapHs3Control.get(mac);
                if (hs3Control != null)
                    hs3Control.disconnect();
            } else if (deviceType.equals(TYPE_HS4S)) {
                Hs4sControl hs4sControl = mapHs4sControl.get(mac);
                if (hs4sControl != null)
                    hs4sControl.disconnect();
            } else {
                mBleComm.disconnect(mac);
            }
        }
    }

    public static final int DEVICE_STATE_CONNECTING = 0;
    public static final int DEVICE_STATE_CONNECTED = 1;
    public static final int DEVICE_STATE_DISCONNECTED = 2;
    public static final int DEVICE_STATE_CONNECTIONFAIL = 3;
    public static final int DEVICE_STATE_RECONNECTING = 4;

    private final BaseCommCallback mBaseCommCallback = new BaseCommCallback() {

        @Override
        public void onConnectionStateChange(String mac, String type, int status, int errorID, Map manufactorData) {
            if (mConnectionThread == null) {
                return;
            }
            boolean reConnectedFlag = false;
            if (DEVICE_STATE_CONNECTED == status) {
                //jing 20160825 对于FD重发的问题进行屏蔽  已经认证通过的，则不再发送连接成功消息
                if (connectedDeviceMap.get(mac) != null) {
                    reConnectedFlag = true;
                } else {
                    addDevice(mac, type);
                }
            } else if (DEVICE_STATE_DISCONNECTED == status) {
                //对 DEVICE_STATE_DISCONNECTED   DEVICE_STATE_CONNECTIONFAIL  再次确认
                if (connectedDeviceMap.get(mac) == null) {
                    status = DEVICE_STATE_CONNECTIONFAIL;
                }
                if (mac != null)
                    removeDevice(mac, type);
            } else if (DEVICE_STATE_CONNECTING == status) {
                if (TYPE_HS5.equals(type)) {
                    HS5ConnecttingMap.put(mac, mac);
                }
            } else {
                if (mac != null)
                    removeDevice(mac, type);
            }
            //jing 20160825 对于FD重发的问题进行屏蔽  已经认证通过的，则不再发送连接成功消息
            if (reConnectedFlag == false) {
                mConnectionThread.setConnectionMessage(mac, type, status, errorID, manufactorData);
                mainThreadHandler.postDelayed(mConnectionThread, 100);
            }
        }
    };

    private iHealthDeviceCloudManager miHealthDeviceCloudManager;

    private String mUserName;

    /**
     * @param con              Context
     * @param userName         the identification of the user, could be the form of email address or mobile
     *                         phone.
     * @param clientId         clientID and clientSecret, as the identification of the SDK, will be issued
     *                         after the iHealth SDK registration. please contact louie@ihealthlabs.com for
     *                         registration.
     * @param clientSecret     as clientId
     * @param clientCallbackId
     */
    public void sdkUserInAuthor(Context con, String userName, String clientId, String clientSecret, int clientCallbackId) {
        Log.p(TAG, Log.Level.INFO, "sdkUserInAuthor", con, userName, clientId, clientSecret, clientCallbackId);
        this.mUserName = userName;
        miHealthDeviceCloudManager = new iHealthDeviceCloudManager(mContext,
                mIHealthDeviceClientMap.getCallback(clientCallbackId));
        miHealthDeviceCloudManager.SDKUserInAuthor(userName, clientId, clientSecret);
    }


    /**
     * @param con              Context
     * @param userName         the identification of the user, could be the form of email address or mobile
     *                         phone.
     * @param clientId         clientID and clientSecret, as the identification of the SDK, will be issued
     *                         after the iHealth SDK registration. please contact louie@ihealthlabs.com for
     *                         registration.
     * @param clientSecret
     * @param clientCallbackId
     * @param filePath         the file path of certificate needed by server
     * @param password         the key of the certificate
     */
    public void sdkUserInAuthor(Context con, String userName, String clientId, String clientSecret, int clientCallbackId, String filePath, String password) {
        Log.p(TAG, Log.Level.INFO, "sdkUserInAuthor", con, userName, clientId, clientSecret, clientCallbackId, filePath, password);
        this.mUserName = userName;
        SharedPreferences sp = mContext.getSharedPreferences("IHCertificateFileInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("cert_path", filePath);
        editor.putString("cert_password", password);
        editor.commit();
        editor.apply();
        miHealthDeviceCloudManager = new iHealthDeviceCloudManager(mContext,
                mIHealthDeviceClientMap.getCallback(clientCallbackId));
        miHealthDeviceCloudManager.SDKUserInAuthor(userName, clientId, clientSecret);
    }


    /**
     * Connection to a valid iHealth device nearby.
     * <p>
     * <p>The function may not be return immediately, but if will be completed when a iHealth device is
     * available. A {@link iHealthDevicesCallback#onDeviceConnectionStateChange} callback will be
     * invoked when the connection state changes as a result of this function.</p>
     * <p>
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.</p>
     * <p>
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.</p>
     *
     * @param userName the identification of the user, could be the form of email address or mobile
     *                 phone
     * @param mac      a valid iHealth device mac
     * @param type     iHealth device type   eg. TYPE_BP3L  TYPE_AM4
     * @return true, if a iHealth device is connected successfully false, if the user does not have
     * permission to connect this device or the mac is not valid
     */
    public boolean connectDevice(String userName, final String mac, final String type) {
        Log.p(TAG, Log.Level.INFO, "connectDevice", userName, mac, type);
        if (mac != null && type != null && mac.length() >= 12 && type.length() > 0) {
            stopDiscoveryForConnect();
            if (!AppsDeviceParameters.isUpLoadData || miHealthDeviceCloudManager.getDevicePermisson(userName, type)) {
                ScanDevice scanDevice = scanBlueDevicesMap.get(mac);
                if (scanDevice == null) {
                    if (type.equals(TYPE_BG5) || type.equals(TYPE_BP5) || type.equals(TYPE_BP7)
                            || type.equals(TYPE_BP7S) || type.equals(TYPE_HS4S)) {
                        connectBTDeviceDirect(mac, type);
                    } else if (type.equals(TYPE_HS5) || type.equals(TYPE_HS6) || type.equals(TYPE_BP3M)) {
                        mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_CONNECTIONFAIL, 0, null);
                        return false;
                    } else {
                        connectBleDeviceDirect(mac);
                    }
                    return true;
                } else {
                    connectDevice(userName, mac);
                    return true;
                }
            }

        }
        mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_CONNECTIONFAIL, 0, null);
        return false;
    }

    /**
     * Connection to a valid iHealth device nearby.
     * <p>
     * <p>The function may not be return immediately, but if will be completed when a iHealth device is
     * available. A {@link iHealthDevicesCallback#onDeviceConnectionStateChange} callback will be
     * invoked when the connection state changes as a result of this function.</p>
     * <p>
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.</p>
     * <p>
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.</p>
     *
     * @param userName the identification of the user, could be the form of email address or mobile
     *                 phone
     * @param mac      a valid iHealth device mac
     * @return true, if a iHealth device is connected successfully false, if the user does not have
     * permission to connect this device or the mac is not valid
     */
    public boolean connectDevice(String userName, final String mac) {
        Log.p(TAG, Log.Level.INFO, "connectDevice", userName, mac);
        stopDiscoveryForConnect();
        if (AppsDeviceParameters.isUpLoadData) {
            if (miHealthDeviceCloudManager == null) {
                return false;
            }
        }
        ScanDevice scanDevice = scanBlueDevicesMap.get(mac);
        if (scanDevice == null) {
            connectBleDeviceDirect(mac);
            return true;
        }
        final String type = scanDevice.getDeviceType();

        //jing 20160801  先判断是否是已经连接上的设备
        if (connectedDeviceMap != null && mac != null && connectedDeviceMap.get(mac) != null) {
            mainThreadHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_CONNECTED, 0, null);
                }
            }, 1000);
            return true;
        }

        BluetoothDevice device = scanDevice.getDevice();
        int linktype = scanDevice.getlinkType();
        Hs5Control hs5Control = scanDevice.getHs5control();
        if (!AppsDeviceParameters.isUpLoadData || miHealthDeviceCloudManager.getDevicePermisson(userName, type)) {
            switch (linktype) {
                case ScanDevice.LINK_BT:
                    connectBtDevice(device, type);
                    break;
                case ScanDevice.LINK_BLE:
                    connectBleDevice(mac, type);
                    break;
                case ScanDevice.LINK_WIFI:
                    connectWifi(mac, hs5Control);
                    break;
                case ScanDevice.LINK_USB:
                    connectUsbDevice(scanDevice.getUsbDevice());
                    break;
                case ScanDevice.LINK_AU:
                    break;
                default:
                    break;
            }
            return true;
        } else {
            mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_CONNECTIONFAIL, 0, null);
            return false;
        }
    }

    private Bp3mControl mBp3mControl;
    private Bp3lControl mBp3lControl;
    private Bp5Control mBp5Control;
    private Bp7Control mBp7Control;
    private Bp7sControl mBp7sControl;
    private Bp550BTControl mBp550btControl;
    private Bp926Control mBp926Control;
    private Bp723Control mBp723Control;
    private ABPMControl mABPMControl;
    private Hs3Control mHs3Control;
    private Hs4Control mHs4Control;
    private Hs4sControl mHs4sControl;
    private Hs5ControlForBt mHs5ControlForBt;
    private Am3Control mAm3Control;
    private Am3sControl mAm3sControl;
    private Am4Control mAm4Control;
    private Po3Control mPo3Control;
    private Hs5Control mHs5Control;
    private Bg5Control mBg5Control;
    private BtmControl mBtmControl;
    private BPControl mBpControl;
    private Bg5lControl mBg5lControl;
    private BgControl mBgControl;
    private PoControl mPoControl;
    private HsControl mHsControl;
    private BsControl mBsControl;

    //存储连接成功的设备列表
    private Map<String, String> connectedDeviceMap = new ConcurrentHashMap<String, String>();

    private void addDevice(String mac, String type) {
        connectedDeviceMap.put(mac, type);
        BTConnecttingMap.remove(mac);

        int linkType = 0;    //0,usb  1,wifi  2,bt  3, ble


        if (type.equals(TYPE_BP3M)) {
            mapBp3mControl.put(mac, mBp3mControl);
        } else if (type.equals(TYPE_BP3L)) {
            linkType = 3;
            mapBp3lControl.put(mac, mBp3lControl);
        } else if (type.equals(TYPE_BP5)) {
            linkType = 2;
            mapBp5Control.put(mac, mBp5Control);
        } else if (type.equals(TYPE_BP7)) {
            linkType = 2;
            mapBp7Control.put(mac, mBp7Control);
        } else if (type.equals(TYPE_BP7S)) {
            linkType = 2;
            mapBp7sControl.put(mac, mBp7sControl);
        } else if (type.equals(TYPE_550BT)) {
            linkType = 3;
            mapBp550BTControl.put(mac, mBp550btControl);
        } else if (type.equals(TYPE_KD926)) {
            linkType = 3;
            mapBp926Control.put(mac, mBp926Control);
        } else if (type.equals(TYPE_KD723)) {
            linkType = 3;
            mapBp723Control.put(mac, mBp723Control);
        } else if (type.equals(TYPE_ABPM)) {
            linkType = 3;
            mapABPMControl.put(mac, mABPMControl);
        } else if (type.equals(TYPE_HS3)) {
            linkType = 2;
            mapHs3Control.put(mac, mHs3Control);
        } else if (type.equals(TYPE_HS4)) {
            linkType = 3;
            mapHs4Control.put(mac, mHs4Control);
        } else if (type.equals(TYPE_HS4S)) {
            linkType = 2;
            mapHs4sControl.put(mac, mHs4sControl);
        } else if (type.equals(TYPE_HS5)) { // wifi
            // mapHs5ControlForBt.put(mac, mHs5ControlForBt);
            linkType = 1;
            HS5WifiMap.put(mac, mHs5Control);
            HS5ConnectedWifiMap.put(mac, mHs5Control);
            HS5ConnecttingMap.remove(mac);
        } else if (type.equals(TYPE_HS6)) {

        } else if (type.equals(TYPE_AM3)) {
            linkType = 3;
            mapAm3Control.put(mac, mAm3Control);
        } else if (type.equals(TYPE_AM3S)) {
            linkType = 3;
            mapAm3sControl.put(mac, mAm3sControl);
        } else if (type.equals(TYPE_AM4)) {
            linkType = 3;
            mapAm4Control.put(mac, mAm4Control);
        } else if (type.equals(TYPE_PO3)) {
            linkType = 3;
            mapPo3Control.put(mac, mPo3Control);
        } else if (type.equals(TYPE_HS5_BT)) {
            linkType = 2;
            mapHs5ControlForBt.put(mac, mHs5ControlForBt);
        } else if (type.equals(TYPE_BG5)) {
            linkType = 2;
            mapBg5Control.put(mac, mBg5Control);
        } else if (type.equals(TYPE_BTM)) {
            linkType = 3;
            mapBtmControl.put(mac, mBtmControl);
        } else if (type.equals(TYPE_CBP)) {
            linkType = 3;
            mapBpControl.put(mac, mBpControl);
        } else if (type.equals(TYPE_BG5l)) {
            linkType = 3;
            mapBG5lControl.put(mac, mBg5lControl);
        } else if (type.equals(TYPE_CBG)) {
            linkType = 3;
            mapBgControl.put(mac, mBgControl);
        } else if (type.equals(TYPE_CPO)) {
            linkType = 3;
            mapPoControl.put(mac, mPoControl);
        } else if (type.equals(TYPE_CHS)) {
            linkType = 3;
            mapHsControl.put(mac, mHsControl);
        } else if (type.equals(TYPE_CBS)) {
            linkType = 3;
            mapBsControl.put(mac, mBsControl);
        }

        if (linkType == 3) {
            //jing 20160621 该手机此版本的系统正常连接BLE标示
            if (Build.VERSION.RELEASE.startsWith("4.3") || Build.VERSION.RELEASE.startsWith("4.4")) {
                Log.v(TAG, "Special phone");
                SharedPreferences sharedPreferences = mContext.getSharedPreferences("SpecialPhone" + Build.VERSION.RELEASE, Context.MODE_PRIVATE);
                boolean firstSpecialFlag = sharedPreferences.getBoolean("FirstSpecialFlag", false);
                if (firstSpecialFlag == false) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("FirstSpecialFlag", true);
                    editor.commit();
                }
            }
        }
    }

    private void removeDevice(String mac, String type) {
        Log.p(TAG, Log.Level.VERBOSE, "removeDevice", mac, type);
        if (connectedDeviceMap != null) {
            connectedDeviceMap.remove(mac);
        }
        if (BTConnecttingMap != null) {
            BTConnecttingMap.remove(mac);
        }
        if (type == null) {
            Log.v(TAG, "removeDevice type==null");
        } else if (TYPE_BP3M.equals(type) && mapBp3mControl != null) {
        } else if (type.equals(TYPE_BP3M) && mapBp3mControl != null) {
            mapBp3mControl.remove(mac);
        } else if (TYPE_BP3L.equals(type) && mapBp3lControl != null) {
            mapBp3lControl.remove(mac);
        } else if (TYPE_BP5.equals(type)) {
            if (mapBp5Control != null) {
                mapBp5Control.remove(mac);
            }
            //jing 20160810 ABI 设备断开后需要清空map
            AbiControlSubManager.getInstance().remove(mac);
        } else if (TYPE_BP7.equals(type) && mapBp7Control != null) {
            mapBp7Control.remove(mac);
        } else if (TYPE_BP7S.equals(type) && mapBp7sControl != null) {
            mapBp7sControl.remove(mac);
        } else if (TYPE_550BT.equals(type) && mapBp550BTControl != null) {
            mapBp550BTControl.remove(mac);
        } else if (TYPE_KD926.equals(type) && mapBp926Control != null) {
            mapBp926Control.remove(mac);
        } else if (TYPE_KD723.equals(type) && mapBp723Control != null) {
            mapBp723Control.remove(mac);
        } else if (TYPE_ABPM.equals(type) && mapABPMControl != null) {
            mapABPMControl.remove(mac);
        } else if (TYPE_HS3.equals(type) && mapHs3Control != null) {
            mapHs3Control.remove(mac);
        } else if (TYPE_HS4.equals(type) && mapHs4Control != null) {
            mapHs4Control.remove(mac);
        } else if (TYPE_HS4S.equals(type) && mapHs4sControl != null) {
            mapHs4sControl.remove(mac);
        } else if (TYPE_HS5.equals(type) && HS5WifiMap != null) {
            HS5WifiMap.remove(mac);
            HS5ConnectedWifiMap.remove(mac);
            HS5ConnecttingMap.remove(mac);
            HS5CountMap.remove(mac);
        } else if (TYPE_HS6.equals(type)) {

        } else if (TYPE_AM3.equals(type) && mapAm3Control != null) {
            mapAm3Control.remove(mac);
        } else if (TYPE_AM3S.equals(type) && mapAm3sControl != null) {
            mapAm3sControl.remove(mac);
        } else if (TYPE_AM4.equals(type) && mapAm4Control != null) {
            mapAm4Control.remove(mac);
        } else if (TYPE_PO3.equals(type) && mapPo3Control != null) {
            mapPo3Control.remove(mac);
        } else if (TYPE_HS5_BT.equals(type) && mapHs5ControlForBt != null) {
            mapHs5ControlForBt.remove(mac);
        } else if (TYPE_BG5.equals(type) && mapBg5Control != null) {
            mapBg5Control.remove(mac);
        } else if (TYPE_BTM.equals(type) && mapBtmControl != null) {
            mapBtmControl.remove(mac);
        } else if (TYPE_CBP.equals(type) && mapBpControl != null) {
            mapBpControl.remove(mac);
        } else if (TYPE_BG5l.equals(type) && mapBG5lControl != null) {
            mapBG5lControl.remove(mac);
        } else if (TYPE_CBG.equals(type) && mapBgControl != null) {
            mapBgControl.remove(mac);
        } else if (TYPE_CPO.equals(type) && mapPoControl != null) {
            mapPoControl.remove(mac);
        } else if (TYPE_CHS.equals(type) && mapHsControl != null) {
            mapHsControl.remove(mac);
        } else if (TYPE_CBS.equals(type) && mapBsControl != null) {
            mapBsControl.remove(mac);
        }
        System.gc();
    }

    /**
     * Get Po3 controller.
     *
     * @param mac a valid Po3 mac and the device is connected
     * @return Po3Control if mac is not valid, return null
     */
    public Po3Control getPo3Control(String mac) {
        return mapPo3Control.get(mac);
    }

    /**
     * Get all Po3 device is connected
     *
     * @return A list of mac
     */
    public List<String> getPo3Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapPo3Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Po3Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Am3 controller.
     *
     * @param mac a valid Am3 mac and the device is connected
     * @return Am3Control if mac is not valid, return null
     */
    public Am3Control getAm3Control(String mac) {
        if (mac == null)
            return null;
        if (mapAm3Control == null)
            return null;
        return mapAm3Control.get(mac);
    }

    /**
     * Get all Am3 device is connected
     *
     * @return List<String>
     */
    public List<String> getAm3Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapAm3Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Am3Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Am3s controller.
     *
     * @param mac a valid Am3s mac and the device is connected
     * @return Am3sControl if mac is not valid, return null
     */
    public Am3sControl getAm3sControl(String mac) {
        if (mac == null)
            return null;
        if (mapAm3sControl == null)
            return null;
        return mapAm3sControl.get(mac);
    }

    /**
     * Get all Am3s device is connected
     *
     * @return List<String>
     */
    public List<String> getAm3sDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapAm3sControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Am3sControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Am4 controller.
     *
     * @param mac a valid Am4 mac and the device is connected
     * @return Am4Control if mac is not valid, return null
     */
    public Am4Control getAm4Control(String mac) {
        if (mac == null)
            return null;
        if (mapAm4Control == null)
            return null;
        return mapAm4Control.get(mac);
    }

    /**
     * Get all Am4 device is connected
     *
     * @return List<String>
     */
    public List<String> getAm4Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapAm4Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Am4Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Bp3l controller.
     *
     * @param mac a valid Bp3l mac and the device is connected
     * @return Bp3lControl if mac is not valid, return null
     */
    public Bp3lControl getBp3lControl(String mac) {
        if (mac == null)
            return null;
        if (mapBp3lControl == null)
            return null;
        return mapBp3lControl.get(mac);
    }

    /**
     * Get all Bp3l device is connected
     *
     * @return Map<String, Bp3lControl>
     */
    public List<String> getBp3lDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBp3lControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bp3lControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Bp3m controller.
     *
     * @param mac a valid Bp3m mac and the device is connected
     * @return Bp3mControl if mac is not valid, return null
     */
    public Bp3mControl getBp3mControl(String mac) {
        if (mac == null)
            return null;
        if (mapBp3mControl == null)
            return null;
        return mapBp3mControl.get(mac);
    }

    /**
     * Get all Bp3m device is connected
     *
     * @return List<String>
     */
    public List<String> getBp3mDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBp3mControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bp3mControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Bp5 controller.
     *
     * @param mac a valid Bp5 mac and the device is connected
     * @return Bp5Control if mac is not valid, return null
     */
    public Bp5Control getBp5Control(String mac) {
        if (mac == null)
            return null;
        if (mapBp5Control == null)
            return null;
        return mapBp5Control.get(mac);
    }

    /**
     * Get all Bp5 device is connected
     *
     * @return List<String>
     */
    public List<String> getBp5Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBp5Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bp5Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Bp7 controller.
     *
     * @param mac a valid Bp7 mac and the device is connected
     * @return Bp7Control if mac is not valid, return null
     */
    public Bp7Control getBp7Control(String mac) {
        if (mac == null)
            return null;
        if (mapBp7Control == null)
            return null;
        return mapBp7Control.get(mac);
    }

    /**
     * Get all Bp7 device is connected
     *
     * @return List<String>
     */
    public List<String> getBp7Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBp7Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bp7Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Bp7s controller.
     *
     * @param mac a valid Bp7s mac and the device is connected
     * @return Bp7sControl if mac is not valid, return null
     */
    public Bp7sControl getBp7sControl(String mac) {
        if (mac == null)
            return null;
        if (mapBp7sControl == null)
            return null;
        return mapBp7sControl.get(mac);
    }

    /**
     * Get all Bp7s device is connected
     *
     * @return List<String>
     */
    public List<String> getBp7sDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBp7sControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bp7sControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Bp550BT controller.
     *
     * @param mac a valid Bp550BT mac and the device is connected
     * @return Bp550BTControl if mac is not valid, return null
     */
    public Bp550BTControl getBp550BTControl(String mac) {
        if (mac == null)
            return null;
        if (mapBp550BTControl == null)
            return null;
        return mapBp550BTControl.get(mac);
    }

    /**
     * Get all Bp550BT device is connected
     *
     * @return List<String>
     */
    public List<String> getBp550BTDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBp550BTControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bp550BTControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Bp926 controller.
     *
     * @param mac a valid Bp550BT mac and the device is connected
     * @return Bp550BTControl if mac is not valid, return null
     */
    public Bp926Control getBp926Control(String mac) {
        if (mac == null)
            return null;
        if (mapBp926Control == null)
            return null;
        return mapBp926Control.get(mac);
    }

    /**
     * Get all Bp926 device is connected
     *
     * @return List<String>
     */
    public List<String> getBp926Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBp926Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bp926Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Bp723 controller.
     *
     * @param mac a valid Bp723 mac and the device is connected
     * @return Bp723Control if mac is not valid, return null
     */
    public Bp723Control getBp723Control(String mac) {
        if (mac == null)
            return null;
        if (mapBp723Control == null)
            return null;
        return mapBp723Control.get(mac);
    }

    /**
     * Get all Bp723 device is connected
     *
     * @return List<String>
     */
    public List<String> getBp723Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBp723Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bp723Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get ABPM controller.
     *
     * @param mac a valid ABPM mac and the device is connected
     * @return ABPMControl if mac is not valid, return null
     */
    public ABPMControl getABPMControl(String mac) {
        if (mac == null)
            return null;
        if (mapABPMControl == null)
            return null;
        return mapABPMControl.get(mac);
    }

    /**
     * Get all ABPM device is connected
     *
     * @return List<String>
     */
    public List<String> getABPMDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapABPMControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ABPMControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Hs3 controller.
     *
     * @param mac a valid Hs3 mac and the device is connected
     * @return Hs3Control if mac is not valid, return null
     */
    public Hs3Control getHs3Control(String mac) {
        if (mac == null)
            return null;
        if (mapHs3Control == null)
            return null;
        return mapHs3Control.get(mac);
    }

    /**
     * Get all Hs3 device is connected
     *
     * @return List<String>
     */
    public List<String> getHs3Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapHs3Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Hs3Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Hs4 controller.
     *
     * @param mac a valid Hs4 mac and the device is connected
     * @return Hs4Control if mac is not valid, return null
     */
    public Hs4Control getHs4Control(String mac) {
        if (mac == null)
            return null;
        if (mapHs4Control == null)
            return null;
        return mapHs4Control.get(mac);
    }

    /**
     * Get all Hs4 device is connected
     *
     * @return List<String>
     */
    public List<String> getHs4Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapHs4Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Hs4Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }


    /**
     * Get Hs4s controller.
     *
     * @param mac a valid Hs4s mac and the device is connected
     * @return Hs4sControl if mac is not valid, return null
     */
    public Hs4sControl getHs4sControl(String mac) {
        if (mac == null)
            return null;
        if (mapHs4sControl == null)
            return null;
        return mapHs4sControl.get(mac);
    }

    /**
     * Get all Hs4s device is connected
     *
     * @return List<String>
     */
    public List<String> getHs4sDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapHs4sControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Hs4sControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Hs5 controller for Bluetooth.
     *
     * @param mac a valid Hs5 mac and the device is connected
     * @return Hs5ControlForBt if mac is not valid, return null
     */
    public Hs5ControlForBt getHs5ControlForBt(String mac) {
        if (mac == null)
            return null;
        if (mapHs5ControlForBt == null)
            return null;
        return mapHs5ControlForBt.get(mac);
    }

    /**
     * Get Bg5 controller for Bluetooth.
     *
     * @param mac a valid Bg5 mac and the device is connected
     * @return Bg5Control if mac is not valid, return null
     */
    public Bg5Control getBg5Control(String mac) {
        if (mac == null)
            return null;
        if (mapBg5Control == null)
            return null;
        return mapBg5Control.get(mac);
    }

    /**
     * Get all Bg5 device is connected
     *
     * @return List<String>
     */
    public List<String> getBg5Devices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBg5Control.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bg5Control> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    ;

    /**
     * Get Bg5 controller for Bluetooth.
     *
     * @param mac a valid Bg5 mac and the device is connected
     * @return Bg5Control if mac is not valid, return null
     */
    public BtmControl getBtmControl(String mac) {
        if (mac == null)
            return null;
        if (mapBtmControl == null)
            return null;
        return mapBtmControl.get(mac);
    }

    /**
     * Get Abi controller for Bluetooth.
     *
     * @param mac a valid Bg5 mac and the device is connected
     * @return AbiControl if mac is not valid, return null
     */
    public AbiControl getAbiControlforUp(String mac) {
        if (mac == null)
            return null;
        return AbiControlSubManager.getInstance().getAbiControlforUp(mac);
    }

    /**
     * Get Abi controller for Bluetooth.
     *
     * @param mac a valid Bg5 mac and the device is connected
     * @return AbiControl if mac is not valid, return null
     */
    public AbiControl getAbiControl(String mac) {
        if (mac == null)
            return null;
        return AbiControlSubManager.getInstance().getAbiControl(mac);
    }

    /**
     * Get Continua BP controller.
     *
     * @param mac a valid Continua BP mac and the device is connected
     * @return BPControl if mac is not valid, return null
     */
    public BPControl getBPControl(String mac) {
        if (mac == null)
            return null;
        if (mapBpControl == null)
            return null;
        return mapBpControl.get(mac);
    }

    /**
     * Get all Continua BP device is connected
     *
     * @return List<String>
     */
    public List<String> getBPDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBpControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, BPControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Continua BG5l controller.
     *
     * @param mac a valid Continua BG5l mac and the device is connected
     * @return Bg5lControl if mac is not valid, return null
     */
    public Bg5lControl getBG5lControl(String mac) {
        if (mac == null)
            return null;
        if (mapBG5lControl == null)
            return null;
        return mapBG5lControl.get(mac);
    }

    /**
     * Get all Continua BG5l device is connected
     *
     * @return List<String>
     */
    public List<String> getBG5lDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBG5lControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Bg5lControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Continua BG controller.
     *
     * @param mac a valid Continua BG mac and the device is connected
     * @return BgControl if mac is not valid, return null
     */
    public BgControl getBgControl(String mac) {
        if (mac == null)
            return null;
        if (mapBgControl == null)
            return null;
        return mapBgControl.get(mac);
    }

    /**
     * Get all Continua BG device is connected
     *
     * @return List<String>
     */
    public List<String> getBgDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBgControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, BgControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Continua PO controller.
     *
     * @param mac a valid Continua PO mac and the device is connected
     * @return PoControl if mac is not valid, return null
     */
    public PoControl getPoControl(String mac) {
        if (mac == null)
            return null;
        if (mapPoControl == null)
            return null;
        return mapPoControl.get(mac);
    }

    /**
     * Get all Continua PO device is connected
     *
     * @return List<String>
     */
    public List<String> getPoDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapPoControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, BgControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Continua HS controller.
     *
     * @param mac a valid Continua HS mac and the device is connected
     * @return PoControl if mac is not valid, return null
     */
    public HsControl getHsControl(String mac) {
        if (mac == null)
            return null;
        if (mapHsControl == null)
            return null;
        return mapHsControl.get(mac);
    }

    /**
     * Get all Continua HS device is connected
     *
     * @return List<String>
     */
    public List<String> getHsDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapHsControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, BgControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /**
     * Get Continua Body Composition Scale controller.
     *
     * @param mac a valid Continua Body Composition Scale mac and the device is connected
     * @return PoControl if mac is not valid, return null
     */
    public BsControl getBsControl(String mac) {
        if (mac == null)
            return null;
        if (mapBsControl == null)
            return null;
        return mapBsControl.get(mac);
    }

    /**
     * Get all Continua Body Composition Scale device is connected
     *
     * @return List<String>
     */
    public List<String> getBsDevices() {
        List<String> list = new ArrayList<String>();
        Iterator iter = mapBsControl.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, BgControl> entry = (Map.Entry) iter.next();
            String key = entry.getKey();
            list.add(key);
        }
        return list;
    }

    /* connection by Bluetooth */
    private void connectBtDevice(BluetoothDevice device, String type) {

        String mac = device.getAddress().replace(":", "");

        if (BTConnecttingMap.containsKey(mac)) {
            Log.v(TAG, "already connecting this device mac =" + mac);
            return;
        }
        BTConnecttingMap.put(mac, mac);
        if (type.contains(TYPE_BG5)) {
            String version = getEE(mac);
            if (version.equals("000")) {
                ConnectThread connectThread = new ConnectThread(device, type, true);
                connectThread.start();
            } else {
                //jing 20160817 存储到mapIdps，供读取IDPS时使用
                setFirmwareVersionForDevice(mac, version);

                ConnectThread connectThread = new ConnectThread(device, type, false);
                connectThread.start();
            }
        } else {
            ConnectThread connectThread = new ConnectThread(device, type, false);
            connectThread.start();
        }

    }

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private void setEE(String mac, String version) {
        // 41542B495353435F4F4B3A30363033303030303031303330300D
        sharedPreferences = mContext.getSharedPreferences("bg5_ee", Activity.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString(mac, version);
        editor.commit();
        //jing 20160817 存储到mapIdps，供读取IDPS时使用
        setFirmwareVersionForDevice(mac, version);
    }

    private void setFirmwareVersionForDevice(String mac, String version) {
        IDPS idps = mapIdps.get(mac);
        if (idps == null) {
            idps = new IDPS();
        }
        idps.setAccessorySerialNumber(mac);
        idps.setAccessoryFirmwareVersion(version);
        mapIdps.put(mac, idps);
    }

    public String getEE(String mac) {
        sharedPreferences = mContext.getSharedPreferences("bg5_ee", Activity.MODE_PRIVATE);
        String version = sharedPreferences.getString(mac, "000");

        return version;
    }

    private class ConnectThread extends Thread {
        private BluetoothDevice device;
        private BluetoothSocket socket;
        private String mType;
        private boolean needReadEE = false;

        public ConnectThread(BluetoothDevice device, String type, boolean needEE) {
            this.device = device;
            this.mType = type;
            this.needReadEE = needEE;
        }

        private Timer timerSocket = null;
        private TimerTask timerTaskSocket = null;

        private void connectTimeout(final String tempMac, final String tempType, long delay) {

            timerSocket = new Timer();
            timerTaskSocket = new TimerTask() {
                @Override
                public void run() {
                    if (mBaseCommCallback != null) {
                        mBaseCommCallback.onConnectionStateChange(tempMac, tempType, DEVICE_STATE_CONNECTIONFAIL, 0, null);
                    }
                }
            };
            timerSocket.schedule(timerTaskSocket, delay);
        }

        @Override
        public void run() {
            super.run();
            try {
                if (needReadEE) {
                    createSocketEE(device);
                    if (socket.isConnected()) {

                    } else {
                        Log.v(TAG, "create ee socket -- continue create Socket");
                        needReadEE = false;
                        createSocket(device);
                    }
                } else {
                    createSocket(device);
                }

                if (socket.isConnected()) {
                    Log.v(TAG, "createIOStream()");
                    createIOStream();
                } else {
                    int errorID = 0;
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        //jing 20160912  App内普通蓝牙配对，部分手机无法自动弹出配对选项，导致连接失败，需要去蓝牙列表手动配对
                        errorID = 1;
                    }
                    mBaseCommCallback.onConnectionStateChange(device.getAddress().replace(":", ""), mType, DEVICE_STATE_CONNECTIONFAIL, errorID, null);
                }

            } catch (Exception e) {
                Log.e(TAG, "createSocket() -- Exception: " + e);
                mBaseCommCallback.onConnectionStateChange(device.getAddress().replace(":", ""), mType, DEVICE_STATE_CONNECTIONFAIL, 0, null);
            }
        }

        private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        /*
         * In order to improve the success rate of Bluetooth connectivity, using three kinds of ways
         * to establish socket.
         */
        private boolean createSocket(final BluetoothDevice device) {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
            } catch (Exception e1) {
                Log.w(TAG, "createSocket() -- e1:" + e1);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    return false;
                }
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                    socket.connect();
                } catch (Exception e2) {
                    Log.w(TAG, "createSocket() -- e2:" + e2);

                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        return false;
                    }
                    //部分手机反射Api不存在，导致卡死
                    if (!device.getName().contains(NAME_BG5)) {

                        connectTimeout(device.getAddress().replace(":", ""), mType, 10000);
                        //jing 20160921  部分手机会卡死，没有返回值，通过定时器解决
                        boolean connectResult = false;
                        Method m;
                        try {
                            m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[]{
                                    int.class
                            });
                            if (m != null) {
                                connectResult = true;
                                socket = (BluetoothSocket) m.invoke(device, 6);
                                socket.connect();
                            } else {
                                Log.e(TAG, "createSocket() -- (m==null)");
                            }
                        } catch (Exception e3) {
                            Log.e(TAG, "createSocket() -- e3:" + e3);
                        } finally {
                            if (timerSocket != null) {
                                timerSocket.cancel();
                                timerSocket = null;
                            }
                            if (timerTaskSocket != null) {
                                timerTaskSocket.cancel();
                                timerTaskSocket = null;
                            }
                        }
                        return connectResult;
                    } else {
                        return false;
                    }
                }

            }
            SystemClock.sleep(300);
            if (socket != null && socket.isConnected()) {
                return true;
            } else {
                return false;
            }
        }

        private boolean createSocketEE(BluetoothDevice device) {
            Method m;
            try {
                m = device.getClass().getMethod("createRfcommSocket", new Class[]{
                        int.class
                });
                socket = (BluetoothSocket) m.invoke(device, 10);
                byte[] b = ByteBufferUtil.hexStringToByte(device.getAddress().replace(":", ""));
                byte[] bs = new byte[4];
                bs[0] = b[b.length - 4];
                bs[1] = b[b.length - 3];
                bs[2] = b[b.length - 2];
                bs[3] = b[b.length - 1];
                m = device.getClass().getMethod("setPin", new Class[]{
                        byte[].class
                });
                m.invoke(device, bs);
                socket.connect();

                SystemClock.sleep(300);
                if (socket != null && socket.isConnected()) {
                    return true;
                } else {
                    return false;
                }

            } catch (SecurityException e1) {
                e1.printStackTrace();

                return false;
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
                return false;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        private void createIOStream() {
            if (needReadEE) {
                try {
                    btCommThreadee = new BtCommThreadEE(mContext, device, socket, mBaseCommCallback);
                    btCommThreadee.start();
                    SystemClock.sleep(500);
                    createControl(btCommThreadee, device.getAddress().replace(":", ""), device.getName(), needReadEE);
                } catch (IOException e) {
                    Log.w(TAG, "createIOStream -- e: " + e);
                    mBaseCommCallback.onConnectionStateChange(device.getAddress().replace(":", ""), mType, DEVICE_STATE_CONNECTIONFAIL, 0, null);
                }
            } else {
                try {
                    BtCommThread btCommThread = new BtCommThread(mContext, device, mType, socket, mBaseCommCallback);
                    btCommThread.start();
                    SystemClock.sleep(500);
                    String mac = device.getAddress().replace(":", "");
                    createControl(btCommThread, mac, device.getName(), needReadEE);
                } catch (IOException e) {
                    Log.w(TAG, "createIOStream -- e: " + e);
                    mBaseCommCallback.onConnectionStateChange(device.getAddress().replace(":", ""), mType, DEVICE_STATE_CONNECTIONFAIL, 0, null);
                }
            }
        }
    }

    private void createControl(BaseComm btct, String mac, String name, boolean needReadEE) {
        if (name.contains(TYPE_BP5)) {
            mBp5Control = new Bp5Control(mContext, btct, mUserName, mac, TYPE_BP5, mInsCallback, mBaseCommCallback); // control类多加一个参数BaseCommCallback
            mBp5Control.init();
            return;
        } else if (name.contains(TYPE_BP7S)) {
            mBp7sControl = new Bp7sControl(mContext, btct, mUserName, mac, TYPE_BP7S, mInsCallback, mBaseCommCallback);
            mBp7sControl.init();
            return;
        } else if (name.contains(TYPE_BP7)) {
            mBp7Control = new Bp7Control(mContext, btct, mUserName, mac, TYPE_BP7, mInsCallback, mBaseCommCallback); // control类多加一个参数BaseCommCallback
            mBp7Control.init();
            return;
        } else if (name.contains(NAME_HS3)) {
            mHs3Control = new Hs3Control(mUserName, btct, mContext, mac, TYPE_HS3, mBaseCommCallback, mInsCallback);
            mHs3Control.init();
            return;
        } else if (name.contains(NAME_HS4S)) {
            mHs4sControl = new Hs4sControl(mUserName, mContext, btct, mac, TYPE_HS4S, mBaseCommCallback, mInsCallback);
            mHs4sControl.init();
            return;
        } else if (name.contains(NAME_HS5)) {
            mHs5ControlForBt = new Hs5ControlForBt(btct, mac, TYPE_HS5_BT, mBaseCommCallback, mInsCallback);
            mHs5ControlForBt.init();
            return;
        } else if (name.contains(NAME_BG5)) {
            mBg5Control = new Bg5Control(mUserName, btct, mContext, mac, TYPE_BG5, needReadEE, mBaseCommCallback, mInsCallback);
            mBg5Control.init();
            return;
        }
    }

    private final InsCallback mInsCallback = new InsCallback() {
        @Override
        public void onNotify(String mac, String type, String action, String message) {
//            if (action.equals(IHEALTH_COMM_TIMEOUT)) {
//                disconnectDevice(mac, type);
//            }
            mNotifyThread.setNotifyMessage(mac, type, action, message);
            mainThreadHandler.post(mNotifyThread);
        }
    };

    //20160708 暴露断开方法给外部直接调用

    /**
     * Disconnects an established bluetooth connection, or cancels a bluetooth low energy connection attempt
     * currently in progress.
     *
     * @param mac        e.g. 8C8B83598AC3
     * @param deviceType e.g. TYPE_BP3L TYPE_AM3S
     */
    public void disconnectDevice(String mac, String deviceType) {
        Log.p(TAG, Log.Level.INFO, "disconnectDevice", mac, deviceType);
        if (deviceType == null) {
            deviceType = mapMacAndType.get(mac);
        }
        if (mac == null || deviceType == null) {
            Log.p(TAG, Log.Level.VERBOSE, "disconnectDevice", mac, deviceType);
            return;
        }
        if (deviceType.equals(TYPE_BP5)) {
            Bp5Control bp5Control = mapBp5Control.get(mac);
            if (bp5Control != null)
                bp5Control.disconnect();
        } else if (deviceType.equals(TYPE_BP7)) {
            Bp7Control bp7Control = mapBp7Control.get(mac);
            if (bp7Control != null)
                bp7Control.disconnect();
        } else if (deviceType.equals(TYPE_BP7S)) {
            Bp7sControl bp7sControl = mapBp7sControl.get(mac);
            if (bp7sControl != null)
                bp7sControl.disconnect();
        } else if (deviceType.equals(TYPE_BG5)) {
            Bg5Control bg5Control = mapBg5Control.get(mac);
            if (bg5Control != null)
                bg5Control.disconnect();
        } else if (deviceType.equals(TYPE_HS3)) {
            Hs3Control hs3Control = mapHs3Control.get(mac);
            if (hs3Control != null)
                hs3Control.disconnect();
        } else if (deviceType.equals(TYPE_HS4S)) {
            Hs4sControl hs4sControl = mapHs4sControl.get(mac);
            if (hs4sControl != null)
                hs4sControl.disconnect();
        } else {
            mBleComm.disconnect(mac);
        }
    }

    private iHealthDeviceClientMap mIHealthDeviceClientMap = new iHealthDeviceClientMap();

    /**
     * Register an message callback to start using iHealth SDK.
     *
     * @param miHealthDevicesCallback ihealth devices callbak, see{@link iHealthDevicesCallback}
     * @return callback id. That is unique number is as callback, be used to distinguish callback.
     */
    public int registerClientCallback(iHealthDevicesCallback miHealthDevicesCallback) {
        Log.p(TAG, Log.Level.INFO, "registerClientCallback", miHealthDevicesCallback);
        return mIHealthDeviceClientMap.add(miHealthDevicesCallback);
    }

    /**
     * Add a or more destination device mac to mathch against.
     * <p/>
     * If any callback are included in the filter, call is trigged, if not, the callback is
     * ingnored. If the filter methods {@link #addCallbackFilterForAddress} and
     * {@link #addCallbackFilterForDeviceType} are not called, none callback is ingnored.
     *
     * @param clientCallbackId This is callback id, calling {@link #registerClientCallback} will
     *                         return the Id.
     * @param macs             iHealth device address without colon.
     * @return true, if all mac is valid.
     */
    public boolean addCallbackFilterForAddress(int clientCallbackId, String... macs) {
        Log.p(TAG, Log.Level.INFO, "addCallbackFilterForAddress", clientCallbackId, macs);
        if (checkDeviceMac(macs)) {
            mIHealthDeviceClientMap.addCallbackFilter(clientCallbackId, macs);
            return true;
        } else {
            return false;
        }

    }

    /* check mac is valid or not */
    private boolean checkDeviceMac(String[] macs) {
        String regEx = "[a-zA-Z0-9]{12}";
        Pattern pattern = Pattern.compile(regEx);

        for (String string : macs) {
            Matcher matcher = pattern.matcher(string);
            if (!matcher.find()) {
                return false;
            } else {
                continue;
            }
        }
        return true;
    }

    /**
     * * Add a or more destination device type to match against.
     * <p/>
     * If any callback are included in the filter, call is triggered, if not, the callback is
     * ingnored. If the filter methods {@link #addCallbackFilterForAddress} and
     * {@link #addCallbackFilterForDeviceType} are not called, none callback is ignored.
     *
     * @param clientCallbackId This is callback id, calling {@link #registerClientCallback} will
     *                         return the Id.
     * @param deviceTypes      iHealth device type.
     * @return true, if all mac is valid.
     */
    public boolean addCallbackFilterForDeviceType(int clientCallbackId, String... deviceTypes) {
        Log.p(TAG, Log.Level.INFO, "addCallbackFilterForDeviceType", clientCallbackId, deviceTypes);
        if (checkDeviceType(deviceTypes)) {
            mIHealthDeviceClientMap.addCallbackFilter(clientCallbackId, deviceTypes);
            return true;
        } else {
            return false;
        }

    }

    /* check type is valid or not */
    private boolean checkDeviceType(String[] devicetype) {
        String type_all = "BP3MBP3LBP5BP7BP7SBG5HS3HS4HS4SHS5HS6AM3AM3SAM4PO3HS5btBG1BG5BG5LABPMCBP926723550";
        for (String string : devicetype) {
            if (type_all.contains(string)) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Unregister a previously registered Callback and filters will be removed.
     *
     * @param clientId callback id.
     */
    public void unRegisterClientCallback(int clientId) {
        Log.p(TAG, Log.Level.INFO, "unRegisterClientCallback", clientId);
        mIHealthDeviceClientMap.remove(clientId);
    }

    /**
     * Start discovery valid iHealth device.
     * <p/>
     * If haven't call {@link #stopDiscovery}, the discovery process usually will last for 12
     * seconds
     * <p/>
     * If calling {@link #connectDevice}, the discovery process will stop.
     * <p/>
     * This is an asynchronous call, it will not return immediately. When a valid iHealth device is
     * found, Callback {@link iHealthDevicesCallback#onScanDevice} will be triggered.
     * <p/>
     * According to Android official document, Device discovery is heavyweight method. When using
     * some iHealth device controller to measure or get historic data, it is not recommended for
     * discovery.
     *
     * @param type eg.  {@link #DISCOVERY_AM3S}
     */
    public void startDiscovery(long type) {
        Log.p(TAG, Log.Level.INFO, "startDiscovery", Long.toHexString(type));
        scanBlueDevicesMap.clear();
        discoveryType |= type;

        //jing 20160808 针对BP3M -- USB 类型特殊处理
        if ((discoveryType & DISCOVERY_BP3M) != 0) {
            UsbDevice usbDevice = getLinkedUsbDevice();
            if (usbDevice != null) {
                discoveryDevice("000000000000", "BP3M", null, null, usbDevice, ScanDevice.LINK_USB, 0, new HashMap<String, Object>());
            }
            if (discoveryType == DISCOVERY_BP3M) {
                return;
            }
        }

        if (((discoveryType & DISCOVERY_HS5_WIFI) != 0) && !isUdpSearch) {
            startUDPSearchTimer();
            if (discoveryType == DISCOVERY_HS5_WIFI) {
                return;
            }
        }
        if (bluetoothAdapter.isEnabled()) {
            if (type <= discoveryTypeForBle) {
                startScanBle();
            } else {
                //jing 20160830  去掉之前的检查已配对iHealth设备的判断
                btScanCount = 1;
                if (bluetoothAdapter == null) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                }
                startBTScan();
            }
        } else {
            Log.e(TAG, "Bluetooth is currently disabled");
            //如果只扫描的BT BLE设备，则停止扫描
            if ((discoveryType & DISCOVERY_HS5_WIFI) == 0 && (discoveryType & DISCOVERY_BP3M) == 0) {
                //回调UI
                if (mScanFinishThread != null)
                    mainThreadHandler.post(mScanFinishThread);
            }
        }
    }


    private long btScandelay = 3000;
    private Timer btScanTimer = null;
    private TimerTask btScanTask = null;
    private int btScanCount = 1;

    private void startBTScan() {
        if (btScanTimer != null) {
            btScanTimer.cancel();
            btScanTimer = null;
        }
        if (btScanTask != null) {
            btScanTask.cancel();
            btScanTask = null;
        }

        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (btScanCount % 2 == 1) {
            bluetoothAdapter.startDiscovery();
            btScandelay = 3000;
        } else {
            bluetoothAdapter.cancelDiscovery();
            btScandelay = 1000;
        }

        btScanTimer = new Timer();
        btScanTask = new TimerTask() {
            public void run() {
                if (btScanCount < 3 * 2) {
                    btScanCount++;
                    startBTScan();
                } else {
                    btScanCount = 1;
                    stopBTScan();
                    //回调UI
                    if (mScanFinishThread != null)
                        mainThreadHandler.post(mScanFinishThread);
                }
            }
        };
        btScanTimer.schedule(btScanTask, btScandelay);
    }


    private boolean stopBTScan() {
        if (btScanTimer != null) {
            btScanTimer.cancel();
            btScanTimer = null;
        }
        if (btScanTask != null) {
            btScanTask.cancel();
            btScanTask = null;
        }

        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        boolean isDiscovering = bluetoothAdapter.isDiscovering();
        bluetoothAdapter.cancelDiscovery();
        return isDiscovering;
    }

    /**
     * Stop discovery process.
     */
    public void stopDiscovery() {
        Log.p(TAG, Log.Level.INFO, "stopDiscovery");
        discoveryType = 0;
        btScanCount = 1;
        boolean isDiscoveringBT = stopBTScan();
        boolean isDiscoveringBle = stopScanBle();

        if (isDiscoveringBT == true || isDiscoveringBle == true) {
            //回调UI
            if (mScanFinishThread != null)
                mainThreadHandler.post(mScanFinishThread);
        }
    }

    //jing 20160525 调用Connect时，内部停止扫描
    private void stopDiscoveryForConnect() {
        stopDiscovery();
    }

    private boolean checkBandingDevice() {
        Set<BluetoothDevice> deviceSet = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : deviceSet) {
            String name = device.getName();
            if (name != null && (name.contains(TYPE_BP5)
                    || name.contains(TYPE_BP7)
                    || name.contains(TYPE_BG5)
                    || name.contains(TYPE_HS3)
                    || name.contains(TYPE_HS4S)
                    || name.contains(TYPE_HS5))) {
                return true;
            }
        }
        return false;
    }

    private void initReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
//        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(IHEALTH_MSG_BG5_EE);
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(iHealthDevicesIDPS.MSG_IHEALTH_DEVICE_IDPS);
        intentFilter.addAction(UsbManager.EXTRA_DEVICE);
        intentFilter.addAction(UsbManager.EXTRA_PERMISSION_GRANTED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        intentFilter.addAction(MSG_BASECOMMTIMEOUT);
        mContext.registerReceiver(bluetoothReceiver, intentFilter);
    }

    private void discoveryDevice(String address, String type, BluetoothDevice device, Hs5Control hs5Control,
                                 UsbDevice usbdevice, int linkType, int rssi, Map<String, Object> manufactorData) {

        String mac = address.replace(":", "");
        mapMacAndType.put(mac, type);
        synchronized (scanlock) {
            if (scanBlueDevicesMap.get(mac) == null) {
                ScanDevice scandevie = new ScanDevice(linkType, device, hs5Control, usbdevice, mac, type);
                scanBlueDevicesMap.put(mac, scandevie);
                mScanThread.setScanMessage(mac, type, rssi, manufactorData);
                mainThreadHandler.post(mScanThread);
            }
        }
    }

    //jing 20160728 多次返回ScanFinished
    private Date lastFinishedDate = new Date();
    private final Object scanlock = new Object();
    //jing 20160819 记录网络状态
    boolean wifiIsOnFlag = false;
    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

        @Override
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {

            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String address = device.getAddress();
                String name = device.getName();
                Log.p(TAG, Log.Level.VERBOSE, "ACTION_FOUND", device.getName(), device.getAddress());
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                        && device.getType() != BluetoothDevice.DEVICE_TYPE_LE)) {

                    if (name != null && name.contains(TYPE_BG5) && ((discoveryType & DISCOVERY_BG5) != 0)) {
                        discoveryDevice(address, TYPE_BG5, device, null, null, ScanDevice.LINK_BT, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(TYPE_BP5) && ((discoveryType & DISCOVERY_BP5) != 0)
                            ) {//|| (discoveryType & DISCOVERY_ABI) != 0
                        discoveryDevice(address, TYPE_BP5, device, null, null, ScanDevice.LINK_BT, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(TYPE_BP7S) && ((discoveryType & DISCOVERY_BP7S) != 0)) {
                        discoveryDevice(address, TYPE_BP7S, device, null, null, ScanDevice.LINK_BT, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(TYPE_BP7) && ((discoveryType & DISCOVERY_BP7) != 0)) {
                        discoveryDevice(address, TYPE_BP7, device, null, null, ScanDevice.LINK_BT, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_HS3) && ((discoveryType & DISCOVERY_HS3) != 0)) {
                        discoveryDevice(address, TYPE_HS3, device, null, null, ScanDevice.LINK_BT, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_HS4S) && ((discoveryType & DISCOVERY_HS4S) != 0)) {
                        discoveryDevice(address, TYPE_HS4S, device, null, null, ScanDevice.LINK_BT, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_HS5) && ((discoveryType & DISCOVERY_HS5_BT) != 0)) {
                        discoveryDevice(address, TYPE_HS5_BT, device, null, null, ScanDevice.LINK_BT, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_KD926) && ((discoveryType & DISCOVERY_KD926) != 0)) {
                        discoveryDevice(address, TYPE_KD926, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_KD723) && ((discoveryType & DISCOVERY_KD723) != 0)) {
                        discoveryDevice(address, TYPE_KD723, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_ABPM) && ((discoveryType & DISCOVERY_ABPM) != 0)) {
                        discoveryDevice(address, TYPE_ABPM, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());
                    }
                }
                //非配对设备，需要先判断下是否是BLE
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                    if (name != null && ((name.contains(NAME_AM3) || (name.contains(TYPE_AM3)) && !(name.contains(TYPE_AM3S)))) && ((discoveryType & DISCOVERY_AM3) != 0)) {
                        discoveryDevice(address, TYPE_AM3, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(TYPE_AM3S) && ((discoveryType & DISCOVERY_AM3S) != 0)) {
                        discoveryDevice(address, TYPE_AM3S, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(TYPE_AM4) && ((discoveryType & DISCOVERY_AM4) != 0)) {
                        discoveryDevice(address, TYPE_AM4, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(TYPE_BP3L) && ((discoveryType & DISCOVERY_BP3L) != 0)) {
                        discoveryDevice(address, TYPE_BP3L, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && ((name.contains(NAME_HS4) || (name.contains(TYPE_HS4)) && !(name.contains(TYPE_HS4S)))) && ((discoveryType & DISCOVERY_HS4) != 0)) {
                        discoveryDevice(address, TYPE_HS4, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && ((name.contains(NAME_PO3) || name.contains(TYPE_PO3))) && ((discoveryType & DISCOVERY_PO3) != 0)) {
                        discoveryDevice(address, TYPE_PO3, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_550BT) && ((discoveryType & DISCOVERY_BP550BT) != 0)) {
                        discoveryDevice(address, TYPE_550BT, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_BTM) && ((discoveryType & DISCOVERY_BTM) != 0)) {
                        discoveryDevice(address, TYPE_BTM, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_KD926) && ((discoveryType & DISCOVERY_KD926) != 0)) {
                        discoveryDevice(address, TYPE_KD926, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_KD723) && ((discoveryType & DISCOVERY_KD723) != 0)) {
                        discoveryDevice(address, TYPE_KD723, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());

                    } else if (name != null && name.contains(NAME_ABPM) && ((discoveryType & DISCOVERY_ABPM) != 0)) {
                        discoveryDevice(address, TYPE_ABPM, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());
                    } else if (name != null && name.contains(NAME_BG5l) && ((discoveryType & DISCOVERY_BG5l) != 0)) {
                        discoveryDevice(address, TYPE_BG5l, device, null, null, ScanDevice.LINK_BLE, 0, new HashMap<String, Object>());
                    }
                }

            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Date nowDate = new Date();
                if ((nowDate.getTime() - lastFinishedDate.getTime()) > 1000) {
                    lastFinishedDate = nowDate;
                    if (mScanFinishThread != null)
                        mainThreadHandler.post(mScanFinishThread);
                } else {

                }


            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager mConnectivityManager = (ConnectivityManager) context.
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {
                    wifiIsOnFlag = true;
                    String name = info.getTypeName();
                    Log.v(TAG, "network name：" + name);
//                    if (((discoveryType & DISCOVERY_HS5_WIFI) != 0) && !isUdpSearch) {
//                        startUDPSearchTimer();
//                    }

                } else {
                    wifiIsOnFlag = false;
                    Log.v(TAG, "net is unavailable");
//                    stopUdpSearchTimer();
                    if (HS5WifiMap.size() != 0) {
                        Iterator<Entry<String, Hs5Control>> iterator = HS5WifiMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Entry<String, Hs5Control> entry = iterator.next();
                            String mac = entry.getKey();
                            mBaseCommCallback.onConnectionStateChange(mac, TYPE_HS5, DEVICE_STATE_DISCONNECTED, 0, null);
                        }
                    }

                }
            } else if (action.equals(IHEALTH_MSG_BG5_EE)) {
                String mac = intent.getStringExtra(IHEALTH_DEVICE_MAC);
                String ee = intent.getStringExtra(IHEALTH_MSG_BG5_EE_EXTRA);
                setEE(mac, ee);
                Log.v(TAG, "Read BG5 idps success");
                btCommThreadee.close();

                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                ConnectThread connectThread = new ConnectThread(device, iHealthDevicesManager.TYPE_BG5, false);
                connectThread.start();

            } else if (action.equals(iHealthDevicesIDPS.MSG_IHEALTH_DEVICE_IDPS)) {
                /* when a new device is connected by Bluetooth 3.0, need to setDeviceIDPS */
                String protoclString = intent.getStringExtra(iHealthDevicesIDPS.PROTOCOLSTRING);
                String accessoryName = intent.getStringExtra(iHealthDevicesIDPS.ACCESSORYNAME);
                String firmwareVersion = intent.getStringExtra(iHealthDevicesIDPS.FIRMWAREVERSION);
                String hardwareVersion = intent.getStringExtra(iHealthDevicesIDPS.HARDWAREVERSION);
                String manufaturer = intent.getStringExtra(iHealthDevicesIDPS.MANUFACTURER);
                String modelNumber = intent.getStringExtra(iHealthDevicesIDPS.MODENUMBER);
                String serialNumber = intent.getStringExtra(iHealthDevicesIDPS.SERIALNUMBER);
                String type = intent.getStringExtra(iHealthDevicesManager.IHEALTH_DEVICE_TYPE);
                IDPS iDPS = new IDPS();
                iDPS.setProtoclString(protoclString);
                iDPS.setAccessoryName(accessoryName);
                iDPS.setAccessoryFirmwareVersion(firmwareVersion);
                iDPS.setAccessoryHardwareVersion(hardwareVersion);
                iDPS.setAccessoryManufaturer(manufaturer);
                iDPS.setAccessoryModelNumber(modelNumber);
                iDPS.setAccessorySerialNumber(serialNumber);
                iDPS.setDeviceType(type);
                mapIdps.put(serialNumber, iDPS);
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice usbdevice = getLinkedUsbDevice();
                if (usbdevice != null && (discoveryType & DISCOVERY_BP3M) != 0) {
                    discoveryDevice("000000000000", "BP3M", null, null, usbdevice, ScanDevice.LINK_USB, 0, new HashMap<String, Object>());
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (mBp3mControl != null) {
                    mBp3mControl = null;
                    mBaseCommCallback.onConnectionStateChange("000000000000", "BP3M", DEVICE_STATE_DISCONNECTED, 0, null);
                }
                if (uartInterface != null) {
                    uartInterface.disConnectFunction();
                }
                if (mPL2303Interface != null) {
                    mPL2303Interface.disConnectFunction();
                }
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                connectUsbDevice();
            } else if (MSG_BASECOMMTIMEOUT.equals(action)) {
                String mac = intent.getStringExtra(iHealthDevicesManager.IHEALTH_DEVICE_MAC);
                mNotifyThread.setNotifyMessage(mac, "", IHEALTH_COMM_TIMEOUT, "");
                mainThreadHandler.post(mNotifyThread);
            }
        }
    };

    private IDPS iDPS;
    private Map<String, IDPS> mapIdps = new ConcurrentHashMap<>();

    private BleComm mBleComm;
    private BaseComm mBaseComm;

    private void bleInit() {
        if (true == isSupportBLE(mContext)) {
            mBleComm = new AndroidBle(mContext, mBtleCallback);
            mBaseComm = mBleComm.getBaseComm();
        }
    }

    /**
     * 是否支持低功耗：true:支持 false:不支持
     *
     * @param context
     * @return
     */
    private boolean isSupportBLE(Context context) {
        boolean hasSystemFeature = context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le");
        if (hasSystemFeature) {
            if (Build.VERSION.SDK_INT >= 18) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private long delay = 12000;
    private Timer timer = null;
    private TimerTask btTask = null;
    private boolean bleScanFlag = false;

    private void startScanBle() {
        //jing 20160525 防止多次触发
        if (timer != null)
            timer.cancel();
        if (btTask != null)
            btTask.cancel();
        if (mBleComm != null) {
            mBleComm.scan(true);
            bleScanFlag = true;
        }

        timer = new Timer();
        btTask = new TimerTask() {
            public void run() {
                bleScanFlag = false;
                stopScanBle();
                //回调UI
                if (mScanFinishThread != null)
                    mainThreadHandler.post(mScanFinishThread);
            }
        };
        timer.schedule(btTask, delay);
    }

    private boolean stopScanBle() {
        if (mBleComm != null) {
            mBleComm.scan(false);
        }
        //jing 20160713  扫描结束后，清除扫描类型，等待下次重新赋值
        discoveryType = 0;
        if (timer != null)
            timer.cancel();
        if (btTask != null)
            btTask.cancel();

        if (bleScanFlag == true) {
            bleScanFlag = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * connection by BluetoothLE.
     *
     * @param deviceMac mac of BluetoothDevice without colon.
     * @param type      type of BluetoothDevice.
     * @hide
     */
    private synchronized void connectBleDevice(final String deviceMac, final String type) {
        mBaseCommCallback.onConnectionStateChange(deviceMac, type, DEVICE_STATE_CONNECTING, 0, null);
        if (scanBlueDevicesMap.get(deviceMac) != null) {
            ScanDevice scanDevice = scanBlueDevicesMap.get(deviceMac);
            if (scanDevice != null) {
                BluetoothDevice device = scanDevice.getDevice();
//                setConnectionTimeOut();
                boolean result = false;
                if (mBleComm != null) {
                    result = mBleComm.connectDevice(device.getAddress());
                }
                Log.v(TAG, "connection result: " + result);
                if (!result) {
                    mainThreadHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBaseCommCallback.onConnectionStateChange(deviceMac, type, DEVICE_STATE_CONNECTIONFAIL, 0, null);
                        }
                    }, 1000);
                }
            }
        }
    }

    private synchronized void connectBleDeviceDirect(String deviceMac) {
        String address = deviceMac.substring(0, 2) + ":" + deviceMac.substring(2, 4) + ":" + deviceMac.substring(4, 6) + ":" +
                deviceMac.substring(6, 8) + ":" + deviceMac.substring(8, 10) + ":" + deviceMac.substring(10, 12);
        boolean result = false;
        if (mBleComm != null) {
            result = mBleComm.connectDevice(address);
        }
        Log.v(TAG, "connection result: " + result);
        if (!result) {
            mBaseCommCallback.onConnectionStateChange(deviceMac, "", DEVICE_STATE_CONNECTIONFAIL, 0, null);
        }
    }

    private synchronized void connectBTDeviceDirect(String deviceMac, String type) {
        String address = deviceMac.substring(0, 2) + ":" + deviceMac.substring(2, 4) + ":" + deviceMac.substring(4, 6) + ":" +
                deviceMac.substring(6, 8) + ":" + deviceMac.substring(8, 10) + ":" + deviceMac.substring(10, 12);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        connectBtDevice(device, type);
    }


    private String mAddress;
    private BleUuid mBleUuid;
    private Map<String, BleUuid> mapBleUuid = new HashMap<String, BleUuid>();
    private ArrayList<String> listUuid;
    private int mUuidId = -1;

    private void setCharacteristic() {
        mUuidId = -1;
    }

    private void getCharacteristic() {
        mUuidId += 1;
        if (mUuidId >= listUuid.size()) {
            if (wrongIdps(iDPS)) {
                String mac = mAddress.replace(":", "");
                mBleComm.refresh(mac);
            } else {
                //IDPS读取成功，Enable 接收数据通道
                String serviceuuid = mBleUuid.BLE_SERVICE;
                String type = getDeviceTypeFromSeviceUUID(serviceuuid, iDPS.getDeviceName());
                //Continua BP特殊处理，读取完IDPS就算连接成功
                if (type.equals(TYPE_CBP) || type.equals(TYPE_CBG) || type.equals(TYPE_CPO) || type.equals(TYPE_CHS)
                        || type.equals(TYPE_CBS)) {
                    identify(iDPS.getDeviceMac(), iDPS.getDeviceType());
                } else {
                    mBleComm.getService(iDPS.getDeviceMac(), mBleUuid.BLE_SERVICE, mBleUuid.BLE_TRANSMIT,
                            mBleUuid.BLE_RECEIVE,
                            mBleUuid.BLE_IDPS_INFO, false);
                }
            }
        } else {
            String uuid = listUuid.get(mUuidId);
            boolean result = mBleComm.Obtain(iDPS.getDeviceMac(), UUID.fromString(mBleUuid.BLE_IDPS_INFO), UUID.fromString(uuid));
            if (!result) {
                Log.v(TAG, "Obtain:" + result);
            }
        }

    }

    public BtleCallback mBtleCallback = new BtleCallback() {

        @Override
        public void onServicesObtain() {
            int delayIdentify = 0;
            //jing 20160618 判断特殊手机，延时5s进行认证
            if (Build.VERSION.RELEASE.startsWith("4.3") || Build.VERSION.RELEASE.startsWith("4.4")) {
                SharedPreferences sharedPreferences = mContext.getSharedPreferences("SpecialPhone" + Build.VERSION.RELEASE, Context.MODE_PRIVATE);
                int phoneStatus = sharedPreferences.getInt("SpecialPhoneStatus", 0);
                if (phoneStatus > 0) {
                    delayIdentify = 5000;
                    Log.v(TAG, "Special phone identify");
                }
            }

            android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    identify(iDPS.getDeviceMac(), iDPS.getDeviceType());
                }
            }, delayIdentify);

        }

        @Override
        public void onServicesDiscovered(BluetoothDevice device, List<UUID> uuids, int status) {
            mBleUuid = BleConfig.getUuidString(uuids);

            //配置部分IDPS
            iDPS = new IDPS();
            iDPS.setDeviceName(device.getName());
            iDPS.setDeviceMac(device.getAddress().replace(":", ""));
            String serviceuuid = mBleUuid.BLE_SERVICE;
            //jing 20160906 Android 7.0bug
            if (serviceuuid == null) {
                Log.e(TAG, "Not found service");
                return;
            }
            String type = getDeviceTypeFromSeviceUUID(serviceuuid, device.getName());
            iDPS.setDeviceType(type);

            //jing 20160704
            mapMacAndType.put(device.getAddress().replace(":", ""), type);

            //准备读取IDPS
            if (listUuid != null)
                listUuid.clear();
            setCharacteristic();
            listUuid = mBleUuid.listUuid;
            //存储扫描到的UUID
            mapBleUuid.put(device.getAddress().replace(":", ""), mBleUuid);

            getCharacteristic();
        }

        @Override
        public void onScanResult(BluetoothDevice scandevice, int rssi, String serviceUuid, Map<String, Object> manufactorData) {
            String address = scandevice.getAddress();
            String name = scandevice.getName();
            if (serviceUuid.contains(BleConfig.UUID_HS4_SERVICE) && ((discoveryType & DISCOVERY_HS4) != 0)) {
                discoveryDevice(address, TYPE_HS4, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if ((serviceUuid.contains(BleConfig.UUID_ABPM_SERVICE) && ((discoveryType & DISCOVERY_ABPM) != 0))) {
                discoveryDevice(address, TYPE_ABPM, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);
            } else if ((serviceUuid.contains(BleConfig.UUID_PO3_SERVICE) || serviceUuid.contains(BleConfig.UUID_PO3_SERVICE_128)) && ((discoveryType & DISCOVERY_PO3) != 0)) {
                discoveryDevice(address, TYPE_PO3, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if ((serviceUuid.contains(BleConfig.UUID_AM3_SERVICE) || serviceUuid.contains(BleConfig.UUID_AM3_Qualcomm_SERVICE)) && ((discoveryType & DISCOVERY_AM3) != 0)) {
                discoveryDevice(address, TYPE_AM3, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_AM3S_SERVICE) && ((discoveryType & DISCOVERY_AM3S) != 0)) {
                discoveryDevice(address, TYPE_AM3S, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_AM4_SERVICE) && ((discoveryType & DISCOVERY_AM4) != 0)) {
                discoveryDevice(address, TYPE_AM4, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_BP3L_SERVICE) && ((discoveryType & DISCOVERY_BP3L) != 0)) {
                discoveryDevice(address, TYPE_BP3L, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_AV10_SERVICE)) {
                if (name != null) {
                    if (name.contains(NAME_KD926) && (discoveryType & DISCOVERY_KD926) != 0) {
                        discoveryDevice(address, TYPE_KD926, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);
                    } else if (name.contains(NAME_KD723) && (discoveryType & DISCOVERY_KD723) != 0) {
                        discoveryDevice(address, TYPE_KD723, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);
                    } else if (name.contains(NAME_550BT) && (discoveryType & DISCOVERY_BP550BT) != 0) {
                        discoveryDevice(address, TYPE_550BT, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);
                    }
                }
            } else if (serviceUuid.contains(BleConfig.UUID_BTM_Primary_SERVICE) && ((discoveryType & DISCOVERY_BTM) != 0)) {
                discoveryDevice(address, TYPE_BTM, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_BP_SERVICE) && ((discoveryType & DISCOVERY_CBP) != 0)) {
                discoveryDevice(address, TYPE_CBP, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_BG5l_SERVICE) && ((discoveryType & DISCOVERY_BG5l) != 0)) {
                discoveryDevice(address, TYPE_BG5l, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_BG_SERVICE) && ((discoveryType & DISCOVERY_CBG) != 0)) {
                discoveryDevice(address, TYPE_CBG, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_PO_SERVICE) && ((discoveryType & DISCOVERY_CPO) != 0)) {
                discoveryDevice(address, TYPE_CPO, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_HS_SERVICE) && ((discoveryType & DISCOVERY_CHS) != 0)) {
                discoveryDevice(address, TYPE_CHS, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else if (serviceUuid.contains(BleConfig.UUID_BS_SERVICE) && ((discoveryType & DISCOVERY_CBS) != 0)) {
                discoveryDevice(address, TYPE_CBS, scandevice, null, null, ScanDevice.LINK_BLE, rssi, manufactorData);

            } else {
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            String mac = device.getAddress().replace(":", "");
            Log.p(TAG, Log.Level.VERBOSE, "onConnectionStateChange", mac, status, newState);
            //对重连状态进行判断
            if (newState == 4) {
                String type = mapMacAndType.get(mac);
                mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_RECONNECTING, status, null);
            } else if ((status == BluetoothGatt.GATT_SUCCESS) && (newState == BluetoothProfile.STATE_CONNECTED)) {
                mAddress = device.getAddress();
            } else if ((status == BluetoothGatt.GATT_SUCCESS) && (newState == BluetoothProfile.STATE_DISCONNECTED)) {
                String type = mapMacAndType.get(mac);
                String tempType = connectedDeviceMap.get(mac);
                if (tempType != null) {
                    mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_DISCONNECTED, status, null);
                } else {
                    commandSaveSpecialPhone(mac);
                    mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_CONNECTIONFAIL, status, null);
                }
            } else {
                String type = mapMacAndType.get(mac);
                String tempType = connectedDeviceMap.get(mac);
                if (tempType != null) {
                    mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_DISCONNECTED, status, null);
                } else {
                    commandSaveSpecialPhone(mac);
                    mBaseCommCallback.onConnectionStateChange(mac, type, DEVICE_STATE_CONNECTIONFAIL, status, null);
                }

            }
        }

        private void commandSaveSpecialPhone(String tempMac) {
            IDPS tempIDPS = mapIdps.get(tempMac);
            if (tempIDPS != null && (Build.VERSION.RELEASE.startsWith("4.3") || Build.VERSION.RELEASE.startsWith("4.4"))) {
                Log.v(TAG, "Special phone");
                SharedPreferences sharedPreferences = mContext.getSharedPreferences("SpecialPhone" + Build.VERSION.RELEASE, Context.MODE_PRIVATE);
                boolean firstSpecialFlag = sharedPreferences.getBoolean("FirstSpecialFlag", false);
                if (firstSpecialFlag == false) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("SpecialPhoneStatus", 1);
                    editor.putBoolean("FirstSpecialFlag", true);
                    editor.commit();
                }
            }
        }

        @Override
        public void onCharacteristicRead(byte[] characteristicReadValue, UUID charUuid, int status) {
            String uuidString = charUuid.toString();
            if (uuidString.equals(mBleUuid.PROTOCOL_STRING)) {
                int len = 0;
                for (byte b : characteristicReadValue) {
                    if (b == 0) {
                        break;
                    } else {
                        len += 1;
                    }
                }
                try {
                    iDPS.setProtoclString(new String(ByteBufferUtil.bufferCut(characteristicReadValue, 0, len), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                getCharacteristic();
//                mBleComm.Obtain(UUID.fromString(mBleUuid.ACCESSORY_NAME));

            } else if (uuidString.equals(mBleUuid.ACCESSORY_NAME)) {
                int len = 0;
                for (byte b : characteristicReadValue) {
                    if (b == 0) {
                        break;
                    } else {
                        len += 1;
                    }
                }
                try {
                    iDPS.setAccessoryName(new String(ByteBufferUtil.bufferCut(characteristicReadValue, 0, len), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                getCharacteristic();
//                mBleComm.Obtain(UUID.fromString(mBleUuid.ACCESSORY_FIRMWARE_VERSION));

            } else if (uuidString.equals(mBleUuid.ACCESSORY_FIRMWARE_VERSION)) {
                if ((characteristicReadValue[0] > 30) && characteristicReadValue.length == 3) {
                    try {
                        iDPS.setAccessoryFirmwareVersion(new String(characteristicReadValue, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    iDPS.setAccessoryFirmwareVersion(ByteBufferUtil.Bytes2HexString(characteristicReadValue));
                }
                getCharacteristic();

            } else if (uuidString.equals(mBleUuid.ACCESSORY_HARDWARE_VERSION)) {
                if ((characteristicReadValue[0] > 30) && characteristicReadValue.length == 3) {
                    try {
                        iDPS.setAccessoryHardwareVersion(new String(characteristicReadValue, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    iDPS.setAccessoryHardwareVersion(ByteBufferUtil.Bytes2HexString(characteristicReadValue));
                }
                getCharacteristic();

            } else if (uuidString.equals(mBleUuid.ACCESSORY_MANUFA)) {
                int len = 0;
                for (byte b : characteristicReadValue) {
                    if (b == 0) {
                        break;
                    } else {
                        len += 1;
                    }
                }
                try {
                    iDPS.setAccessoryManufaturer(new String(ByteBufferUtil.bufferCut(characteristicReadValue, 0, len),
                            "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                getCharacteristic();

            } else if (uuidString.equals(mBleUuid.ACCESSORY_MODEL)) {
                int len = 0;
                for (byte b : characteristicReadValue) {
                    if (b == 0) {
                        break;
                    } else {
                        len += 1;
                    }
                }
                try {
                    iDPS.setAccessoryModelNumber(new String(ByteBufferUtil.bufferCut(characteristicReadValue, 0, len),
                            "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                getCharacteristic();

            } else if (uuidString.equals(mBleUuid.ACCESSORY_SERIAL)) {
                iDPS.setAccessorySerialNumber(ByteBufferUtil.Bytes2HexString(characteristicReadValue));
                getCharacteristic();
            } else {
                Log.v(TAG, "Invalidate characteristic UUID");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothDevice device, byte[] characteristicChangedValue) {
            Log.p(TAG, Log.Level.VERBOSE, "onCharacteristicChanged", device.getAddress().replace(":", ""));
        }
    };

    private String getDeviceTypeFromModeNumber(String mode) {
        if (mode == null) {
            return null;
        } else if (mode.contains(NAME_AM3) || (mode.contains(TYPE_AM3) && !(mode.contains(TYPE_AM3S)))) {
            return TYPE_AM3;
        } else if (mode.contains(TYPE_AM3S)) {
            return TYPE_AM3S;
        } else if (mode.contains(TYPE_AM4)) {
            return TYPE_AM4;
        } else if (mode.contains(NAME_PO3) || mode.contains(TYPE_PO3)) {
            return TYPE_PO3;
        } else if (mode.contains(NAME_HS4) || (mode.contains(TYPE_HS4) && !(mode.contains(TYPE_HS4S)))) {
            return TYPE_HS4;
        } else if (mode.contains(TYPE_BP3L)) {
            return TYPE_BP3L;
        } else if (mode.contains(NAME_550BT)) {
            return TYPE_550BT;
        } else if (mode.contains(NAME_KD926)) {
            return TYPE_KD926;
        } else if (mode.contains(NAME_KD723)) {
            return TYPE_KD723;
        } else if (mode.contains(TYPE_ABPM)) {
            return TYPE_ABPM;
        } else if (mode.contains(NAME_BTM)) {
            return TYPE_BTM;
        } else {
            return null;
        }
    }

    private String getDeviceTypeFromSeviceUUID(String uuid, String deviceName) {
        if (uuid == null) {
            return "";
        } else if (uuid.equals(BleConfig.UUID_AM3_SERVICE) || uuid.equals(BleConfig.UUID_AM3_Qualcomm_SERVICE)) {
            return TYPE_AM3;
        } else if (uuid.equals(BleConfig.UUID_AM3S_SERVICE)) {
            return TYPE_AM3S;
        } else if (uuid.equals(BleConfig.UUID_AM4_SERVICE)) {
            return TYPE_AM4;
        } else if (uuid.equals(BleConfig.UUID_PO3_SERVICE) || uuid.equals(BleConfig.UUID_PO3_SERVICE_128)) {
            return TYPE_PO3;
        } else if (uuid.equals(BleConfig.UUID_HS4_SERVICE)) {
            return TYPE_HS4;
        } else if (uuid.equals(BleConfig.UUID_BP3L_SERVICE)) {
            return TYPE_BP3L;
        } else if (uuid.equals(BleConfig.UUID_BTM_READ_DATA_SERVICE)) {
            return TYPE_BTM;
        } else if (uuid.equals(BleConfig.UUID_AV10_SERVICE) && (deviceName == null || deviceName.contains(NAME_550BT))) {
            // 这个几个设备都是用的一个类型,以后如果没有连接上,如何判断是否呢?  只能加上name一起判断了，后续更新
            return TYPE_550BT;
        } else if (uuid.equals(BleConfig.UUID_AV10_SERVICE) && (deviceName == null || deviceName.contains(NAME_KD926))) {
            return TYPE_KD926;
        } else if (uuid.equals(BleConfig.UUID_AV10_SERVICE) && (deviceName == null || deviceName.contains(NAME_KD723))) {
            return TYPE_KD723;
        } else if (uuid.equals(BleConfig.UUID_ABPM_SERVICE)) {
            return TYPE_ABPM;
        } else if (uuid.equals(BleConfig.UUID_BP_SERVICE)) {
            return TYPE_CBP;
        } else if (uuid.equals(BleConfig.UUID_BG5l_SERVICE)) {
            return TYPE_BG5l;
        } else if (uuid.equals(BleConfig.UUID_BG_SERVICE)) {
            return TYPE_CBG;
        } else if (uuid.equals(BleConfig.UUID_PO_SERVICE)) {
            return TYPE_CPO;
        } else if (uuid.equals(BleConfig.UUID_HS_SERVICE)) {
            return TYPE_CHS;
        } else if (uuid.equals(BleConfig.UUID_BS_SERVICE)) {
            return TYPE_CBS;
        } else {
            return "";
        }
    }

    private void identify(String mac, String type) {
        if (type == null) {
            type = getDeviceTypeFromModeNumber(iDPS.getAccessoryModelNumber());
        }
        Log.p(TAG, Log.Level.VERBOSE, "identify", mac, type);

        iDPS.setDeviceType(type);
        mapIdps.put(mac, iDPS);

        DeviceControl deviceControl = null;
        if (type.equals(TYPE_AM3)) {
            mAm3Control = new Am3Control(mBaseComm, mContext, mac, type, mUserName, mBaseCommCallback, mInsCallback);
            deviceControl = mAm3Control;

        } else if (type.equals(TYPE_AM3S)) {
            mAm3sControl = new Am3sControl(mBaseComm, mContext, mac, type, mUserName, mBaseCommCallback, mInsCallback);
            deviceControl = mAm3sControl;

        } else if (type.equals(TYPE_AM4)) {
            mAm4Control = new Am4Control(mBaseComm, mContext, mac, type, mUserName, mBaseCommCallback, mInsCallback);
            deviceControl = mAm4Control;

        } else if (type.equals(TYPE_PO3)) {
            String firmString = iDPS.getAccessoryFirmwareVersion();
            mPo3Control = new Po3Control(mUserName, mContext, firmString, mBaseComm, mac, type, mBaseCommCallback, mInsCallback);
            deviceControl = mPo3Control;

        } else if (type.equals(TYPE_HS4)) {
            mHs4Control = new Hs4Control(mUserName, mContext, mBaseComm, mac, type, mBaseCommCallback, mInsCallback);
            deviceControl = mHs4Control;

        } else if (type.equals(TYPE_BP3L)) {
            mBp3lControl = new Bp3lControl(mContext, mBaseComm, mUserName, mac, type, mBaseCommCallback, mInsCallback);
            deviceControl = mBp3lControl;

        } else if (type.equals(TYPE_550BT)) {
            mBp550btControl = new Bp550BTControl(mContext, mBaseComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mBp550btControl;

        } else if (type.equals(TYPE_KD926)) {
            mBp926Control = new Bp926Control(mContext, mBaseComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mBp926Control;

        } else if (type.equals(TYPE_KD723)) {
            mBp723Control = new Bp723Control(mContext, mBaseComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mBp723Control;

        } else if (type.equals(TYPE_ABPM)) {
            mABPMControl = new ABPMControl(mContext, mBaseComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mABPMControl;

        } else if (type.equals(TYPE_BTM)) {
            mBtmControl = new BtmControl(mContext, mBaseComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mBtmControl;

        } else if (type.equals(TYPE_CBP)) {
            mBpControl = new BPControl(mContext, mBaseComm, mBleComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mBpControl;

        } else if (type.equals(TYPE_BG5l)) {
            mBg5lControl = new Bg5lControl(mContext, mBaseComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mBg5lControl;

        } else if (type.equals(TYPE_CBG)) {
            mBgControl = new BgControl(mContext, mBaseComm, mBleComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mBgControl;

        } else if (type.equals(TYPE_CPO)) {
            mPoControl = new PoControl(mContext, mBaseComm, mBleComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mPoControl;

        } else if (type.equals(TYPE_CHS)) {
            mHsControl = new HsControl(mContext, mBaseComm, mBleComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mHsControl;

        } else if (type.equals(TYPE_CBS)) {
            mBsControl = new BsControl(mContext, mBaseComm, mBleComm, mUserName, mac, type, mInsCallback, mBaseCommCallback);
            deviceControl = mBsControl;

        } else {
            Log.v(TAG, "no such device");
        }

        if (deviceControl != null) {
            deviceControl.init();
        }
    }

    private boolean wrongIdps(IDPS idps) {
        //type == null 代表出现异常
        String type = idps.getDeviceType();
        if (type == null) {
            return true;
        }
        //非iHealth产品不验证idps
        if (type.equals(TYPE_BTM) || type.equals(TYPE_CBP) || type.equals(TYPE_CBG) || type.equals(TYPE_CPO)
                || type.equals(TYPE_CHS) || type.equals(TYPE_CBS)) {
            return false;
        }
        String name = idps.getDeviceName();
        if (name != null && name.contains(NAME_BTM)) {
            return false;
        }
        if (!idps.getProtoclString().startsWith("com.")) {
            return true;
        }
        if (idps.getAccessoryFirmwareVersion() == null) {
            return true;
        }
        if (idps.getAccessoryHardwareVersion() == null) {
            return true;
        }
        return false;
    }

    /**
     * Get IDPS message from iHealth device.
     *
     * @hide
     */
    public IDPS getIdps(String mac) {
        return mapIdps.get(mac);
    }

    /***************************** wifi manager **************************************************/
    /***************************************************************************************/
    private WifiCommThread wifiCommThread;
    private WifiManager mWifiManager;
    private Handler mHandler;
    private HandlerThread handlerthread;
    private static DatagramSocket udpSocket; // UDP socket
    // private int userId = 0;
    private boolean firstTime = true; // the first time open app

    /**
     * wifi 建立udpSocket以及收发数据的线程
     */
    private void wifiInit(Context context, BaseCommCallback baseCommCallback, InsCallback insCallback) {
        // hs5wifi
        HS5WifiMap.clear();
        HS5ConnectedWifiMap.clear();
        HS5ConnecttingMap.clear();
        HS5CountMap.clear();
        // 创建收发数据线程
        if (handlerthread != null && handlerthread.isAlive()) {
            Log.v(TAG, "handlerthread has existed");
        } else {
            handlerthread = new HandlerThread("MyHandlerThread");
            handlerthread.start();
        }
        if (mHandler != null) {
            Log.v(TAG, "mHandler has existed");
        } else {
            mHandler = new Handler(handlerthread.getLooper());
        }
        mWifiManager = (WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE);
        openUdpSoket();
    }

    /**
     * scan idps callback
     */
    private UdpSearchCallback udpSearchCallback = new UdpSearchCallback() {
        public void searchUdpNotify(byte[] datas) {
            byte[] mac = ByteBufferUtil.bytesCutt(datas.length - 7, datas.length - 2, datas);
            String receiveMac = ByteBufferUtil.Bytes2HexString(mac);
            AnalysisIDPS(receiveMac, ByteBufferUtil.bytesCutt(6, datas.length - 8, datas));
        }

        ;
    };

    /**
     * close udpsocket and destroy wifi thread
     */
    private void closeUdpSocket() {
        if (wifiCommThread != null && mHandler != null) {
            wifiCommThread.stopFlag = true;
            mHandler.removeCallbacks(wifiCommThread);
        }
        if (udpSocket != null) {
            udpSocket.close();
            udpSocket = null;
        }

    }

    /**
     * creat udpsocket and wifi thread
     */
    private void openUdpSoket() {

        if (mWifiManager != null) {
            if (udpSocket == null) {
                try {
                    udpSocket = new DatagramSocket(8000);
                    if (wifiCommThread != null && wifiCommThread.isAlive()) {
                        Log.v(TAG, "wifiCommThread has existed");
                    } else {
                        wifiCommThread = new WifiCommThread(udpSearchCallback, mContext, udpSocket, mWifiManager);
                        if (mHandler != null) {
                            mHandler.post(wifiCommThread);
                        }

                    }
                } catch (Exception e) {
                    Log.w(TAG, "openUdpSoket() -- e: " + e);
                }
            }
        } else {
            Log.e(TAG, "wifi is closed");
        }

    }

    private TimerTask wifiTask;

    private boolean isUdpSearch;
    private static Timer wifitimer;
    public static boolean stopUDPSearch = false;

    /**
     * udp scan timer ,need call wifiInit firstly
     */
    private void startUDPSearchTimer() {
        stopUdpSearchTimer();
        isUdpSearch = true;
        wifitimer = new Timer();
        wifiTask = new TimerTask() {
            public void run() {
                if (!stopUDPSearch) {
                    try {
                        UDPSearch();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.v(TAG, "+udp scan is be stopped");
                }
            }
        };
        wifitimer.schedule(wifiTask, 1000, 3000);
    }

    private InetAddress serverAddress;// UDP search address
    private DatagramPacket packet;
    // UDP search commands
    private byte[] UDP_SEARCH_COMMAND = {
            (byte) 0xB0, (byte) 0x04, (byte) 0x00,
            (byte) 0x00, (byte) 0xFF, (byte) 0xD0, (byte) 0xCF
    };

    private void UDPSearch() {
        //jing 20160819 先检查网络状态，网络打开再开启扫描
        if (wifiIsOnFlag == false) {
            Log.e(TAG, "UDPSearch but wifi is invalid");
            return;
        }
        try {
            udpSocket.setBroadcast(true);
            serverAddress = InetAddress.getByName("255.255.255.255");

            packet = new DatagramPacket(UDP_SEARCH_COMMAND, 7, serverAddress, 10000); // com port is
            // 10000
            udpSocket.send(packet);

            Iterator<Entry<String, Integer>> iterhs5Count = HS5CountMap.entrySet().iterator();
            while (iterhs5Count.hasNext()) {
                Entry<String, Integer> entry = (Entry<String, Integer>) iterhs5Count.next();
                Log.v(TAG, "start wifi scan ，need all wifi device count - 1,current mac = " + entry.getKey());

                entry.setValue(entry.getValue() - 1);
                if (entry.getValue() == 0) { // need disconnect
                    HS5CountMap.remove(entry.getKey());
                    HS5ConnectedWifiMap.remove(entry.getKey());
                    HS5ConnecttingMap.remove(entry.getKey());
                    // disconnect device
                    mBaseCommCallback.onConnectionStateChange(entry.getKey(), TYPE_HS5, DEVICE_STATE_DISCONNECTED, 0, null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (udpSocket != null) {
                udpSocket.close();
                udpSocket = null;
            }
        }
    }

    /**
     * stop udp scan
     */
    private void stopUdpSearchTimer() {
        isUdpSearch = false;
        if (wifitimer != null) {
            wifitimer.cancel();
            wifitimer = null;
        }
        if (wifiTask != null) {
            wifiTask.cancel();
            wifiTask = null;
        }
    }

    private void connectWifi(String mac, Hs5Control hs5Control) {
        hs5Control.init();
    }

    private void AnalysisIDPS(String devicemac, byte[] idpsData) {
        byte[] protocolVersion = new byte[16]; // 15
        byte[] accessoryName = new byte[10]; // 10
        byte[] firmware = new byte[3];
        byte[] hardware = new byte[3];
        byte[] manufacturer = new byte[7];
        byte[] modelNumber = new byte[9];
        byte[] serialNumber = new byte[16];
        byte[] deviceName = new byte[32];
        byte[] tempID = new byte[4];
        byte[] deviceMac = new byte[6];

        String str = "";
        String protocolVerStr = "";
        String accessoryNameStr = "";
        String firmwareStr = "";
        String hardwareStr = "";
        String manufacturerStr = "";
        String modelNumberStr = "";
        String serialNumberStr = "";
        String deviceNameStr = "";

        try {
            for (int i = 0; i < protocolVersion.length; i++) {
                protocolVersion[i] = idpsData[i + 0];
            }
            protocolVerStr = new String(protocolVersion, "UTF-8");
            for (int i = 0; i < accessoryName.length; i++) {
                accessoryName[i] = idpsData[i + 16];
            }
            accessoryNameStr = new String(accessoryName, "UTF-8");
            for (int i = 0; i < firmware.length; i++) {
                firmware[i] = idpsData[i + 32];
            }
            // firmwareStr = ByteBufferUtil.Bytes2HexString(firmware);
            // Log.v(TAG1, TAG + "firmwareStr:" + firmwareStr);
            firmwareStr = new String(firmware, "UTF-8");

            for (int i = 0; i < hardware.length; i++) {
                hardware[i] = idpsData[i + 35];
            }
            // hardwareStr = ByteBufferUtil.Bytes2HexString(hardware);
            // Log.v(TAG1, TAG + "hardwareStr:" + hardwareStr);
            hardwareStr = new String(hardware, "UTF-8");
            for (int i = 0; i < manufacturer.length; i++) {
                manufacturer[i] = idpsData[i + 38];
            }
            manufacturerStr = new String(manufacturer, "UTF-8");
            for (int i = 0; i < modelNumber.length; i++) {
                modelNumber[i] = idpsData[i + 54];
            }
            modelNumberStr = new String(modelNumber, "UTF-8");
            for (int i = 0; i < serialNumber.length; i++) {
                serialNumber[i] = idpsData[i + 70];
            }
            serialNumberStr = ByteBufferUtil.Bytes2HexString(serialNumber);
            for (int i = 0; i < deviceName.length; i++) {
                deviceName[i] = idpsData[i + 86];
            }
            deviceNameStr = ByteBufferUtil.Bytes2HexString(deviceName);
            for (int i = 0; i < tempID.length; i++) {
                tempID[i] = idpsData[i + 118];
            }
            // for (int i = 0; i < deviceMac.length; i++) {
            // deviceMac[i] = idpsData[i + 122];
            // }

            String tempIPStr = ByteBufferUtil.Bytes2HexString(tempID);
            String deviceIP = Integer.valueOf(tempIPStr.substring(0, 2), 16) + "."
                    + Integer.valueOf(tempIPStr.substring(2, 4), 16) + "."
                    + Integer.valueOf(tempIPStr.substring(4, 6), 16) + "."
                    + Integer.valueOf(tempIPStr.substring(6), 16);
            // String deviceMacStr = ByteBufferUtil.Bytes2HexString(deviceMac);

            str += "protocolVerStr：" + protocolVerStr + "\n";
            str += "accessoryNameStr：" + accessoryNameStr + "\n";
            str += "firmwareStr：" + firmwareStr + "\n";
            str += "hardwareStr：" + hardwareStr + "\n";
            str += "manufacturerStr：" + manufacturerStr + "\n";
            str += "modelNumberStr：" + modelNumberStr + "\n";
            str += "serialNumberStr：" + serialNumberStr + "\n";
            str += "deviceNameStr：" + deviceNameStr + "\n";
            str += "Device IP：" + deviceIP + "\n";
            str += "Device Mac：" + devicemac + "\n";

            if (!HS5ConnectedWifiMap.containsKey(devicemac)) { // 该设备不在map中
                if (!HS5ConnecttingMap.containsKey(devicemac)) {
                    Log.v(TAG, "new HS5：" + str);
                    if (wifiCommThread != null) {
                        mBaseCommCallback.onConnectionStateChange(devicemac, iHealthDevicesManager.TYPE_HS5,
                                iHealthDevicesManager.DEVICE_STATE_CONNECTING, 0, null);
                        mHs5Control = new Hs5Control(mUserName, mContext, devicemac, deviceIP, wifiCommThread,
                                mBaseCommCallback, mInsCallback, iHealthDevicesManager.TYPE_HS5);
                        IDPS wifiIDPSData = new IDPS();
                        wifiIDPSData.setProtoclString(protocolVerStr);
                        wifiIDPSData.setAccessoryName(accessoryNameStr);
                        wifiIDPSData.setAccessoryFirmwareVersion(firmwareStr);
                        wifiIDPSData.setAccessoryHardwareVersion(hardwareStr);
                        wifiIDPSData.setAccessoryManufaturer(manufacturerStr);
                        wifiIDPSData.setAccessoryModelNumber(modelNumberStr);
                        wifiIDPSData.setAccessorySerialNumber(serialNumberStr);
                        wifiIDPSData.setDeviceName(deviceNameStr);
                        wifiIDPSData.setDeviceIP(deviceIP);
                        wifiIDPSData.setDeviceMac(devicemac);
                        wifiIDPSData.setDeviceType(iHealthDevicesManager.TYPE_HS5);
                        mHs5Control.setWifiIDPSData(wifiIDPSData);
                        // notify scan callback
                        discoveryDevice(devicemac, TYPE_HS5, null, mHs5Control, null, ScanDevice.LINK_WIFI, 0, new HashMap<String, Object>());
                        // mHs5Control.init();
                    }

                } else {
                    Log.v(TAG, "HS5 connectting……");
                }

            } else {
                Log.v(TAG, "HS5 has been in the Map");
                Iterator<Entry<String, Integer>> iterhs5 = HS5CountMap.entrySet().iterator();
                while (iterhs5.hasNext()) {
                    Entry<String, Integer> entry = (Entry<String, Integer>) iterhs5.next();
                    Log.v(TAG, "the device is in map--mac=" + entry.getKey() + "still has " + entry.getValue()
                            + "  times changes");
                    if (entry.getKey().equals(devicemac)) {
                        entry.setValue(6);
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "AnalysisIDPS() -- e: " + e);
        }

    }

    /**
     * Get the information of a connected device
     *
     * @param mac The mac address of device
     * @return A string a json
     * <p>eg.{"protocolstring":"com.jiuan.AMV12","accessoryname":"AM4","firmwareversion":"138","hardwareversion":"100","manufacture":"iHealth","serialnumber":"004D32079148","modenumber":"AM4 11070"}</p>
     */
    public String getDevicesIDPS(String mac) {
        IDPS idps = mapIdps.get(mac);
        if (idps == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(iHealthDevicesIDPS.PROTOCOLSTRING, idps.getProtoclString());
            jsonObject.put(iHealthDevicesIDPS.ACCESSORYNAME, idps.getAccessoryName());
            jsonObject.put(iHealthDevicesIDPS.FIRMWAREVERSION, idps.getAccessoryFirmwareVersion());
            jsonObject.put(iHealthDevicesIDPS.HARDWAREVERSION, idps.getAccessoryHardwareVersion());
            jsonObject.put(iHealthDevicesIDPS.MANUFACTURER, idps.getAccessoryManufaturer());
            jsonObject.put(iHealthDevicesIDPS.SERIALNUMBER, idps.getAccessorySerialNumber());
            jsonObject.put(iHealthDevicesIDPS.MODENUMBER, idps.getAccessoryModelNumber());
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private NotifyThread mNotifyThread;

    private class NotifyThread implements Runnable {

        //jing 20160818 解决快速post引起的，只保留最后一组返回值的bug。。。
        private class NotifyInfoClass {
            public String mac;
            public String type;
            public String action;
            public String message;
        }

        //缓存post的信息，等待run执行时获取
        private Queue<NotifyInfoClass> notifyResultQueue = new LinkedList<>();


        public NotifyThread() {
        }

        public void setNotifyMessage(String mac, String type, String action, String message) {
            NotifyInfoClass notifyInfoClass = new NotifyInfoClass();
            notifyInfoClass.mac = mac;
            notifyInfoClass.type = type;
            notifyInfoClass.action = action;
            notifyInfoClass.message = message;
            notifyResultQueue.offer(notifyInfoClass);

            // //jing 20161011 Execute next command      gao 执行下一步先比对，再移除顶指令 防止死循环
            CommandCacheControl commandCacheControl = commandCacheControlMap.get(mac);
            if(commandCacheControl != null){
                if(commandCacheControl.getCommandCacheQueue().size() > 0){
                    commandCacheControl.commandExecuteAction(action);
                }
            }
        }

        @Override
        public void run() {
            boolean hasCallbackFlag = false;
            NotifyInfoClass notifyInfoClass = notifyResultQueue.poll();
            if (notifyInfoClass != null) {
                Log.p(TAG, Log.Level.INFO, "onDeviceNotify", notifyInfoClass.mac, notifyInfoClass.type,
                        notifyInfoClass.action, notifyInfoClass.message);
                List<iHealthDevicesCallback> list = mIHealthDeviceClientMap.getCallbacklist(notifyInfoClass.mac, notifyInfoClass.type);
                Iterator<iHealthDevicesCallback> i = list.iterator();
                while (i.hasNext()) {
                    hasCallbackFlag = true;
                    iHealthDevicesCallback m = i.next();
                    m.onDeviceNotify(notifyInfoClass.mac, notifyInfoClass.type, notifyInfoClass.action, notifyInfoClass.message);
                }
            }
            if (hasCallbackFlag == false) {
                Log.w(TAG, "NotifyThread -- Invalid callback");
            }
        }

    }

    private ScanThread mScanThread;

    private class ScanThread implements Runnable {
        //jing 20160818 解决快速post引起的，只保留最后一组返回值的bug。。。
        private class ScanInfoClass {
            public String mac = "";
            public String type = "";
            public int rssi = 0;
            public Map<String, Object> manufactureData = new HashMap<>();
        }

        //缓存post的信息，等待run执行时获取
        private Queue<ScanInfoClass> scanResultQueue = new LinkedList<>();

        public ScanThread() {
        }

        public void setScanMessage(String mac, String type, int rssi, Map<String, Object> manufactureData) {
            ScanInfoClass scanInfoClass = new ScanInfoClass();
            scanInfoClass.mac = mac;
            scanInfoClass.type = type;
            scanInfoClass.rssi = rssi;
            scanInfoClass.manufactureData = manufactureData;
            scanResultQueue.offer(scanInfoClass);
        }

        @Override
        public void run() {
            boolean hasCallbackFlag = false;
            //从队列中获取缓存的信息
            ScanInfoClass scanInfoClass = scanResultQueue.poll();
            if (scanInfoClass != null) {
                Log.p(TAG, Log.Level.INFO, "onScanDevice", scanInfoClass.mac, scanInfoClass.type,
                        scanInfoClass.rssi, scanInfoClass.manufactureData);
                List<iHealthDevicesCallback> list = mIHealthDeviceClientMap.getCallbacklist(scanInfoClass.mac, scanInfoClass.type);
                Iterator<iHealthDevicesCallback> i = list.iterator();
                while (i.hasNext()) {
                    hasCallbackFlag = true;
                    iHealthDevicesCallback m = i.next();
                    m.onScanDevice(scanInfoClass.mac, scanInfoClass.type, scanInfoClass.rssi);
                    m.onScanDevice(scanInfoClass.mac, scanInfoClass.type, scanInfoClass.rssi, scanInfoClass.manufactureData);
                }
                if (hasCallbackFlag == false) {
                    Log.w(TAG, "ScanThread -- Invalid callback");
                }
            }
        }
    }

    ScanFinishThread mScanFinishThread;

    private class ScanFinishThread implements Runnable {

        @Override
        public void run() {
            boolean hasCallbackFlag = false;
            Log.p(TAG, Log.Level.INFO, "onScanFinish");
            List<iHealthDevicesCallback> list = mIHealthDeviceClientMap.getCallbacklist_All();
            Iterator<iHealthDevicesCallback> i = list.iterator();
            while (i.hasNext()) {
                hasCallbackFlag = true;
                iHealthDevicesCallback m = i.next();
                m.onScanFinish();
            }
            if (hasCallbackFlag == false) {
                Log.w(TAG, "ScanFinishThread -- Invalid callback");
            }
        }
    }

    private ConnectionThread mConnectionThread;

    private class ConnectionThread implements Runnable {

        //jing 20160818 解决快速post引起的，只保留最后一组返回值的bug。。。
        class ConnectionInfoClass {
            public String mac;
            public String type;
            public int status;
            public int errorID;
            public Map manufactorData;
        }

        //缓存post的信息，等待run执行时获取
        private Queue<ConnectionInfoClass> connectionResultQueue = new LinkedList<>();


        public ConnectionThread() {
        }

        public void setConnectionMessage(String mac, String type, int status, int errorID, Map manufactorData) {
            ConnectionInfoClass connectionInfoClass = new ConnectionInfoClass();
            connectionInfoClass.mac = mac;
            connectionInfoClass.type = type;
            connectionInfoClass.status = status;
            connectionInfoClass.errorID = errorID;
            connectionInfoClass.manufactorData = manufactorData;
            connectionResultQueue.offer(connectionInfoClass);

            // 当链接断开之后 移除对应Mac地址的  缓存队列
            if (status == DEVICE_STATE_DISCONNECTED) {
                // 获取对应control
                CommandCacheControl commandCacheControl = commandCacheControlMap.get(mac);
                if (commandCacheControl != null) {
                    commandCacheControl.commandClearCache();
                    commandCacheControlMap.remove(mac);
                }
            }
        }

        @Override
        public void run() {
            boolean hasCallbackFlag = false;
            ConnectionInfoClass connectionInfoClass = connectionResultQueue.poll();
            if (connectionInfoClass != null) {
                Log.p(TAG, Log.Level.INFO, "onDeviceConnectionStateChange", connectionInfoClass.mac, connectionInfoClass.type,
                        connectionInfoClass.status, connectionInfoClass.errorID);
                List<iHealthDevicesCallback> list = mIHealthDeviceClientMap.getCallbacklist(connectionInfoClass.mac, connectionInfoClass.type);
                if (list.size() > 0) {
                    Iterator<iHealthDevicesCallback> i = list.iterator();
                    while (i.hasNext()) {
                        hasCallbackFlag = true;
                        iHealthDevicesCallback m = i.next();
                        m.onDeviceConnectionStateChange(connectionInfoClass.mac, connectionInfoClass.type, connectionInfoClass.status, connectionInfoClass.errorID);
                        m.onDeviceConnectionStateChange(connectionInfoClass.mac, connectionInfoClass.type, connectionInfoClass.status, connectionInfoClass.errorID, connectionInfoClass.manufactorData);
                    }
                } else {
                    //jing 20160728 无callback时，对连接成功的设备，断开连接
                    disconnenctAllConnectDevice();
                }
                if (hasCallbackFlag == false) {
                    Log.w(TAG, "ConnectionThread -- Invalid callback");
                }
            }
        }
    }

    /**
     * @hide
     */
    private class ScanDevice {

        public static final int LINK_BT = 201;
        public static final int LINK_BLE = 202;
        public static final int LINK_WIFI = 203;
        public static final int LINK_USB = 204;
        public static final int LINK_AU = 205;

        private int mlinkType = 0;
        private BluetoothDevice mDevice = null;
        private UsbDevice mUsbDevice = null;
        private String mDeviceType = null;
        private String mDeviceMac = null;
        private Hs5Control mHs5Control;

        public ScanDevice(int mlinkType, BluetoothDevice bluetoothDevice, Hs5Control hs5Control, UsbDevice usbDevice,
                          String mac, String type) {
            this.mlinkType = mlinkType;
            this.mDevice = bluetoothDevice;
            this.mDeviceMac = mac;
            this.mDeviceType = type;
            this.mHs5Control = hs5Control;
            this.mUsbDevice = usbDevice;
        }

        public int getlinkType() {
            return mlinkType;
        }

        public BluetoothDevice getDevice() {
            return mDevice;
        }

        public String getDeviceType() {
            return mDeviceType;
        }

        public String getDeviceMac() {
            return mDeviceMac;
        }

        public Hs5Control getHs5control() {
            return mHs5Control;
        }

        public UsbDevice getUsbDevice() {
            return mUsbDevice;
        }

    }

    /* USB Connection Manager */
    private static final int USBDEVICE_FTDI = 0x01;
    private static final int USBDEVICE_PL2303 = 0X02;
    private int mState = 0;

    private UsbManager manager;

    private UsbDevice getLinkedUsbDevice() {
        manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> dmMap = manager.getDeviceList();
        Iterator<Entry<String, UsbDevice>> iter = dmMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, UsbDevice> entry = (Entry<String, UsbDevice>) iter.next();
            int vendorId = entry.getValue().getVendorId();
            int productId = entry.getValue().getProductId();
            if (vendorId == 1118 && productId == 688) {
                mState = USBDEVICE_FTDI;
                return entry.getValue();
            } else if (vendorId == 1027 && productId == 24577) {
                mState = USBDEVICE_FTDI;
                return entry.getValue();
            } else if (vendorId == 1027 && productId == 24596) {
                mState = USBDEVICE_FTDI;
                return entry.getValue();
            } else if (vendorId == 1027 && productId == 24592) {
                mState = USBDEVICE_FTDI;
                return entry.getValue();
            } else if (vendorId == 1027 && productId == 24593) {
                mState = USBDEVICE_FTDI;
                return entry.getValue();
            } else if (vendorId == 1027 && productId == 24597) {
                mState = USBDEVICE_FTDI;
                return entry.getValue();
            } else if (vendorId == 1412 && productId == 45088) {
                mState = USBDEVICE_FTDI;
                return entry.getValue();
            } else if (vendorId == 1659 && productId == 8963) {
                mState = USBDEVICE_PL2303;
                return entry.getValue();
            } else if (vendorId == 1659 && productId == 45088) {
                mState = USBDEVICE_PL2303;
                return entry.getValue();
            }
        }
        return null;
    }

    private int baudRate = 57600; /* baud rate */
    private byte stopBit = 1; /* 1:1stop bits, 2:2 stop bits */
    private byte dataBit = 8; /* 8:8bit, 7: 7bit */
    private byte parity = 0; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
    private byte flowControl = 0; /* 0:none, 1: flow control(CTS,RTS) */
    // private int portNumber = 1; /*port number*/
    private int DevCount;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private PendingIntent mPermissionIntent;

    private void connectUsbDevice(UsbDevice device) {
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        if (device != null) {
            if (manager.hasPermission(device)) {
                connectUsbDevice();
            } else {
                manager.requestPermission(device, mPermissionIntent);
            }
        }
    }

    private void connectUsbDevice() {
        if (USBDEVICE_FTDI == mState) {
            Toast.makeText(mContext, "USBDEVICE_FTDI == mState", Toast.LENGTH_LONG).show();
            connectUsbUart();
        } else if (USBDEVICE_PL2303 == mState) {
            Toast.makeText(mContext, "USBDEVICE_PL2303 == mState)", Toast.LENGTH_LONG).show();
            connectUsb2303();
        }
    }

    private FtdiUsb uartInterface;

    private void connectUsbUart() {
        uartInterface = new FtdiUsb(mContext);
        DevCount = 0;
        DevCount = uartInterface.createDeviceList();
        if (DevCount > 0) {
            uartInterface.connectFunction();
            uartInterface.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
            startConnectUSB(uartInterface);
        }
    }

    private Pl2303Usb mPL2303Interface;

    private void connectUsb2303() {
        mPL2303Interface = new Pl2303Usb(mContext);
        mPL2303Interface.initSerialPort();
        SystemClock.sleep(500);
        int res = mPL2303Interface.setSerialPort();
        if (res < 0) {
            return;
        }
        mPL2303Interface.openUsbSerial();
        mPL2303Interface.readUsbSerialThread();
        startConnectUSB(mPL2303Interface);
    }

    private void startConnectUSB(BaseComm mUsbComm) {
        mBp3mControl = new Bp3mControl(mContext, mUsbComm, mUserName, "000000000000", "BP3M", mInsCallback, mBaseCommCallback);
        mBp3mControl.init();
    }
}
