package com.ihealth.communication.control;

import android.content.Context;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.ins.AaInsSet;
import com.ihealth.communication.ins.F0InsSet;
import com.ihealth.communication.ins.InsCallback;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesIDPS;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.FirmWare;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Jeepend on 4/8/2016.
 * Base class for AM devices.
 */

class AmControlBase implements DeviceControl {
    private final String TAG;
    protected Context mContext;
    protected AaInsSet aaInsSet;
    protected F0InsSet f0InsSet;
    private BaseComm mComm;
    private String mAddress;
    private String currentUpgradeDevice = null;
    private InsCallback mInsCallback;
    private String mType;
    protected int mFirmwareVersion;

    public AmControlBase(String tag, BaseComm com, Context context, String mac, String type, String userName,
                  BaseCommCallback baseCommCallback, InsCallback insCallback) {
        TAG = tag;
        mComm = com;
        mContext = context;
        mAddress = mac;
        aaInsSet = new AaInsSet(com, context, mac, type, userName, baseCommCallback, insCallback);	//修改构造函数
        BaseCommProtocol baseCommProtocol = aaInsSet.getBaseCommProtocol();
        f0InsSet = new F0InsSet(com, baseCommProtocol, context, mac, type, insCallback);
        mInsCallback = insCallback;
        mType = type;
        mFirmwareVersion = Integer.parseInt(iHealthDevicesManager.getInstance().getIdps(mAddress).getAccessoryFirmwareVersion());
    }

    /**
     * Get AM device's IDPS information
     * @return JSON string of AM device's IDPS, the keys will show in the <u>{@linkplain iHealthDevicesIDPS KeyList of iHealthDevicesIDPS}</u>
     */
    public String getIdps(){
        return iHealthDevicesManager.getInstance().getDevicesIDPS(mAddress);
    }

    /**
     * Reset the device
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If reset successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_RESET_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_RESET_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param id User's ID<br/>
     *           <b>Range:</b> [1, 2147483647(0x7FFFFFFF)]
     */
    public void reset(int id) {
        if (id < 1 || id > 2147483647) {
            notifyParameterError("reset() parameter id should in the range [1, 2147483647(0x7FFFFFFF)]");
            return;
        }
        aaInsSet.a1Ins(id);
    }

    /**
     * Get user id
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_USERID_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_USERID_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getUserId() {
        aaInsSet.a2Ins();
    }

    /**
     * Get alarms' count
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_GET_ALARMNUM_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_GET_ALARMNUM_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getAlarmClockNum() {
        aaInsSet.a6Ins();
    }

    /**
     * Get alarm information by id
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_GET_ALARMINFO_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_GET_ALARMINFO_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param ids Alarm id array to be query.<br/>
     *            <ul>
     *            <li>The parameters should be 1, 2, or 3</li>
     *            <li>The duplicate parameters will be IGNORED</li>
     *            <li>The query result will be in ascending order of id.</li>
     *            <li>If specified alarm not set yet, the result will not include the id.</li>
     *            </ul>
     */
    public void getAlarmClockDetail(int... ids) {
        if (ids.length == 0) {
            notifyParameterError("getAlarmClockDetail() parameter can not be empty.");
            return;
        }
        HashSet<Integer> idSet = new HashSet<>();
        for (int id : ids) {
            if (id < 1 || id > 3) {
                notifyParameterError("getAlarmClockDetail() parameter should be 1, 2 or 3.");
                return;
            }
            idSet.add(id);
        }
        int[] parameterArray = new int[idSet.size()];
        int index = 0;
        for (int parameter : idSet) {
            parameterArray[index++] = parameter;
        }
        Arrays.sort(parameterArray);
        aaInsSet.a7Ins(parameterArray);
    }

