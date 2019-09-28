package com.ihealth.communication.ins;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.control.AbiProfile;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.privatecontrol.AbiControlSubManager;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Private API for Abi devices. 
 * {@hide}
 */
public class AbiInsSet extends IdentifyIns implements NewDataCallback {

	private static final String TAG = "AbiInsSet";
	
	private static final byte deviceType = (byte) 0xa1;
	private final BaseComm mBaseComm;

	private Context mContext;
	private BaseCommProtocol btcm;
	private String mAddress;
	private String mType;
	private String mAbiType;
	private String mUserName;
	
	/* Product protocol callback */
	private InsCallback mInsCallback;
	
	/* Communication callback */
	private BaseCommCallback mBaseCommCallback;	
	private A1DBtools mA1DBtools;
	/**
	 * a constructor for A1InsSet.
	 * @hide
	 */
	public AbiInsSet(Context context, BaseComm mBaseComm, BaseCommProtocol comm, String userName, String mac, String type, String abiType, InsCallback insCallback, BaseCommCallback mBaseCommCallback){
		this.btcm = comm;
		btcm.setInsSet(AbiInsSet.this);
		this.mContext = context;
		this.mAddress = mac;
		this.mType = type;
		this.mAbiType = abiType;
		this.mInsCallback = insCallback;
		this.mBaseCommCallback = mBaseCommCallback;		
		mA1DBtools = new A1DBtools();
		this.mBaseComm = mBaseComm;

		setInsSetCallbak(insCallback, mac, type, mBaseComm);
	}
	
	/**
	 * Authentication
	 * @hide
	 */
	public void identify() {
		startTimeout(0xfa, AppsDeviceParameters.Delay_Medium, 0xfb, 0xfd, 0xfe);
		btcm.packageData(null, identify(deviceType));
	}

	/**
	 * Get battery of the current Bp device.
	 * @hide
	 */
	public void getBatteryLevel() {

		byte[] returnCommand = new byte[5];
		byte commandID = (byte) 0x20;
		returnCommand[0] = deviceType;
		returnCommand[1] = commandID;
		returnCommand[2] = (byte) 0x00;
		returnCommand[3] = (byte) 0x00;
		returnCommand[4] = (byte) 0x00;
		btcm.packageData(null, returnCommand);
	}

	/* start On-line measurement */
	public void startMeasure() {
		hasGetResult = false;
		byte lastHBP = 0x00;
		boolean samePerson = false;
		byte tempSP = 0x00;
		byte pressureFastLimit = 0x4b;
		byte pressureSlowLimit1H = 0x00;
		byte pressureSlowLimit1L = 0x29;
		byte pressureSlowLimit2H = 0x00;
		byte pressureSlowLimit2L = 0x1b;
		byte pressureLimit = 0x1e;

		if (samePerson)
			tempSP = (byte) 0x55;
		else
			tempSP = (byte) 0x00;

		byte[] returnCommand = new byte[10];
		byte commandID = (byte) 0x31;
		returnCommand[0] = deviceType;
		returnCommand[1] = commandID;
		returnCommand[2] = lastHBP;
		returnCommand[3] = tempSP;
		returnCommand[4] = pressureFastLimit;
		returnCommand[5] = pressureSlowLimit1H;
		returnCommand[6] = pressureSlowLimit1L;
		returnCommand[7] = pressureSlowLimit2H;
		returnCommand[8] = pressureSlowLimit2L;
		returnCommand[9] = pressureLimit;
		btcm.packageData(null, returnCommand);
	}

	/* interrupt the measurement immediately  */
	public void interruptMeasure() {
		byte[] returnCommand = new byte[5];
		byte commandID = (byte) 0x37;
		returnCommand[0] = deviceType;
		returnCommand[1] = commandID;
		returnCommand[2] = (byte) 0x00;
		returnCommand[3] = (byte) 0x00;
		returnCommand[4] = (byte) 0x00;
		btcm.packageData(null, returnCommand);

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				mInsCallback.onNotify(mAddress,mType,AbiProfile.ACTION_STOP_ABI,"");
			}
		},500);
	}

	private void startPressure() {
		byte[] returnCommand = new byte[5];
		byte commandID = (byte) 0x50;
		returnCommand[0] = deviceType;
		returnCommand[1] = commandID;
		returnCommand[2] = (byte) 0x00;
		returnCommand[3] = (byte) 0x00;
		returnCommand[4] = (byte) 0x00;
		btcm.packageData(null, returnCommand);
	}
	
	private void measureFinish(){
		byte[] returnCommand = new byte[5];
		byte commandID = (byte) 0x3f;
		returnCommand[0] = deviceType;
		returnCommand[1] = commandID;
		returnCommand[2] = (byte) 0x00;
		returnCommand[3] = (byte) 0x00;
		returnCommand[4] = (byte) 0x00;
		btcm.packageData(null, returnCommand);
	}
	
	private void ask() {
		byte[] returnCommand = new byte[1];
		returnCommand[0] = deviceType;
		btcm.packageDataAsk(returnCommand);
	}
	
