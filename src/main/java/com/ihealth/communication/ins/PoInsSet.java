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
import com.ihealth.communication.control.PoProfile;
import com.ihealth.communication.utils.ContinuaDataAnalysis;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by jing on 16/8/30.
 */
public class PoInsSet implements NewDataCallback {
    private static final String TAG = "PoInsSet";
    private Context mContext;
    private BaseComm mComm;
    private BleComm mBleCom;
    private String mAddress;
    private String mType;
    private InsCallback mInsCallback;
    private BleCommContinueProtocol mbleCommContinueProtocol;
    private boolean enableSpotCheckFlag;
    private boolean enableContinuousFlag;
    private boolean enableRecordFlag;

    public PoInsSet(Context context, BaseComm com, BleComm bleComm, String userName, String mac, String type, InsCallback insCallback, BaseCommCallback mBaseCommCallback) {
        Log.p(TAG, Log.Level.INFO, "PoInsSet_Constructor", userName, mac, type);
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

    public boolean getFeatures() {
        Log.p(TAG, Log.Level.INFO, "getFeatures");
        BleComm bleComm;
        if (!(mComm instanceof BleComm))
            return false;
        else
            bleComm = (BleComm) mComm;
        UUID serviceUuid = UUID.fromString(BleConfig.UUID_PO_SERVICE);
        UUID charaticUuid = UUID.fromString(BleConfig.UUID_PO_READ);
        return bleComm.Obtain(mAddress, serviceUuid, charaticUuid);
    }

    /**
     * get PLX Spot-check Measurement
     */
    public void getSpotCheck() {
        Log.p(TAG, Log.Level.INFO, "getSpotCheck");
        if (!enableSpotCheckFlag) {
            //使能PO的Measurement Notify，之后PO会自动发送数据上来
            mBleCom.getService(mAddress, BleConfig.UUID_PO_SERVICE, BleConfig.UUID_PO_SEND_AND_RECEIVE, BleConfig.UUID_PO_RECEIVE, BleConfig.UUID_180A, true);
        }
    }

    /**
     * get PLX Continuous Measurement
     */
    public void getContinuous() {
        Log.p(TAG, Log.Level.INFO, "getContinuous");
        if (!enableContinuousFlag) {
            //使能PO的Measurement Notify，之后PO会自动发送数据上来
            mBleCom.getService(mAddress, BleConfig.UUID_PO_SERVICE, BleConfig.UUID_PO_SEND_AND_RECEIVE, BleConfig.UUID_PO_RECEIVE_CONTINUOUS, BleConfig.UUID_180A, false);
        }
    }

    /**
     * get record
     */
    public void getRecord() {
        Log.p(TAG, Log.Level.INFO, "getRecord");
        //未使能Indicate需要先使能； 如果已经使能，则直接发送命令
        if (!enableRecordFlag) {
            //使能PO的Measurement Context Notify，之后PO会自动发送数据上来
            mBleCom.getService(mAddress, BleConfig.UUID_PO_SERVICE, BleConfig.UUID_PO_SEND_AND_RECEIVE, BleConfig.UUID_PO_SEND_AND_RECEIVE, BleConfig.UUID_180A, true);
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
                    jsonObject.put(PoProfile.BATTERY_PO, batteryLevel);
                    mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_BATTERY_PO, jsonObject.toString());
                } catch (JSONException e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                break;
            }
            case BleConfig.UUID_PO_RECEIVE: {
                if (command == null) {
                    enableSpotCheckFlag = true;
                    return;
                }
                //解析标准Continua协议
                try {
                    JSONObject temp = decodeSpotCheckResult(command);
                    if (temp == null) {
                        return;
                    }
                    mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_SPOT_CHECK_CPO, temp.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                break;
            }
            case BleConfig.UUID_PO_RECEIVE_CONTINUOUS: {
                if (command == null) {
                    enableContinuousFlag = true;
                    return;
                }
                //解析标准Continua协议
                try {
                    JSONObject temp = decodeContinuousResult(command);
                    if (temp == null) {
                        return;
                    }
                    mInsCallback.onNotify(mAddress, mType, PoProfile.ACTION_CONTINUOUS_CPO, temp.toString());
                } catch (Exception e) {
                    Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
                }
                break;
            }
            case BleConfig.UUID_PO_SEND_AND_RECEIVE:
                if (command == null) {
                    enableRecordFlag = true;
                    //需要依据Continua 写命令给设备，写成功后，设备会发送Record上来
                    return;
                }
                //解析标准Continua协议

                break;
        }
    }

