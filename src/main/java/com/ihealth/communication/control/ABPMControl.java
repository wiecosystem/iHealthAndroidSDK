package com.ihealth.communication.control;

import android.content.Context;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.ins.A1InsSetforABPM;
import com.ihealth.communication.ins.InsCallback;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesIDPS;
import com.ihealth.communication.manager.iHealthDevicesManager;

/**
 * Public API for the ABPM
 * <p> The class provides methods to control ABPM device.
 * You need to call the device method, and then call the connection method
 * <p> If you want to connect a ABPM device, you need to call{@link iHealthDevicesManager#startDiscovery} to discovery a new ABPM device,
 * and then call{@link iHealthDevicesManager#connectDevice}to connect ABPM device.
 */
public class ABPMControl implements DeviceControl {

    private static final String TAG = "ABPMControl";

    public static final int UNIT_MMHG = 1;
    public static final int UNIT_KPA = 2;

    private Context mContext;
    private A1InsSetforABPM a1InsSet;
    private BaseComm mComm;
    private String mAddress;

    /**
     * A construct of ABPMControl
     *
     * @param context           Context.
     * @param com               class for communication.
     * @param userName          the identification of the user, could be the form of email address or mobile phone.
     * @param mac               valid Ble address(without colon).
     * @param insCallback       Po3 device callback.
     * @param mBaseCommCallback class for communication.
     * @hide
     */
    public ABPMControl(Context context, BaseComm com, String userName, String mac, String name, InsCallback insCallback, BaseCommCallback mBaseCommCallback) {
        this.mComm = com;
        this.mContext = context;
        this.mAddress = mac;
        a1InsSet = new A1InsSetforABPM(context, com, userName, mac, name, insCallback, mBaseCommCallback);
    }

    /**
     * Get the ABPM information
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * <li>The action of the callback is {@link iHealthDevicesIDPS#MSG_IHEALTH_DEVICE_IDPS}.</li>
     * <li>Thr Keys of message will show in the KeyList of {@link iHealthDevicesIDPS#MSG_IHEALTH_DEVICE_IDPS}.</li>
     * </ul>
     */
    public String getIdps() {
        return iHealthDevicesManager.getInstance().getDevicesIDPS(mAddress);
    }

    /**
     * Get the ABPM battery status
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * <li>This is an asynchronous call, it will not return immediately. After get the battery status,
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * <li>The action of the callback is {@link BpProfile#ACTION_BATTERY_BP}.</li>
     * <li>Thr Keys of message will show in the KeyList of {@link BpProfile#ACTION_BATTERY_BP}.</li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getBattery() {
        a1InsSet.getBatteryLevel();
    }

    /**
     * sync real time:Synchronize the system time to ABPM device.
     * <ul>
     * <li>Attention, call {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)} firstly.</li>
     * <li>This is an asynchronous call, it will not return immediately.
     * Callback {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)} will be triggered.</li>
     * <li>The action of the callback is {@link BpProfile#ACTION_FUNCTION_INFORMATION_BP}.</li>
     * <li>Thr Keys of return message will show in the KeyList of {@link BpProfile#ACTION_FUNCTION_INFORMATION_BP}.</li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getFunctionInfo() {
        a1InsSet.getFunctionInfo();
    }

    /**
     * Set the display unit<br/>
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set/unset successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link BpProfile#ACTION_SET_UNIT_SUCCESS_BP}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     *
     * @param unit display unit.
     *             The unit of {@link #UNIT_MMHG} and {@link #UNIT_KPA}.
     */
    public void setDisplayUnit(int unit) {
        a1InsSet.setDisplayUnit(unit);
    }

    /**
     * Set automatic cycle measurement setup.<br/>
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set/unset successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link BpProfile#ACTION_SET_CYCLE_MEASURE_SUCCESS}.</li>
     * <li>The <b>message</b> will be null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     *
     * @param length     Measure time length, unit is hour.
     * @param isMedicine Whether the user take medicine
     * @param times      Measuring time array , Maximum 4 is variable in length.
     *                   Each time array is 4, the first time to measure the number of hours,
     *                   number of minutes of the second for measuring time,
     *                   a third for measuring intervals on the number of minutes,
     *                   a fourth way to remind(value:0,1,2,3):
     *                   0:vibration off and sound off
     *                   1:vibration on and sound off
     *                   2:vibration off and sound on
     *                   3:vibration on and sound on
     */
    public void setMeasureTime(int length, boolean isMedicine, int[]... times) {
        a1InsSet.setMeasureTime(length, isMedicine, times);
    }

    /**
     * Get automatic cycle measurement setup.
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link BpProfile#ACTION_GET_CYCLE_MEASURE}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain BpProfile#ACTION_GET_CYCLE_MEASURE KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getMeasureTime() {
        a1InsSet.getMeasureTime();
    }

    /**
     * Set automatic cycle measurement setup.<br/>
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If set/unset successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     *
     * @param args Alarm parameters array , Maximum 6 is variable in length,Each set of parameters length is 4.
     *             The first parameter to alarm type
     *             The second parameter for the alarm switch(value:0,1,2,3):
     *             0:sound off and visual off
     *             1:sound on and visual off
     *             2:sound off and visual on
     *             3:sound on and visual on
     *             The third parameters for alarm limit,
     *             The fourth parameters for alarm lower limit
     */
    public void setAlarm(int[]... args) {
        a1InsSet.setAlarm(args);
    }

    /**
     * Get automatic cycle measurement setup.
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link BpProfile#ACTION_ALARM_SETTING_BP}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain BpProfile#ACTION_ALARM_SETTING_BP KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getAlarmSetting() {
        a1InsSet.getAlarmSetting();
    }

    /**
     * Get automatic cycle measurement setup.
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link BpProfile#ACTION_ALARM_TYPE_BP}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain BpProfile#ACTION_ALARM_TYPE_BP KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getAlarmType() {
        a1InsSet.getAlarmType();
    }

    /**
     * Get the number of historical data in the ABPM,select memory size.
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link BpProfile#ACTION_HISTORICAL_NUM_BP}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain BpProfile#ACTION_HISTORICAL_NUM_BP KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getOfflineNum() {
        a1InsSet.getOfflineData = false;
        a1InsSet.getOffLineDataNum();
    }

    /**
     * Get the value of historical data in the ABPM
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> is {@link BpProfile#ACTION_HISTORICAL_DATA_BP}.</li>
     * <li>The keys of the <b>message</b> will show in {@linkplain BpProfile#ACTION_HISTORICAL_DATA_BP KeyList of the action}.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void getOfflineData() {
        a1InsSet.getOfflineData = true;
        a1InsSet.getOffLineDataNum();
    }

    /**
     * Get the value of historical data in the ABPM
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>If get successfully, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link BpProfile#ACTION_ERROR_BP}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain BpProfile#ACTION_ERROR_BP KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     */
    public void deleteAllMemory() {
        a1InsSet.deleteAllMemory();
    }


    /**
     * @hide
     */
    @Override
    public void init() {
        a1InsSet.identify();
    }

    /**
     * Disconnect the ABPM
     */
    @Override
    public void disconnect() {
        mComm.disconnect(mAddress);
    }

    @Override
    public void destroy() {
        if (a1InsSet != null)
            a1InsSet.destroy();
        a1InsSet = null;
        mContext = null;
        mComm = null;
    }

}
