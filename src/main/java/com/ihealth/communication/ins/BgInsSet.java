package com.ihealth.communication.ins;

import android.content.Context;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.ble.BleComm;
import com.ihealth.communication.base.ble.BleConfig;
import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BleCommContinueProtocol;
import com.ihealth.communication.control.BgProfile;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.ContinuaDataAnalysis;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by jing on 16/8/30.
 */
public class BgInsSet implements NewDataCallback {

    private static final String TAG = "BgInsSet";
    private Context mContext;
    private BaseComm mComm;
    private BleComm mBleCom;
    private String mAddress;
    private String mType;
    private InsCallback mInsCallback;
    private BleCommContinueProtocol mbleCommContinueProtocol;
    private boolean enableMeasurementFlag = false;
    private boolean enableMeasurementContextFlag = false;
    private boolean enableRecordFlag = false;

    public BgInsSet(Context context, BaseComm com,  BleComm bleComm, String userName, String mac, String type, InsCallback insCallback, BaseCommCallback mBaseCommCallback){
        this.mContext = context;
        this.mComm = com;
        this.mBleCom = bleComm;
        this.mAddress = mac;
        this.mType = type;
        this.mInsCallback = insCallback;
        this.mbleCommContinueProtocol = new BleCommContinueProtocol(com, mac, this);
    }


    /**
     * get battery
     */
    public boolean getBattery(){
        BleComm bleComm = null;
        if (!(mComm instanceof BleComm))
            return false;
        else
            bleComm = (BleComm)mComm;
        UUID serviceUuid = UUID.fromString(BleConfig.UUID_BTM_BATTERY_SERVICE);
        UUID charaticUuid = UUID.fromString(BleConfig.UUID_BTM_BATTERY_LEVEL_CHARACTERISTIC);
        return bleComm.Obtain(mAddress, serviceUuid, charaticUuid);
    }

    /**
     * get the feature of measurement
     */
    public boolean getFeature(){
        BleComm bleComm = null;
        if (!(mComm instanceof BleComm))
            return false;
        else
            bleComm = (BleComm)mComm;
        UUID serviceUuid = UUID.fromString(BleConfig.UUID_BG_SERVICE);
        UUID charaticUuid = UUID.fromString(BleConfig.UUID_BG_READ);
        return bleComm.Obtain(mAddress, serviceUuid, charaticUuid);
    }

    /**
     * get measurement context
     */
    public void getMeasurement(){
        if (enableMeasurementFlag == false) {
            //使能BG的Measurement Notify，之后BG会自动发送数据上来
            mBleCom.getService(mAddress, BleConfig.UUID_BG_SERVICE, BleConfig.UUID_BG_SEND_AND_RECEIVE, BleConfig.UUID_BG_RECEIVE, BleConfig.UUID_180A, false);
        }
    }

    /**
     * get measurement context
     */
    public void getMeasurementContext(){
        if (enableMeasurementContextFlag == false) {
            //使能BG的Measurement Context Notify，之后BG Context会自动发送数据上来
            mBleCom.getService(mAddress, BleConfig.UUID_BG_SERVICE, BleConfig.UUID_BG_SEND_AND_RECEIVE, BleConfig.UUID_BG_RECEIVE_CONTENT, BleConfig.UUID_180A, false);
        }
    }

    /**
     * get record
     */
    public void getRecord(){
        //未使能Indicate需要先使能； 如果已经使能，则直接发送命令
        if (enableRecordFlag == false) {
            //使能BG的Measurement Context Notify，之后BG Context会自动发送数据上来
            mBleCom.getService(mAddress, BleConfig.UUID_BG_SERVICE, BleConfig.UUID_BG_SEND_AND_RECEIVE, BleConfig.UUID_BG_SEND_AND_RECEIVE, BleConfig.UUID_180A, true);
        } else {
            byte[] returnCommand = new byte[4];
            returnCommand[0] = 0x01;   //0x01   0x04
            returnCommand[1] = 0x01;
            returnCommand[2] = 0x00;
            returnCommand[3] = 0x00;
            mComm.sendData(mAddress, returnCommand);
        }
    }


