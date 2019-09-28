
package com.ihealth.communication.control;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.ins.Hs5InsSet;
import com.ihealth.communication.ins.InsCallback;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Public API for the HS5 bluetooth
 * <p>
 * The class provides methods to control HS5 device. You need to call the device method, and then
 * call the connection method
 * <p>
 * If you want to connect a HS5 device, you need to call
 * {@link iHealthDevicesManager#startDiscovery} to discovery a new HS5 device, and then call
 * {@link iHealthDevicesManager#connectDevice}to connect HS5 device.
 */
public class Hs5ControlForBt {

    private static final String TAG = "Hs5ControlForBt";

    protected Hs5InsSet mHs5InsSet;
    private BaseComm mComm;
    private BaseCommCallback mBaseCommCallback;
    private String mAddress;

    private String mType;
    private InsCallback mInsCallback;

    /**
     * A constructor for Hs5ControForBt.
     * 
     * @param mBaseComm class for communication.
     * @param mac valid Bluetooth address(without colon).
     * @param type type of iHealth device {@link iHealthDevicesManager#TYPE_HS5_BT}
     * @param baseCommCallback communication callback.
     * @param insCallback Hs5 Bluetooth device callback.
     * @hide
     */
    public Hs5ControlForBt(BaseComm mBaseComm, String mac,
            String type, BaseCommCallback baseCommCallback, InsCallback insCallback) {
        this.mComm = mBaseComm;
        this.mAddress = mac;
        this.mBaseCommCallback = baseCommCallback;
        mType = type;
        mInsCallback = insCallback;
        mHs5InsSet = new Hs5InsSet(mac, type, mBaseComm, baseCommCallback, insCallback);
    }

    /**
     * Notify the Hs5 Bluetooth connected
     * 
     * @hide
     */
    public void init() {
        mBaseCommCallback.onConnectionStateChange(mAddress, iHealthDevicesManager.TYPE_HS5_BT,
                iHealthDevicesManager.DEVICE_STATE_CONNECTED, 0, null);
    }

    /**
     * Disconnect the device
     */
    public void disconnect() {
        mComm.disconnect();
    }

    /** 
     * set the wifi to the scale
     * <ul>
     * <li>This is an asynchronous call, it will return immediately.</li>
     * <li>During the progress, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be:
     * <ul>
     * <li>{@link HsProfile#ACTION_SETTINGWIFI}</li>
     * <li>{@link HsProfile#ACTION_SETWIFI_SUCCESS}</li>
     * <li>{@link HsProfile#ACTION_SETWIFI_FAIL}</li>
     * <li>{@link HsProfile#ACTION_SETWIFI_UNKNOW}</li>
     * </ul>
     * </li>
     * <li>The <b>message</b> is null.</li>
     * </ul>
     * </li>
     * <li>If error happens, following callback will be triggered:<br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <ul>
     * <li>The <b>action</b> will be {@link HsProfile#ACTION_ERROR_HS}</li>
     * <li>The <b>message</b> will be a JSON string with error ID and description,<br/>
     * the keys will show in the {@linkplain HsProfile#ACTION_ERROR_HS KeyList of the action}.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Attention, it is mandatory to call following method before you call this method:<br/>
     * {@link iHealthDevicesManager#registerClientCallback(iHealthDevicesCallback)}
     * </li>
     * </ul>
     * @param ssid The ssid of the wifi.
     * @param type Reserved parameter, default 0.
     * @param pw The password of the wifi.
     */
    public void setWifi(String ssid, int type, String pw) {
        Context context = mComm.getContext();
        if (context == null) {
            throw new RuntimeException("Context is null.");
        }
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!manager.isWifiEnabled()) {
            notifyError(HsProfile.ERROR_ID_WIFI_DISABLED, "setWifi() wifi is disabled, please enable it.");
            return;
        }
        type = 0;
        List<ScanResult> wifiList = manager.getScanResults();
        for (ScanResult wifi : wifiList) {
            if (wifi.SSID.equals(ssid)) {
                String capabilities = wifi.capabilities;
                if (capabilities.contains("WPA2")
                        && capabilities.contains("WPA")
                        && capabilities.contains("Mixed")) {
                    type = 4;
                } else if (capabilities.contains("WPA2")) {
                    type = 3;
                } else if (capabilities.contains("WPA")) {
                    type = 2;
                } else if (capabilities.contains("WEP")) {
                    type = 1;
                }
                break;
            }
        }
        mHs5InsSet.setWifi(ssid, type, pw);
    }

    private void notifyError(int errorID, String description) {
        Log.w(TAG, description);
        try {
            JSONObject object = new JSONObject();
            object.put(HsProfile.ERROR_NUM_HS, errorID);
            object.put(HsProfile.ERROR_DESCRIPTION_HS, description);
            mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_ERROR_HS, object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
