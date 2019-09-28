package com.ihealth.communication.ins;

import android.content.Context;

import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.ble.BleComm;
import com.ihealth.communication.base.ble.BleConfig;
import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BleCommContinueProtocol;
import com.ihealth.communication.control.HsProfile;
import com.ihealth.communication.utils.ContinuaDataAnalysis;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by jing on 16/8/30.
 */
public class HsInsSet implements NewDataCallback {
    private static final String TAG = "HsInsSet";
    private Context mContext;
    private BaseComm mComm;
    private BleComm mBleCom;
    private String mAddress;
    private String mType;
    private InsCallback mInsCallback;
    private BleCommContinueProtocol mbleCommContinueProtocol;
    private boolean enableMeasurementFlag;

    public HsInsSet(Context context, BaseComm com, BleComm bleComm, String userName, String mac, String type, InsCallback insCallback, BaseCommCallback mBaseCommCallback) {
        Log.p(TAG, Log.Level.INFO, "HsInsSet_Constructor", userName, mac, type);
        this.mContext = context;
        this.mComm = com;
        this.mBleCom = bleComm;
        this.mAddress = mac;
        this.mType = type;
        this.mInsCallback = insCallback;
        this.mbleCommContinueProtocol = new BleCommContinueProtocol(com, mac, this);
    }

    public boolean getBattery() {
        Log.p(TAG, Log.Level.INFO, "getBattery");
        BleComm bleComm;
        if (!(mComm instanceof BleComm))
            return false;
        else
            bleComm = (BleComm) mComm;
        UUID serviceUuid = UUID.fromString(BleConfig.UUID_BTM_BATTERY_SERVICE);
        UUID charaticUuid = UUID.fromString(BleConfig.UUID_BTM_BATTERY_LEVEL_CHARACTERISTIC);
        return bleComm.Obtain(mAddress, serviceUuid, charaticUuid);
    }

    /**
     * get the feature of measurement
     */
    public boolean getFeature() {
        Log.p(TAG, Log.Level.INFO, "getFeature");
        BleComm bleComm;
        if (!(mComm instanceof BleComm))
            return false;
        else
            bleComm = (BleComm) mComm;
        UUID serviceUuid = UUID.fromString(BleConfig.UUID_HS_SERVICE);
        UUID charaticUuid = UUID.fromString(BleConfig.UUID_HS_READ);
        return bleComm.Obtain(mAddress, serviceUuid, charaticUuid);
    }

    /**
     * get measurement context
     */
    public void getMeasurement() {
        Log.p(TAG, Log.Level.INFO, "getMeasurement");
        if (!enableMeasurementFlag) {
            //使能HS的Measurement Notify，之后HS会自动发送数据上来
            mBleCom.getService(mAddress, BleConfig.UUID_HS_SERVICE, null, BleConfig.UUID_HS_RECEIVE, BleConfig.UUID_180A, true);
        }
    }


    @Override
    public void haveNewData(int what, int stateId, byte[] command) {

    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", uuid, ByteBufferUtil.Bytes2HexString(command));
        switch (uuid) {
            case BleConfig.UUID_BTM_BATTERY_LEVEL_CHARACTERISTIC: {
                int batteryLevel = command[0];
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(HsProfile.CHS_BATTERY, batteryLevel);
                    mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_BATTERY_CHS, jsonObject.toString());
                } catch (JSONException e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                break;
            }
            case BleConfig.UUID_HS_RECEIVE: {
                if (command == null) {
                    enableMeasurementFlag = true;
                    return;
                }
                //解析标准Continua协议
                try {
                    JSONObject temp = decodeCHSResult(command);
                    if (temp == null) {
                        return;
                    }
                    mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_CHS_MEASUREMENT_DATA, temp.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                break;
            }
            case BleConfig.UUID_HS_READ: {
                if (command == null) {
                    enableMeasurementFlag = true;
                    return;
                }
                //解析标准Continua协议
//                JSONObject jsonObject = new JSONObject();
//                JSONObject temp = null;
//                try {
//                    temp = decodeCHSFeature(command);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                if (temp == null) {
//                    return;
//                }
//                try {
//                    jsonObject.put(HsProfile.CHS_FEATURE_DATA, temp);
//                    mInsCallback.onNotify(mAddress, mType, HsProfile.ACTION_CHS_FEATURE_DATA, jsonObject.toString());
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
                break;
            }
        }
    }

    /**
     * This method decode bp value received from Health bp device
     */
    private JSONObject decodeCHSResult(byte[] command) throws Exception {
        if (command.length < 3) {
            return null;
        }

        //数据解析过程
        ContinuaDataAnalysis dataAnalysis = new ContinuaDataAnalysis(command, command.length);
        JSONObject offlineData = new JSONObject();
        try {
            byte flag = dataAnalysis.read8ByteValue();
            //计量单位
            int unitFlag = flag & 0x01;
            offlineData.put(HsProfile.CHS_UNIT_FLAG, unitFlag);
            //有无时间戳
            int timestampFlag = (flag >> 1) & 0x01;
            offlineData.put(HsProfile.CHS_TIMESTAMP_FLAG, timestampFlag);
            //有无用户ID
            int userIDFlag = (flag >> 2) & 0x01;
            offlineData.put(HsProfile.CHS_USER_ID_FLAG, userIDFlag);
            //有无体重和身高
            int BMIFlag = (flag >> 3) & 0x01;
            offlineData.put(HsProfile.CHS_BMI_AND_Height_FLAG, BMIFlag);

            if (unitFlag == 0) {
                offlineData.put(HsProfile.CHS_WEIGHT_SI, dataAnalysis.readUInt16Value());
            } else {
                offlineData.put(HsProfile.CHS_WEIGHT_IMPERIAL, dataAnalysis.readUInt16Value());
            }

            //测量时间
            if (timestampFlag == 1) {
                String measureTime = dataAnalysis.readDateValue();
                offlineData.put(HsProfile.CHS_TIME_STAMP, measureTime);
            }

            //用户ID
            if (userIDFlag == 1) {
                int userID = dataAnalysis.readUInt8Value() & 0xFF;
                offlineData.put(HsProfile.CHS_USER_ID, userID);
            }

            //BMI
            if (BMIFlag == 1) {
                offlineData.put(HsProfile.CHS_BMI, dataAnalysis.readUInt16Value());
                if (unitFlag == 0) {
                    offlineData.put(HsProfile.CHS_HEIGHT_SI, dataAnalysis.readUInt16Value());
                } else {
                    offlineData.put(HsProfile.CHS_HEIGHT_IMPERIAL, dataAnalysis.readUInt16Value());
                }
            }
        } catch (JSONException exception) {
            Log.p(TAG, Log.Level.WARN, "Exception", exception.getMessage());
        }

        return offlineData;
    }

    public void destroy() {
        Log.p(TAG, Log.Level.INFO, "destroy");
        mInsCallback = null;
        mContext = null;
        if (mbleCommContinueProtocol != null)
            mbleCommContinueProtocol.destroy();
        mbleCommContinueProtocol = null;
    }
}