    /**
     * Set/Unset alarm<br/>
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set/unset successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SET_ALARMINFO_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     *
     * @param id       Alarm id<br/>
     *                 <b>Range:</b> 1, 2 or 3
     * @param hour     Alarm hour part<br/>
     *                 <b>Range:</b> [0, 23]
     * @param min      Alarm minute part<br/>
     *                 <b>Range:</b> [0, 59]
     * @param isRepeat Indicates whether it will repeat:
     *                 <ul>
     *                 <li>true indicates that it will repeat as the <b>weeks</b> parameter</li>
     *                 <li>false indicates that it will only play one time and <b>IGNORE</b> the <b>weeks</b> parameter</li>
     *                 </ul>
     * @param weeks    The days in a week to repeat the alarm, week[0~6] indicates Sun~Sat.<br/>
     *                 And 1 indicates open, 0 indicates close.<br/>
     *                 <b>For example:</b><br/>
     *                 {0, 1, 1, 1, 1, 1, 0} means the alarm will repeat on Mon, Tue, Wed, Thu, Fri.
     * @param isOn     true if want to set the alarm, false to unset it.
     */
    public void setAlarmClock(int id, int hour, int min, boolean isRepeat, int[] weeks, boolean isOn) {
        if (id < 1 || id > 3) {
            notifyParameterError("setAlarmClock() parameter id should be 1, 2 or 3.");
            return;
        }
        if (hour < 0 || hour > 23) {
            notifyParameterError("setAlarmClock() parameter hour should be in the range [0, 23].");
            return;
        }
        if (min < 0 || min > 59) {
            notifyParameterError("setAlarmClock() parameter min should be in the range [0, 59].");
            return;
        }
        if (weeks == null) {
            notifyParameterError("setAlarmClock() parameter weeks is null.");
            return;
        }
        if (weeks.length != 7) {
            notifyParameterError("setAlarmClock() parameter weeks length should be 7.");
            return;
        }
        for(int i : weeks) {
            if (i < 0 || i > 1) {
                notifyParameterError("setAlarmClock() parameter weeks item should be 0 or 1.");
                return;
            }
        }
        byte week = (byte) ((weeks[6] << 6) | (weeks[5] << 5) | (weeks[4] << 4) | (weeks[3] << 3) | (weeks[2] << 2)
                | (weeks[1] << 1) | (weeks[0] << 0));
        aaInsSet.a8Ins(id, hour, min, isRepeat, week, isOn);
    }

    /**
     * Delete alarm by id
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If delete successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_DELETE_ALARM_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param id Alarm id(should be 1, 2 or 3)
     */
    public void deleteAlarmClock(int id) {
        if (id < 1 || id > 3) {
            notifyParameterError("deleteAlarmClock() parameter id should be 1, 2, or 3.");
            return;
        }
        aaInsSet.a9Ins(id);
    }

    /**
     * Get activity remind setting.
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_GET_ACTIVITY_REMIND_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_GET_ACTIVITY_REMIND_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getActivityRemind() {
        aaInsSet.b3Ins();
    }

    /**
     * Set/Unset activity remind
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set/unset successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SET_ACTIVITYREMIND_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     *
     * @param hour Activity remind hour part<br/>
     *             <b>Range:</b> [0, 23]
     * @param min  Activity remind minute part<br/>
     *             <b>Range:</b>[0, 59]
     * @param isOn true if want to set activity remind, false to unset it.
     */
    public void setActivityRemind(int hour, int min, boolean isOn) {
        if (hour < 0 || hour > 23) {
            notifyParameterError("setActivityRemind() parameter hour should be in the range [0, 23].");
            return;
        }
        if (min < 0 || min > 59) {
            notifyParameterError("setActivityRemind() parameter min should be in the range [0, 59].");
            return;
        }
        if (hour == 0 && min == 0) {
            notifyParameterError("setActivityRemind() time(hour * 60 + min) should be larger than 0 min.");
            return;
        }
        int minss = hour * 60 + min;
        aaInsSet.aaIns(minss, isOn);
    }

    protected void notifyParameterError(String description) {
        notifyError(AmProfile.ERROR_ID_ILLEGAL_ARGUMENT, description);
    }

    protected void notifyVersionError(String description) {
        notifyError(AmProfile.ERROR_ID_VERSION_NOT_SUPPORT, description);
    }

