package com.ihealth.communication.ins;

import android.content.Context;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.NewDataCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.control.UpDeviceControl;
import com.ihealth.communication.control.UpgradeProfile;
import com.ihealth.communication.manager.iHealthDevicesUpgradeManager;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.FirmWare;
import com.ihealth.communication.utils.Log;
import com.ihealth.communication.utils.PublicMethod;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class F0InsSet extends IdentifyIns implements NewDataCallback, GetBaseCommProtocolCallback {

    private static final String TAG = "F0InsSet";


    private Context mContext;
    private String mAddress;
    private String mType;
    private byte deviceType = (byte) 0xf0;
    private BaseCommProtocol mComm;
    private InsCallback mInsCallback;
    private Map<String, String> upgradeModeMap = new HashMap<>();

    private static void printMethodInfo(String methodName, Object... parameters) {
        Log.p(TAG, Log.Level.INFO, methodName, parameters);
    }

    public F0InsSet(BaseComm mBaseComm, BaseCommProtocol com, Context context, String mac, String type, InsCallback insCallback) {
        printMethodInfo("F0InsSet_Constructor", mac, type);
        this.mContext = context;
        this.mAddress = mac;
        this.mType = type;
        this.mComm = com;
        this.mInsCallback = insCallback;

        setInsSetCallbak(insCallback, mac, type, mBaseComm);
    }

    public void queryInformation() {
        printMethodInfo("queryInformation");
        byte commandID = (byte) 0xD0;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        startTimeout(0xD0, AppsDeviceParameters.Delay_Medium, 0xD0, 0xD6);
        mComm.packageData(mAddress, returnCommand);
    }

    /**
     * 开始升级
     */
    public void startUpdate() {
        printMethodInfo("startUpdate");
        byte commandID = (byte) 0xD1;
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        startTimeout(0xD1, AppsDeviceParameters.Delay_Medium, 0xD1, 0xD2, 0xD6);
        mComm.packageData(mAddress, returnCommand);
    }

    /**
     * 准备好升级
     */
    public void readyUpdate() {
        printMethodInfo("readyUpdate");
        byte commandID = (byte) 0xD2;
        byte[] returnCommand = new byte[2 + infoDatas.size()];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        for (int i = 0; i < infoDatas.size(); i++) {
            returnCommand[i + 2] = infoDatas.get(i);
        }
        returnCommand[2] = inBlockNum[0];
        returnCommand[3] = inBlockNum[1];
        startTimeout(0xD2, AppsDeviceParameters.Delay_Long, 0xD3, 0xD6);
        mComm.packageData(mAddress, returnCommand);
    }

    private FirmWare mFirmWare;
    private List<byte[]> upDatas;
    private List<Byte> infoDatas;

    public void setInfo(List<Byte> list) {
        printMethodInfo("setInfo", list);
        this.infoDatas = list;
    }

    public void setFirmWare(FirmWare firmWare, List<byte[]> list) {
        printMethodInfo("setFirmWare", firmWare, list);
        this.mFirmWare = firmWare;
        this.upDatas = list;
    }

    /**
     * 请求数据
     *
     * @param index
     */
    public void queryData(int index, String mac, int progress) {
        printMethodInfo("queryData", index, mac, progress);
        byte[] temp = upDatas.get(index);
        byte[] temp2 = mFirmWare.getCrcList().get(index);
        byte commandID = (byte) 0xD3;
        byte[] returnCommand = new byte[temp.length + 6];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        returnCommand[3] = (byte) ((index >> 8) & 0xff);
        returnCommand[2] = (byte) (index & 0xff);
        returnCommand[4] = temp2[0];
        returnCommand[5] = temp2[1];
        for (int i = 0; i < temp.length; i++) {
            returnCommand[i + 6] = temp[i];
        }
        String upgradeMode = upgradeModeMap.get(mac);
        if (upgradeMode != null && upgradeMode.equals("100") && progress > 90) {
            startTimeout(0xD3, AppsDeviceParameters.Delay_LongLong, 0xD3, 0xD5, 0xD6);
        } else {
            startTimeout(0xD3, AppsDeviceParameters.Delay_Long, 0xD3, 0xD5, 0xD6);
        }
        mComm.packageData(mAddress, returnCommand);
    }

    /**
     * 中止升级
     */
    public void stopUpdate() {
        printMethodInfo("stopUpdate");
        byte commandID = (byte) 0xD6;
        byte[] returnCommand = new byte[6];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        mComm.packageData(mAddress, returnCommand);

        stopTimeout(0XD3);
    }

    private void allPkgOk(byte commandID) {
        byte[] returnCommand = new byte[2];
        returnCommand[0] = deviceType;
        returnCommand[1] = commandID;
        mComm.packageData(mAddress, returnCommand);
    }

    private Map upgradeStateMap = new ConcurrentHashMap<String, Boolean>();

    /**
     * 设置当前状态 如果在自升级状态 设置相应mac为true 反之 false
     *
     * @param stateTemp
     */
    public void setCurrentState(String mac, boolean stateTemp) {
        printMethodInfo("setCurrentState", mac, stateTemp);
        upgradeStateMap.put(mac, stateTemp);
    }

    /**
     * 返回当前状态
     *
     * @return
     */
    public boolean getCurrentState(String mac) {
        printMethodInfo("getCurrentState", mac);
        if (!upgradeStateMap.isEmpty() && upgradeStateMap.get(mac) != null) {
            return (Boolean) upgradeStateMap.get(mac);
        } else {
            return false;
        }
    }

    private enum Command {
        Unknown(0),
        GetUpgradeInfo(0xd0),
        FirmwareTransmission_Start(0xd1),
        Upgrade_Ready(0xd2),
        FirmwareTransmission_InProgress(0xd3),
        FirmwareTransmission_Finish(0xd5),
        Upgrade_Stop(0xd6);
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

    private byte[] inBlockNum = {0x00, 0x00};

    @Override
    public void haveNewData(int what, int stateId, byte[] command) {
        Command cmd = Command.parseCommand(what);
        Log.p(TAG, Log.Level.DEBUG, "haveNewData", cmd, stateId, ByteBufferUtil.Bytes2HexString(command));
        stopTimeout(what);
        JSONObject jsonObject = new JSONObject();
        switch (cmd) {
            case GetUpgradeInfo://(0xd0),
                if (command != null) {
                    this.dataProcessGetUpInfo(command, jsonObject);
                }
                break;
            case FirmwareTransmission_Start://(0xd1),
                //20160724 jing AM 有bug，此标示位应该传0，1   实际传输的为随机数，不可用
                try {
                    jsonObject.put(UpgradeProfile.DEVICE_START_UP_FLAG, 0);
                    mInsCallback.onNotify(mAddress, mType, UpgradeProfile.ACTION_DEVICE_START_UP, jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case Upgrade_Ready://(0xd2),
                readyUpdate();
                break;
            case FirmwareTransmission_InProgress://(0xd3),
                int index = (command[1] & 0xFF) * 256 + (command[0] & 0xFF);
                int progress = index * 100 / upDatas.size();
                if (progress > 100) {
                    progress = 100;
                }
                queryData(index, mAddress, progress);
                try {
                    jsonObject.put(UpgradeProfile.DEVICE_PROGRESS_VALUE, progress);
                    mInsCallback.onNotify(mAddress, mType, UpgradeProfile.ACTION_DEVICE_UP_PROGRESS, jsonObject.toString());
                } catch (Exception e) {
                    // TODO: handle exception
                }
                break;
            case FirmwareTransmission_Finish://(0xd5),
                allPkgOk((byte) (0xd5));
                int reqd5 = command[0] & 0xff;
                if (reqd5 == 0 || reqd5 == 1) {
                    try {
                        jsonObject.put(UpgradeProfile.DEVICE_PROGRESS_VALUE, 100);
                        mInsCallback.onNotify(mAddress, mType, UpgradeProfile.ACTION_DEVICE_UP_PROGRESS, jsonObject.toString());
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }

                //归还comm
                returnComm();

                jsonObject = new JSONObject();
                try {
                    if (reqd5 == 0 || reqd5 == 1) {
                        jsonObject.put(UpgradeProfile.DEVICE_FINISH_UP_FLAG, 1);
                    } else {
                        jsonObject.put(UpgradeProfile.DEVICE_FINISH_UP_FLAG, 0);
                    }
                    mInsCallback.onNotify(mAddress, mType, UpgradeProfile.ACTION_DEVICE_FINISH_UP, jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                break;
            case Upgrade_Stop://(0xd6);

                allPkgOk((byte) (0xd6));
                if (mInsCallback != null) {
                    mInsCallback.onNotify(mAddress, mType, UpgradeProfile.ACTION_DEVICE_STOP_UP, null);
                }
                //归还comm
                returnComm();

                break;
            default:
                break;
        }
    }

    private void returnComm() {
        UpDeviceControl mUpDeviceControl = iHealthDevicesUpgradeManager.getInstance().getUpDeviceControl(mAddress, mType);
        if (mUpDeviceControl != null) {
            mUpDeviceControl.returnComm();
        }
    }

    //发送D0返回的消息
    private void dataProcessGetUpInfo(byte[] data, JSONObject jsonObject) {
        //续传编号
        inBlockNum = new byte[2];
        inBlockNum[0] = data[0];
        inBlockNum[1] = data[1];
        //type
        byte[] deviceType = new byte[1];
        String deviceTypeString = "";
        deviceType[0] = data[2];
//		Log.i(TAG, "产品类型 = "+ PublicMethod.Bytes2HexString(deviceType));
        deviceTypeString = PublicMethod.Bytes2HexString(deviceType);
        if (deviceTypeString.length() < 2) {
            deviceTypeString = "0x0" + deviceTypeString;
        } else {
            deviceTypeString = "0x" + deviceTypeString;
        }
        //mode
        byte[] deviceMode = new byte[16];
        String deviceModeString = "";
        for (int i = 0; i < 16; i++) {
            deviceMode[i] = data[3 + i];
        }
        int lenMode = 0;
        for (byte b : deviceMode) {
            if (b == 0) {
                break;
            } else {
                lenMode += 1;
            }
        }
        try {
            deviceModeString = new String(ByteBufferUtil.bufferCut(deviceMode, 0, lenMode), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //f,h
        String f, h;
        byte[] HardwareVersion = new byte[3];
        for (int i = 0; i < 3; i++) {
            HardwareVersion[i] = data[19 + i];
        }
        h = new String(HardwareVersion);

        byte[] FirmwareVersion = new byte[3];
        for (int i = 0; i < 3; i++) {
            FirmwareVersion[i] = data[22 + i];
        }
        f = new String(FirmwareVersion);


        h = h.substring(0, 1) + "." + h.substring(1, 2) + "." + h.substring(2, 3);

        f = f.substring(0, 1) + "." + f.substring(1, 2) + "." + f.substring(2, 3);

        //升级允许
        int upgradeFlag = data[28] & 0xff;

        //升级方式
        String upgradeModeString = "100";
        if (data.length >= 32) {
            byte[] upgradeMode = new byte[3];
            for (int i = 0; i < 3; i++) {
                upgradeMode[i] = data[29 + i];
            }
            upgradeModeString = new String(upgradeMode);
        }
        upgradeModeMap.put(mAddress, upgradeModeString);

        //HS4S BUG
        if (data[19] == 0 && data[20] == 0x31 && data[21] == 0x30) {
            h = "5.0.1";
        }

        //jing 20160831 判断是否是续传   比较总块数 与 续传块编号
        int recLength = (data[25] & 0xff) + (data[26] & 0xff) * 256 + (data[27] & 0xff) * 256 * 256;
        int totalBag = (recLength % 128 != 0) ? recLength / 128 + 1 : recLength / 128;
        int blockNum = (inBlockNum[0] & 0xff + (inBlockNum[1] & 0xff) * 256);
        int blockFlag = 1;
        if (blockNum == 0 || (blockNum >= totalBag)) {
            blockFlag = 0;
        }
        try {
            jsonObject.put(UpgradeProfile.DEVICE_UPGRADE_FLAG, blockFlag);    //续传编号
            jsonObject.put(UpgradeProfile.DEVICE_FIRMWARE_VERSION, f);    //固件
            jsonObject.put(UpgradeProfile.DEVICE_UP_FLAG, upgradeFlag);    //允许升级
            jsonObject.put(UpgradeProfile.DEVICE_UP_MODE, upgradeModeString);    //升级方式

            mInsCallback.onNotify(mAddress, mType, UpgradeProfile.ACTION_DEVICE_UP_INFO, jsonObject.toString());

            //jing 20160723 查询设备版本完成后，内部自动调用查询云端最新版本
            jsonObject.put(UpgradeProfile.DEVICE_TYPE, deviceTypeString);    //类型
            jsonObject.put(UpgradeProfile.DEVICE_MODE, deviceModeString);    //型号
            jsonObject.put(UpgradeProfile.DEVICE_HARDWARE_VERSION, h);    //硬件
            iHealthDevicesUpgradeManager.getInstance().queryLatestVersionFromCloud(mAddress, mType, jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //归还comm
        UpDeviceControl mUpDeviceControl = iHealthDevicesUpgradeManager.getInstance().getUpDeviceControl(mAddress, mType);
        if (mUpDeviceControl != null) {
            mUpDeviceControl.returnComm();
        }
    }

    @Override
    public BaseCommProtocol getBaseCommProtocol() {
        return mComm;
    }

    @Override
    public void haveNewDataUuid(String uuid, byte[] command) {

    }

    public void destroy() {
        printMethodInfo("destroy");
        mComm = null;
        mInsCallback = null;
    }
}