    @Override
    public void haveNewData(int what, int stateId, byte[] command) {

    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {
        if (uuid.equals(BleConfig.UUID_BTM_BATTERY_LEVEL_CHARACTERISTIC)){
            int batteryLevel = command[0];
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(BgProfile.BATTERY_CBG, batteryLevel);
                mInsCallback.onNotify(mAddress, mType,  BgProfile.ACTION_BATTERY_CBG, jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if(uuid.equals(BleConfig.UUID_BG_RECEIVE)){
            if (command == null) {
                enableMeasurementFlag = true;
                Log.i(TAG, "Enable indicate success");
                return;
            }
            //解析标准Continua协议
            JSONObject temp = null;
            try {
                temp = decodeBGResult(command, command.length);
            }catch (Exception e){
                e.printStackTrace();
            }

            if (temp == null) {
                return;
            }
            mInsCallback.onNotify(mAddress, mType, BgProfile.ACTION_MEASUREMENT_RESULT_CBG, temp.toString());

        } else if(uuid.equals(BleConfig.UUID_BG_RECEIVE_CONTENT)){
            if (command == null) {
                enableMeasurementContextFlag = true;
                Log.i(TAG, "Enable indicate success");
                return;
            }
            //解析标准Continua协议
            JSONObject temp = null;
            try {
                temp = decodeBGResultContext(command, command.length);
            }catch (Exception e){
                e.printStackTrace();
            }

            if (temp == null) {
                return;
            }
            mInsCallback.onNotify(mAddress, mType, BgProfile.ACTION_MEASUREMENT_CONTEXT_CBG, temp.toString());

        } else if(uuid.equals(BleConfig.UUID_BG_SEND_AND_RECEIVE)) {
            if (command == null) {
                enableRecordFlag = true;
                Log.i(TAG, "Enable indicate success for iHealth");
                //需要依据Continua 写命令给设备，写成功后，设备会发送Record上来
                byte[] returnCommand = new byte[4];
                returnCommand[0] = 0x01;   //0x01   0x04
                returnCommand[1] = 0x01;
                returnCommand[2] = 0x00;
                returnCommand[3] = 0x00;
                mComm.sendData(mAddress, returnCommand);
                return;
            } else {
                //解析标准Continua协议
                Log.i(TAG, "Record:" + ByteBufferUtil.Bytes2HexString(command));
            }

        } else if(uuid.equals(BleConfig.UUID_BG_READ)) {
            //解析标准Continua协议
            JSONObject temp = null;
            try {
                temp = decodeBGFeature(command, command.length);
            }catch (Exception e){
                e.printStackTrace();
            }

            if (temp == null) {
                return;
            }
            mInsCallback.onNotify(mAddress, mType, BgProfile.ACTION_FEATURE_CBG, temp.toString());
        }
    }


    private JSONObject decodeBGResult(byte[] data, int length) {
        if (length<10) {
            Log.e(TAG,"Invalidate data");
            return null;
        }
        JSONObject jsonObject = new JSONObject();

        try {

            ContinuaDataAnalysis dataAnalysis = new ContinuaDataAnalysis(data, length);
            byte measureFlag = dataAnalysis.read8ByteValue();

            //有无时间偏差标识
            int timeOffsetPresentFlag = measureFlag & 0x01;
            jsonObject.put(BgProfile.CBGINFO_TIME_OFFSET_FLAG,timeOffsetPresentFlag);


            //有无测量结果、血液类型、采血位置标识
            int typeAndLocationFlag = (measureFlag>>1) & 0x01;
            jsonObject.put(BgProfile.CBGINFO_TYPE_LOCATION_FLAG,typeAndLocationFlag);

            //血糖单位标识
            int unitFlag = (measureFlag>>2) & 0x01;
            jsonObject.put(BgProfile.CBGINFO_UNIT,unitFlag);

            //传感器状态标识
            int sensorStatusFlag = (measureFlag>>3) & 0x01;
            jsonObject.put(BgProfile.CBGINFO_SENSOR_STATUS_FLAG,sensorStatusFlag);

            //补充信息标识
            int contextInformationFlag = (measureFlag>>4) & 0x01;
            jsonObject.put(BgProfile.CBGINFO_CONTEXT_INFORMATION_FLAG,contextInformationFlag);


            //序列号
            int sequenceNumber = dataAnalysis.readUInt16Value();
            jsonObject.put(BgProfile.SEQUENCE_NUMBER_CBG,sequenceNumber);

            //测量时间
            String measureTime = dataAnalysis.readDateValue();
            jsonObject.put(BgProfile.MEASURE_TIME_CBG,measureTime);

            //时间偏差
            if (timeOffsetPresentFlag == 1) {
                int timeOffset = dataAnalysis.readSInt16Value();
                jsonObject.put(BgProfile.TIME_OFFSET_CBG,timeOffset);
            }

            //血糖浓度
            if (typeAndLocationFlag == 1) {
                float glucoseConcentration = dataAnalysis.readSFloatValue();
                jsonObject.put(BgProfile.RESULT_CBG,glucoseConcentration);

                byte typeAndLocation = dataAnalysis.readNibbleValue();

                int type = typeAndLocation&0x0f;
                jsonObject.put(BgProfile.TYPE_MEASUREMENT_CBG,type);

                int location = (typeAndLocation>>4)&0x0f;
                jsonObject.put(BgProfile.LOCATION_MEASUREMENT_CBG,location);


            }

            //传感器状态
            if (sensorStatusFlag == 1) {
                short status = dataAnalysis.read16ByteValue();

                //低电  lowBattery
                jsonObject.put(BgProfile.CBGINFO_LOW_BATTERY, status&0x01);
                //传感器故障  sensorMalfunction
                jsonObject.put(BgProfile.CBGINFO_SENSOR_MAL_FUNCTION, (status>>1)&0x01);
                //样本量不足  insufficientSample
                jsonObject.put(BgProfile.CBGINFO_INSUFFICIENT_SAMPLE, (status>>2)&0x01);
                //插入错误    stripInsertionError
                jsonObject.put(BgProfile.CBGINFO_STRIP_INSERTION_ERROR, (status>>3)&0x01);
                //试条类型错误   stripTypeIncorrect
                jsonObject.put(BgProfile.CBGINFO_TYPE_INCORRECT, (status>>4)&0x01);
                //测量结果超出范围  resultHigher
                jsonObject.put(BgProfile.CBGINFO_RESULT_HIGHER, (status>>5)&0x01);
                //测量结果低于范围  resultLower
                jsonObject.put(BgProfile.CBGINFO_RESULT_LOWER, (status>>6)&0x01);
                //传感器温度过高    sensorTemperatureTooHigh
                jsonObject.put(BgProfile.CBGINFO_TEMPERATURE_TOO_HIGH, (status>>7)&0x01);
                //传感器温度过低    sensorTemperatureTooLow
                jsonObject.put(BgProfile.CBGINFO_TEMPERATURE_TOO_LOW, (status>>8)&0x01);
                //试条拔出太早     stripPullTooEarly
                jsonObject.put(BgProfile.CBGINFO_STRIP_PULL_TOO_EARLY, (status>>9)&0x01);
                //传感器故障       sensorFault
                jsonObject.put(BgProfile.CBGINFO_SENSOR_FAULT, (status>>10)&0x01);
                //时间异常         timeFault
                jsonObject.put(BgProfile.CBGINFO_TIME_FAULT, (status>>11)&0x01);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return jsonObject;
    }


    private JSONObject decodeBGResultContext(byte[] data, int length) {
        if (length<3) {
            Log.e(TAG,"Invalidate data");
            return null;
        }
        JSONObject jsonObject = new JSONObject();

        try {

            ContinuaDataAnalysis dataAnalysis = new ContinuaDataAnalysis(data, length);

            //Flag
            byte status = dataAnalysis.read8ByteValue();

            int carbohydratePresentFlag = status & 0x01;
            jsonObject.put(BgProfile.CONTEXT_CARBOGYDRATE_PRESNET_CBG,carbohydratePresentFlag);

            int mealPresentFlag = (status>>1) & 0x01;
            jsonObject.put(BgProfile.CONTEXT_MEAL_PRESNET_CBG,mealPresentFlag);

            int testerHealthPresentFlag = (status>>2) & 0x01;
            jsonObject.put(BgProfile.CONTEXT_TESTER_HEALTH_PRESNET_CBG,testerHealthPresentFlag);

            int exercisePresentFlag = (status>>3) & 0x01;
            jsonObject.put(BgProfile.CONTEXT_EXERCISE_PRESNET_CBG,exercisePresentFlag);

            int medicationPresentFlag = (status>>4) & 0x01;
            jsonObject.put(BgProfile.CONTEXT_MEDICATION_PRESNET_CBG,medicationPresentFlag);

            int medicationUnitFlag = (status>>5) & 0x01;
            jsonObject.put(BgProfile.CONTEXT_MEDICATION_UNIT_CBG,medicationUnitFlag);

            int hba1cPresentFlag = (status>>6) & 0x01;
            jsonObject.put(BgProfile.CONTEXT_HBA1C_PRESNET_CBG,hba1cPresentFlag);

            int extendedFlagPresentFlag = (status>>7) & 0x01;
            jsonObject.put(BgProfile.CONTEXT_EXTENDED_FLAG_PRESNET_CBG,extendedFlagPresentFlag);

            //Sequence Number
            int sequenceNumber = dataAnalysis.readUInt16Value();
            jsonObject.put(BgProfile.CONTEXT_SEQUENCE_NUMBER_CBG,sequenceNumber);

            //Extended Flags
            if (extendedFlagPresentFlag == 1) {
                byte extendedFlag = dataAnalysis.read8ByteValue();
                jsonObject.put(BgProfile.CONTEXT_EXTENDED_FLAG_CBG, extendedFlag);
            }

            //Carbohydrate ID
            if (carbohydratePresentFlag == 1) {
                short carbohydrateID = dataAnalysis.readUInt8Value();
                jsonObject.put(BgProfile.CONTEXT_CARBOHYDRATE_ID_CBG, carbohydrateID);

                //Carbohydrate
                float carbohydrate = dataAnalysis.readSFloatValue();
                jsonObject.put(BgProfile.CONTEXT_CARBOHYDRATE_CBG, carbohydrate);
            }

            //Meal
            if (mealPresentFlag == 1) {
                short meal = dataAnalysis.readUInt8Value();
                jsonObject.put(BgProfile.CONTEXT_MEAL_CBG, meal);
            }

            //Tester
            if (testerHealthPresentFlag == 1) {
                byte carbohydrateID = dataAnalysis.readNibbleValue();
                byte tester = (byte) (carbohydrateID&0x0F);
                jsonObject.put(BgProfile.CONTEXT_TESTER_CBG, tester);

                //Health
                byte health = (byte) ((carbohydrateID>>4)&0x0F);
                jsonObject.put(BgProfile.CONTEXT_HEALTH_CBG, health);
            }

            //Exercise Duration
            if (exercisePresentFlag == 1) {
                int exerciseDuration = dataAnalysis.readUInt16Value();
                jsonObject.put(BgProfile.CONTEXT_EXERCISE_DURATION_CBG, exerciseDuration);

                //Exercise Intensity
                short exerciseIntensity = dataAnalysis.readUInt8Value();
                jsonObject.put(BgProfile.CONTEXT_EXERCISE_INTENSITY_CBG, exerciseIntensity);
            }

            //Medication ID
            if (medicationPresentFlag == 1) {
                int medicationID = dataAnalysis.readUInt8Value();
                jsonObject.put(BgProfile.CONTEXT_MEDICATION_ID_CBG, medicationID);

                //Medication
                float medication = dataAnalysis.readSFloatValue();
                jsonObject.put(BgProfile.CONTEXT_MEDICATION_CBG, medication);
            }

            //HbA1c
            if (hba1cPresentFlag == 1) {
                float hba1c = dataAnalysis.readSFloatValue();
                jsonObject.put(BgProfile.CONTEXT_HBA1C_CBG, hba1c);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


    private JSONObject decodeBGFeature(byte[] data, int length) {
        if (length<2) {
            Log.e(TAG,"Invalidate data");
            return null;
        }
        JSONObject jsonObject = new JSONObject();

        try {

            ContinuaDataAnalysis dataAnalysis = new ContinuaDataAnalysis(data, length);

            short status = dataAnalysis.read16ByteValue();

            jsonObject.put(BgProfile.FEATURE_LOW_BATTERY_CBG, status&0x01);
            jsonObject.put(BgProfile.FEATURE_SENSOR_MALFUNCTION_DETECTION_CBG, (status>>1)&0x01);
            jsonObject.put(BgProfile.FEATURE_SAMPLE_SIZE_CBG, (status>>2)&0x01);
            jsonObject.put(BgProfile.FEATURE_STRIP_INSERTION_ERROR_CBG, (status>>3)&0x01);
            jsonObject.put(BgProfile.FEATURE_STRIP_TYPE_ERROR_CBG, (status>>4)&0x01);
            jsonObject.put(BgProfile.FEATURE_RESULT_HIGH_LOW_DETECTION_CBG, (status>>5)&0x01);
            jsonObject.put(BgProfile.FEATURE_TEMPERATURE_HIGH_LOW_DETECTION_CBG, (status>>6)&0x01);
            jsonObject.put(BgProfile.FEATURE_READ_INTERRUPT_CBG, (status>>7)&0x01);
            jsonObject.put(BgProfile.FEATURE_GENERAL_FAULT_SUPPORT_CBG, (status>>8)&0x01);
            jsonObject.put(BgProfile.FEATURE_TIME_FAULT_SUPPORT_CBG, (status>>9)&0x01);
            jsonObject.put(BgProfile.FEATURE_MULTIPLE_BOND_SUPPORTED_CBG, (status>>10)&0x01);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;

    }


    public void destroy(){
        mInsCallback = null;
        mContext = null;
        if(mbleCommContinueProtocol != null)
            mbleCommContinueProtocol.destroy();
        mbleCommContinueProtocol = null;
    }


}
