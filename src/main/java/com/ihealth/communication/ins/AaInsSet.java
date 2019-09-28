
package com.ihealth.communication.ins;

import android.content.Context;
import android.content.SharedPreferences;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.base.protocol.BleCommProtocol;
import com.ihealth.communication.cloud.data.AM_CommCloud;
import com.ihealth.communication.cloud.data.AM_InAuthor;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.cloud.tools.Method;
import com.ihealth.communication.control.AmProfile;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.DataThreadPoolManager;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class AaInsSet extends IdentifyIns implements NewDataCallback, GetBaseCommProtocolCallback {

    private static final String TAG = "AaInsSet";

    /**
     * beyond the 1.1.0 version, the number of steps am3s from 2 bytes to 4 bytes.
     */
    private static String UP110FirmwareVersion = "110";
    private final BaseComm mBaseComm;


    private Context mContext;
    private String mAddress;
    private String mType;
    // private String hVer;
    // private String fVer;
    private BleCommProtocol blecm;
    private byte deviceType = (byte) 0xaa;
    
    private InsCallback mInsCallback;
    private BaseCommCallback mBaseCommCallback;
    private String mUserName;
    //当前固件版本
    private int currentFirmwareVersion;
    //阶段性协议修改版本
    private int RightFirmwareVersion = 134;
    //用户信息协议修改版本 AM4 +游泳目标参数 阶段性 +统计页面查看数量
    private int SwimTargetFirmwareVersion = 138;
    //支持游泳目标显示的版本
    private int SwimDisplayFirmwareVersion = 139;
    //用户绑定解绑
    private AM_CommCloud am_CommCloud;
    //token host
    private String accessToken = null;
    private String host = null;

    private static void printMethodInfo(String methodName, Object... parameters) {
        Log.p(TAG, Log.Level.INFO, methodName, parameters);
    }
    /**
     * Constructor
     * @param com 
     * @param context 
     * @param mac 
     * @param type
     * @param BaseCommCallback 
     * @param insCallback 
     * @hide
     */
    public AaInsSet(BaseComm com, Context context, String mac, String type, String userName, BaseCommCallback BaseCommCallback, InsCallback insCallback) {
        printMethodInfo("AaInsSet_Constructor", mac, type, userName);
        this.mContext = context;
        this.mAddress = mac;
        this.mType = type;
        this.mUserName = userName;
        this.mBaseCommCallback = BaseCommCallback;
        this.mInsCallback = insCallback;
        this.mBaseComm = com;
        blecm = new BleCommProtocol(context, com, mAddress, deviceType, this);
        listActivity = new ArrayList<byte[]>();
        listSleep = new ArrayList<byte[]>();
        listStageForm = new ArrayList<byte[]>();
        currentFirmwareVersion = Integer.parseInt(iHealthDevicesManager.getInstance().getIdps(mAddress).getAccessoryFirmwareVersion());
        /* 将用户名等参数写入文件 */
        AM_InAuthor am_inAuthor = new AM_InAuthor();
        am_inAuthor.initAuthor(context, userName);
        /* 用户绑定 解绑 */
        am_CommCloud = new AM_CommCloud(context);

        getUserToken();

        setInsSetCallbak(insCallback, mac, type, com);
    }


    /**
     * identify the connection between AM and phone
     * @hide
     */
    public void identify() {
        printMethodInfo("identify");
        //4S timer
        startTimeout(0xfa, AppsDeviceParameters.Delay_Medium, 0xfb, 0xfd, 0xfe);
        blecm.packageData(mAddress, identify(deviceType));
    }

    /**
     * get swimming information
     * @hide
     */
    public void checkSwimPara() {
        printMethodInfo("checkSwimPara");
        byte commandID = (byte) 0x05;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0x05, AppsDeviceParameters.Delay_Medium, 0x05);
        blecm.packageData(mAddress, returnCommand);

    }

    /**
     * set swimming information
     * @param isOpen 开关
     * @param poolLength
     * @param hours
     * @param minites
     * @param unit
     * @hide
     */
    public void setSwimPara(boolean isOpen, byte poolLength, byte hours, byte minites, byte unit) {
        printMethodInfo("setSwimPara", isOpen, poolLength, hours, minites, unit);
        byte commandID = (byte) 0x06;
        byte[] returnCommand = new byte[7];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        if (isOpen) {
            if (currentFirmwareVersion >= SwimDisplayFirmwareVersion) {
                returnCommand[2] = 0x02;
            }
            else {
                returnCommand[2] = 0x01;
            }
        } else {
            returnCommand[2] = 0x00;
        }
        returnCommand[3] = poolLength;
        returnCommand[4] = hours;
        returnCommand[5] = minites;
        returnCommand[6] = unit;
        //4S timer
        startTimeout(0x06, AppsDeviceParameters.Delay_Medium, 0x06);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * reset
     * @param userId 
     * @hide
     */
    public void a1Ins(int userId) {
        printMethodInfo("a1Ins", userId);
        byte[] id = ByteBufferUtil.intTo4Byte(userId);
        byte commandID = (byte) 0xA1;
        byte[] returnCommand = new byte[6];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        for (int i = 0; i < id.length; i++) {
            returnCommand[i + 2] = (byte) 0xFF;
        }
        //4S timer
        startTimeout(0xA1, AppsDeviceParameters.Delay_Medium, 0xA1);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get user id
     * @hide
     */
    public void a2Ins() { // 查询用户编号
        printMethodInfo("a2Ins");
        byte commandID = (byte) 0xA2;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0xA2, AppsDeviceParameters.Delay_Medium, 0xA2);
        blecm.packageData(mAddress, returnCommand);
    }

//    private int mUserId = 0;
//    public void setUserId(int userId) {
//        this.mUserId = userId;
//    }

    /**
     * set user id
     * @hide
     */
    public void a3Ins(int userId) {
        printMethodInfo("a3Ins", userId);
        byte[] id = ByteBufferUtil.intToUserId(userId);
        byte commandID = (byte) 0xA3;
        byte[] returnCommand = new byte[6];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        for (int i = 0; i < id.length; i++) {
            returnCommand[i + 2] = id[i];
        }
        //4S timer
        startTimeout(0xA3, AppsDeviceParameters.Delay_Medium, 0xA3);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * sync real time
     * @param year 
     * @param month 
     * @param day 
     * @param hour 
     * @param min 
     * @param sed 
     * @param week 
     * @hide
     */
    public void a4Ins(int year, int month, int day, int hour, int min,
            int sed, int week) {
        printMethodInfo("a4Ins", year, month, day, hour, min, sed, week);
        if (String.valueOf(year).length() > 2) {
            year = year - 2000;
        }
        byte bYear = (byte) (year & 0xFF);
        byte bMonth = (byte) (month & 0xFF);
        byte bDay = (byte) (day & 0xFF);
        byte bHour = (byte) (hour & 0xFF);
        byte bMin = (byte) (min & 0xFF);
        byte bSed = (byte) (sed & 0xFF);
        byte bWeek = (byte) (week & 0xFF);
        byte[] timeoffset = new byte[] {
                (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00
        };

        byte commandID = (byte) 0xA4;

        byte[] returnCommand = new byte[13];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = bYear;
        returnCommand[3] = bMonth;
        returnCommand[4] = bDay;
        returnCommand[5] = bHour;
        returnCommand[6] = bMin;
        returnCommand[7] = bSed;
        returnCommand[8] = bWeek;

        for (int i = 0; i < timeoffset.length; i++) {
            returnCommand[i + 9] = timeoffset[i];
        }
        //4S timer
        startTimeout(0xA4, AppsDeviceParameters.Delay_Medium, 0xA4);
        blecm.packageData(mAddress, returnCommand);
    }

    
    private void setUserMessage() {
        byte commandID = (byte) 0xA5;
        byte[] returnCommand;

        if (bTarget1.length == 4) {

            if (mType.equals(iHealthDevicesManager.TYPE_AM3)||mType.equals(iHealthDevicesManager.TYPE_AM3S)||(mType.equals(iHealthDevicesManager.TYPE_AM4)
                    && currentFirmwareVersion < SwimTargetFirmwareVersion)) {
                returnCommand = new byte[21];

                returnCommand[9] = bTarget1[0];
                returnCommand[10] = bTarget1[1];
                returnCommand[11] = bTarget1[2];
                returnCommand[12] = bTarget1[3];
                returnCommand[13] = bTarget2[0];
                returnCommand[14] = bTarget2[1];
                returnCommand[15] = bTarget2[2];
                returnCommand[16] = bTarget2[3];
                returnCommand[17] = bTarget3[0];
                returnCommand[18] = bTarget3[1];
                returnCommand[19] = bTarget3[2];
                returnCommand[20] = bTarget3[3];
            } else {
                returnCommand = new byte[23];

                returnCommand[9] = bTarget1[0];
                returnCommand[10] = bTarget1[1];
                returnCommand[11] = bTarget1[2];
                returnCommand[12] = bTarget1[3];
                returnCommand[13] = bTarget2[0];
                returnCommand[14] = bTarget2[1];
                returnCommand[15] = bTarget2[2];
                returnCommand[16] = bTarget2[3];
                returnCommand[17] = bTarget3[0];
                returnCommand[18] = bTarget3[1];
                returnCommand[19] = bTarget3[2];
                returnCommand[20] = bTarget3[3];

                returnCommand[21] = this.swimTargetHour;
                returnCommand[22] = this.swimTargetMin;
            }
        } else {
            returnCommand = new byte[15];
            returnCommand[9] = bTarget1[0];
            returnCommand[10] = bTarget1[1];
            returnCommand[11] = bTarget2[0];
            returnCommand[12] = bTarget2[1];
            returnCommand[13] = bTarget3[0];
            returnCommand[14] = bTarget3[1];
        }
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = bAge;
        returnCommand[3] = bStep;
        returnCommand[4] = (byte) heightzheng;
        returnCommand[5] = bSex;
        returnCommand[6] = (byte) weightzheng;
        returnCommand[7] = (byte) weightxiao;
        returnCommand[8] = bWeightUnit;
        //4S timer
        startTimeout(0xA5, AppsDeviceParameters.Delay_Medium, 0xA5);
        blecm.packageData(mAddress, returnCommand);

    }

    private byte bAge;
    private byte bStep;
    private int weightzheng;
    private int weightxiao;
    private int heightzheng;
    private byte bSex;
    private byte bWeightUnit;
    private byte[] bTarget1;
    private byte[] bTarget2;
    private byte[] bTarget3;
    private int mBmr;
    private int mHourType;
    private boolean notSendConnected;
    private byte swimTargetHour;
    private byte swimTargetMin;

    private boolean mNeedSetUserInfoAfterSetBMR = false;
//    private String AccessoryFirmwareVersion;
    /**
     * set user information
     * @hide
     */
    public void setUserInfo(int age, int step, float height, int sex,
            float weight, int weightUnit, int target1, int target2, int target3, int bmr) {
        printMethodInfo("setUserInfo", age, step, height, sex, weight, weightUnit, target1, target2, target3, bmr);
    	this.bAge = (byte) age;
        this.bStep = (byte) step;
        this.weightzheng = (int) Float.parseFloat(weight + "");
        this.weightxiao = ((int) (Float.parseFloat(weight + "") * 10)) % 10;
        this.heightzheng = (int) Float.parseFloat(height + "");
        this.bSex = (byte) sex;
        this.bWeightUnit = (byte) weightUnit;

        if (mType.equals(iHealthDevicesManager.TYPE_AM3S) && currentFirmwareVersion >= Integer
                .parseInt(UP110FirmwareVersion)) {
        	
            this.bTarget1 = ByteBufferUtil.intTo4Byte(target1);
            this.bTarget2 = ByteBufferUtil.intTo4Byte(target2);
            this.bTarget3 = ByteBufferUtil.intTo4Byte(target3);
        } else if (mType.equals(iHealthDevicesManager.TYPE_AM4)) {
            this.bTarget1 = ByteBufferUtil.intTo4Byte(target1);
            this.bTarget2 = ByteBufferUtil.intTo4Byte(target2);
            this.bTarget3 = ByteBufferUtil.intTo4Byte(target3);
		} else {
            this.bTarget1 = ByteBufferUtil.intTo2Byte(target1);
            this.bTarget2 = ByteBufferUtil.intTo2Byte(target2);
            this.bTarget3 = ByteBufferUtil.intTo2Byte(target3);
        }

        this.mBmr = bmr;

        //jing 20160929  SDK对内对外区分,是否自己计算BMR下发
        if (AppsDeviceParameters.isUpLoadData == false) {
            this.setUserMessage();
        } else {
            this.b7Ins(mBmr, true);
        }
    }

    /**
     * set user information
     * @hide
     */
    public void setUserInfoForAM4Plus(int age, int step, float height, int sex,
                            float weight, int weightUnit, int target1, int target2, int target3, int bmr, int min) {
        printMethodInfo("setUserInfoForAM4Plus", age, step, height, sex, weight, weightUnit, target1, target2, target3, bmr, min);
        this.bAge = (byte) age;
        this.bStep = (byte) step;
        this.weightzheng = (int) Float.parseFloat(weight + "");
        this.weightxiao = ((int) (Float.parseFloat(weight + "") * 10)) % 10;
        this.heightzheng = (int) Float.parseFloat(height + "");
        this.bSex = (byte) sex;
        this.bWeightUnit = (byte) weightUnit;
        this.swimTargetHour = (byte) (min/60);
        this.swimTargetMin = (byte) (min%60);

        if (mType.equals(iHealthDevicesManager.TYPE_AM3S) && currentFirmwareVersion >= Integer
                .parseInt(UP110FirmwareVersion)) {

            this.bTarget1 = ByteBufferUtil.intTo4Byte(target1);
            this.bTarget2 = ByteBufferUtil.intTo4Byte(target2);
            this.bTarget3 = ByteBufferUtil.intTo4Byte(target3);
        } else if (mType.equals(iHealthDevicesManager.TYPE_AM4)) {
            this.bTarget1 = ByteBufferUtil.intTo4Byte(target1);
            this.bTarget2 = ByteBufferUtil.intTo4Byte(target2);
            this.bTarget3 = ByteBufferUtil.intTo4Byte(target3);
        } else {
            this.bTarget1 = ByteBufferUtil.intTo2Byte(target1);
            this.bTarget2 = ByteBufferUtil.intTo2Byte(target2);
            this.bTarget3 = ByteBufferUtil.intTo2Byte(target3);
        }

        this.mBmr = bmr;

        //jing 20160929  SDK对内对外区分,是否自己计算BMR下发
        if (AppsDeviceParameters.isUpLoadData == false) {
            this.setUserMessage();
        } else {
            this.b7Ins(mBmr, true);
        }

    }

    /**
     * get alarm number
     * @hide
     */
    public void a6Ins() {
        printMethodInfo("a6Ins");
        byte commandID = (byte) 0xA6;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0xA6, AppsDeviceParameters.Delay_Medium, 0xA6);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get alarm information by id
     * @param ids
     * @hide
     */
    private int[] alarmIds;
    private int alarmIndex = 0;
    public void a7Ins(int[] ids) {
        printMethodInfo("a7Ins", Arrays.toString(ids));
        alarmIndex = 0;
        alarmIds = new int[ids.length];
        // Add by Jeepend 2016.07.28
        // Add to fix get alarm timeout issue.
        System.arraycopy(ids, 0, alarmIds, 0, ids.length);
        byte commandID = (byte) 0xA7;
        byte[] returnCommand = new byte[3];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) (ids[alarmIndex] & 0xFF);
        //4S timer
        startTimeout(0xA7, AppsDeviceParameters.Delay_Medium, 0xA7);
        blecm.packageData(mAddress, returnCommand);
    }

    private void a7Ins(int index) {
        printMethodInfo("a7Ins", index);
        byte commandID = (byte) 0xA7;
        byte[] returnCommand = new byte[3];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) (alarmIds[index] & 0xFF);
        //4S timer
        startTimeout(0xA7, AppsDeviceParameters.Delay_Medium, 0xA7);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * set alarm information by id
     * @param id 
     * @param hour 
     * @param min 
     * @param isRepeat 
     * @param week 
     * @param isOpen 
     * @hide
     */
    public void a8Ins(int id, int hour, int min, boolean isRepeat, byte week, boolean isOpen) {
        printMethodInfo("a8Ins", id, hour, min, isRepeat, week, isOpen);
        byte commandID = (byte) 0xA8;
        byte bId = (byte) (id & 0xFF);
        byte bHour = (byte) (hour & 0xFF);
        byte bMin = (byte) (min & 0xFF);
        byte bIsRepeat = (byte) (0x01);
        if (isRepeat)
            bIsRepeat = (byte) (0x01);
        else
            bIsRepeat = (byte) (0x00);
        byte bIsOpen = (byte) (0x01);
        if (isOpen)
            bIsOpen = (byte) (0x01);
        else
            bIsOpen = (byte) (0x00);
        byte[] returnCommand = new byte[8];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = bId;
        returnCommand[3] = bHour;
        returnCommand[4] = bMin;
        returnCommand[5] = bIsRepeat;
        returnCommand[6] = week;
        returnCommand[7] = bIsOpen;
        //4S timer
        startTimeout(0xA8, AppsDeviceParameters.Delay_Medium, 0xA8);
        blecm.packageData(mAddress, returnCommand);

    }
    
    /**
     * delete alarm by id
     * @param id 
     * @hide
     */
    public void a9Ins(int id) {
        printMethodInfo("a9Ins", id);
        byte commandID = (byte) 0xA9;
        byte[] returnCommand = new byte[3];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) (id & 0xFF);
        //4S timer
        startTimeout(0xA9, AppsDeviceParameters.Delay_Medium, 0xA9);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * set remind
     * @param hour 
     * @param minute 
     * @param isOpen 
     * @hide
     */
    public void aaIns(int hour, int minute, boolean isOpen) {
        printMethodInfo("aaIns", hour, minute, isOpen);
        byte commandID = (byte) 0xaa;
        byte[] returnCommand = new byte[5];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) hour;
        returnCommand[3] = (byte) minute;
        if (isOpen)
            returnCommand[4] = (byte) (0x01);
        else
            returnCommand[4] = (byte) (0x00);
        //4S timer
        startTimeout(0xaa, AppsDeviceParameters.Delay_Medium, 0xaa);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * sync activity data
     * @hide
     */
    public void acIns() {
        printMethodInfo("acIns");
        byte commandID = (byte) 0xac;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0xac, AppsDeviceParameters.Delay_Medium, 0xab);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * sync sleep data
     * @hide
     */
    public void b0Ins() {
        printMethodInfo("b0Ins");
        byte commandID = (byte) 0xb0;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0xb0, AppsDeviceParameters.Delay_Medium, 0xaf);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * sync stage report data
     * @hide
     */
    public void x0aIns() {
        printMethodInfo("x0aIns");
        byte commandID = (byte) 0x0a;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0x0a, AppsDeviceParameters.Delay_Medium, 0x0b);
        blecm.packageData(mAddress, returnCommand);
    }
    
    /**
     * set remind
     * @param mins 
     * @param isOpen 
     * @hide
     */
    public void aaIns(int mins, boolean isOpen) {
        printMethodInfo("aaIns", mins, isOpen);
        byte commandID = (byte) 0xaa;
        byte[] returnCommand = new byte[5];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) ((mins&0xFFFF)/60);
        returnCommand[3] = (byte) ((mins&0xFFFF)%60);
        if (isOpen) {
            returnCommand[4] = 1;
        } else {
            returnCommand[4] = 0;
        }
        //4S timer
        startTimeout(0xaa, AppsDeviceParameters.Delay_Medium, 0xaa);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get remind
     * @hide
     */
    public void b3Ins() {
        printMethodInfo("b3Ins");
        byte commandID = (byte) 0xb3;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0xb3, AppsDeviceParameters.Delay_Medium, 0xb3);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get device state and battery
     * @hide
     */
    public void b4Ins() { // 查询电池电量
        printMethodInfo("b4Ins");
        byte commandID = (byte) 0xb4;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0xb4, AppsDeviceParameters.Delay_Medium, 0xb4);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * set device mode
     * @param state mode{activity mode：0x00, sleep mode：0x01, workout mode: 0x02, swim mode: 0x03, flight mode: 0x04}
     * @hide
     */
    
    public void b5Ins(int state) {
        printMethodInfo("b5Ins", state);
        byte commandID = (byte) 0xB5;
        byte[] returnCommand = new byte[3];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) (state & 0xFF);
        blecm.packageData(mAddress, returnCommand);
    }


    public void b5Ins(int state, int minute) {
        printMethodInfo("b5Ins", state, minute);
        byte commandID = (byte) 0xB5;
        byte[] returnCommand = new byte[5];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) (state & 0xFF);
        returnCommand[3] = (byte) ((minute & 0xFF) /60);
        returnCommand[4] = (byte) ((minute & 0xFF) %60);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get proflie
     * @hide
     */
    public void b6Ins() {
        printMethodInfo("b6Ins");
        byte commandID = (byte) 0xB6;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0xB6, AppsDeviceParameters.Delay_Medium, 0xB6);
        blecm.packageData(mAddress, returnCommand);
    }


    /**
     * set BMR
     * @param bmr
     * @hide
     */
    public void b7Ins(int bmr) {
        printMethodInfo("b7Ins", bmr);
        b7Ins(bmr, false);
    }

    /**
     * Internal set BMR method.
     * @param bmr User's BMR
     * @param needSetUserInfo Whether should set user's info after set BMR successfully.
     */
    private void b7Ins(int bmr, boolean needSetUserInfo) {
        saveBMR(bmr);
        mNeedSetUserInfoAfterSetBMR = needSetUserInfo;
        byte[] bBmr = ByteBufferUtil.intTo2Byte(bmr);
        byte commandID = (byte) 0xB7;
        byte[] returnCommand = new byte[4];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = bBmr[0];
        returnCommand[3] = bBmr[1];
        //4S timer
        startTimeout(0xB7, AppsDeviceParameters.Delay_Medium, 0xB7);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get real activity data
     * @hide
     */
    public void bfIns() {
        printMethodInfo("bfIns");
        byte commandID = (byte) 0xBF;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0xBF, AppsDeviceParameters.Delay_Medium, 0xBF);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get hour mode
     * @hide
     */
    public void x01Ins() {
        printMethodInfo("x01Ins");
        byte commandID = (byte) 0x01;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        //4S timer
        startTimeout(0x01, AppsDeviceParameters.Delay_Medium, 0x01);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * set hour mode
     * @param hour {12hour mode：0x00,24hour mode：0x01}
     * @hide
     */
    public void x02Ins(int hour) {
        printMethodInfo("x02Ins", hour);
        byte commandID = (byte) 0x02;
        byte[] returnCommand = new byte[3];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) hour;
        //4S timer
        startTimeout(0x02, AppsDeviceParameters.Delay_Medium, 0x02);
        blecm.packageData(mAddress, returnCommand);
    }

    private byte[] mRandom;

    /**
     * set random to bound
     * @param random 
     * @hide
     */
    public void x09Ins(Integer[] random) {
        printMethodInfo("x09Ins", Arrays.toString(random));
        byte commandID = (byte) 0x09;
        byte[] returnCommand = new byte[8];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        mRandom = new byte[6];
        for (int i = 0; i < 6; i++) {
            returnCommand[i + 2] = random[i].byteValue();
            mRandom[i] = random[i].byteValue();
        }
        //4S timer
        startTimeout(0x09, AppsDeviceParameters.Delay_Medium, 0x09);
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * get picture
     * @hide
     */
    public void x10Ins() {
        printMethodInfo("x10Ins");
        byte commandID = (byte) (0x10 & 0xff);
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

    /**
     * set picture
     * @param which 
     * @hide
     */
    public void x11Ins(int which) {
        printMethodInfo("x11Ins", which);
        byte commandID = (byte) (0x11 & 0xff);
        byte[] returnCommand = new byte[3];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[2] = (byte) (which & 0xff);
        blecm.packageData(mAddress, returnCommand);
    }

    public void xBAIns() {
        printMethodInfo("xBAIns");
        byte commandID = (byte) (0xba & 0xff);
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

    public void xD6Ins(String account, String mac) {
        printMethodInfo("xD6Ins", account, mac);
        byte commandID = (byte) (0xd6 & 0xff);
        byte[] returnCommand = new byte[18];
        returnCommand[0] = (byte) 0xaa;
        returnCommand[1] = commandID;
        byte[] md5 = MD5.md5(account + mac);
        for (int i = 0; i < md5.length; i++) {
            returnCommand[2 + i] = md5[i];
        }
        blecm.packageData(mAddress, returnCommand);
    }

    private void allPkgOk(byte commandID) {
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        blecm.packageData(mAddress, returnCommand);
    }

    private List<byte[]> listStageForm;
    private byte[] stageForms;
    private List<byte[]> listActivity;
    private byte[] activitys;
    private List<byte[]> listSleep;
    private byte[] sleeps;
    private int battery = 0;

    public int getBattery() {
        printMethodInfo("getBattery");
        return battery;
    }

    private enum Command {
        Unknown(0),
        Verification_Feedback(0xfb),
        Verification_Success(0xfd),
        Verification_Failed(0xfe),
        Reset_Success(0xa1),
        GetUserId_Success(0xa2),
        SetUserId_Success(0xa3),
        SyncTime_Success(0xa4),
        SetUserInfo_Success(0xa5),
        GetUserInfo_Success(0xb6),
        GetAlarmNum_Success(0xa6),
        GetAlarmInfo_Success(0xa7),
        SetAlarmInfo_Success(0xa8),
        DeleteAlarm_Success(0xa9),
        GetReminder_Success(0xb3),
        SetReminder_Success(0xaa),
        SetMode_Success(0xb5),
        SyncActivityData_Start(0xab),
        SyncActivityData_Data(0xad),
        SyncActivityData_Finish(0xae),
        SyncSleepData_Start(0xaf),
        SyncSleepData_Data(0xb1),
        SyncSleepData_Finish(0xb2),
        GetDeviceInfo_Success(0xb4),
        SyncStageData_Start(0x0b),
        SyncStageData_Data(0x0c),
        SyncStageData_Finish(0x0d),
        SyncRealTimeData_Success(0xbf),
        SetBMR_Success(0xb7),
        GetSwimParameter_Success(0x05),
        SetSwimParameter_Success(0x06),
        SendRandomNumber_Success(0x09),
        GetPicture_Success(0x10),
        SetPicture_Success(0x11),
        GetHourMode_Success(0x01),
        SetHourMode_Success(0x02);
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
    	JSONObject o = new JSONObject();
        stopTimeout(what);
        switch (command) {
            case Verification_Feedback://(0xfb),
                byte[] req = deciphering(returnData, mType, deviceType);
                startTimeout(0xfc, AppsDeviceParameters.Delay_Medium, 0xfd, 0xfe);
                blecm.packageData(mAddress, req);
                break;
            case Verification_Success://(0xfd),
    			this.mBaseCommCallback.onConnectionStateChange(mAddress, mType, iHealthDevicesManager.DEVICE_STATE_CONNECTED, 0, null);	//添加

                break;
            case Verification_Failed://(0xfe),
                mBaseComm.disconnect();

                break;
            case Reset_Success://(0xa1)
                if (returnData != null) {
					this.dataProcessReset(returnData,o);

                    resetFromCloud ();
				}
                break;
            case GetUserId_Success://(0xa2),
            	if (returnData != null) {
            		this.dataProcessGetUserID(returnData, o);

                    getUserMacFromCloud (o);
				}
                break;
            case SetUserId_Success://(0xa3),

				mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_USERID_SUCCESS_AM, null);

                /* 上云绑定 */
                this.bindAMDevice();
                break;
            case SyncTime_Success://(0xa4),

                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SYNC_TIME_SUCCESS_AM, null);
                break;
            case SetUserInfo_Success://(0xa5),

            	mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_USERINFO_SUCCESS_AM, null);
                break;
            case GetUserInfo_Success://(0xb6),
            	if (returnData != null) {
    				this.dataProcessGetUserInfo(returnData, o);
				}
            	break;
            case GetAlarmNum_Success://(0xa6),
            	if (returnData != null) {
					this.dataProcessGetAlarmNum(returnData, o);
				}
                break;
            case GetAlarmInfo_Success://(0xa7),
            	if (returnData != null) {
					this.dataProcessGetAlarmInfo(returnData, o);
				}
                break;
            case SetAlarmInfo_Success://(0xa8),
				mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_ALARMINFO_SUCCESS_AM, null);
                break;
            case DeleteAlarm_Success://(0xa9),

            	mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_DELETE_ALARM_SUCCESS_AM, null);
                break;
            case GetReminder_Success://(0xb3),
                if (returnData != null) {
                	this.dataProcessGetRemind(returnData, o);
				}
                break;
            case SetReminder_Success://(0xaa),
            	mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_ACTIVITYREMIND_SUCCESS_AM, null);
            	break;
            case SetMode_Success://(0xb5),
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_DEVICE_MODE_AM, null);
                break;
            case SyncActivityData_Start://(0xab),
                allPkgOk((byte) 0xab);
                if (activitys != null) {
                    listActivity.add(activitys);
                    activitys = null;
                }
                activitys = ByteBufferUtil.rejectBuffer(returnData);
                break;
            case SyncActivityData_Finish://(0xae),
                allPkgOk((byte) 0xae);
                if (activitys != null) {
                    listActivity.add(activitys);
                    activitys = null;
                }
                if (listActivity != null) {
                	this.dataProcessGetActivityData(listActivity, o);
				}
                listActivity = null;
                listActivity = new ArrayList<byte[]>();
                break;
            case SyncActivityData_Data://(0xad),
            	allPkgOk((byte) 0xad);
                activitys = ByteBufferUtil.BufferMerger(activitys, returnData);
                break;
                
                
            case SyncSleepData_Start://(0xaf),
                allPkgOk((byte) 0xaf);
                if (sleeps != null) {
                    listSleep.add(sleeps);
                    sleeps = null;
                }
                sleeps = ByteBufferUtil.rejectBuffer(returnData);
                break;
            case SyncSleepData_Finish://(0xb2),
                allPkgOk((byte) 0xb2);
                if (sleeps != null) {
                    listSleep.add(sleeps);
                    sleeps = null;
                }
                if (listSleep != null) {
                	this.dataProcessGetSleepData(listSleep, o);
				}
                listSleep = null;
                listSleep = new ArrayList<byte[]>();
                break;
            case SyncSleepData_Data://(0xb1),
                allPkgOk((byte) 0xb1);
                sleeps = ByteBufferUtil.BufferMerger(sleeps, returnData);
                break;
                
            case GetDeviceInfo_Success://(0xb4),
            	if (returnData != null) {
					this.dataProcessGetBattery(returnData, o);
				}
                break;
                
            case SyncStageData_Start://(0x0b),
                allPkgOk((byte) 0x0b);
                break;
            case SyncStageData_Data://(0x0c),
                allPkgOk((byte) 0x0c);
                stageForms = ByteBufferUtil.BufferMerger(stageForms, returnData);
                listStageForm.add(stageForms);
                break;
            case SyncStageData_Finish://(0x0d),
                allPkgOk((byte) 0x0d);
                this.dataProcessGetStageReportData(listStageForm, o);
                listStageForm = null;
                listStageForm = new ArrayList<byte[]>();
                break;
                
            case SyncRealTimeData_Success://(0xbf),
            	if (returnData != null) {
					this.dataProcessGetRealData(returnData, o);
				}
                break;
            case SetBMR_Success://(0xb7),
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_BMR_SUCCESS_AM, null);
                if (mNeedSetUserInfoAfterSetBMR) {
                    //设置个人信息
                    this.setUserMessage();
                }

                break;
            case GetSwimParameter_Success://(0x05),
                if (returnData != null) {
					this.dataProcessGetSwimInfo(returnData, o);
				}
                break;
            case SetSwimParameter_Success://(0x06),
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_SWIMINFO_AM, null);
                break;
            case SendRandomNumber_Success://(0x09),
                String random = "";
                for (int i = 0; i < mRandom.length; i++) {
                    random += (mRandom[i] + "");
                }
                try {
					o.put(AmProfile.GET_RANDOM_AM, random);
					mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_GET_RANDOM_AM, o.toString());
				} catch (Exception e) {
					// TODO: handle exception
				}
                break;
            case GetPicture_Success://(0x10),
                int which = returnData[0] & 0xff;
                try {
                    o.put(AmProfile.GET_PICTURE_AM, which);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_GET_PICTURE_AM, o.toString());
                break;
            case SetPicture_Success://(0x11),
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_PICTURE_SUCCESS_AM, null);
                break;
            case GetHourMode_Success://(0x01),
                int hourMode = returnData[0] & 0xff;
                try {
                    o.put(AmProfile.GET_HOUR_MODE_AM, hourMode);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_GET_HOUR_MODE_AM, o.toString());
                break;

            case SetHourMode_Success://(0x02);
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SET_HOUR_MODE_SUCCESS_AM, null);
                break;

            default:
                break;
        }
    }


    /**
     * stage report data up to cloud
     * @param o
     */
    private void upStageReportData2Cloud(JSONObject o) {
    	if (o != null) {
    		Method.parseStageDataJson(mContext, mUserName, mAddress, mType, o);
		}
	}

	/**
     * sleep data up to cloud
     * @param o
     */
    private void upSleepData2Cloud(JSONObject o) {
    	if (o != null) {
    		Method.parseSleepDataJson(mContext, mUserName, mAddress, mType, o);
		}
	}

	/**
	 * activity data up to cloud
	 * @param o
	 */
    private void upActivityData2Cloud(JSONObject o) {
    	if (o != null) {
    		Method.parseActivityDataJson(mContext, mUserName, mAddress, mType, o);
		}
	}
    
	/**
     * get stage report data
     * @param lsf
     * @param o
     */
    private void dataProcessGetStageReportData(List<byte[]> lsf,
			final JSONObject o) {
        //当前固件版本
//        String AccessoryFirmwareVersion = iHealthDevicesManager.getInstance().getIdps(mAddress).getAccessoryFirmwareVersion();
        //如果是AM4，且固件版本小于134,直接返回
        if (mType.equals(iHealthDevicesManager.TYPE_AM4)&&currentFirmwareVersion < RightFirmwareVersion) {
            try {
                o.put(AmProfile.SYNC_STAGE_DATA_AM, convertStageForms(new byte[]{}));
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SYNC_STAGE_DATA_AM, o.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }

        if(lsf != null && lsf.size() > 0) {
            try {
                o.put(AmProfile.SYNC_STAGE_DATA_AM, convertStageForms(lsf.get(lsf.size()-1)));
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SYNC_STAGE_DATA_AM, o.toString());

                if (AppsDeviceParameters.isUpLoadData) {
                    //up cloud
                    DataThreadPoolManager.getInstance().addExecuteTask(new Runnable() {

                        @Override
                        public void run() {
                            upStageReportData2Cloud(o);
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            try {
                o.put(AmProfile.SYNC_STAGE_DATA_AM, convertStageForms(new byte[]{}));
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SYNC_STAGE_DATA_AM, o.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
	}

    /**
     * get sleep data
     * @param ls
     * @param o
     */
	private void dataProcessGetSleepData(List<byte[]> ls, 
			final JSONObject o) {
    	try {
			o.put(AmProfile.SYNC_SLEEP_DATA_AM, convertSleep(ls));
			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SYNC_SLEEP_DATA_AM, o.toString());

            if (AppsDeviceParameters.isUpLoadData) {
                //up cloud
                DataThreadPoolManager.getInstance().addExecuteTask(new Runnable() {

                    @Override
                    public void run() {
                        upSleepData2Cloud(o);
                    }
                });
            }

		} catch (Exception e) {
            e.printStackTrace();
		}
	}
	
	/**
	 * get activity data
	 * @param la
	 * @param o
	 */
	private void dataProcessGetActivityData(List<byte[]> la,
			final JSONObject o) {
    	try {
			o.put(AmProfile.SYNC_ACTIVITY_DATA_AM, convertActivity(la));
			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SYNC_ACTIVITY_DATA_AM, o.toString());

            if (AppsDeviceParameters.isUpLoadData) {
                //up cloud
                DataThreadPoolManager.getInstance().addExecuteTask(new Runnable() {

                    @Override
                    public void run() {
                        upActivityData2Cloud(o);
                    }
                });
            }

		} catch (Exception e) {
            e.printStackTrace();
		}
	}

	/**
     * get real activity data
     * @param returnData
     * @param o
     */
    private void dataProcessGetRealData(byte[] returnData, JSONObject o) {
    	int tempRealStep = 0;
		int tempRealCal = 0;
//   	 	String AccessoryFirmwareVersion = iHealthDevicesManager.getInstance().getIdps(mAddress).getAccessoryFirmwareVersion();
    	if(mType.equals(iHealthDevicesManager.TYPE_AM3S) && currentFirmwareVersion >= Integer
                .parseInt(UP110FirmwareVersion)){
    		tempRealStep = (int)(returnData[0]&0xFF) * 256 * 256 * 256 
    				+ (int)(returnData[1]&0xFF) * 256 * 256
    				+ (int)(returnData[2]&0xFF) * 256 
			        + (int)(returnData[3]&0xFF);
    		tempRealCal = (int)(returnData[4]&0xFF)*256 + (int)(returnData[5]&0xFF);
    	} else if (mType.equals(iHealthDevicesManager.TYPE_AM4)) {
            tempRealStep = (int)(returnData[0]&0xFF) * 256 * 256 * 256
                    + (int)(returnData[1]&0xFF) * 256 * 256
                    + (int)(returnData[2]&0xFF) * 256
                    + (int)(returnData[3]&0xFF);
            tempRealCal = (int)(returnData[4]&0xFF)*256 + (int)(returnData[5]&0xFF);
        } else {
    		tempRealStep = (int)(returnData[0]&0xFF)*256 + (int)(returnData[1]&0xFF);
    		tempRealCal = (int)(returnData[2]&0xFF)*256 + (int)(returnData[3]&0xFF);
    	}
		
		try {
			o.put(AmProfile.SYNC_REAL_STEP_AM, tempRealStep);
			o.put(AmProfile.SYNC_REAL_CALORIE_AM, tempRealCal);
            o.put(AmProfile.SYNC_REAL_TOTALCALORIE_AM, (tempRealCal+getCurrentBMR()));
			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_SYNC_REAL_DATA_AM, o.toString());
		} catch (Exception e) {
            e.printStackTrace();
		}
	}

	/**
     * swimming information
     * @param returnData
     * @param o 
     */
    private void dataProcessGetSwimInfo(byte[] returnData, JSONObject o) {
        int swimSwitch = returnData[0]&0xff;        //游泳开关
    	int swimLaneLength = returnData[1]&0xff;    //泳池长度
        int hour = returnData[2] & 0xff;        //小时
        int min = returnData[3] & 0xff;         //分钟
        int unit = returnData[4] & 0xff;        //单位

		try {
            o.put(AmProfile.GET_SWIM_SWITCH_AM, swimSwitch);
			o.put(AmProfile.GET_SWIMLANE_LENGTH_AM, swimLaneLength);
            o.put(AmProfile.GET_SWIM_CUTOUT_HOUR_AM, hour);
            o.put(AmProfile.GET_SWIM_CUTOUT_MINUTE_AM, min);
            o.put(AmProfile.GET_SWIM_UNIT_AM, unit);

			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_GET_SWIMINFO_AM, o.toString());
		} catch (Exception e) {
            e.printStackTrace();
		}
	}

	/**
     * am state and battery
     * @param returnData
     * @param o
     */
    private void dataProcessGetBattery(byte[] returnData, JSONObject o) {
    	int state = returnData[0]&0xff;
		int battery = returnData[1]&0xff;
		try {
			o.put(AmProfile.QUERY_STATE_AM, state);
			o.put(AmProfile.QUERY_BATTERY_AM, battery);
			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_QUERY_STATE_AM, o.toString());
		} catch (Exception e) {
            e.printStackTrace();
		}
	}

	/**
     * remind
     * @param returnData
     * @param o
     */
    private void dataProcessGetRemind(byte[] returnData, JSONObject o) {
//    	int timeLength = ByteBufferUtil.byte2ToInt(returnData);	//时间长度
        int hour = 0;
        int time = 0;
        if (returnData[1] > 59) {
            int minitue = (returnData[0]&0xff)*256 + (returnData[1]&0xff);
            hour = (byte)(minitue/60);
            time = (byte)(minitue%60);
        }
        else {
            hour = returnData[0];
            time = returnData[1];
        }
        String timeStr = String.format("%02d:%02d", hour, time);
    	int isOn = returnData[2]&0xff;	//1->开，0->关
    	try {
			o.put(AmProfile.GET_ACTIVITY_REMIND_TIME_AM, timeStr);
            if(isOn == 0){
                o.put(AmProfile.GET_ACTIVITY_REMIND_ISON_AM, false);
            }else{
                o.put(AmProfile.GET_ACTIVITY_REMIND_ISON_AM, true);
            }
			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_GET_ACTIVITY_REMIND_AM, o.toString());
		} catch (Exception e) {
            e.printStackTrace();
		}
	}

    private JSONArray alarmInfos;
	/**
     * alarm information
     * @param returnData
     * @param o
     */
    private void dataProcessGetAlarmInfo(byte[] returnData, JSONObject o) {
        try {
            if (alarmIndex == 0) {
                alarmInfos = new JSONArray();
            }
            //ID
            int alarmID = returnData[0] & 0xff;    //1、2、3
            if (alarmID != 0) {
                // The alarm exist.
                //hour
                int hour = returnData[1] & 0xff;
                //min
                int min = returnData[2] & 0xff;
                //is repeat or not
                int isRepeat = returnData[3] & 0xff;    //1->重复，0->否
                //week
                JSONObject tempObject = new JSONObject();
                int week = returnData[4];
                if ((int) (week & 0x01) == 0) {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_SUNDAY_AM, false);
                } else {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_SUNDAY_AM, true);
                }
                if ((int) (week & 0x02) == 0) {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_MONDAY_AM, false);
                } else {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_MONDAY_AM, true);
                }
                if ((int) (week & 0x04) == 0) {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_TUESDAY_AM, false);
                } else {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_TUESDAY_AM, true);
                }
                if ((int) (week & 0x08) == 0) {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_WEDNESDAY_AM, false);
                } else {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_WEDNESDAY_AM, true);
                }
                if ((int) (week & 0x10) == 0) {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_THURSDAY_AM, false);
                } else {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_THURSDAY_AM, true);
                }
                if ((int) (week & 0x20) == 0) {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_FRIDAY_AM, false);
                } else {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_FRIDAY_AM, true);
                }
                if ((int) (week & 0x40) == 0) {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_SATURDAY_AM, false);
                } else {
                    tempObject.put(AmProfile.GET_ALARM_WEEK_SATURDAY_AM, true);
                }
                //is open or not
                int isOn = returnData[5] & 0xff;    //关->0,开->1
                o.put(AmProfile.GET_ALARM_ID_AM, alarmID);
                // time format: HH:mm
                String time = String.format("%02d:%02d", hour, min);
                o.put(AmProfile.GET_ALARM_TIME_AM, time);

                if (isRepeat == 1) {
                    o.put(AmProfile.GET_ALARM_ISREPEAT_AM, true);
                } else {
                    o.put(AmProfile.GET_ALARM_ISREPEAT_AM, false);
                }

                o.put(AmProfile.GET_ALARM_WEEK_AM, tempObject);

                if (isOn == 0) {
                    o.put(AmProfile.GET_ALARM_ISON_AM, false);
                } else {
                    o.put(AmProfile.GET_ALARM_ISON_AM, true);
                }
                alarmInfos.put(o);
            }
            alarmIndex += 1;
            if (alarmIndex >= alarmIds.length) {
                JSONObject json = new JSONObject();
                json.put(AmProfile.GET_ALARM_CLOCK_DETAIL, alarmInfos);
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_GET_ALARMINFO_AM, json.toString());
            } else {
                a7Ins(alarmIndex);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

	/**
     * alarm num
     * @param returnData
     * @param o
     */
    private void dataProcessGetAlarmNum(byte[] returnData, JSONObject o) {
    	int alarmNum = returnData[0]&0xff;
        JSONArray arrayId = new JSONArray();
        for(int i = 1; i < returnData.length; i++){
            arrayId.put((int)returnData[i]);
        }
		try {
			o.put(AmProfile.GET_ALARMNUM_AM, alarmNum);
            o.put(AmProfile.GET_ALARMNUM_ID_AM, arrayId);
			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_GET_ALARMNUM_AM, o.toString());
		} catch (Exception e) {
            e.printStackTrace();
		}
	}

	/**
     * user info
     * @param returnData
     * @param o
     */
    private void dataProcessGetUserInfo(byte[] returnData, JSONObject o) {
        int age = returnData[0]&0xff;
        int step = returnData[1]&0xff;
        int height = returnData[2]&0xff;
        int sex = returnData[3] & 0xff;
        String w = String.valueOf(returnData[4]&0xff) + "." + String.valueOf(returnData[5]&0xff);
        double weight = 0.0;
        weight = Double.valueOf(w.toString());

        int weightUnit = returnData[6]&0xff;
        int target1 = 0;
        int target2 = 0;
        int target3 = 0;
        int swimTargetHour = 0;
        int swimTargetMin = 0;
        //判断步数是否是四字节
//        String AccessoryFirmwareVersion = iHealthDevicesManager.getInstance().getIdps(mAddress).getAccessoryFirmwareVersion();
        if(mType.equals(iHealthDevicesManager.TYPE_AM3S) && currentFirmwareVersion >= Integer
                .parseInt(UP110FirmwareVersion)){
            target1 = (returnData[7]&0xff) * 256 * 256 * 256 + (returnData[8]&0xff) * 256 * 256 +
                    (returnData[9]&0xff) * 256 + (returnData[10]&0xff);
            target2 = (returnData[11]&0xff) * 256 * 256 * 256 + (returnData[12]&0xff) * 256 * 256 +
                    (returnData[13]&0xff) * 256 + (returnData[14]&0xff);
            target3 = (returnData[15]&0xff) * 256 * 256 * 256 + (returnData[16]&0xff) * 256 * 256 +
                    (returnData[17]&0xff) * 256 + (returnData[18]&0xff);
        } else if (mType.equals(iHealthDevicesManager.TYPE_AM4)) {
            target1 = (returnData[7]&0xff) * 256 * 256 * 256 + (returnData[8]&0xff) * 256 * 256 +
                    (returnData[9]&0xff) * 256 + (returnData[10]&0xff);
            target2 = (returnData[11]&0xff) * 256 * 256 * 256 + (returnData[12]&0xff) * 256 * 256 +
                    (returnData[13]&0xff) * 256 + (returnData[14]&0xff);
            target3 = (returnData[15]&0xff) * 256 * 256 * 256 + (returnData[16]&0xff) * 256 * 256 +
                    (returnData[17]&0xff) * 256 + (returnData[18]&0xff);
        } else {
            target1 = (returnData[7]&0xff) * 256 + (returnData[8]&0xff);
            target2 = (returnData[9]&0xff) * 256 + (returnData[10]&0xff);
            target3 = (returnData[11]&0xff) * 256 + (returnData[12]&0xff);
        }

        if (mType.equals(iHealthDevicesManager.TYPE_AM3)||mType.equals(iHealthDevicesManager.TYPE_AM3S)||(mType.equals(iHealthDevicesManager.TYPE_AM4)
                && currentFirmwareVersion < SwimTargetFirmwareVersion)) {
        } else {
            swimTargetHour = returnData[19] & 0xff;
            swimTargetMin = (returnData[20] & 0xff) + swimTargetHour * 60;
        }

        try {
            o.put(AmProfile.GET_USER_AGE_AM, age);
            o.put(AmProfile.GET_USER_STEP_AM, step);
            o.put(AmProfile.GET_USER_HEIGHT_AM, height);
            o.put(AmProfile.GET_USER_SEX_AM, sex);
            o.put(AmProfile.GET_USER_WEIGHT_AM, weight);
            o.put(AmProfile.GET_USER_UNIT_AM, weightUnit);
            o.put(AmProfile.GET_USER_TARGET1_AM, target1);
            o.put(AmProfile.GET_USER_TARGET2_AM, target2);
            o.put(AmProfile.GET_USER_TARGET3_AM, target3);
            if (mType.equals(iHealthDevicesManager.TYPE_AM3)||mType.equals(iHealthDevicesManager.TYPE_AM3S)||(mType.equals(iHealthDevicesManager.TYPE_AM4)
                    && currentFirmwareVersion < SwimTargetFirmwareVersion)) {

            } else {
                o.put(AmProfile.GET_USER_SWIMTARGET_AM, swimTargetMin);
            }

            mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_GET_USERINFO_AM, o.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }


	}

	/**
     * ueser ID
     * @param returnData
     * @param o
     */
    private void dataProcessGetUserID(byte[] returnData, JSONObject o) {
    	int userId = ByteBufferUtil.byteToUserId(returnData);
		try {
			o.put(AmProfile.USERID_AM, userId);
			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_USERID_AM, o.toString());
		} catch (Exception e) {
            e.printStackTrace();
		}
	}

	/**
	 * result of reset
	 * @param returnData
	 * @param o JsonObject
	 */
    private void dataProcessReset(byte[] returnData, JSONObject o) {
    	int confirmFlag = returnData[0]&0xff;
		try {
			o.put(AmProfile.RESET_AM, confirmFlag);
			mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_RESET_AM, o.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
    
    
    
    private boolean is4Bytes = false;
    /**
     * convert activity data to Json 
     * @param datas
     * @return
     */
    private JSONArray convertActivity(List<byte[]> datas){
    	JSONArray list = new JSONArray();
//    	 String AccessoryFirmwareVersion = iHealthDevicesManager.getInstance().getIdps(mAddress).getAccessoryFirmwareVersion();
         int fiveMinsStepLen = 6; 
         if (mType.equals(iHealthDevicesManager.TYPE_AM3S) && currentFirmwareVersion >= Integer
                 .parseInt(UP110FirmwareVersion)) {
             is4Bytes = true;
             fiveMinsStepLen = 8;
         } else if (mType.equals(iHealthDevicesManager.TYPE_AM4)) {
             is4Bytes = true;
             fiveMinsStepLen = 8;
		 } else {
             is4Bytes = false;
         }
    	for (int i = 0; i < datas.size(); i++) {
			byte[] data = datas.get(i);
			JSONObject object = new JSONObject();
    		JSONArray array = new JSONArray();
    		JSONObject stoneObject;
    		try {
				int year = (data[0] & 0xff) + 2000;
    			int month = data[1] & 0xff;
    			int day = data[2] & 0xff;
    			int stepLength = data[3] & 0xff;

    			for(int j = 6; j < data.length; j += fiveMinsStepLen){
    				stoneObject = new JSONObject();
        			int hour = data[j] & 0xff;
        			int min = data[j + 1] & 0xff;
                    // Format: yyyy-MM-dd HH:mm:ss
        			String strTime = String.format("%d-%02d-%02d %02d:%02d:00", year, month, day, hour, min);
    				stoneObject.put(AmProfile.SYNC_ACTIVITY_DATA_TIME_AM, strTime);
    				stoneObject.put(AmProfile.SYNC_ACTIVITY_DATA_STEP_LENGTH_AM, stepLength);
    				int step,calorie;
    				if(is4Bytes){
    					step = (int)(data[j + 2]&0xFF) * 256 * 256 * 256
    							+ (int)(data[j + 3]&0xFF) * 256 * 256
    							+ (int)(data[j + 4]&0xFF) * 256
    							+ (int)(data[j + 5]&0xFF);
            			stoneObject.put(AmProfile.SYNC_ACTIVITY_DATA_STEP_AM, step);
            			calorie = (int)(data[j + 6]&0xFF)*256 + (int)(data[j + 7]&0xFF);
            			stoneObject.put(AmProfile.SYNC_ACTIVITY_DATA_CALORIE_AM, calorie);
    				}else{
    					step = (int)(data[j + 2]&0xFF)*256 + (int)(data[j + 3]&0xFF);
            			stoneObject.put(AmProfile.SYNC_ACTIVITY_DATA_STEP_AM, step);
            			calorie = (int)(data[j + 4]&0xFF)*256 + (int)(data[j + 5]&0xFF);
            			stoneObject.put(AmProfile.SYNC_ACTIVITY_DATA_CALORIE_AM, calorie);
    				}
                    stoneObject.put(AmProfile.DATAID, MD5.md5String(PublicMethod.getAMDataID(mAddress, step + "", PublicMethod.String2TS(strTime))));  //mac + 结束时间 + 总步数
        			array.put(stoneObject);
    			}		
    			object.putOpt(AmProfile.SYNC_ACTIVITY_EACH_DATA_AM, array);
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    		
    		list.put(object);
		}
    	
		return list;
    }
    
    /**
     * convert sleep data to Json
     * @param datas
     * @return
     */
    private JSONArray convertSleep(List<byte[]> datas){
    	JSONArray list = new JSONArray();
    	long off = 5 * 60; // 5分钟间隔
    	for (int i = 0; i < datas.size(); i++) {
			byte[] data = datas.get(i);
    		JSONArray array = new JSONArray();
    		JSONObject stoneObject = null;
    		try {
				String year = String.valueOf((data[0] & 0xff) + 2000);
    			String month = String.valueOf(data[1] & 0xff);
    			String day = String.valueOf(data[2] & 0xff);
    			String hour = String.valueOf(data[3] & 0xff);
    			if (Integer.parseInt(hour)<10) {
					hour = "0"+hour;
				}
    			String mins = String.valueOf(data[4] & 0xff);
    			if (Integer.parseInt(mins)<10) {
    				mins = "0"+mins;
				}
    			String sed = String.valueOf(data[5] & 0xff);
    			if (Integer.parseInt(sed)<10) {
    				sed = "0"+sed;
				}
    			String strTime = year + "-" + month + "-" + day + " " + hour + ":" + mins + ":" + sed;
    			long startTimeTs = PublicMethod.String2TS(strTime);
    			for(int j = 8; j < data.length; j++){
    				stoneObject = new JSONObject();
    				String str = PublicMethod.TS2String(startTimeTs);
    				stoneObject.put(AmProfile.SYNC_SLEEP_DATA_TIME_AM, str);
        			stoneObject.put(AmProfile.SYNC_SLEEP_DATA_LEVEL_AM, (int)(data[j]&0xFF) + "");
                    stoneObject.put(AmProfile.DATAID, MD5.md5String(PublicMethod.getAMDataID(mAddress, (data[j] & 0xFF) + "", PublicMethod.String2TS(str))));  //mac + 结束时间 + level
        			startTimeTs += off;
        			array.put(stoneObject);
    			}
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(AmProfile.SYNC_SLEEP_EACH_DATA_AM, array);
                list.put(jsonObject);
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
		}
		return list;
    }
    
    /**
     * check stage data is swim/workout/sleep
     * @param datas
     * @return
     */
    private JSONArray convertStageForms(byte[] datas){
        byte[] temp;
        JSONArray list = new JSONArray();
        int stageDataLength = 21;
        if (mType.equals(iHealthDevicesManager.TYPE_AM3S)) {
            stageDataLength = 17;
        }
        for (int i = 0; i < datas.length; i+=stageDataLength) {
            temp = new byte[stageDataLength];
            for (int j = 0; j < stageDataLength; j++) {
                temp[j] = datas[i + j];
            }
            switch (temp[0]&0xff) {
                case 0:
                    list.put(convertStageFormsSwim(temp));
                    break;
                case 1:
                    list.put(convertStageFormsWorkOut(temp));
                    break;
                case 2:
                    list.put(convertStageFormsSleep(temp));
                    break;
                case 3:
                    list.put(convertPagesNum(temp));
                    break;
                default:
                    break;
            }
        }
		return list;
    }
    
    /**
     * convert workout data
     * @param data
     * @return
     */
    private JSONObject convertStageFormsWorkOut(byte[] data){
    	JSONObject object = new JSONObject();
		try {
			int year = (data[1] & 0xff) + 2000;
			int month = data[2] & 0xff;
			int day = data[3] & 0xff;
			int hour = data[4] & 0xff;
			int mins = data[5] & 0xff;
            String strTime = String.format("%d-%02d-%02d %02d:%02d:00", year, month, day, hour, mins);

			int timeStr = (data[6]&0xff) * 256 + (data[7]&0xff);

            int step;
            String distance = null;
            int calorie;

            if (mType.equals(iHealthDevicesManager.TYPE_AM3S) && currentFirmwareVersion >= Integer
                    .parseInt(UP110FirmwareVersion)) {
                is4Bytes = true;
            } else if (mType.equals(iHealthDevicesManager.TYPE_AM4)) {
                is4Bytes = true;
            } else {
                is4Bytes = false;
            }
            if (is4Bytes) {
                step = (data[8]&0xff) * 256 * 256 * 256 + (data[9]&0xff) * 256 * 256 +
                        (data[10]&0xff) * 256 + (data[11]&0xff);
                distance = String.valueOf(data[12]&0xff) + "." + String.valueOf(data[13]&0xff);
                calorie = (data[14]&0xff) * 256 + (data[15]&0xff);
            }else {
                step = (data[8]&0xff) * 256 + (data[9]&0xff);
                distance = String.valueOf(data[10]&0xff) + "." + String.valueOf(data[11]&0xff);
                calorie = (data[12]&0xff) * 256 + (data[13]&0xff);
            }
			
			object.put(AmProfile.SYNC_STAGE_DATA_TYPE_AM, AmProfile.SYNC_STAGE_DATA_TYPE_WORKOUT_AM);
			object.put(AmProfile.SYNC_STAGE_DATA_STOP_TIME_AM, strTime);
			object.put(AmProfile.SYNC_STAGE_DATA_USED_TIME_AM, timeStr);
			object.put(AmProfile.SYNC_STAGE_DATA_WORKOUT_STEP_AM, step);
			object.put(AmProfile.SYNC_STAGE_DATA_DISTANCE_AM, distance);
			object.put(AmProfile.SYNC_STAGE_DATA_CALORIE_AM, calorie);

            object.put(AmProfile.DATAID, MD5.md5String(PublicMethod.getAMDataID(mAddress, calorie+"", PublicMethod.String2TS(strTime)))); //mac + 结束时间 + 卡路里
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return object;
    }
   
    /**
     * convert stage sleep data
     * @param data
     * @return
     */
    private JSONObject convertStageFormsSleep(byte[] data){
    	JSONObject object = new JSONObject();
		try {
            int year = (data[1] & 0xff) + 2000;
            int month = data[2] & 0xff;
            int day = data[3] & 0xff;
            int hour = data[4] & 0xff;
            int min = data[5] & 0xff;
            String strTime = String.format("%d-%02d-%02d %02d:%02d:00", year, month, day, hour, min);
			int timeStr = (data[6]&0xff) * 256 + (data[7]&0xff);
			int efficiency = (data[8]&0xff) * 256 + (data[9]&0xff);
            int is50Min = data[10] & 0xff;

			object.put(AmProfile.SYNC_STAGE_DATA_TYPE_AM, AmProfile.SYNC_STAGE_DATA_TYPE_SLEEP_AM);
			object.put(AmProfile.SYNC_STAGE_DATA_STOP_TIME_AM, strTime);
			object.put(AmProfile.SYNC_STAGE_DATA_USED_TIME_AM, timeStr);
			object.put(AmProfile.SYNC_STAGE_DATA_SLEEP_EFFICIENCY_AM, efficiency / 10.0);
            object.put(AmProfile.SYNC_STAGE_DATA_SLEEP_IS50MIN_AM, is50Min);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return object;
    }
    
    /**
     * convert stage swim data
     * @param data
     * @return
     */
    private JSONObject convertStageFormsSwim(byte[] data){
    	JSONObject object = new JSONObject();
		try {
            int year = (data[1] & 0xff) + 2000;
            int month = data[2] & 0xff;
            int day = data[3] & 0xff;
            int hour = data[4] & 0xff;
            int min = data[5] & 0xff;
            int sec = data[6] & 0xff;
            String strTime = String.format("%d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, min, sec);
			
			int usedTime = (data[7]&0xff) * 256 + (data[8]&0xff);	//用时
			int pullTimes = (data[12]&0xff) * 256 + (data[13]&0xff);	//划水次数
			int calorie = (data[14]&0xff) * 256 + (data[15]&0xff);	//卡路里
			int swimmingStroke = data[11]&0xff;						//泳姿
			int numberOfTurns = (data[9]&0xff);						//程数
			int poolLength = data[10] & 0xff;						//泳池长度

            int swim_CutInTimeDif = (int) (data[16] & 0xff) * 256 + (int) (data[17] & 0xff);
            int swim_CutOutTimeDif = (int) (data[18] & 0xff) * 256 + (int) (data[19] & 0xff);
            int swim_ProcessFlag = (int) (data[20] & 0xff);

			object.put(AmProfile.SYNC_STAGE_DATA_TYPE_AM, AmProfile.SYNC_STAGE_DATA_TYPE_SWIM_AM);
			object.put(AmProfile.SYNC_STAGE_DATA_STOP_TIME_AM, strTime);
			object.put(AmProfile.SYNC_STAGE_DATA_USED_TIME_AM, usedTime);
			object.put(AmProfile.SYNC_STAGE_DATA_SWIM_PULL_TIMES_AM, pullTimes);
			object.put(AmProfile.SYNC_STAGE_DATA_CALORIE_AM, calorie);
			object.put(AmProfile.SYNC_STAGE_DATA_SWIM_STROKE_AM, swimmingStroke);
			object.put(AmProfile.SYNC_STAGE_DATA_SWIM_TURNS_AM, numberOfTurns);
			object.put(AmProfile.SYNC_STAGE_DATA_SWIMPOOL_LENGTH_AM, poolLength);

            object.put(AmProfile.SYNC_STAGE_DATA_SWIM_CUTINDIF_AM, swim_CutInTimeDif);
            object.put(AmProfile.SYNC_STAGE_DATA_SWIM_CUTOUTDIF_AM, swim_CutOutTimeDif);
            object.put(AmProfile.SYNC_STAGE_DATA_SWIM_PROCESSFLAG_AM, swim_ProcessFlag);

            //结束时间
            long endTime = PublicMethod.String2TS(strTime);
            long startTime = endTime - usedTime;
            String dataID = mAddress+startTime+endTime;
            object.put(AmProfile.DATAID, MD5.md5String(dataID));   //mac+start+end
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return object;
    }

	private void syncTime() {
        Calendar calenda = Calendar.getInstance();
        calenda.setTimeZone(TimeZone.getDefault());
        int year = calenda.get(Calendar.YEAR) - 2000;
        int month = calenda.get(Calendar.MONTH) + 1;
        int day = calenda.get(Calendar.DAY_OF_MONTH);
        int week = calenda.get(Calendar.DAY_OF_WEEK);
        int hour = calenda.get(Calendar.HOUR_OF_DAY);
        int min = calenda.get(Calendar.MINUTE);
        int sed = calenda.get(Calendar.SECOND);
        a4Ins(year, month, day, hour, min, sed, week);
    }

    private void saveBMR(int bmr) {
        SharedPreferences bmrPreferences = mContext.getSharedPreferences("BMR_Content", mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = bmrPreferences.edit();
        editor.putString("current_bmr", String.valueOf(bmr));
        editor.commit();
    }

    private int getCurrentBMR() {
        SharedPreferences bmrPreferences = mContext.getSharedPreferences("BMR_Content", mContext.MODE_PRIVATE);
        String bmr = bmrPreferences.getString("current_bmr", "");
        if (bmr == null || bmr =="") {
            bmr = String.valueOf(mBmr);
        }
        int currentBMR = Integer.parseInt(bmr);
        if (String.valueOf(currentBMR) == null || String.valueOf(currentBMR) == "") {
            currentBMR = 0;
        }
        long CurrentDate = System.currentTimeMillis() / 1000;
        int hour = Integer.parseInt(PublicMethod.TS2String(CurrentDate).split(" ")[1].split(":")[0]);
        int minute = Integer.parseInt(PublicMethod.TS2String(CurrentDate).split(" ")[1].split(":")[1]);
        int second = Integer.parseInt(PublicMethod.TS2String(CurrentDate).split(" ")[1].split(":")[2]);
        currentBMR = (int) (((float) ((float) currentBMR / (float) (24 * 60 * 60))) * (float) ((hour * 60 * 60) + (minute * 60) + second));
        return currentBMR;
    }

    /**
     * 访问每一页的次数
     * @param data
     */
    private JSONObject convertPagesNum(byte[] data) {
        JSONObject object = new JSONObject();
        try {
            int year = (data[1] & 0xff) + 2000;
            int month = data[2] & 0xff;
            int day = data[3] & 0xff;
            String formatDateString = String.format("%d-%02d-%02d", year, month, day);
            int stepPage;
            int distancePage;
            int caloriePage;
            int targetPage;
            int swimSummaryPage;
            stepPage = (data[4]&0xff) * 256 + (data[5] & 0xff);
            distancePage = (data[6] & 0xff) * 256 + (data[7] & 0xff);
            caloriePage = (data[8] & 0xff) * 256 + (data[9] & 0xff);
            targetPage = (data[10] & 0xff) * 256 + (data[11] & 0xff);
            swimSummaryPage = (data[12] & 0xff) * 256 + (data[13] & 0xff);
            object.put(AmProfile.SYNC_STAGE_DATA_TYPE_AM, AmProfile.SYNC_STAGE_DATA_TYPE_PAGE_VIEW_SUMMARY);
            object.put(AmProfile.SYNC_STAGE_DATA_VIEW_SUMMARY_DATE_AM, formatDateString);
            object.put(AmProfile.SYNC_STAGE_DATA_VIEW_SUMMARY_STEP_AM, stepPage);
            object.put(AmProfile.SYNC_STAGE_DATA_VIEW_SUMMARY_DISTANCE_AM, distancePage);
            object.put(AmProfile.SYNC_STAGE_DATA_VIEW_SUMMARY_CALORIE_AM, caloriePage);
            object.put(AmProfile.SYNC_STAGE_DATA_VIEW_SUMMARY_TARGET_AM, targetPage);
            object.put(AmProfile.SYNC_STAGE_DATA_VIEW_SUMMARY_SWIM_AM, swimSummaryPage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * 获取用户token和host
     */
    private void getUserToken() {
        host = mContext.getSharedPreferences(mUserName + "userinfo", 0).getString("Host", "");//获取最优服务器
        if ("".equals(host)) {
            host = AppsDeviceParameters.webSite;
        }

        accessToken = mContext.getSharedPreferences(mUserName + "userinfo", 0).getString("accessToken", "");
    }

    /**
     * 绑定设备
     */
    private void bindAMDevice() {
        //数据上云开关 如果带有数据上云 则将绑定功能加入线程池
        if (AppsDeviceParameters.isUpLoadData) {
            if (host != null && accessToken != null) {
                DataThreadPoolManager.getInstance().addExecuteTask(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            int bindResult = am_CommCloud.ambinding(mUserName, mAddress, accessToken, host);
                            if (bindResult == 100)
                                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_BINDING_AM_SUCCESS, null);
                            else
                                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_BINDING_AM_FAIL, null);
                        } catch (Exception e) {
                            mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_BINDING_AM_FAIL, null);
                            e.printStackTrace();
                        }
                    }
                });

            } else {
                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_BINDING_AM_FAIL, null);
            }
        } else {
            mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_BINDING_AM_SUCCESS, null);
        }
    }

    /**
     * 从云端获取用户绑定的设备
     */
    private void getUserMacFromCloud(final JSONObject o) {
        if (AppsDeviceParameters.isUpLoadData) {
            if (host != null && accessToken != null) {
                DataThreadPoolManager.getInstance().addExecuteTask(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            int searchResult = am_CommCloud.amsearch(mUserName, accessToken, host);
                            if (searchResult == 100) {
                                o.put(AmProfile.CLOUD_SEARCH_AM, am_CommCloud.getAmsearch_return().mac[0]);
//                                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_SEARCH_AM, o.toString());
                            } else {
//                                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_SEARCH_FAIL_AM, null);
                            }
                        } catch (Exception e) {
//                            mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_SEARCH_FAIL_AM, null);
                            e.printStackTrace();
                        }
                    }
                });

            } else {
//                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_SEARCH_FAIL_AM, null);
            }
        } else {
            try {
                o.put(AmProfile.CLOUD_SEARCH_AM, 0);
//                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_SEARCH_AM, o.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 云端解绑
     */
    private void resetFromCloud() {
        if (AppsDeviceParameters.isUpLoadData) {
            if (host != null && accessToken != null) {
                DataThreadPoolManager.getInstance().addExecuteTask(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            am_CommCloud.amunbinding(mUserName, mAddress, accessToken, host);
//                            int unBindResult = am_CommCloud.amunbinding(mUserName, mAddress, accessToken, host);
//                            if (unBindResult == 100)
//                                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_UNBINDING_AM_SUCCESS, null);
//                            else
//                                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_UNBINDING_AM_FAIL, null);
                        } catch (Exception e) {
//                            mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_UNBINDING_AM_FAIL, null);
                            e.printStackTrace();
                        }
                    }
                });

            } else {
//                mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_UNBINDING_AM_FAIL, null);
            }
        } else {
//            mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_CLOUD_UNBINDING_AM_SUCCESS, null);
        }
    }

    @Override
    public BaseCommProtocol getBaseCommProtocol() {
        return blecm;
    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {

    }

    public void destroy(){
        if(blecm != null)
            blecm.destroy();
        blecm = null;
        mContext = null;
        mBaseCommCallback = null;
        mInsCallback = null;
        if(listActivity != null)
            listActivity.clear();
        listActivity = null;
        if(listSleep != null)
            listSleep.clear();
        listSleep = null;
        if(listStageForm != null)
            listStageForm.clear();
        listStageForm = null;
    }
}