    private void notifyError(int errorID, String description) {
        Log.w(TAG, description);
        try {
            JSONObject object = new JSONObject();
            object.put(AmProfile.ERROR_NUM_AM, errorID);
            object.put(AmProfile.ERROR_DESCRIPTION_AM, description);
            mInsCallback.onNotify(mAddress, mType, AmProfile.ACTION_ERROR_AM, object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get device state and battery information
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_QUERY_STATE_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_QUERY_STATE_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void queryAMState() {
        aaInsSet.b4Ins();
    }

    /**
     * Set user ID
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SET_USERID_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param id new user id <br/>
     *           <b>Range:</b> [1, 2147483647(0x7FFFFFFF)]
     */
    public void setUserId(int id) {
        if (id < 1 || id > 2147483647) {
            notifyParameterError("setUserId() parameter id should be in the range [1, 2147483647(0x7FFFFFFF)]");
            return;
        }
        aaInsSet.a3Ins(id);
    }

    private float mWeight;
    private int mLengthType = 1;
    private float mHeight;
    private int mAge;
    private int mSex;
    private int mActivityLevel;
    private int mTarget;
    private int mBmr;

    /**
     * Get user information
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_GET_USERINFO_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_GET_USERINFO_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getUserInfo() {
        aaInsSet.b6Ins();
    }



    private int getUserBmr() {
        int result;
        double p = 0;
        if (mSex == 0) {// male
            p = (13.397 * mWeight + 4.799 * mHeight - 5.677 * mAge + 88.362);
        } else if (mSex == 1) {// female
            p = (9.247 * mWeight + 3.098 * mHeight - 4.330 * mAge + 447.593);
        }
        switch (mActivityLevel) {
            case 1:
                p = (p * 1.0);
                break;
            case 2:
                p = (p * 1.05);
                break;
            case 3:
                p = (p * 1.10);
                break;
            default:
                break;
        }
        p = ((p < 0) ? p * (-1) : p);
        BigDecimal big = new BigDecimal(p);
        result = big.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
        return (result - 2);
    }

    /**
     * Set user's BMR
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SET_BMR_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param bmr User's BMR<br/>
     *            <b>Range:</b> [1, 65535(0xFFFF)]
     */
    public void setUserBmr(int bmr){
        if (bmr < 1 || bmr > 65535) {
            notifyParameterError("setUserBmr() parameter bmr should be in range [1, 65535(0xFFFF)]");
            return;
        }
        aaInsSet.b7Ins(bmr);
    }

    /**
     * Get the activity data.
     * <ul>
     * <li>
     * This is an asynchronous call, it will return immediately.
     * </li>
     * <li>
     * After getting the activity data, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The action will be {@linkplain AmProfile#ACTION_SYNC_ACTIVITY_DATA_AM ACTION_SYNC_ACTIVITY_DATA_AM}</li>
     * <li>The keys of message will show in the <u>{@linkplain AmProfile#ACTION_SYNC_ACTIVITY_DATA_AM KeyList of the action}</u>.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void syncActivityData() {
        aaInsSet.acIns();
    }

    /**
     * Get current time activity data
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SYNC_REAL_DATA_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_SYNC_REAL_DATA_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void syncRealData() {
        aaInsSet.bfIns();
    }

    /**
     * Get sleep data
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SYNC_SLEEP_DATA_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_SYNC_SLEEP_DATA_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void syncSleepData() {
        aaInsSet.b0Ins();
    }

    /**
     * Set the system time to AM device.
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SYNC_TIME_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void syncRealTime() {
        Calendar calenda = Calendar.getInstance();
        calenda.setTimeZone(TimeZone.getDefault());
        int year = calenda.get(Calendar.YEAR) - 2000;
        int month = calenda.get(Calendar.MONTH) + 1;
        int day = calenda.get(Calendar.DAY_OF_MONTH);
        int week = calenda.get(Calendar.DAY_OF_WEEK);
        int hour = calenda.get(Calendar.HOUR_OF_DAY);
        int min = calenda.get(Calendar.MINUTE);
        int sed = calenda.get(Calendar.SECOND);
        aaInsSet.a4Ins(year, month, day, hour, min, sed, week);
    }

    /**
     * Set hour mode
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SET_HOUR_MODE_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param hourMode The value should be one of following:
     *                 <ul>
     *                 <li>{@link AmProfile#AM_SET_12_HOUR_MODE}</li>
     *                 <li>{@link AmProfile#AM_SET_24_HOUR_MODE}</li>
     *                 <li>{@link AmProfile#AM_SET_EXCEPT_EUROPE_12_HOUR_MODE}</li>
     *                 <li>{@link AmProfile#AM_SET_EUROPE_12_HOUR_MODE}</li>
     *                 <li>{@link AmProfile#AM_SET_EXCEPT_EUROPE_24_HOUR_MODE}</li>
     *                 <li>{@link AmProfile#AM_SET_EUROPE_24_HOUR_MODE}</li>
     *                 </ul>
     */
    public void setHourMode(int hourMode) {
        if (hourMode < 0 || hourMode > 5) {
            notifyParameterError("setHourMode() parameter hourMode should be in range [0, 5]");
            return;
        }
        aaInsSet.x02Ins(hourMode);
    }

    /**
     * Get hour mode
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_GET_HOUR_MODE_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_GET_HOUR_MODE_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getHourMode() {
        aaInsSet.x01Ins();
    }

    /**
     * @hide
     */
    @Override
    public void init() {
        aaInsSet.identify();
    }

    @Override
    public void disconnect() {
        this.mComm.disconnect(mAddress);
    }



    private UpDeviceControl mUpDeviceControl = new UpDeviceControl() {
        @Override
        public void setInformation(List<Byte> list) {
            f0InsSet.setInfo(list);
        }

        @Override
        public void setData(FirmWare firmware, List<byte[]> list) {
            f0InsSet.setFirmWare(firmware, list);
        }

        @Override
        public void startUpgrade() {
            f0InsSet.startUpdate();
        }

        @Override
        public void stopUpgrade() {
            f0InsSet.stopUpdate();
        }

        @Override
        public void borrowComm() {
            aaInsSet.getBaseCommProtocol().setInsSet(f0InsSet);
            //设置当前为自升级状态
            f0InsSet.setCurrentState(mAddress, true);
        }


        @Override
        public void returnComm() {
            f0InsSet.getBaseCommProtocol().setInsSet(aaInsSet);
            //设置当前不是自升级状态
            f0InsSet.setCurrentState(mAddress, false);
        }

        @Override
        public void judgUpgrade() {
            f0InsSet.queryInformation();
        }

        @Override
        public boolean isUpgradeState() {
            return f0InsSet.getCurrentState(mAddress);
        }

        @Override
        public void setCurrentMac(String mac) {
            currentUpgradeDevice = mac;
        }

        @Override
        public String getCurrentMac() {
            return currentUpgradeDevice;
        }
    };

    /**
     * Get UpDeviceControl's implementation. <br/>
     * We recommend not use this method,
     * use {@linkplain com.ihealth.communication.manager.iHealthDevicesUpgradeManager iHealthDevicesUpgradeManager} instead.
     * @see com.ihealth.communication.manager.iHealthDevicesUpgradeManager
     * @return UpDeviceControl's implementation
     */
    @Deprecated
    public UpDeviceControl getUpDeviceControl() {
        return mUpDeviceControl;
    }

    @Override
    public void destroy() {
        mContext = null;
        if(aaInsSet != null)
            aaInsSet.destroy();
        aaInsSet = null;
        if(f0InsSet != null)
            f0InsSet.destroy();
        f0InsSet = null;
        mComm = null;
    }

    //==============================================================================================
    // Below method need to be override in child class
    // to make it public or disable it.
    //==============================================================================================


    /**
     * Set user information
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li><b>It will calculate and set BMR first and then set user information. </b></li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_SET_BMR_SUCCESS_AM} and {@link AmProfile#ACTION_SET_USERINFO_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     *
     * @param age           User's age<br/>
     *                      <b>Range:</b> [1, 255]
     * @param height        User's height(int in cm)<br/>
     *                      <b>Range:</b> [1, 255]
     * @param weight        User's weight(float)<br/>
     *                      <b>Range:</b> [1.0, 255.0]
     * @param gender        User's gender<br/>
     *                      <b>Value:</b>
     *                      <ul>
     *                      <li>{@link AmProfile#AM_SET_FEMALE}</li>
     *                      <li>{@link AmProfile#AM_SET_MALE}</li>
     *                      </ul>
     * @param unit          Distance's unit type(kilometre or miles)<br/>
     *                      <b>Value:</b>
     *                      <ul>
     *                      <li>{@link AmProfile#AM_SET_UNIT_IMPERIAL_STANDARD} (miles)</li>
     *                      <li>{@link AmProfile#AM_SET_UNIT_METRIC} (kilometre)</li>
     *                      </ul>
     * @param target        The goal of maximum steps<br/>
     *                      <b>Range:</b> [4, 65535(0xFFFF)]
     * @param activityLevel The level of activity strength<br/>
     *                      <ul>
     *                      <li>1 indicates sedentary</li>
     *                      <li>2 indicates active</li>
     *                      <li>3 indicates very active</li>
     *                      </ul>
     */
    protected void setUserInfo(int age, int height, float weight, int gender, int unit, int target, int activityLevel) {
        // Method for AM3 AM3S
        if (age < 1 || age > 255) {
            notifyParameterError("setUserInfo() parameter age should be in range [1, 255].");
            return;
        }
        if (height < 1 || height > 255) {
            notifyParameterError("setUserInfo() parameter height should be in range [1, 255].");
            return;
        }
        if (weight < 1 || weight > 255) {
            notifyParameterError("setUserInfo() parameter weight should be in range [1.0, 255.0].");
            return;
        }
        if (gender < 0 || gender > 1) {
            notifyParameterError("setUserInfo() parameter gender should be 0 or 1.");
            return;
        }
        if (unit < 0 || unit > 1) {
            notifyParameterError("setUserInfo() parameter unit should be 0 or 1.");
            return;
        }
        if (target < 4 || target > 65535) {
            notifyParameterError("setUserInfo() parameter target should be in range [4, 65535(0xFFFF)].");
            return;
        }
        if (activityLevel < 1 || activityLevel > 3) {
            notifyParameterError("setUserInfo() parameter activityLevel should be 1, 2 or 3.");
            return;
        }
        mWeight = weight;
        mHeight = height;
        mAge = age;
        mSex = gender;
        mActivityLevel = activityLevel;
        mTarget = target;
        int stepLenght = (int) ((height - 2.0 / 3.0 * height / 7.0) / 2.0);
        mBmr = getUserBmr();
//        mBmr = Method.getBMR(mContext);
        int target1 = mTarget;
        int target2 = mTarget / 2;
        int target3 = mTarget / 4;
        aaInsSet.setUserInfo(age, stepLenght, height, gender, weight, unit, target1, target2, target3, mBmr);
    }

    /**
     * Set user information
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li><b>It will calculate and set BMR first and then set user information. </b></li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_SET_BMR_SUCCESS_AM} and {@link AmProfile#ACTION_SET_USERINFO_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     *
     * @param age           User's age<br/>
     *                      <b>Range:</b> [1, 255]
     * @param height        User's height(int in cm)<br/>
     *                      <b>Range:</b> [1, 255]
     * @param weight        User's weight(float)<br/>
     *                      <b>Range:</b> [1.0, 255.0]
     * @param gender        User's gender<br/>
     *                      <b>Value:</b>
     *                      <ul>
     *                      <li>{@link AmProfile#AM_SET_FEMALE}</li>
     *                      <li>{@link AmProfile#AM_SET_MALE}</li>
     *                      </ul>
     * @param unit          Distance's unit type(kilometre or miles)<br/>
     *                      <b>Value:</b>
     *                      <ul>
     *                      <li>{@link AmProfile#AM_SET_UNIT_IMPERIAL_STANDARD} (miles)</li>
     *                      <li>{@link AmProfile#AM_SET_UNIT_METRIC} (kilometre)</li>
     *                      </ul>
     * @param target        The goal of maximum steps<br/>
     *                      <b>Range:</b> [4, 2147483647(0x7FFFFFFF)]
     * @param activityLevel The level of activity strength<br/>
     *                      <ul>
     *                      <li>1 indicates sedentary</li>
     *                      <li>2 indicates active</li>
     *                      <li>3 indicates very active</li>
     *                      </ul>
     * @param min           swim target time(in minutes)<br/>
     *                      <b>Range:</b> [1, 1439(23 * 60 + 59)]
     */
    protected void setUserInfo(int age, int height, float weight, int gender, int unit, int target, int activityLevel, int min) {
        // Method for AM4+
        if (age < 1 || age > 255) {
            notifyParameterError("setUserInfo() parameter age should be in range [1, 255].");
            return;
        }
        if (height < 1 || height > 255) {
            notifyParameterError("setUserInfo() parameter height should be in range [1, 255].");
            return;
        }
        if (weight < 1 || weight > 255) {
            notifyParameterError("setUserInfo() parameter weight should be in range [1.0, 255.0].");
            return;
        }
        if (gender < 0 || gender > 1) {
            notifyParameterError("setUserInfo() parameter gender should be 0 or 1.");
            return;
        }
        if (unit < 0 || unit > 1) {
            notifyParameterError("setUserInfo() parameter unit should be 0 or 1.");
            return;
        }
        if (target < 4 || target > 2147483647) {
            notifyParameterError("setUserInfo() parameter target should be in range [4, 2147483647(0x7FFFFFFF)].");
            return;
        }
        if (activityLevel < 1 || activityLevel > 3) {
            notifyParameterError("setUserInfo() parameter activityLevel should be 1, 2 or 3.");
            return;
        }
        if (min < 1 || min > 1439) {
            notifyParameterError("setUserInfo() parameter min should be in range [1, 1439(23 * 60 + 59)].");
            return;
        }
        mWeight = weight;
        mHeight = height;
        mAge = age;
        mSex = gender;
        mActivityLevel = activityLevel;
        mTarget = target;
        int stepLenght = (int) ((height - 2.0 / 3.0 * height / 7.0) / 2.0);
        mBmr = getUserBmr();
//        mBmr = Method.getBMR(mContext);
        int target1 = mTarget;
        int target2 = mTarget / 2;
        int target3 = mTarget / 4;
        aaInsSet.setUserInfoForAM4Plus(age, stepLenght, height, gender, weight, unit, target1, target2, target3, mBmr, min);
    }

    /**
     * Set device mode
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SET_DEVICE_MODE_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param mode Device mode<br/>
     *             <b>Value:</b>
     *             <ul>
     *             <li>{@link AmProfile#AM_DEVICE_MODE_SLEEP}</li>
     *             <li>{@link AmProfile#AM_DEVICE_MODE_ACTIVITY}</li>
     *             <li>{@link AmProfile#AM_DEVICE_MODE_FLIGHT}</li>
     *             <li>{@link AmProfile#AM_DEVICE_MODE_DRIVING}</li>
     *             </ul>
     */
    protected void setMode(int mode) {
        if (mode < 0 || mode > 3) {
            notifyParameterError("setMode() parameter mode should be 0, 1 , 2 or 3.");
            return;
        }
        aaInsSet.b5Ins(mode);
    }

    /**
     * Get stage report data
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SYNC_STAGE_DATA_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_SYNC_STAGE_DATA_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    protected void syncStageReprotData() {
        aaInsSet.x0aIns();
    }

    /**
     * Send random number to device to prepare for binding, the number will be displayed on the device.
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_GET_RANDOM_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_GET_RANDOM_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    protected void sendRandom(){
        Integer[] mRandom = new Integer[6];
        for (int i = 0; i < 6; i++) {
            mRandom[i] = (int) (Math.random() * 10);
        }
        aaInsSet.x09Ins(mRandom);
    }

    /**
     * Get swim parameters
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_GET_SWIMINFO_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_GET_SWIMINFO_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    protected void checkSwimPara() {
        aaInsSet.checkSwimPara();
    }

    /**
     * Set swim parameters
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SET_SWIMINFO_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param isOpen Set true to open swim function, set false to close it.
     * @param poolLength the swimming pool's length.<br/>
     *                   <b>Range:</b> [1, 255]
     * @param hours The cut out time's hour part<br/>
     *              <b>Range:</b> [0, 23]
     * @param minutes The cut out time's minute part<br/>
     *                Range[0, 59]
     * @param unit The pool length's unit type(metre or yard).
     *             <ul>
     *             <li>{@link AmProfile#AM_SET_UNIT_IMPERIAL_STANDARD} (yard)</li>
     *             <li>{@link AmProfile#AM_SET_UNIT_METRIC} (metre)</li>
     *             </ul>
     */
    protected void setSwimPara(boolean isOpen,int poolLength, int hours, int minutes, int unit) {
        if (poolLength < 1 || poolLength > 255) {
            notifyParameterError("setSwimPara() parameter poolLength should be in range [1, 255].");
            return;
        }
        if (hours < 0 || hours > 23) {
            notifyParameterError("setSwimPara() parameter hours should be in range [0, 23].");
            return;
        }
        if (minutes < 0 || minutes > 59) {
            notifyParameterError("setSwimPara() parameter minutes should be in range [0, 59].");
            return;
        }
        if (unit < 0 || unit > 1) {
            notifyParameterError("setSwimPara() parameter unit should be 0 or 1.");
            return;
        }
        byte pool_length = (byte) poolLength;
        byte hr = (byte) hours;
        byte min = (byte) minutes;
        byte u = (byte) unit;
        aaInsSet.setSwimPara(isOpen, pool_length, hr, min,u);
    }

    /**
     * Set picture for AM
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_SET_PICTURE_SUCCESS_AM}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * </ul>
     * @param index The index of picture
     *             <ul>
     *             <li>0 indicates the frog picture.</li>
     *             <li>1 indicates the default picture.</li>
     *             </ul>
     */
    protected void setPicture(int index) {
        if (index < 0 || index > 1) {
            notifyParameterError("setPicture() parameter index should be 0 or 1.");
            return;
        }
        aaInsSet.x11Ins(index);
    }
    /**
     * Get AM picture's index
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link AmProfile#ACTION_GET_PICTURE_AM}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain AmProfile#ACTION_GET_PICTURE_AM KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link AmProfile#ACTION_ERROR_AM}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain AmProfile#ACTION_ERROR_AM KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    protected void getPicture() {
        aaInsSet.x10Ins();
    }
}
