package com.ihealth.communication.control;

import android.content.Context;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.ins.InsCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

/**
 * Public API for the AM3S
 * <p> The class provides methods to control AM3S device.
 * You need to call the device method, and then call the connection method
 * <p> If you want to connect a AM3S device, you need to call{@link iHealthDevicesManager#startDiscovery} to discovery a new AM3S device,
 * and then call{@link iHealthDevicesManager#connectDevice}to connect AM3S device.
 */
public class Am3sControl extends AmControlBase {
	
	private static final String TAG = "Am3sControl";
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
	public Am3sControl(BaseComm com, Context context, String mac, String type, String userName,
			BaseCommCallback baseCommCallback, InsCallback insCallback) {
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

    /**
     * Set mode <br/>
     */
    @Override
	@Deprecated
    protected void setMode(int mode) {
		super.setMode(mode);
    }

	/**
	 * Set mode <br/>
	 * @param mode    0: sleep
	 * @param minute  minute >= 30 && minute < 30*48  && (minute%30)==0
	 * Callback{@link AmProfile#ACTION_SET_DEVICE_MODE_AM}
	 */
	@Deprecated
	public void setMode(int mode, int minute) {
		if (minute % 30 == 0 && minute >= 30 && minute < 30 * 48) {
			aaInsSet.b5Ins(mode, minute);
		} else {
			notifyParameterError("setMode() parameter minute should be: (minute % 30 == 0 && minute >= 30 && minute < 30 * 48)");
		}
	}

	// Enable it
	@Override
	public void syncStageReprotData() {
		super.syncStageReprotData();
	}

	// Enable it
	@Override
	public void sendRandom() {
		super.sendRandom();
	}

	/**
	 * Disabled method, should not be called.<br/>
	 * If called, UnsupportedOperationException will be thrown.
	 */
	@Override
	protected void checkSwimPara() {
		throw new UnsupportedOperationException("AM3S not support swim function.");
	}

	/**
	 * Disabled method, should not be called.<br/>
	 * If called, UnsupportedOperationException will be thrown.
	 */
	@Override
	protected void setSwimPara(boolean isOpen, int poolLength, int hours, int minutes, int unit) {
		throw new UnsupportedOperationException("AM3S not support swim function.");
	}

	// Enable it
	@Override
	public void getPicture() {
		super.getPicture();
	}

	// Enable it
	@Override
	public void setPicture(int index) {
		super.setPicture(index);
	}
}
