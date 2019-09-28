package com.ihealth.communication.control;

import android.content.Context;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.ins.InsCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

/**
 * Public API for the AM3
 * <p> The class provides methods to control AM3 device.
 * You need to call the device method, and then call the connection method
 * <p> If you want to connect a AM3 device, you need to call{@link iHealthDevicesManager#discoveryType} to discovery a new AM3 device,
 * and then call{@link iHealthDevicesManager#connectDevice}to connect AM3 device.
 */
public class Am3Control extends AmControlBase {

	private static final String TAG = "Am3Control";
    private static final int VERSION_SUPPORT_FLIGHT_MODE = 101;
    private static final int VERSION_SUPPORT_DRIVING_MODE = 111;

    /**
	 * Constructor
	 * @param com 
	 * @param context 
	 * @param mac 
	 * @param type 
	 * @param userName
	 * @param baseCommCallback 
	 * @param insCallback
	 * @hide
	 */
    public Am3Control(BaseComm com, Context context, String mac, String type, String userName, BaseCommCallback baseCommCallback, InsCallback insCallback) {
        super(TAG, com, context, mac, type, userName, baseCommCallback, insCallback);
    }

    // Enable it
    @Override
    public void setUserInfo(int age, int height, float weight, int gender, int unit, int target, int activityLevel) {
        super.setUserInfo(age, height, weight, gender, unit, target, activityLevel);
    }

    /**
     * Disabled method, should not be called.<br/>
     * If called, UnsupportedOperationException will be thrown.<br/>
     * Should call this: {@link #setUserInfo(int, int, float, int, int, int, int)}
     */
    @Override
    protected void setUserInfo(int age, int height, float weight, int gender, int unit, int target, int activityLevel, int min) {
        throw new UnsupportedOperationException("AM3S not support this method, please call setUserInfo(int, int, float, int, int, int, int) instead.");
    }

    // Enable it
    @Override
    public void setMode(int mode) {
        if (mode == AmProfile.AM_DEVICE_MODE_FLIGHT && mFirmwareVersion < VERSION_SUPPORT_FLIGHT_MODE) {
            notifyVersionError("AM3 does not support flight mode below version 1.0.1.");
            return;
        }
        if (mode == AmProfile.AM_DEVICE_MODE_DRIVING && mFirmwareVersion < VERSION_SUPPORT_DRIVING_MODE) {
            notifyVersionError("AM3 dose not support driving mode below version 1.1.1.");
            return;
        }
        super.setMode(mode);
    }

    /**
     * Disabled method, should not be called.<br/>
     * If called, UnsupportedOperationException will be thrown.
     */
    @Override
    protected void syncStageReprotData() {
        throw new UnsupportedOperationException("AM3 not support stage function.");
    }

    /**
     * Disabled method, should not be called.<br/>
     * If called, UnsupportedOperationException will be thrown.
     */
    @Override
    protected void sendRandom() {
        throw new UnsupportedOperationException("AM3 not support send random number.");
    }

    /**
     * Disabled method, should not be called.<br/>
     * If called, UnsupportedOperationException will be thrown.
     */
    @Override
    protected void checkSwimPara() {
        throw new UnsupportedOperationException("AM3 not support swim function.");
    }

    /**
     * Disabled method, should not be called.<br/>
     * If called, UnsupportedOperationException will be thrown.
     */
    @Override
    protected void setSwimPara(boolean isOpen, int poolLength, int hours, int minutes, int unit) {
        throw new UnsupportedOperationException("AM3 not support swim function.");
    }

    /**
     * Disabled method, should not be called.<br/>
     * If called, UnsupportedOperationException will be thrown.
     */
    @Override
    protected void getPicture() {
        throw new UnsupportedOperationException("AM3 not support getPicture.");
    }

    /**
     * Disabled method, should not be called.<br/>
     * If called, UnsupportedOperationException will be thrown.
     */
    @Override
    protected void setPicture(int index) {
        throw new UnsupportedOperationException("AM3 not support setPicture.");
    }
}