    private JSONObject decodeSpotCheckResult(byte[] command) {
        if (command.length < 5) {
            return null;
        }

        //数据解析过程
        ContinuaDataAnalysis dataAnalysis = new ContinuaDataAnalysis(command, command.length);
        JSONObject data = new JSONObject();
        try {
            byte flag = dataAnalysis.read8ByteValue();
            //有无时间戳
            int timestampFlag = flag & 0x01;
            data.put(PoProfile.CPO_TIMESTAMP_FLAG, timestampFlag);
            //有无测量状态字段
            int measurementStatusFlag = (flag >> 1) & 0x01;
            data.put(PoProfile.CPO_MEASUREMENT_STATUS_FLAG, measurementStatusFlag);
            //有无设备和传感器状态字段
            int DSStatusFlag = (flag >> 2) & 0x01;
            data.put(PoProfile.CPO_DS_STATUS_FLAG, DSStatusFlag);
            //有无脉膊振幅索引
            int PAIndexFlag = (flag >> 3) & 0x01;
            data.put(PoProfile.CPO_PA_INDEX_FLAG, PAIndexFlag);
            //有无设置设备闹钟
            int clockSetFlag = (flag >> 4) & 0x01;
            data.put(PoProfile.CPO_CLOCK_SET_FLAG, clockSetFlag);

            data.put(PoProfile.CPO_SPO2_DATA, dataAnalysis.readSFloatValue());
            data.put(PoProfile.CPO_PR_DATA, dataAnalysis.readSFloatValue());

            //测量时间
            if (timestampFlag == 1) {
                String measureTime = dataAnalysis.readDateValue();
                data.put(PoProfile.CPO_TIMESTAMP_DATA, measureTime);
            }

            if (measurementStatusFlag == 1) {
                short status = dataAnalysis.read16ByteValue();
                data.put(PoProfile.CPO_MEASUREMENT_ONGOING_DATA, (status >> 5) & 0x01);
                data.put(PoProfile.CPO_EARLY_ESTIMATED_DATA, (status >> 6) & 0x01);
                data.put(PoProfile.CPO_VALIDATED_DATA, (status >> 7) & 0x01);
                data.put(PoProfile.CPO_FULLY_QUALIFIED_DATA, (status >> 8) & 0x01);
                data.put(PoProfile.CPO_MEASUREMENT_STORAGE_DATA, (status >> 9) & 0x01);
                data.put(PoProfile.CPO_DEMONSTRATION_DATA, (status >> 10) & 0x01);
                data.put(PoProfile.CPO_TESTING_DATA, (status >> 11) & 0x01);
                data.put(PoProfile.CPO_CALIBRATION_ONGOING_DATA, (status >> 12) & 0x01);
                data.put(PoProfile.CPO_MEASUREMENT_UNAVAILABLE_DATA, (status >> 13) & 0x01);
                data.put(PoProfile.CPO_QUESTIONABLE_DETECTED_DATA, (status >> 14) & 0x01);
                data.put(PoProfile.CPO_INVALID_DETECTED_DATA, (status >> 15) & 0x01);
            }

            if (DSStatusFlag == 1) {
                int status = dataAnalysis.read24ByteValue();
                data.put(PoProfile.CPO_EXTENDED_DISPLAY_UPDATE_ONGOING_DATA, status & 0x01);
                data.put(PoProfile.CPO_EQUIPMENT_MALFUNCTION_DATA, (status >> 1) & 0x01);
                data.put(PoProfile.CPO_SIGNAL_PROCESSING_IRREGULARITY_DATA, (status >> 2) & 0x01);
                data.put(PoProfile.CPO_INADEQUATE_SIGNAL_DETECTED_DATA, (status >> 3) & 0x01);
                data.put(PoProfile.CPO_POOR_SIGNAL_DETECTED_DATA, (status >> 4) & 0x01);
                data.put(PoProfile.CPO_LOW_PERFUSION_DETECTED_DATA, (status >> 5) & 0x01);
                data.put(PoProfile.CPO_ERRATIC_SIGNAL_DETECTED_DATA, (status >> 6) & 0x01);
                data.put(PoProfile.CPO_NONPULSATILE_SIGNAL_DETECTED_DATA, (status >> 7) & 0x01);
                data.put(PoProfile.CPO_QUESTIONABLE_PULSE_DETECTED_DATA, (status >> 8) & 0x01);
                data.put(PoProfile.CPO_SIGNAL_ANALYSIS_ONGOING_DATA, (status >> 9) & 0x01);
                data.put(PoProfile.CPO_SENSOR_INTERFACE_DETECTED_DATA, (status >> 10) & 0x01);
                data.put(PoProfile.CPO_SENSOR_UNCONNECTED_TO_USER_DATA, (status >> 11) & 0x01);
                data.put(PoProfile.CPO_UNKNOWN_SENSOR_CONNECTED_DATA, (status >> 12) & 0x01);
                data.put(PoProfile.CPO_SENSOR_DISPLACED_DATA, (status >> 13) & 0x01);
                data.put(PoProfile.CPO_SENSOR_MALFUNCTION_DATA, (status >> 14) & 0x01);
                data.put(PoProfile.CPO_SENSOR_DISCONNECTED_DATA, (status >> 15) & 0x01);
            }

            if (PAIndexFlag == 1) {
                data.put(PoProfile.CPO_PULSE_AMPLITUDE_INDEX_DATA, dataAnalysis.readSFloatValue());
            }
        } catch (JSONException exception) {
            Log.p(TAG, Log.Level.WARN, "Exception", exception.getMessage());
        }

        return data;
    }