//	private void allPkgOk(byte commandID) {
//        byte[] returnCommand = new byte[2];
//        returnCommand[0] = deviceType;
//        returnCommand[1] = commandID;
//        btcm.packageData(mAddress, returnCommand);
//    } 

	private boolean hasGetResult = false;
	
	@Override
	public void haveNewData(int what, int stateId, byte[] returnData) {
		if (0 == stateId) {
			ask();
		}
		stopTimeout(what);
		JSONObject jsonObject = new JSONObject();
		switch (what) {
		case 0xfb:
			byte[] req = deciphering(returnData, "BPabi", deviceType);
			startTimeout(0xfc, AppsDeviceParameters.Delay_Medium, 0xfd, 0xfe);
			btcm.packageData(null, req);
			break;
		case 0xfd:
			Map<String, String> deviceDetailMap = new HashMap<>();
			deviceDetailMap.put("subtype","abi");
			if(mAbiType.equals(AbiProfile.ABI_ARM)){
				deviceDetailMap.put("position","arm");
				AbiControlSubManager.getInstance().addAbiControlSubUp(mAddress);
			}else if(mAbiType.equals(AbiProfile.ABI_LEG)){
				deviceDetailMap.put("position","leg");
				AbiControlSubManager.getInstance().addAbiControlSubDown(mAddress);
			}else if(mAbiType.equals(AbiProfile.ABI_UNKNOWN)){
				deviceDetailMap.put("position","unknown");
			}

			this.mBaseCommCallback.onConnectionStateChange(mAddress, mType, iHealthDevicesManager.DEVICE_STATE_CONNECTED,0,deviceDetailMap);
			break;
		case 0xfe:
			mBaseComm.disconnect();
			break;
			
		case 0x20:
			int batteryLevel = returnData[0]&0xff;											
			if (!((batteryLevel>0) && (batteryLevel <= 100))) {
				/* if the battery beyond 100, set battery to 100. */
				batteryLevel = 100; 
			}
			try {																	
				jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_MAC, mAddress);	
				jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);	
				jsonObject.put(AbiProfile.BATTERY_ABI, batteryLevel);
				mInsCallback.onNotify(mAddress, mType, AbiProfile.ACTION_BATTERY_ABI, jsonObject.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			break;
			
		case 0x24:
			break;
			
		case 0x30:
			/* Bp device initializing is doing*/ 
			if((returnData[0] & 0x1) == 1){
				try {
    				jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
    				mInsCallback.onNotify(mAddress, mType, AbiProfile.ACTION_ZERO_OVER_ABI, jsonObject.toString());
    			} catch (JSONException e) {
    				e.printStackTrace();
    			}
    			AbiControlSubManager.getInstance().setZore(mAddress);
    			if(AbiControlSubManager.getInstance().notNeedWaitZore(mAddress))
    				startPressure();
    			
            } else {
            	 try {
     				jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
     				mInsCallback.onNotify(mAddress, mType, AbiProfile.ACTION_ZEROING_ABI, jsonObject.toString());
     			} catch (JSONException e) {
     				e.printStackTrace();
     			}
            }
			break;
			
		case 0x31:
			break;
			
		case 0x32:
			/* Bp device initializing is done*/ 
			break;
			
		case 0x3b:
			try {
				jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_MAC, mAddress);
				jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
				mInsCallback.onNotify(mAddress, mType,AbiProfile.ACTION_INTERRUPTED_ABI, jsonObject.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;
			
		case 0x3c:
			convertWave(returnData, false);
			if((returnData[returnData.length - 1] & 0x1) == 1){
				AbiControlSubManager.getInstance().setPressure(mAddress);
    			if(AbiControlSubManager.getInstance().notNneedWaitPressure(mAddress))
    				measureFinish();
			} 
			break;
			
		case 0x3d:
			convertWave(returnData, true);
			if((returnData[returnData.length - 1] & 0x1) == 1){
				AbiControlSubManager.getInstance().setPressure(mAddress);
    			if(AbiControlSubManager.getInstance().notNneedWaitPressure(mAddress))
    				measureFinish();
			} 
			break;
			
		case 0x3e:
			int pressureData=(((returnData[0]&0xff)*256+(returnData[1]&0xff))*300+150)/4096;   
			try {
				jsonObject.put(AbiProfile.BLOOD_PRESSURE_ABI, pressureData);
				mInsCallback.onNotify(mAddress, mType, AbiProfile.ACTION_ONLINE_PRESSURE_ABI, jsonObject.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;
			
		case 0x36:
			if(!hasGetResult){
				hasGetResult = true;
				convertOnline(returnData);
			}
			break;
			
		case 0x38:
			int errorNum = (int)(returnData[0] & 0xff);
			try {
				jsonObject.put(iHealthDevicesManager.IHEALTH_DEVICE_TYPE, mType);
				jsonObject.put(AbiProfile.ERROR_NUM_ABI, errorNum);
				mInsCallback.onNotify(mAddress, mType, AbiProfile.ACTION_ERROR_ABI, jsonObject.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;

			
		default:
			Log.i(TAG, "no method");
			break;
		}
	}

	
	private void convertWave(byte[] datas, boolean heartbeat){
		int pressure = (((datas[0] & 0xff) * 256 + (datas[1] & 0xff)) * 300 + 150) / 4096;               
        byte[] measureData = new byte[8];
        for (int i = 2; i < 10; i++) {
            measureData[i-2] = datas[i];
        }
        String wave = "[" + String.valueOf(measureData[0]&0xff) + ","
        		+ String.valueOf(measureData[1]&0xff) + ","
        		+ String.valueOf(measureData[2]&0xff) + ","
        		+ String.valueOf(measureData[3]&0xff) + ","
        		+ String.valueOf(measureData[4]&0xff) + ","
        		+ String.valueOf(measureData[5]&0xff) + ","
        		+ String.valueOf(measureData[6]&0xff) + ","
        		+ String.valueOf(measureData[7]&0xff) + "]";
        
        JSONObject o = null;
        try {
            o = new JSONObject();
            o.put(AbiProfile.BLOOD_PRESSURE_ABI, pressure);
            o.put(AbiProfile.FLAG_HEARTBEAT_ABI, heartbeat);
            o.put(AbiProfile.PULSE_WAVE_ABI, wave);
            mInsCallback.onNotify(mAddress, mType, AbiProfile.ACTION_ONLINE_PULSE_WAVE_ABI, o.toString());
        } catch (Exception e) {                   
            e.printStackTrace();
        }
       
	}
	

	
	private void convertOnline(byte[] datas){
		int high_pressure_temp = (datas[0]&0xff);
		int low_pressure = (datas[1]&0xff);
		int pulse = (datas[2]&0xff);
		int ahr = 0;
		if (datas[3] != 0) {
			ahr = 1;
		}
		int hsd = (datas[4]&0xff);
		int high_pressure = high_pressure_temp + low_pressure;
		long TS = System.currentTimeMillis() / 1000;
		mA1DBtools.save(mContext, mUserName, mAddress, mType, high_pressure, low_pressure, pulse, TS);
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(AbiProfile.HIGH_BLOOD_PRESSURE_ABI, high_pressure);
			jsonObject.put(AbiProfile.LOW_BLOOD_PRESSURE_ABI, low_pressure);
			jsonObject.put(AbiProfile.PULSE_ABI, pulse);
			jsonObject.put(AbiProfile.MEASUREMENT_AHR_ABI, ahr);
			jsonObject.put(AbiProfile.MEASUREMENT_HSD_ABI, hsd);
			jsonObject.put(AbiProfile.DATAID, MD5.md5String(PublicMethod.getBPDataID(mAddress, pulse+"", TS)));
			mInsCallback.onNotify(mAddress, mType, AbiProfile.ACTION_ONLINE_RESULT_ABI, jsonObject.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void haveNewDataUuid(String uuid, byte[] command) {

	}

	public void destroy(){

	}
}
