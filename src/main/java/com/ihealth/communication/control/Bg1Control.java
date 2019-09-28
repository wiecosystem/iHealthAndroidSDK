package com.ihealth.communication.control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;

import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.utils.Log;

import com.ihealth.androidbg.audio.AudioTrackManager;
import com.ihealth.androidbg.audio.BG1.BG1_Command_Interface;
import com.ihealth.androidbg.audio.BG1.CommSound;
import com.ihealth.androidbg.audio.BG1.CommSound_new;
import com.ihealth.androidbg.audio.CrcCheck;
import com.ihealth.androidbg.audio.TunnerThread;
import com.ihealth.androidbg.audio.XXTEA;
import com.ihealth.communication.cloud.data.BG_InAuthor;
import com.ihealth.communication.cloud.data.DataBaseConstants;
import com.ihealth.communication.cloud.data.DataBaseTools;
import com.ihealth.communication.cloud.data.Data_BG_Result;
import com.ihealth.communication.cloud.data.Make_Data_Util;
import com.ihealth.communication.cloud.tools.AppIDFactory;
import com.ihealth.communication.cloud.tools.Method;
import com.ihealth.communication.ins.IdentifyIns;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Public API for the BG1
 * <p> The class provides methods to control BG1 device.
 * You need to call the device method, and then call the connection method
 * <p> If you want to use a BG1 device, you need to call{@link Bg1Control#connect()} to connect
 * the device when you get the OS Action {@link Intent#ACTION_HEADSET_PLUG} (headset in) firstly,
 * and then call{@link Bg1Control#sendCode(String)}to send code to the device,
 * after that you can take measurement with the device.
 */
public class Bg1Control implements BG1_Command_Interface {

    private static final String TAG = Bg1Control.class.getSimpleName();
    private static final String ctlCode = "02323c64323c01006400fa00e103016800f000f0" +
            "015e025814012c0000a0002800a003d100320046" +
            "005a006e0082009600aa00b400e601040118012c" +
            "01400168017c0190064d05f905a8055c051304cf" +
            "048f047103e803a203790353033202fc02e702d7" +
            "1027383d4e6f646464646464646464640319010b" +
            "0701";
    //For BG1 1303 1304 1305 1307 BG5  BG5L
    private static final String stripCode = "02323C64323C01006400FA00E1030168003C003C" +
            "01F4025814015E3200A0005A00A0032000320046" +
            "005A006E0082009600AA00B400E6010401180140" +
            "0168017C0190019A04C604A8048B04700456043E" +
            "0428041D03E803E803E803B303A3039D03990398" +
            "10273D464E6F646464646464646464640319010B" +
            "0701";
    //ONECODE
    private static final String oneCodeBG1Str = "02323C46323C01006400FA00E1030168003C003C" +
            "01F4025814015E3200A0002800A0032000320046" +
            "005A006E0082009600AA00B400E6010401180140" +
            "0168017C0190019A04CA04B10497047D04630449" +
            "0430042303E203BB03A2036E033A0321030702FA" +
            "10273D464E6F271A4557ED14194760FF0319010B" +
            "0701";
    //——————————————————————————————————————————
    /**
     * constant parameters
     */
    private static final long OneCodeBottleID = (long) 0xFF << 24 | 0xFF << 16 | 0xFF << 8 | 0xFF;
    private static final int filter1304 = 0x00FF1304;
    private static final int filter1305 = 0x00FF1305;
    private static final int filter1307 = 0x00FF1307;
    private static final IdentifyIns mIdentifyIns = new IdentifyIns();
    //——————————————————————————————————————————
    private CommSound myCommSound = null;
    private CommSound_new myCommSound_new = null;
    private TunnerThread tunner = null;
    private Context context = null;
    private String userName = "";
    private byte[] allCodeBuf = null;
    private String DeviceId1304 = "";
    private String DeviceId1305 = "";
    private long UpbottleId = 0;
    private Thread mConnect1305Thread = null;
    private Thread mConnect1304Thread = null;
    private Timer BG1Timer = null;
    private TimerTask BG1TimerTask = null;
    private Timer timer1305 = null;
    private TimerTask timerTask1305 = null;
    private AudioManager myAudio = null;
    private boolean isAG680 = false;
    private boolean isNeedProcessFAC = false;//1305的校验和出错问题
    private int deviceModel = 1304;
    private int currentDevice = 1305;//1305:1305+; 1304:1304
    private boolean running1305 = false;//避免握手信号接收较晚，产生的两个流程交叉
    private int currentVolumeIndex = 5;//当前耳机音量
    private boolean isError4 = false;
    private boolean isError = false;
    private boolean isIdent = false;
    private boolean isUnident10 = false;
    private boolean isRemember = false;
    private boolean isShow_UI = false;
    private Runnable connect1305_Runnable = new Runnable() {

        @Override
        public void run() {
            setMaxVolume();
            connect1305Device();
            resetVolume();
        }
    };
    private boolean isReadyforIden = false;
    private int commandID;
    private Runnable connect1304_Runnable = new Runnable() {
        @Override
        public void run() {
            setMaxVolume();
            connect1304Device();
            resetVolume();
        }
    };

    /**
     * Get the controller object.
     */
    public static Bg1Control getInstance() {
        return SingletonHolder.INSTANCE;
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
     * @param hex
     * @return
     * @hide
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
     * @hide
     */
    private static byte toByte(char cr) {
        byte b = (byte) "0123456789ABCDEF".indexOf(cr);
        return b;
    }

    /**
     * Parse bottle Info of the QRCode
     *
     * @param QRCode the QRCode of scan
     * @return jsonString like {"bottleInfo":[{"bottleId":"18882266","overDate":"2015-06-26","stripNum":"25"}]}
     */
    public static String getBottleInfoFromQR(String QRCode) {
        Log.p(TAG, Log.Level.INFO, "getBottleInfoFromQR", QRCode);

        if (PublicMethod.isOneCode(QRCode)) {
            String[] bottleInfo = new String[3];
            bottleInfo[0] = "" + OneCodeBottleID;
            bottleInfo[1] = "2099-12-16";
            bottleInfo[2] = "" + 255;
            String arrName[] = new String[]{"bottleId", "overDate", "stripNum"};
            String jsonData = changeStringToJson("bottleInfo", bottleInfo, arrName);
            return jsonData;
        }

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
    }

    /**
     * 判断系统音效设置
     */
    private static boolean isDolbyOn() {
        try {
            Class getSysProp = null;
            getSysProp = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method method = null;
            method = getSysProp.getDeclaredMethod("get", String.class);

            String prop = null;
            prop = (String) method.invoke(null, "dolby.ds.state");
            Log.v(TAG, "dolby.ds.state " + prop);
            if (prop.contains("on")) {
                return true;
            }
        } catch (Exception e) {
            Log.v(TAG, "dolby.ds.state " + e.toString());
            return false;
        }
        return false;
    }

    /**
     * Init the controller.
     *
     * @param context  Context.
     * @param userName UserName
     * @param filter   0
     */
    public void init(Context context, String userName, int filter) {
        Log.p(TAG, Log.Level.INFO, "init", userName, filter);

        this.isShow_UI = false;
        this.context = context;
        this.userName = userName;
        this.myAudio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (filter == filter1304) {
            this.deviceModel = 1304;
        } else if (filter == filter1305) {
            this.deviceModel = 1305;
        } else if (filter == filter1307) {
            this.deviceModel = 1307;
        }
        if (isDolbyOn()) {
            Intent intentCodeErr = new Intent(Bg1Profile.ACTION_BG1_MEASURE_ERROR);
            intentCodeErr.putExtra(Bg1Profile.BG1_MEASURE_ERROR, 401);
            //jing 20160731
            intentCodeErr.setPackage(context.getPackageName());
            context.sendBroadcast(intentCodeErr);
        }
    }

    /**
     * Init the controller.
     *
     * @param context  Context.
     * @param userName UserName
     * @param filter   0
     * @param showUI   Whether show a toast when modify system volume.
     *                 <p>
     *                 If true,some device may show a system toast to let the user know
     *                 "Listening at high volume for long periods(Continuing to increase the volume)
     *                 may damage your hearing." and then let user choose "OK" to
     *                 have a better experience with iHealth Align.
     *                 <p>
     *                 value true is recommended to have a better iHealth Align compatibility.
     */
    public void init(Context context, String userName, int filter, boolean showUI) {
        Log.p(TAG, Log.Level.INFO, "init", userName, filter, showUI);

        this.isShow_UI = showUI;
        this.context = context;
        this.userName = userName;
        this.myAudio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (filter == filter1304) {
            this.deviceModel = 1304;
        } else if (filter == filter1305) {
            this.deviceModel = 1305;
        } else if (filter == filter1307) {
            this.deviceModel = 1307;
        }
        if (isDolbyOn()) {
            Intent intentCodeErr = new Intent(Bg1Profile.ACTION_BG1_MEASURE_ERROR);
            intentCodeErr.putExtra(Bg1Profile.BG1_MEASURE_ERROR, 401);
            //jing 20160731
            intentCodeErr.setPackage(context.getPackageName());
            context.sendBroadcast(intentCodeErr);
        }
    }

    /**
     * Connect bg1 device
     * <ul>
     * <li>The action of the callback is {@link Bg1Profile#ACTION_BG1_CONNECT_RESULT}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     */
    public void connect() {
        this.currentVolumeIndex = this.myAudio.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.v(TAG, "volume    ----------> " + this.myAudio.getStreamVolume(AudioManager.STREAM_MUSIC));
        Log.v(TAG, "volume MAX----------> " + this.myAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

        // support communication
        start();
        startTimer1305();//10s time out
        this.running1305 = true;
        if (this.deviceModel == 1304) {
            startBG1Timer();
        }
    }

    /**
     * 1305
     * connect
     * 32 连接超时
     */
    private void startTimer1305() {
        this.timer1305 = new Timer();
        this.timerTask1305 = new TimerTask() {
            @Override
            public void run() {
                Log.e(TAG, "connection failed :: flag 32");
                Intent intent = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
                intent.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 32);
                //jing 20160731
                intent.setPackage(context.getPackageName());
                context.sendBroadcast(intent);
            }
        };
        this.timer1305.schedule(this.timerTask1305, 10000);
    }

    private void closeTimer1305() {
        if (null != this.timer1305) {
            this.timer1305.purge();
            this.timer1305.cancel();
            this.timer1305 = null;
        }
        if (null != this.timerTask1305) {
            this.timerTask1305.cancel();
            this.timerTask1305 = null;
        }
    }

    private void startBG1Timer() {
        this.BG1Timer = new Timer();
        this.BG1TimerTask = new TimerTask() {
            @Override
            public void run() {
                closeTimer1305();
                if (mConnect1304Thread == null) {
                    mConnect1304Thread = new Thread(connect1304_Runnable);
                    mConnect1304Thread.start();
                }
            }
        };
        this.BG1Timer.schedule(this.BG1TimerTask, 5000);
    }

    private void closeBG1Timer() {
        closeTimer1305();
        if (null != this.BG1Timer) {
            this.BG1Timer.purge();
            this.BG1Timer.cancel();
            this.BG1Timer = null;
        }
        if (null != this.BG1TimerTask) {
            this.BG1TimerTask.cancel();
            this.BG1TimerTask = null;
        }
    }


    /**
     * Send code to bg1 device
     * <ul>
     * <li>The action of the callback is {@link Bg1Profile#ACTION_BG1_SENDCODE_RESULT}, the keys of message will show in the KeyList of action.
     * </li>
     * </ul>
     *
     * @param QRCode
     */
    public void sendCode(final String QRCode) {
        Log.p(TAG, Log.Level.INFO, "sendCode", QRCode);

        new Thread() {
            public void run() {
                if (!isValidCode(QRCode)) {
                    Intent intentCodeErr = new Intent(Bg1Profile.ACTION_BG1_MEASURE_ERROR);
                    intentCodeErr.putExtra(Bg1Profile.BG1_MEASURE_ERROR, 7);
                    //jing 20160731
                    intentCodeErr.setPackage(context.getPackageName());
                    context.sendBroadcast(intentCodeErr);
                    return;
                }

                setMaxVolume();
                if (currentDevice == 1305) {
                    preSendQRCode(QRCode);
                } else if (currentDevice == 1304) {
                    if (isError4) {
                        SystemClock.sleep(1500L);
                        AudioTrackManager.inCommunication = true;
                        sendCode(QRCode, false);
                    } else {
                        sendCode(QRCode, isRemember);
                    }

                }
                resetVolume();
            }
        }.start();
    }

    synchronized private boolean isValidCode(String QRCode) {
        if (PublicMethod.isOneCode(QRCode)) {
            Log.v(TAG, "Code type :: OneCode ------> ");
            return true;
        }
        if (PublicMethod.isCtlCode(QRCode)) {
            Log.v(TAG, "Code type :: CtlCode ------> ");
            return true;
        }
        if (QRCode == null || QRCode.length() != 60) {
            Log.e(TAG, "Code type :: Error Code ------> " + QRCode);
            return false;
        }
        byte[] qrcodes = hexStringToByte(QRCode);
        if (qrcodes.length == 30) {
            CrcCheck ccQR = new CrcCheck(hexByteToInt(qrcodes, 28));
            int chechsumFAC = ccQR.getCRCValue();
            if (qrcodes[28] == (byte) ((chechsumFAC & 0Xff00) >> 8)
                    && qrcodes[29] == (byte) (chechsumFAC & 0X00ff)) {
                Log.v(TAG, "Code type :: Normal QRCode ------> ");
                return true;
            }
        }
        Log.e(TAG, "Code type :: Error Code ------> " + QRCode);
        return false;
    }

    /**
     * @return
     * @hide
     */
    private String getAppID() {
        String appID = "appID";
        AppIDFactory appIDFactory = new AppIDFactory(context);
        appID = appIDFactory.getAppID();
        return appID.substring(appID.length() - 8, appID.length());
    }

    /**
     * @return
     * @hide
     */
    private String getAppID_new() {
        String appID = "appID";
        AppIDFactory appIDFactory = new AppIDFactory(context);
        appID = appIDFactory.getAppID();
        return appID.substring(0, 2);
    }

    /**
     * Method to connect bg1 device
     * <p> return identify result to notify what to do next
     * <p> If communication error,return corresponding error num
     * <p>
     * 1304连接分为两步，首先认证下位机，之后发送Code；
     * 根据认证结果 1：发简包   2：发全包
     * 分步处理，为了通信流程，分步操作，先认证下位机再判断试条数是否为0、Code是否过期等
     * <p/>
     * 第一步,connect
     * <p>
     * 0 成功
     * 1 握手失败
     * 2 获取DeviceId失败  Setmode
     * 3 下发APPID失败
     * 4 获取Idps信息失败
     * 5 认证失败
     * <p/>
     * 第二步,sendCode
     * <p>
     * 6 发送Code 2-1失败
     * 7 发送Code 2-2失败
     * 8 发全包第6-1包错误
     * 9 发全包第6-2包错误
     * 10 发全包第6-3包错误
     * 11 发全包第6-4包错误
     * 12 发全包第6-5包错误
     * 13 发全包第6-6包错误
     */
    private void connect1304Device() {
        start_old();
        isError4 = false;
        running1305 = false;
        isNeedProcessFAC = false;
        if (handShake()) {
            String deviceId = setMode(2);
            DeviceId1304 = deviceId;
            if (deviceId != "") {
                String isRem = sendAPPId(getAppID());
                Log.v(TAG, "isRem " + isRem);
                if (isRem.equals("1")) {// 简易
                    isRemember = true;
                    currentDevice = 1304;
                    Intent intent1 = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
                    intent1.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 0);
                    //jing 20160731
                    intent1.setPackage(context.getPackageName());
                    context.sendBroadcast(intent1);
                } else if (isRem.equals("2")) {// 完整
                    isRemember = false;
                    if (identification()) {
                        currentDevice = 1304;
                        Intent intent1iden = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
                        intent1iden.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 0);
                        //jing 20160731
                        intent1iden.setPackage(context.getPackageName());
                        context.sendBroadcast(intent1iden);
                    } else {
                        Log.e(TAG, "connection failed :: flag 5");
                        Intent intent2 = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
                        intent2.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 5);
                        //jing 20160731
                        intent2.setPackage(context.getPackageName());
                        context.sendBroadcast(intent2);
                    }
                } else {
                    Log.e(TAG, "connection failed :: flag 3");
                    Intent intent4 = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
                    intent4.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 3);
                    //jing 20160731
                    intent4.setPackage(context.getPackageName());
                    context.sendBroadcast(intent4);
                }
            } else {
                Log.e(TAG, "connection failed :: flag 2");
                Intent intent5 = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
                intent5.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 2);
                //jing 20160731
                intent5.setPackage(context.getPackageName());
                context.sendBroadcast(intent5);
            }
        } else {
            Log.e(TAG, "connection failed :: flag 1");

            Intent intent6 = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
            intent6.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 1);
            //jing 20160731
            intent6.setPackage(context.getPackageName());
            context.sendBroadcast(intent6);
        }

    }

    /**
     * Method to connect new bg1 device.
     * If communication error,return corresponding error num
     * <p>
     * 1305连接过程
     * 接收到握手信号，下发APPID
     * 接收idps信息（认证通过与否）决定通信流程
     * 发Code流程
     * <p>
     * 第一步,connect
     * <p>
     * 0 成功
     * 16 下发APPID失败，即获取idps失败
     * <p>
     * 第二步,sendCode
     * <p>
     * 0 成功
     * 17 认证失败且10次未认证
     * 18 发bottleId失败
     * 19 发简包第3-1包错误
     * 20 发全包第3-2包错误
     * 21 发全包第3-3包错误
     * 22 发全包第7-1包错误
     * 23 发全包第7-2包错误
     * 24 发全包第7-3包错误
     * 25 发全包第7-4包错误
     * 26 发全包第7-5包错误
     * 27 发全包第7-6包错误
     * 28 发全包第7-7包错误
     */
    private void connect1305Device() {
        sendAPPId_new(getAppID_new());
    }

    /**
     * send code to new bg1 device
     *
     * @param QRCode
     */
    private void preSendQRCode(String QRCode) {
        Log.v(TAG, "send code");
        if (isError && !isError4) {
            Log.v(TAG, "error ！！！return ");
            return;//如果有错误，什么也不做，等待错误指令
        }
        long bottleId = 0;
        int version = 1;

        if (PublicMethod.isOneCode(QRCode)) {
            bottleId = OneCodeBottleID;
            version = 2;
        } else {
            bottleId = getBottleIdFromQRCode(QRCode);
            version = 1;
        }

        if (isUnident10 && !isError4) {
            if (identification_new()) {
                isIdent = true;
            } else {
                Log.v(TAG, "failed 10 !!!");
                Intent intent10 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent10.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 17);
                //jing 20160731
                intent10.setPackage(context.getPackageName());
                context.sendBroadcast(intent10);
                return;
            }
        }

        //int res = sendBottleIdAndVer(bottleId, version);

        if (isError4) {
            SystemClock.sleep(1500L);
            AudioTrackManager.inCommunication = true;
            Log.v(TAG, "error 4 send code directly ---> bottleId = " + bottleId + " " + "version = " + version);
            sendCode_new(QRCode, false);
        } else {
            Log.v(TAG, "normal bottleId = " + bottleId + " " + "version = " + version);
            AudioTrackManager.inCommunication = true;
            int res = sendBottleIdAndVer(bottleId, version);

            if (res == 1) {
                sendCode_new(QRCode, false);
            } else if (res == 2) {
                sendCode_new(QRCode, true);
            } else if (res == 3) {
                //不需要任何操作，等待下位机指令
                Intent intent10 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent10.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 0);
                //jing 20160731
                intent10.setPackage(context.getPackageName());
                context.sendBroadcast(intent10);
            } else {
                Log.e(TAG, "connection failed :: flag 18");
                Intent intent10 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent10.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 18);
                //jing 20160731
                intent10.setPackage(context.getPackageName());
                context.sendBroadcast(intent10);
            }
        }
        //以备再次开始握手
        if (this.mConnect1305Thread != null) {
            this.mConnect1305Thread.interrupt();
            this.mConnect1305Thread = null;
        }
    }

    /**
     * hand shake
     *
     * @return
     * @hide
     */
    private boolean handShake() {
        Log.v(TAG, "handshake----");
        byte[] result = null;
        if (null != this.myCommSound) {
            result = this.myCommSound.sendCommand(0);
        }
        if (result == null) {
            return false;
        } else {
            Log.v(TAG, "handshake success----");
            return true;
        }
    }

    /**
     * set device mode
     *
     * @param mode 1，factory；2，product
     * @return deviceId
     * @hide
     */
    private String setMode(int mode) {
        Log.v(TAG, "mode 2-- deviceId");
        byte[] command = new byte[]{(byte) 0xA2, (byte) 0x4F, (byte) mode, (byte) 0x00, (byte) 0x00};
        byte[] result = null;
        String resultStr = "";
        if (null != myCommSound) {
            result = myCommSound.sendCommand((byte) 0xA0, command, 1);
        }
        if (result == null) {
            resultStr = "";
        } else {
            // 解析设备ID
            resultStr = getDeviceIdByOrder(result);
        }
        return resultStr;
    }

    /**
     * get device id from 1304BG1
     *
     * @param order
     * @return
     * @hide
     */
    private String getDeviceIdByOrder(byte[] order) {
        if (order.length > 2) {
            byte[] deviceId1 = new byte[order.length - 6];
            byte[] deviceId2 = new byte[4];
            for (int i = 0; i < order.length - 6; i++) {
                deviceId1[i] = order[i + 1];
            }
            for (int j = 0; j < 4; j++) {
                deviceId2[j] = order[j + 12];
            }

            return ((new String(deviceId1)) + Bytes2HexString(deviceId2, deviceId2.length));
        }
        return "";
    }

    /**
     * send APPID to 1304BG1
     *
     * @param APPId
     * @return 0:error;1:remember;2:not remember
     * @hide
     */
    private String sendAPPId(String APPId) {
        Log.v(TAG, "send AppID");
        byte[] command = new byte[10];
        command[0] = (byte) 0xA2;
        command[1] = (byte) 0x2D;
        byte[] app = APPId.getBytes();
        for (int i = 0; i < 8; i++) {
            command[i + 2] = app[i];
        }
        byte[] result = null;
        String resultStr = "0";
        if (null != myCommSound) {
            result = myCommSound.sendCommand((byte) 0xA0, command, 2);
        }
        if (result == null) {
            resultStr = "0";
        } else {
            //判断记住没
            if (result[0] == (byte) 0x2E) {//记住
                resultStr = "1";
            } else {//新的
                resultStr = "2";
            }
        }
        return resultStr;
    }

    /**
     * send APPID to 1305BG1
     *
     * @param APPId
     * @return
     * @hide
     */
    private String sendAPPId_new(String APPId) {
        Log.v(TAG, "send AppID new");
        String resultStr = "";
        byte[] command = new byte[3];
        command[0] = (byte) 0xD0;
        byte[] app = APPId.getBytes();
        for (int i = 0; i < 2; i++) {
            command[i + 1] = app[i];
        }
        byte[] result = null;
        if (null != myCommSound_new) {
            result = myCommSound_new.sendCommand((byte) 0x00, command, 2);
        } else {
            Log.v(TAG, "myCommSound_new == null");
        }

        if (result == null) {
            Log.e(TAG, "connection failed :: flag 16");
            resultStr = "";
            Intent intent8 = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
            intent8.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 16);
            //jing 20160731
            intent8.setPackage(context.getPackageName());
            context.sendBroadcast(intent8);
        } else {
            currentDevice = 1305;

            resultStr = getIDPSByOrder_new(result);
            Intent intent9 = new Intent(Bg1Profile.ACTION_BG1_IDPS);
            intent9.putExtra(Bg1Profile.BG1_IDPS, resultStr);
            //jing 20160731
            intent9.setPackage(context.getPackageName());
            context.sendBroadcast(intent9);

            Intent intent8 = new Intent(Bg1Profile.ACTION_BG1_CONNECT_RESULT);
            //jing 20160731
            intent8.setPackage(context.getPackageName());
            intent8.putExtra(Bg1Profile.BG1_CONNECT_RESULT, 0);
            context.sendBroadcast(intent8);

        }
        return resultStr;
    }

    /**
     * parse idps
     *
     * @param order
     * @return
     * @hide
     */
    private String getIDPSByOrder_new(byte[] order) {//with order ID byte[]
        Log.v(TAG, "order = " + Bytes2HexString(order, order.length));
        long BG1ID = (long) (order[1] & 0xff) * 256 * 256 * 256 * 256
                + (long) (order[2] & 0xff) * 256 * 256 * 256
                + (long) (order[3] & 0xff) * 256 * 256
                + (long) (order[4] & 0xff) * 256
                + (long) (order[5] & 0xff);

        //DA 00 01 D0 8E 53 AF 55 8E 37 83 78 00
        String FWNumber = "" + ((order[6] & 0xfc) >> 2) + "." + ((order[6] & 0x03) * 2 + ((order[7] & 0x80) >> 7)) + "." + ((order[7] & 0x70) >> 4);
        String HDNumber = "" + ((order[7] & 0x0F) * 4 + ((order[8] & 0xC0) >> 6)) + "." + ((order[8] & 0x38) >> 3) + "." + (order[8] & 0x07);
        isError = (order[order.length - 1] & 0x01) == 1 ? true : false;
        isIdent = (order[order.length - 1] & 0x02) == 2 ? true : false;
        isUnident10 = (order[order.length - 1] & 0x04) == 4 ? true : false;
        Log.v(TAG, "DeviceId=" + BG1ID + " FirmWare=" + FWNumber + " HardWare=" + HDNumber
                + " isError = " + isError + " isIdent = " + isIdent + " isUnident10 = " + isUnident10);
        if (FWNumber.equals("13.5.0") || FWNumber.equals("13.6.0")) {
            isNeedProcessFAC = true;
        } else {
            isNeedProcessFAC = false;
        }
        String[] BG1Info = new String[3];
        BG1Info[0] = BG1ID + "";
        BG1Info[1] = FWNumber;
        BG1Info[2] = HDNumber;
        String arrName[] = new String[]{"DeviceId", "FirmWare", "HardWare"};
        String idps = changeStringToJson("IDPS", BG1Info, arrName);
        return idps;
    }

    /**
     * identify 1304BG1
     *
     * @return
     * @hide
     */
    private boolean identification() {
        byte[] R1 = new byte[16];//随机数R1——用于认证发给下位机
        byte[] R1_stroke = new byte[16];//R1'——用于保存下位机传上来的R1'
        byte[] R1_back = new byte[16];//R1——对下位机传上来的R1'进行解密后生成的R1，用于比较随机数R1

        byte[] R2_stroke = new byte[16];//R2'——用于保存下位机传上来的R2'
        byte[] R2 = new byte[16];//R2——对R2'解密后生成的R2
        byte[] deviceID = new byte[16];//下位机传上来的产品ID
        //key用于加密
        byte[] key = mIdentifyIns.getKa("BG1304");
        // 生成R1,16字节 (正整数)
        new Random(System.currentTimeMillis()).nextBytes(R1);
        for (int i = 0; i < 16; i++) {
            if (R1[i] < 0) R1[i] = (byte) (0 - R1[i]);
        }
        byte[] command = new byte[18];
        command[0] = (byte) 0xA2;
        command[1] = (byte) 0xFA;
        for (int i = 0; i < R1.length; i++) {
            command[i + 2] = R1[i];
        }

        byte[] returnDataFB_22 = null;
        if (null != myCommSound)
            returnDataFB_22 = myCommSound.sendCommand((byte) 0xA0, command, 43);
        if (returnDataFB_22 == null) {
            Log.v(TAG, "－－－－－－identify first step error－－－－－－");
            return false;
        }
        byte[] returnDataFB_21 = null;
        if (null != myCommSound)
            returnDataFB_21 = myCommSound.sendCommand((byte) 0xA2, new byte[]{(byte) 0xA2, (byte) 0xFB}, 42);
        if (returnDataFB_21 == null) {
            Log.v(TAG, "－－－－－－identify second step error－－－－－－");
            return false;
        }
        byte[] returnDataFB_20 = null;
        if (null != myCommSound)
            returnDataFB_20 = myCommSound.sendCommand((byte) 0xA1, new byte[]{(byte) 0xA2, (byte) 0xFB}, 41);
        if (returnDataFB_20 == null) {
            Log.v(TAG, "－－－－－－identify third step error－－－－－－");
            return false;
        }
        //分析FB
        for (int i = 0; i < 16; i++) {
            deviceID[i] = returnDataFB_22[i + 1];//ID
            R1_stroke[i] = returnDataFB_21[i + 1];//R1'
            R2_stroke[i] = returnDataFB_20[i + 1];//R2'
        }
        Log.v(TAG, "-----------------------------------------");
        Log.v(TAG, "ID =" + Bytes2HexString(deviceID, 16));
        Log.v(TAG, "R1'=" + Bytes2HexString(R1_stroke, 16));
        Log.v(TAG, "R2'=" + Bytes2HexString(R2_stroke, 16));
        Log.v(TAG, "-----------------------------------------");
        // 以key对ID加密生成KEY'
        byte[] K = new byte[16];//对接收到的ID以key加密后生成新key，并以此对R1'和R2'进行加密
        K = XXTEA.encrypt(reverseByteArray(deviceID), key);
        R1_back = XXTEA.encrypt(reverseByteArray(R1_stroke), K);
        R1_back = reverseByteArray(R1_back);
        Log.v(TAG, "-----------------------------------------");
        Log.v(TAG, "R1     =" + Bytes2HexString(R1, 16));
        Log.v(TAG, "R1_back=" + Bytes2HexString((R1_back), 16));
        Log.v(TAG, "-----------------------------------------");
        //比较R1和R1_back
        if (Bytes2HexString((R1_back), 16).equals(Bytes2HexString(R1, 16))) {
            R2 = XXTEA.encrypt(reverseByteArray(R2_stroke), K);
            R2 = reverseByteArray(R2);
            command = new byte[18];
            command[0] = (byte) 0xA2;
            command[1] = (byte) 0xFC;
            for (int i = 0; i < R2.length; i++) {
                command[i + 2] = R2[i];
            }
            //发送
            byte[] returnDataFD = null;
            if (null != myCommSound)
                returnDataFD = myCommSound.sendCommand((byte) 0xA0, command, 40);
            if (returnDataFD == null) {
                return false;
            }
            if (returnDataFD[0] == (byte) 0xFD) {
                Log.v(TAG, "identify success");
                return true;
            } else {
                Log.v(TAG, "returnDataFD error");
            }
        } else {
            Log.v(TAG, "R1 do not match the R1 back～～");
        }
        return false;
    }

    /**
     * identify 1305BG1
     *
     * @return
     * @hide
     */
    private boolean identification_new() {
        byte[] R1 = new byte[8];//随机数R1——用于认证发给下位机
        byte[] R1_stroke = new byte[8];//R1'——用于保存下位机传上来的R1'
        byte[] R1_back = new byte[8];//R1——对下位机传上来的R1'进行解密后生成的R1，用于比较随机数R1

        byte[] R2_stroke = new byte[8];//R2'——用于保存下位机传上来的R2'
        byte[] R2 = new byte[8];//R2——对R2'解密后生成的R2
        byte[] deviceID = new byte[8];//下位机传上来的产品ID（8位，前面8位补零）
        //key用于加密
        byte[] key = new byte[16];
        if (isAG680) {
            key = mIdentifyIns.getKa("BGAG680");
        } else {
            key = mIdentifyIns.getKa("BG1305");
        }
        // 生成R1,16字节 (正整数)
        new Random(System.currentTimeMillis()).nextBytes(R1);
        for (int i = 0; i < 8; i++) {
            if (R1[i] < 0) R1[i] = (byte) (0 - R1[i]);
        }
        byte[] command = new byte[9];
        command[0] = (byte) 0xFA;
        for (int i = 0; i < R1.length; i++) {
            command[i + 1] = R1[i];
        }
        byte[] returnDataFB_11 = null;
        if (null != myCommSound_new) {
            returnDataFB_11 = myCommSound_new.sendCommand((byte) 0x00, command, 42);
        }
        if (returnDataFB_11 == null) {
            return false;
        }
        Log.v(TAG, "returnDataFB_11 =" + Bytes2HexString(returnDataFB_11, returnDataFB_11.length));
        byte[] returnDataFB_10 = null;
        if (null != myCommSound_new) {
            returnDataFB_10 = myCommSound_new.sendCommand((byte) 0xA1, new byte[]{(byte) 0xFB}, 41);
        }
        if (returnDataFB_10 == null) {
            return false;
        }
        Log.v(TAG, "returnDataFB_10 =" + Bytes2HexString(returnDataFB_10, returnDataFB_10.length));

        //分析FB
        for (int i = 0; i < 8; i++) {
            deviceID[i] = returnDataFB_11[i + 1];//ID
            if (i < 6) {
                R1_stroke[i] = returnDataFB_11[i + 8 + 1];//R1'
            } else {
                R1_stroke[i] = returnDataFB_10[i - 6];//R1'
            }

            R2_stroke[i] = returnDataFB_10[i + 2];//R2'
        }
        Log.v(TAG, "-----------------------------------------");
        Log.v(TAG, "ID =" + Bytes2HexString(deviceID, 8));
        Log.v(TAG, "R1'=" + Bytes2HexString(R1_stroke, 8));
        Log.v(TAG, "R2'=" + Bytes2HexString(R2_stroke, 8));
        Log.v(TAG, "-----------------------------------------");
        // 以key对ID加密生成KEY'
        byte[] K = new byte[8];//对接收到的ID以key加密后生成新key，并以此对R1'和R2'进行加密
        K = XXTEA.encrypt(reverseByteArray_new(deviceID), key);
        byte[] K_new = new byte[16];
        for (int i = 0; i < 8; i++) {
            K_new[i] = K[i];
        }
        R1_back = XXTEA.encrypt(reverseByteArray_new(R1_stroke), K_new);
        R1_back = reverseByteArray_new(R1_back);
        Log.v(TAG, "-----------------------------------------");
        Log.v(TAG, "R1     =" + Bytes2HexString(R1, 8));
        Log.v(TAG, "R1_back=" + Bytes2HexString((R1_back), 8));
        Log.v(TAG, "-----------------------------------------");
        //比较R1和R1_back
        if (Bytes2HexString((R1_back), 8).equals(Bytes2HexString(R1, 8))) {
            R2 = XXTEA.encrypt(reverseByteArray_new(R2_stroke), K_new);
            Log.v(TAG, "-----------------------------------------");
            Log.v(TAG, "R2        =" + Bytes2HexString(R2, 8));
            //翻转R2
            R2 = reverseByteArray_new(R2);
            Log.v(TAG, "R2_reverse=" + Bytes2HexString(R2, 8));
            Log.v(TAG, "-----------------------------------------");
            //数据-1
            command = new byte[9];
            command[0] = (byte) 0xFC;
            for (int i = 0; i < R2.length; i++) {
                command[i + 1] = R2[i];
            }
            //发送
            byte[] returnDataFD = null;
            if (null != myCommSound_new) {
                returnDataFD = myCommSound_new.sendCommand((byte) 0xA0, command, 40);
            }
            if (returnDataFD == null) {
                return false;
            }
            if (returnDataFD[0] == (byte) 0xFD) {
                Log.v(TAG, "identify success");
                return true;
            } else {
                Log.v(TAG, "returnDataFD error");
            }
        } else {
            Log.v(TAG, "R1 do not match the R1 back～～");
        }
        return false;
    }

    /**
     * @param data
     * @return
     * @hide
     */
    private byte[] reverseByteArray(byte[] data) {
        byte[] result = new byte[16];
        for (int i = 0; i < 4; i++) {
            result[i] = data[3 - i];
            result[i + 4] = data[7 - i];
            result[i + 8] = data[11 - i];
            result[i + 12] = data[15 - i];
        }
        return result;
    }

    /**
     * @param data
     * @return
     * @hide
     */
    private byte[] reverseByteArray_new(byte[] data) {
        byte[] result = new byte[8];
        for (int i = 0; i < 4; i++) {
            result[i] = data[3 - i];
            result[i + 4] = data[7 - i];
            //            result[i + 8] = data[11 - i];
            //            result[i + 12] = data[15 - i];
        }
        return result;
    }

    /**
     * @param qr
     * @hide
     */
    private void compileQR(String qr) {

        allCodeBuf = new byte[126];

        if (PublicMethod.isOneCode(qr)) {
            byte[] temp = hexStringToByte(oneCodeBG1Str);
            System.arraycopy(temp, 0, allCodeBuf, 0, 122);
            CrcCheck cc = new CrcCheck(hexByteToInt(allCodeBuf, 122));
            int chechsum = cc.getCRCValue();

            allCodeBuf[122] = (byte) ((chechsum & 0Xff00) >> 8);
            allCodeBuf[123] = (byte) (chechsum & 0X00ff);

            allCodeBuf[124] = (byte) 0x00;
            allCodeBuf[125] = (byte) 0x01;

        } else if (PublicMethod.isCtlCode(qr)) {//下发全包还是简包？ 20160914
            byte[] temp = hexStringToByte(ctlCode);
            System.arraycopy(temp, 0, allCodeBuf, 0, 122);
            CrcCheck cc = new CrcCheck(hexByteToInt(allCodeBuf, 122));
            int chechsum = cc.getCRCValue();

            allCodeBuf[122] = (byte) ((chechsum & 0Xff00) >> 8);
            allCodeBuf[123] = (byte) (chechsum & 0X00ff);

            allCodeBuf[124] = (byte) 0x00;
            allCodeBuf[125] = (byte) 0x01;
        } else {
            byte[] someCodeBuf = hexStringToByte(qr);
            byte[] temp = hexStringToByte(stripCode);
            allCodeBuf[0] = someCodeBuf[0];
            allCodeBuf[1] = someCodeBuf[1];
            allCodeBuf[2] = someCodeBuf[2];
            allCodeBuf[3] = someCodeBuf[3];
            allCodeBuf[4] = someCodeBuf[4];
            allCodeBuf[5] = someCodeBuf[5];

            allCodeBuf[6] = (byte) 0x01;

            int sampleTime = (int) ((someCodeBuf[6] & 0xFF) * 0.1 * 1000 / 20);
            allCodeBuf[7] = (byte) ((sampleTime & 0xFF00) >> 8);
            allCodeBuf[8] = (byte) (sampleTime & 0x00FF);
            sampleTime = (int) ((someCodeBuf[7] & 0xFF) * 0.1 * 1000 / 20);
            allCodeBuf[9] = (byte) ((sampleTime & 0xFF00) >> 8);
            allCodeBuf[10] = (byte) (sampleTime & 0x00FF);
            sampleTime = (int) ((someCodeBuf[8] & 0xFF) * 0.1 * 1000 / 20);
            allCodeBuf[11] = (byte) ((sampleTime & 0xFF00) >> 8);
            allCodeBuf[12] = (byte) (sampleTime & 0x00FF);

            // 13-29
            for (int i = 13; i < 30; i++) {
                allCodeBuf[i] = temp[i];
            }
            sampleTime = (int) ((someCodeBuf[9] & 0xFF) * 0.1 * 1000 / 20);
            allCodeBuf[30] = (byte) ((sampleTime & 0xFF00) >> 8);
            allCodeBuf[31] = (byte) (sampleTime & 0x00FF);
            allCodeBuf[32] = someCodeBuf[10];
            allCodeBuf[33] = someCodeBuf[11];
            for (int i = 34; i < 106; i++) {
                allCodeBuf[i] = temp[i];
            }
            allCodeBuf[106] = someCodeBuf[12];
            allCodeBuf[107] = someCodeBuf[13];
            allCodeBuf[108] = someCodeBuf[14];
            allCodeBuf[109] = someCodeBuf[15];
            allCodeBuf[110] = someCodeBuf[16];
            allCodeBuf[111] = someCodeBuf[17];
            allCodeBuf[112] = someCodeBuf[18];
            allCodeBuf[113] = someCodeBuf[19];
            allCodeBuf[114] = someCodeBuf[20];
            allCodeBuf[115] = someCodeBuf[21];
            allCodeBuf[116] = temp[116];
            allCodeBuf[117] = 0x19;//下位机固定试条数

            // 试纸条数
            allCodeBuf[118] = someCodeBuf[23];
            //出场日期写死
            allCodeBuf[119] = (byte) 0x0B;
            allCodeBuf[120] = (byte) 0x07;
            allCodeBuf[121] = (byte) 0x01;
            CrcCheck cc = new CrcCheck(hexByteToInt(allCodeBuf, 122));
            int chechsum = cc.getCRCValue();

            allCodeBuf[122] = (byte) ((chechsum & 0Xff00) >> 8);
            allCodeBuf[123] = (byte) (chechsum & 0X00ff);

            allCodeBuf[124] = (byte) 0x00;
            allCodeBuf[125] = (byte) 0x01;

            /**
             * @param qr
             * @author zhaoyongguang
             * <p> modify date 20151120
             * <p> 针对1305发送Code，非首包数据第四位出现0xFA或者0xFC的情况进行处理，其他版本正常
             */
            Log.v(TAG, "Code process before = " + Bytes2HexString(allCodeBuf, allCodeBuf.length));
            if (isNeedProcessFAC) {
                if (allCodeBuf[123] == (byte) (0xFA & 0xFF) || allCodeBuf[123] == (byte) (0xFC & 0xFF)) {
                    //最终方案，对Code[19]最低位取反
                    if ((allCodeBuf[113] & 0X01) == 1) {
                        allCodeBuf[113] = (byte) (allCodeBuf[113] & 0xFE);
                        Log.v(TAG, "1->0");
                    } else {
                        allCodeBuf[113] = (byte) (allCodeBuf[113] | 0x01);
                        Log.v(TAG, "0->1");
                    }
                    CrcCheck ccFAC = new CrcCheck(hexByteToInt(allCodeBuf, 122));
                    int chechsumFAC = ccFAC.getCRCValue();

                    allCodeBuf[122] = (byte) ((chechsumFAC & 0Xff00) >> 8);
                    allCodeBuf[123] = (byte) (chechsumFAC & 0X00ff);
                }
            }
            Log.v(TAG, "Code process after = " + Bytes2HexString(allCodeBuf, allCodeBuf.length));
        }

    }

    /**
     * @param bt
     * @param len
     * @return
     * @hide
     */
    private int[] hexByteToInt(byte[] bt, int len) {
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
     * send bottleId and the version of ensure info
     *
     * @param bottleId
     * @param ver
     * @return
     * @hide
     */
    private int sendBottleIdAndVer(long bottleId, int ver) {
        Log.v(TAG, "send BottleId  new");
        int res = 0;
        byte[] command = new byte[6];
        command[0] = (byte) 0x53;
        command[1] = (byte) ((bottleId >> 24) & 0xFF);
        command[2] = (byte) ((bottleId >> 16) & 0xFF);
        command[3] = (byte) ((bottleId >> 8) & 0xFF);
        command[4] = (byte) (bottleId & 0xFF);
        command[5] = (byte) ver;
        byte[] result = null;
        if (null != myCommSound_new) {
            result = myCommSound_new.sendCommand((byte) 0x00, command, 3);
            if (result == null) {
                return 0;//没有返回
            }

            if (result[0] == (byte) 0x54) {
                Log.v(TAG, "different version of ensure info, send full-----------------> 1");
                res = 1;
            } else if (result[0] == (byte) 0x55) {
                Log.v(TAG, "different  bottle id, send short----------------------------> 2");
                res = 2;
            } else if (result[0] == (byte) 0x56) {
                Log.v(TAG, "all the same,not need to send--------------------------------> 3");
                res = 3;
            } else {
                if (AudioTrackManager.isR2017) {
                    Log.v(TAG, "receive error, send full-----------------> 1");
                    res = 1;
                }
            }
        }
        return res;
    }

    /**
     * send code to 1304BG1
     *
     * @param QRCode the QRCode of scan
     * @param isKnow whether connected before
     * @return
     */
    private boolean sendCode(String QRCode, boolean isKnow) {
        //解析二维码
        compileQR(QRCode);
        UpbottleId = getBottleIdFromQRCode(QRCode);

        if (isKnow) {//上下位机认识－－发简易code
            byte[] code_11 = new byte[]{//第2-1包
                    (byte) 0xa2,
                    (byte) 0x50,
                    allCodeBuf[1],
                    allCodeBuf[2],
                    allCodeBuf[3],
                    allCodeBuf[4],
                    allCodeBuf[5],
                    allCodeBuf[6],
                    allCodeBuf[7],
                    allCodeBuf[8],
                    allCodeBuf[9],
                    allCodeBuf[10],
                    allCodeBuf[11],
                    allCodeBuf[12],
                    allCodeBuf[28],
                    allCodeBuf[29],
                    allCodeBuf[30]
            };
            byte[] returnData_11 = null;
            if (null != myCommSound) {
                returnData_11 = myCommSound.sendCommand((byte) 0x11, code_11, 51);
            }
            if (returnData_11 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 6－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 6);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code sueccess 2-1－－－－－－");
            }
            byte[] code_10 = new byte[]{//第2-2包
                    (byte) 0xa2,
                    (byte) 0x50,
                    allCodeBuf[31],
                    allCodeBuf[32],
                    allCodeBuf[33],
                    allCodeBuf[106],
                    allCodeBuf[107],
                    allCodeBuf[108],
                    allCodeBuf[109],
                    allCodeBuf[110],
                    allCodeBuf[111],
                    allCodeBuf[112],
                    allCodeBuf[113],
                    allCodeBuf[114],
                    allCodeBuf[115],
                    allCodeBuf[122],
                    allCodeBuf[123]
            };
            byte[] returnData_10 = null;
            if (null != myCommSound) {
                returnData_10 = myCommSound.sendCommand((byte) 0x10, code_10, 50);
            }
            if (returnData_10 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 7－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 7);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 2-2－－－－－－");
                currentDevice = 1304;
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 0);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return true;
            }
        } else {//不认识－－发全部code
            byte[] code_55 = new byte[]{//第6-1包
                    (byte) 0xa2,
                    (byte) 0x25,
                    allCodeBuf[1],
                    allCodeBuf[2],
                    allCodeBuf[3],
                    allCodeBuf[4],
                    allCodeBuf[5],
                    allCodeBuf[6],
                    allCodeBuf[7],
                    allCodeBuf[8],
                    allCodeBuf[9],
                    allCodeBuf[10],
                    allCodeBuf[11],
                    allCodeBuf[12],
                    allCodeBuf[28],
                    allCodeBuf[29],
                    allCodeBuf[30]
            };
            byte[] returnData_55 = null;
            if (null != myCommSound) {
                returnData_55 = myCommSound.sendCommand((byte) 0x55, code_55, 65);
            }
            if (returnData_55 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 8－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 8);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 6-1－－－－－－");
            }
            byte[] code_54 = new byte[]{//第6-2包
                    (byte) 0xa2,
                    (byte) 0x25,
                    allCodeBuf[31],
                    allCodeBuf[32],
                    allCodeBuf[33],
                    allCodeBuf[106],
                    allCodeBuf[107],
                    allCodeBuf[108],
                    allCodeBuf[109],
                    allCodeBuf[110],
                    allCodeBuf[111],
                    allCodeBuf[112],
                    allCodeBuf[113],
                    allCodeBuf[114],
                    allCodeBuf[115],
                    allCodeBuf[122],
                    allCodeBuf[123]
            };
            byte[] returnData_54 = null;
            if (null != myCommSound) {
                returnData_54 = myCommSound.sendCommand((byte) 0x54, code_54, 64);
            }
            if (returnData_54 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 9－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 9);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 6-2－－－－－－");
            }
            byte[] code_53 = new byte[]{//第6-3包
                    (byte) 0xa2,
                    (byte) 0x25,
                    allCodeBuf[36],
                    allCodeBuf[37],
                    allCodeBuf[38],
                    allCodeBuf[39],
                    allCodeBuf[40],
                    allCodeBuf[41],
                    allCodeBuf[42],
                    allCodeBuf[43],
                    allCodeBuf[44],
                    allCodeBuf[45],
                    allCodeBuf[46],
                    allCodeBuf[47],
                    allCodeBuf[48],
                    allCodeBuf[49],
                    allCodeBuf[50],
                    allCodeBuf[51]
            };
            byte[] returnData_53 = null;
            if (null != myCommSound) {
                returnData_53 = myCommSound.sendCommand((byte) 0x53, code_53, 63);
            }
            if (returnData_53 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 10－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 10);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 6-3－－－－－－");
            }
            byte[] code_52 = new byte[]{//第6-4包
                    (byte) 0xa2,
                    (byte) 0x25,
                    allCodeBuf[52],
                    allCodeBuf[53],
                    allCodeBuf[54],
                    allCodeBuf[55],
                    allCodeBuf[56],
                    allCodeBuf[57],
                    allCodeBuf[58],
                    allCodeBuf[59],
                    allCodeBuf[60],
                    allCodeBuf[61],
                    allCodeBuf[62],
                    allCodeBuf[63],
                    allCodeBuf[64],
                    allCodeBuf[65],
                    allCodeBuf[66],
                    allCodeBuf[67]
            };
            byte[] returnData_52 = null;
            if (null != myCommSound) {
                returnData_52 = myCommSound.sendCommand((byte) 0x52, code_52, 62);
            }
            if (returnData_52 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 11－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 11);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 6-4－－－－－－");
            }
            byte[] code_51 = new byte[]{//第6-5包
                    (byte) 0xa2,
                    (byte) 0x25,
                    allCodeBuf[68],
                    allCodeBuf[69],
                    allCodeBuf[70],
                    allCodeBuf[71],
                    allCodeBuf[72],
                    allCodeBuf[73],
                    allCodeBuf[74],
                    allCodeBuf[75],
                    allCodeBuf[76],
                    allCodeBuf[77],
                    allCodeBuf[78],
                    allCodeBuf[79],
                    allCodeBuf[80],
                    allCodeBuf[81],
                    allCodeBuf[82],
                    allCodeBuf[83]
            };
            byte[] returnData_51 = null;
            if (null != myCommSound) {
                returnData_51 = myCommSound.sendCommand((byte) 0x51, code_51, 61);
            }
            if (returnData_51 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 12－－－－－－");

                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 12);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 6-5－－－－－－");
            }
            byte[] code_50 = new byte[]{//第6-6包
                    (byte) 0xa2,
                    (byte) 0x25,
                    allCodeBuf[84],
                    allCodeBuf[85],
                    allCodeBuf[86],
                    allCodeBuf[87],
                    allCodeBuf[88],
                    allCodeBuf[89],
                    allCodeBuf[90],
                    allCodeBuf[91],
                    allCodeBuf[92],
                    allCodeBuf[93],
                    allCodeBuf[94],
                    allCodeBuf[95],
                    allCodeBuf[96],
                    allCodeBuf[97],
                    allCodeBuf[98],
                    allCodeBuf[99]
            };
            byte[] returnData_50 = null;
            if (null != myCommSound) {
                returnData_50 = myCommSound.sendCommand((byte) 0x50, code_50, 60);
            }
            if (returnData_50 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 13－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 13);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 6-6－－－－－－");
                currentDevice = 1304;
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 0);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return true;
            }
        }
    }

    /**
     * send code to 1305BG1
     *
     * @param QR     the QRCode of scan
     * @param isKnow whether connected before
     * @return
     */
    private boolean sendCode_new(String QR, boolean isKnow) {
        Log.v(TAG, "send CODE new");
        compileQR(QR);

        UpbottleId = getBottleIdFromQRCode(QR);

        if (isKnow) {//上下位机认识－－发简易code
            SystemClock.sleep(100);
            byte[] code_22 = new byte[]{//  第1／3包
                    (byte) 0x50,
                    allCodeBuf[1],
                    allCodeBuf[2],
                    allCodeBuf[3],
                    allCodeBuf[4],
                    allCodeBuf[5],
                    allCodeBuf[6],
                    allCodeBuf[7],
                    allCodeBuf[8],
                    allCodeBuf[9],
                    allCodeBuf[10],
                    allCodeBuf[11],
                    allCodeBuf[12],
                    allCodeBuf[28],
                    allCodeBuf[29],
            };
            byte[] returnData_22 = null;
            if (null != myCommSound_new) {
                returnData_22 = myCommSound_new.sendCommand((byte) 0x22, code_22, 52);
            }
            if (returnData_22 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 19－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 19);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 3-1－－－－－－");
            }
            SystemClock.sleep(100);
            byte[] code_21 = new byte[]{//  第2／3包
                    allCodeBuf[30],
                    allCodeBuf[31],
                    allCodeBuf[32],
                    allCodeBuf[33],
                    allCodeBuf[106],
                    allCodeBuf[107],
                    allCodeBuf[108],
                    allCodeBuf[109],
                    allCodeBuf[110],
                    allCodeBuf[111],
                    allCodeBuf[112],
                    allCodeBuf[113],
                    allCodeBuf[114],
                    allCodeBuf[115],
                    allCodeBuf[122]
            };
            byte[] returnData_21 = null;
            if (null != myCommSound_new) {
                returnData_21 = myCommSound_new.sendCommand((byte) 0x21, code_21, 51);
            }
            if (returnData_21 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 20－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 20);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 3-2－－－－－－");
            }
            SystemClock.sleep(100);
            byte[] code_20 = new byte[]{//  第3／3包
                    allCodeBuf[123]
            };
            byte[] returnData_20 = null;
            if (null != myCommSound_new) {
                returnData_20 = myCommSound_new.sendCommand((byte) 0x20, code_20, 50);
            }
            if (returnData_20 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 21－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 21);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 3-3－－－－－－");
                currentDevice = 1305;
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 0);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return true;
            }

        } else {//不认识－－发全部code
            SystemClock.sleep(100);
            byte[] code_66 = new byte[]{// 第7-1包
                    (byte) 0x25,
                    allCodeBuf[1],
                    allCodeBuf[2],
                    allCodeBuf[3],
                    allCodeBuf[4],
                    allCodeBuf[5],
                    allCodeBuf[6],
                    allCodeBuf[7],
                    allCodeBuf[8],
                    allCodeBuf[9],
                    allCodeBuf[10],
                    allCodeBuf[11],
                    allCodeBuf[12],
                    allCodeBuf[28],
                    allCodeBuf[29]
            };
            byte[] returnData_66 = null;
            if (null != myCommSound_new) {
                returnData_66 = myCommSound_new.sendCommand((byte) 0x66, code_66, 66);
            }
            if (returnData_66 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 22－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 22);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 7-1－－－－－－");
            }
            SystemClock.sleep(100);
            byte[] code_65 = new byte[]{// 第7-2包
                    allCodeBuf[30],
                    allCodeBuf[31],
                    allCodeBuf[32],
                    allCodeBuf[33],
                    allCodeBuf[36],
                    allCodeBuf[37],
                    allCodeBuf[38],
                    allCodeBuf[39],
                    allCodeBuf[40],
                    allCodeBuf[41],
                    allCodeBuf[42],
                    allCodeBuf[43],
                    allCodeBuf[44],
                    allCodeBuf[45],
                    allCodeBuf[46]
            };
            byte[] returnData_65 = null;
            if (null != myCommSound_new) {
                returnData_65 = myCommSound_new.sendCommand((byte) 0x65, code_65, 65);
            }
            if (returnData_65 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 23－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 23);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 7-2－－－－－－");
            }
            SystemClock.sleep(100);
            byte[] code_64 = new byte[]{// 第7-3包
                    allCodeBuf[47],
                    allCodeBuf[48],
                    allCodeBuf[49],
                    allCodeBuf[50],
                    allCodeBuf[51],
                    allCodeBuf[52],
                    allCodeBuf[53],
                    allCodeBuf[54],
                    allCodeBuf[55],
                    allCodeBuf[56],
                    allCodeBuf[57],
                    allCodeBuf[58],
                    allCodeBuf[59],
                    allCodeBuf[60],
                    allCodeBuf[61]
            };
            byte[] returnData_64 = null;
            if (null != myCommSound_new) {
                returnData_64 = myCommSound_new.sendCommand((byte) 0x64, code_64, 64);
            }
            if (returnData_64 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 24－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 24);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 7-3－－－－－－");
            }
            SystemClock.sleep(100);
            byte[] code_63 = new byte[]{// 第7-4包
                    allCodeBuf[62],
                    allCodeBuf[63],
                    allCodeBuf[64],
                    allCodeBuf[65],
                    allCodeBuf[66],
                    allCodeBuf[67],
                    allCodeBuf[68],
                    allCodeBuf[69],
                    allCodeBuf[70],
                    allCodeBuf[71],
                    allCodeBuf[72],
                    allCodeBuf[73],
                    allCodeBuf[74],
                    allCodeBuf[75],
                    allCodeBuf[76]
            };
            byte[] returnData_63 = null;
            if (null != myCommSound_new) {
                returnData_63 = myCommSound_new.sendCommand((byte) 0x63, code_63, 63);
            }
            if (returnData_63 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 25－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 25);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 7-4－－－－－－");
            }
            SystemClock.sleep(100);
            byte[] code_62 = new byte[]{// 第7-5包
                    allCodeBuf[77],
                    allCodeBuf[78],
                    allCodeBuf[79],
                    allCodeBuf[80],
                    allCodeBuf[81],
                    allCodeBuf[82],
                    allCodeBuf[83],
                    allCodeBuf[84],
                    allCodeBuf[85],
                    allCodeBuf[86],
                    allCodeBuf[87],
                    allCodeBuf[88],
                    allCodeBuf[89],
                    allCodeBuf[90],
                    allCodeBuf[91]
            };
            byte[] returnData_62 = null;
            if (null != myCommSound_new) {
                returnData_62 = myCommSound_new.sendCommand((byte) 0x62, code_62, 62);
            }
            if (returnData_62 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 26－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 26);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 7-5－－－－－－");
            }
            SystemClock.sleep(100);
            byte[] code_61 = new byte[]{// 第7-6包
                    allCodeBuf[92],
                    allCodeBuf[93],
                    allCodeBuf[94],
                    allCodeBuf[95],
                    allCodeBuf[96],
                    allCodeBuf[97],
                    allCodeBuf[98],
                    allCodeBuf[99],
                    allCodeBuf[106],
                    allCodeBuf[107],
                    allCodeBuf[108],
                    allCodeBuf[109],
                    allCodeBuf[110],
                    allCodeBuf[111],
                    allCodeBuf[112]
            };
            byte[] returnData_61 = null;
            if (null != myCommSound_new) {
                returnData_61 = myCommSound_new.sendCommand((byte) 0x61, code_61, 61);
            }
            if (returnData_61 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 27－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 27);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 7-6－－－－－－");
            }
            SystemClock.sleep(100);
            byte[] code_60 = new byte[]{// 第7-7包
                    allCodeBuf[113],
                    allCodeBuf[114],
                    allCodeBuf[115],
                    allCodeBuf[122],
                    allCodeBuf[123]
            };
            byte[] returnData_60 = null;
            if (null != myCommSound_new) {
                returnData_60 = myCommSound_new.sendCommand((byte) 0x60, code_60, 60);
            }
            if (returnData_60 == null) {
                Log.e(TAG, "－－－－－－send code failed :: flag 28－－－－－－");
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 28);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return false;
            } else {
                Log.v(TAG, "－－－－－－send code success 7-7－－－－－－");
                currentDevice = 1305;
                Intent intent27 = new Intent(Bg1Profile.ACTION_BG1_SENDCODE_RESULT);
                intent27.putExtra(Bg1Profile.BG1_SENDCODE_RESULT, 0);
                //jing 20160731
                intent27.setPackage(context.getPackageName());
                context.sendBroadcast(intent27);
                return true;
            }
        }
    }

    /**
     * Start BG1 communication
     */
    private void start() {
        disconnect();
        Log.v(TAG, "start BG1 communication");
        Log.v(TAG, "brand = " + android.os.Build.BRAND);
        Log.v(TAG, "model = " + android.os.Build.MODEL);
        Log.v(TAG, "RELEASE = " + Build.VERSION.RELEASE);

        //其他机型，也可以尝试此算法 20150723
        tunner = new TunnerThread();
        tunner.msgSubject.detach(this);
        tunner.msgSubject.attach(this);

        myCommSound_new = new CommSound_new(tunner);
        AudioTrackManager.inCommunication = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                myCommSound_new.sendCommand();//先尝试输出一段波形
            }
        }).start();
    }

    /**
     * Start BG1 communication
     */
    private void start_old() {
        disconnect();
        tunner = new TunnerThread();
        tunner.msgSubject.detach(this);
        tunner.msgSubject.attach(this);

        myCommSound = new CommSound(tunner);
        AudioTrackManager.inCommunication = true;
    }

    /**
     * Close the communication
     */
    public void disconnect() {
        AudioTrackManager.inCommunication = false;
        if (tunner != null) {
            tunner.close();
            tunner = null;
            if (myCommSound_new != null) {
                myCommSound_new.audio.stop();
                myCommSound_new = null;
            }
            if (myCommSound != null) {
                myCommSound.audio.stop();
                myCommSound = null;
            }
        }

        if (mConnect1305Thread != null) {
            mConnect1305Thread.interrupt();
            mConnect1305Thread = null;
        }
        if (mConnect1304Thread != null) {
            mConnect1304Thread.interrupt();
            mConnect1304Thread = null;
        }

        commandID = 0;
        closeBG1Timer();
    }

    /**
     * analysis the command back
     *
     * @param command
     * @hide
     */
    private void Analysis(byte[] command) {
        if (command != null) {
            Log.v(TAG, "analysis:" + Bytes2HexString(command, command.length));
            if (command[0] != (byte) 0xA0) {// 头命令不对
                Log.v(TAG, "header error");
                return;
            }
            int lengthReceive = command.length;
            int lenPackage = command[1];
            if (lenPackage + 3 != lengthReceive) {// 长度错误
                Log.v(TAG, "length error");
                return;
            }
            if (command[4] != (byte) 0xA2) {// 产品ID不对
                Log.v(TAG, "product id error");
                return;
            }
            int tempCommandId = (command[5] & 0xFF);

            switch (command[5]) {
                case (byte) 0x2C:
                    // 报错信息
                    int errNum = 0;
                    switch (command[6]) {
                        case (byte) 0x00:
                            errNum = 0;
                            break;
                        case (byte) 0x01:
                            errNum = 1;
                            break;
                        case (byte) 0x02:
                            errNum = 2;
                            break;
                        case (byte) 0x03:
                            errNum = 3;
                            break;
                        case (byte) 0x04:
                            errNum = 4;
                            isError4 = true;
                            AudioTrackManager.inCommunication = false;
                            break;
                        case (byte) 0x05:
                            errNum = 5;
                            break;
                        case (byte) 0x06:
                            errNum = 6;
                            break;
                        case (byte) 0x07:
                            errNum = 7;
                            break;
                        case (byte) 0x08:
                            errNum = 8;
                            break;
                        case (byte) 0x09:
                            errNum = 9;
                            break;
                        case (byte) 0x0A:
                            errNum = 10;
                            break;
                        default:
                            break;
                    }

                    if (commandID != tempCommandId) {
                        commandID = tempCommandId;
                        Intent intent49 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_ERROR);
                        intent49.putExtra(Bg1Profile.BG1_MEASURE_ERROR, errNum);
                        //jing 20160731
                        intent49.setPackage(context.getPackageName());
                        context.sendBroadcast(intent49);
                    } else {
                        Log.v(TAG, "2C already received------");
                    }
                    break;
                case (byte) 0x33:
                    // 试条插入
                    if (commandID != tempCommandId) {
                        commandID = tempCommandId;
                        Intent intent50 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_STRIP_IN);
                        //jing 20160731
                        intent50.setPackage(context.getPackageName());
                        context.sendBroadcast(intent50);
                    } else {
                        Log.v(TAG, "33 already received------");
                    }
                    break;
                case (byte) 0x34:
                    // 试条滴血
                    if (commandID != tempCommandId) {
                        commandID = tempCommandId;
                        Intent intent51 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_GET_BLOOD);
                        //jing 20160731
                        intent51.setPackage(context.getPackageName());
                        context.sendBroadcast(intent51);
                    } else {
                        Log.v(TAG, "34 already received------");
                    }
                    break;
                case (byte) 0x52:
                    // 试条拔出
                    if (commandID != tempCommandId) {
                        commandID = tempCommandId;
                        Intent intent51 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_STRIP_OUT);
                        //jing 20160731
                        intent51.setPackage(context.getPackageName());
                        context.sendBroadcast(intent51);
                    } else {
                        Log.v(TAG, "52 already received------");
                    }
                    break;
                case (byte) 0x36:
                    // 结果
                    if (commandID != tempCommandId) {
                        commandID = tempCommandId;
                        int value = (int) (command[6] & 0xFF) * 256 + (int) (command[7] & 0xFF);
                        if (!(value >= 20 && value <= 600)) {
                            Intent intent53 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_ERROR);
                            intent53.putExtra(Bg1Profile.BG1_MEASURE_ERROR, 1);
                            //jing 20160731
                            intent53.setPackage(context.getPackageName());
                            context.sendBroadcast(intent53);
                        } else {
                            Intent intent53 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_RESULT);
                            intent53.putExtra(Bg1Profile.BG1_MEASURE_RESULT, value);
                            intent53.putExtra(Bg1Profile.DATA_ID, Method.getBgDataId(DeviceId1304, Method.getTS(), value));
                            //jing 20160731
                            intent53.setPackage(context.getPackageName());
                            context.sendBroadcast(intent53);
                        }

                        if (AppsDeviceParameters.isUpLoadData) {
                            //将结果存数据库
                            Data_BG_Result Bgr = Make_Data_Util.makeDataSingleBg(userName, value, "BG1", DeviceId1304, UpbottleId);
                            DataBaseTools db = new DataBaseTools(this.context);
                            db.addData(DataBaseConstants.TABLE_TB_BGRESULT, Bgr);
                            //开启数据上云
                            BG_InAuthor sdk_InAuthor = BG_InAuthor.getInstance();
                            sdk_InAuthor.initAuthor(context, userName);
                            sdk_InAuthor.run();
                        }
                    } else {
                        Log.v(TAG, "36 already received------");
                    }
                    break;
                case (byte) 0x3B:
                    // 待机
                    if (commandID != tempCommandId) {
                        commandID = tempCommandId;
                        Intent intent54 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_STANDBY);
                        //jing 20160731
                        intent54.setPackage(context.getPackageName());
                        context.sendBroadcast(intent54);
                    } else {
                        Log.v(TAG, "3B already received------");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * analysis the command back from 1305BG1
     *
     * @param command
     * @hide
     */
    private void Analysis_new(byte[] command) {

        if (command != null) {
            if (command.length == 3) {
                switch (deviceModel) {
                    case 1304:
                    case 1305:
                        //1305
                        if (command[0] == (byte) 0xA2 && command[1] == (byte) 0x01 && command[2] == (byte) 0xA3) {//下位机的握手指令
                            closeBG1Timer();
                            isError4 = false;
                            isAG680 = false;
                            Intent intent55 = new Intent(Bg1Profile.ACTION_BG1_DEVICE_READY);
                            //jing 20160731
                            intent55.setPackage(context.getPackageName());
                            context.sendBroadcast(intent55);

                            if (mConnect1305Thread == null) {
                                mConnect1305Thread = new Thread(connect1305_Runnable);
                                mConnect1305Thread.start();
                            }
                        }
                        //AG680
                        if (command[0] == (byte) 0xA2 && command[1] == (byte) 0x03 && command[2] == (byte) 0xA5) {//下位机的握手指令
                            closeBG1Timer();
                            isError4 = false;
                            isAG680 = true;
                            Intent intent55 = new Intent(Bg1Profile.ACTION_BG1_DEVICE_READY);
                            //jing 20160731
                            intent55.setPackage(context.getPackageName());
                            context.sendBroadcast(intent55);

                            if (mConnect1305Thread == null) {
                                mConnect1305Thread = new Thread(connect1305_Runnable);
                                mConnect1305Thread.start();
                            }
                        }
                    case 1307:
                        //1307
                        if ((command[0] == (byte) 0x23 && command[1] == (byte) 0x45 && command[2] == (byte) 0x68)
                                || (command[0] == (byte) 0x01 && command[1] == (byte) 0x23 && command[2] == (byte) 0x46)
                                || (AudioTrackManager.isR2017 && command[0] == (byte) 0x34 && command[1] == (byte) 0x56 && command[2] == (byte) 0x80)) {//下位机的握手指令
                            closeBG1Timer();
                            isError4 = false;
                            isAG680 = false;
                            if (null != tunner) {
                                tunner.set1307(true);
                            }
                            Intent intent55 = new Intent(Bg1Profile.ACTION_BG1_DEVICE_READY);
                            //jing 20160731
                            intent55.setPackage(context.getPackageName());
                            context.sendBroadcast(intent55);

                            if (mConnect1305Thread == null) {
                                mConnect1305Thread = new Thread(connect1305_Runnable);
                                mConnect1305Thread.start();
                            }
                        }
                        break;
                    default:
                        break;
                }
            } else {
                int tempCommandId;
                if (command.length == 5) {
                    tempCommandId = (command[3] & 0xFF);
                    switch (command[3]) {
                        case (byte) 0x2C://错误
                            sendACK_New((byte) 0x2C);
                            if (commandID != tempCommandId) {
                                commandID = tempCommandId;
                                Intent intent56 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_ERROR);
                                intent56.putExtra(Bg1Profile.BG1_MEASURE_ERROR, (int) (command[4] & 0xFF));
                                //jing 20160731
                                intent56.setPackage(context.getPackageName());
                                context.sendBroadcast(intent56);
                            } else {
                                Log.v(TAG, "2C already received------");
                            }

                            if (command[4] == 4) {
                                isError4 = true;
                                AudioTrackManager.inCommunication = false;
                            }
                            break;
                        default:
                            break;
                    }
                } else if (command.length == 7) {
                    tempCommandId = (command[3] & 0xFF);
                    switch (command[3]) {
                        case (byte) 0x33://插条
                            sendACK_New((byte) 0x33);
                            if (commandID != tempCommandId) {
                                commandID = tempCommandId;
                                Intent intent57 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_STRIP_IN);
                                //jing 20160731
                                intent57.setPackage(context.getPackageName());
                                context.sendBroadcast(intent57);
                            } else {
                                Log.v(TAG, "33 already received------");
                            }

                            break;
                        case (byte) 0x34://滴血
                            sendACK_New((byte) 0x34);
                            if (commandID != tempCommandId) {
                                commandID = tempCommandId;
                                Intent intent58 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_GET_BLOOD);
                                //jing 20160731
                                intent58.setPackage(context.getPackageName());
                                context.sendBroadcast(intent58);
                            } else {
                                Log.v(TAG, "34 already received------");
                            }

                            break;
                        case (byte) 0x52://拔条
                            sendACK_New((byte) 0x52);
                            if (commandID != tempCommandId) {
                                commandID = tempCommandId;
                                Intent intent59 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_STRIP_OUT);
                                //jing 20160731
                                intent59.setPackage(context.getPackageName());
                                context.sendBroadcast(intent59);
                            } else {
                                Log.v(TAG, "52 already received------");
                            }

                            break;
                        case (byte) 0x36://结果
                            sendACK_New((byte) 0x36);
                            if (commandID != tempCommandId) {
                                commandID = tempCommandId;
                                int value = (int) ((command[4] & 0xFF) * 256) + (int) (command[5] & 0xFF);

                                if (!(value >= 20 && value <= 600)) {
                                    Intent intent60 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_ERROR);
                                    intent60.putExtra(Bg1Profile.BG1_MEASURE_ERROR, 1);
                                    //jing 20160731
                                    intent60.setPackage(context.getPackageName());
                                    context.sendBroadcast(intent60);
                                } else {
                                    Intent intent60 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_RESULT);
                                    intent60.putExtra(Bg1Profile.BG1_MEASURE_RESULT, value);
                                    intent60.putExtra(Bg1Profile.DATA_ID, Method.getBgDataId(DeviceId1305, Method.getTS(), value));
                                    //jing 20160731
                                    intent60.setPackage(context.getPackageName());
                                    context.sendBroadcast(intent60);
                                }
                                if (AppsDeviceParameters.isUpLoadData) {
                                    //将结果存数据库
                                    Data_BG_Result Bgr = Make_Data_Util.makeDataSingleBg(userName, value, "BG1", DeviceId1305, UpbottleId);
                                    DataBaseTools db = new DataBaseTools(this.context);
                                    db.addData(DataBaseConstants.TABLE_TB_BGRESULT, Bgr);
                                    //开启数据上云
                                    BG_InAuthor sdk_InAuthor = BG_InAuthor.getInstance();
                                    sdk_InAuthor.initAuthor(context, userName);
                                    sdk_InAuthor.run();
                                }

                            } else {
                                Log.v(TAG, "36 already received------");
                            }

                            if (!isReadyforIden) {
                                isReadyforIden = true;
                                new Thread() {
                                    public void run() {
                                        try {
                                            Thread.sleep(2000L);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        if (!isIdent) {
                                            setMaxVolume();
                                            isIdent = identification_new();
                                            resetVolume();
                                        }
                                        isReadyforIden = false;
                                    }
                                }.start();
                            }
                            break;
                        case (byte) 0x3B://待机
                            sendACK_New((byte) 0x3B);
                            if (commandID != tempCommandId) {
                                commandID = tempCommandId;
                                Intent intent61 = new Intent(Bg1Profile.ACTION_BG1_MEASURE_STANDBY);
                                //jing 20160731
                                intent61.setPackage(context.getPackageName());
                                context.sendBroadcast(intent61);
                            } else {
                                Log.v(TAG, "3B already received------");

                            }

                            break;
                        case (byte) 0x59://次数用完
                            sendACK_New((byte) 0x59);
                            if (!isReadyforIden) {
                                isReadyforIden = true;
                                new Thread() {
                                    public void run() {
                                        try {
                                            Thread.sleep(2000L);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        setMaxVolume();
                                        isIdent = identification_new();
                                        resetVolume();
                                        isReadyforIden = false;
                                    }
                                }.start();
                            }

                            break;
                        default:
                            break;
                    }
                }
            }

        }
    }

    /**
     * 1305BG1 send acknowledge
     *
     * @param commandId
     * @hide
     */
    private void sendACK_New(final byte commandId) {
        new Thread() {
            public void run() {
                if (null != myCommSound_new) {
                    setMaxVolume();
                    myCommSound_new.sendACK((byte) 0x00, commandId);
                    resetVolume();
                }
            }
        }.start();
    }

    /**
     * @param b
     * @param len
     * @return
     * @hide
     */
    @SuppressLint("DefaultLocale")
    private String Bytes2HexString(byte[] b, int len) {
        String ret = "";
        for (int i = 0; i < len; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

    /**
     * @param stripCodeOFScan
     * @return
     * @hide
     */
    private long getBottleIdFromQRCode(String stripCodeOFScan) {
        long bottleID = 0;
        byte buffer[] = hexStringToByte(stripCodeOFScan);
        if (buffer != null && buffer.length == 30) {
            bottleID = (long) (buffer[26] & 0xFF) * 256 * 256 * 256
                    + (long) (buffer[27] & 0xFF) * 256 * 256
                    + (long) (buffer[24] & 0xFF) * 256
                    + (long) (buffer[25] & 0xFF);
        }
        return bottleID;
    }

    /**
     * @hide
     */
    @Override
    public void msgBytes(byte[] msg) {
        Log.v(TAG, "msg:" + Bytes2HexString(msg, msg.length));

        switch (msg.length) {
            case 3:// 下位机发起的握手
            case 5:// 错误
            case 7:// 插条 滴血 结果 拔条
                if (running1305) {
                    Analysis_new(msg);
                }
                break;
            case 10:// 1304
                Analysis(msg);
                break;
            default:
                Log.v(TAG, "------------- in communication process -------------");
                break;
        }
    }

    /**
     * TODO 设置最大音量
     */
    private void setMaxVolume() {
        int max = this.myAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        this.myAudio.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.RINGER_MODE_SILENT);
        int temp = max;
        if (max != this.myAudio.getStreamVolume(AudioManager.STREAM_MUSIC)) {
            section:
            {
                try {
                    //1s时间太长，导致某些指令的回复超时，MI 4C。改为0.1s 20160921_zyg
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    temp = max;
                    break section;
                }
                temp = max;
            }
        } else {
            temp = max;
        }

        while (temp > max * 2 / 3 && temp != this.myAudio.getStreamVolume(AudioManager.STREAM_MUSIC)) {
            --temp;
            this.myAudio.setStreamVolume(AudioManager.STREAM_MUSIC, temp, AudioManager.RINGER_MODE_SILENT);
        }

        if (Build.VERSION.RELEASE.compareTo("4.3") >= 0 && temp < max) {
            if (isShow_UI) {
                this.myAudio.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI);
            } else {
                this.myAudio.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.RINGER_MODE_SILENT);
            }
        }

        Log.v(TAG, "set volume ----------> " + this.myAudio.getStreamVolume(AudioManager.STREAM_MUSIC));

    }

    private void resetVolume() {
        this.myAudio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolumeIndex, AudioManager.RINGER_MODE_SILENT);
        Log.v(TAG, "reset volume ----------> " + this.myAudio.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    private static class SingletonHolder {
        static final Bg1Control INSTANCE = new Bg1Control();
    }

}