    private JSONObject decodeContinuousResult(byte[] command) {
        if (command.length < 5) {
            return null;
        }

        //数据解析过程
        ContinuaDataAnalysis dataAnalysis = new ContinuaDataAnalysis(command, command.length);
        JSONObject data = new JSONObject();
        try {
            byte flag = dataAnalysis.read8ByteValue();
            int fastFlag = flag & 0x01;
            data.put(PoProfile.CPO_SPO2_PR_FAST_FLAG, fastFlag);
            int slowFlag = (flag >> 1) & 0x01;
            data.put(PoProfile.CPO_SPO2_PR_SLOW_FLAG, slowFlag);
            //有无测量状态字段
            int measurementStatusFlag = (flag >> 2) & 0x01;
            data.put(PoProfile.CPO_MEASUREMENT_STATUS_FLAG, measurementStatusFlag);
            //有无设备和传感器状态字段
            int DSStatusFlag = (flag >> 3) & 0x01;
            data.put(PoProfile.CPO_DS_STATUS_FLAG, DSStatusFlag);
            //有无脉膊振幅索引
            int PAIndexFlag = (flag >> 4) & 0x01;
            data.put(PoProfile.CPO_PA_INDEX_FLAG, PAIndexFlag);

            data.put(PoProfile.CPO_SPO2_DATA, dataAnalysis.readSFloatValue());
            data.put(PoProfile.CPO_PR_DATA, dataAnalysis.readSFloatValue());

            if (fastFlag == 1) {
                data.put(PoProfile.CPO_SPO2_FAST_DATA, dataAnalysis.readSFloatValue());
                data.put(PoProfile.CPO_PR_FAST_DATA, dataAnalysis.readSFloatValue());
            }

            if (slowFlag == 1) {
                data.put(PoProfile.CPO_SPO2_SLOW_DATA, dataAnalysis.readSFloatValue());
                data.put(PoProfile.CPO_PR_SLOW_DATA, dataAnalysis.readSFloatValue());
            }

            if (measurementStatusFlag == 1) {
                short status = dataAnalysis.read16ByteValue();
                data.put(PoProfile.CPO_MEASUREMENT_ONGOING_DATA, (status >> 5) & 0x01);
                data.put(PoProfile.CPO_EARLY_ESTIMATED_DATA, (status >> 6) & 0x01);
                data.put(PoProfile.CPO_VALIDATED_DATA, (status >> 7) & 0x01);
                data.put(PoProfile.CPO_FULLY_QUALIFIED_DATA, (status >> 8) & 0x01);
                data.put(PoProfile.CPO_MEASUREMENT_STORAGE_DATA, (status >> 9) & 0x01);
                data.put(PoProfile.CPO_DEMONSTRATION_DATA, (status >> 10) & 0x01);
                data.put(PoProfile.CPO_TESTING_DATA, (status >> 11) & 0x01);
                data.put(PoProfile.CPO_CALIBRATION_ONGOING_DATA, (status >> 12) & 0x01);
                data.put(PoProfile.CPO_MEASUREMENT_UNAVAILABLE_DATA, (status >> 13) & 0x01);
                data.put(PoProfile.CPO_QUESTIONABLE_DETECTED_DATA, (status >> 14) & 0x01);
                data.put(PoProfile.CPO_INVALID_DETECTED_DATA, (status >> 15) & 0x01);
            }

            if (DSStatusFlag == 1) {
                int status = dataAnalysis.read24ByteValue();
                data.put(PoProfile.CPO_EXTENDED_DISPLAY_UPDATE_ONGOING_DATA, status & 0x01);
                data.put(PoProfile.CPO_EQUIPMENT_MALFUNCTION_DATA, (status >> 1) & 0x01);
                data.put(PoProfile.CPO_SIGNAL_PROCESSING_IRREGULARITY_DATA, (status >> 2) & 0x01);
                data.put(PoProfile.CPO_INADEQUATE_SIGNAL_DETECTED_DATA, (status >> 3) & 0x01);
                data.put(PoProfile.CPO_POOR_SIGNAL_DETECTED_DATA, (status >> 4) & 0x01);
                data.put(PoProfile.CPO_LOW_PERFUSION_DETECTED_DATA, (status >> 5) & 0x01);
                data.put(PoProfile.CPO_ERRATIC_SIGNAL_DETECTED_DATA, (status >> 6) & 0x01);
                data.put(PoProfile.CPO_NONPULSATILE_SIGNAL_DETECTED_DATA, (status >> 7) & 0x01);
                data.put(PoProfile.CPO_QUESTIONABLE_PULSE_DETECTED_DATA, (status >> 8) & 0x01);
                data.put(PoProfile.CPO_SIGNAL_ANALYSIS_ONGOING_DATA, (status >> 9) & 0x01);
                data.put(PoProfile.CPO_SENSOR_INTERFACE_DETECTED_DATA, (status >> 10) & 0x01);
                data.put(PoProfile.CPO_SENSOR_UNCONNECTED_TO_USER_DATA, (status >> 11) & 0x01);
                data.put(PoProfile.CPO_UNKNOWN_SENSOR_CONNECTED_DATA, (status >> 12) & 0x01);
                data.put(PoProfile.CPO_SENSOR_DISPLACED_DATA, (status >> 13) & 0x01);
                data.put(PoProfile.CPO_SENSOR_MALFUNCTION_DATA, (status >> 14) & 0x01);
                data.put(PoProfile.CPO_SENSOR_DISCONNECTED_DATA, (status >> 15) & 0x01);
            }

            if (PAIndexFlag == 1) {
                data.put(PoProfile.CPO_PULSE_AMPLITUDE_INDEX_DATA, dataAnalysis.readSFloatValue());
            }
        } catch (JSONException exception) {
            Log.p(TAG, Log.Level.WARN, "Exception", exception.getMessage());
        }

        return data;
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
