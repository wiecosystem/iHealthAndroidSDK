
package com.ihealth.communication.base.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.SparseArray;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.manager.iHealthDevicesIDPS;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.Log;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @hide
 */
public class AndroidBle implements BleCallback, BleComm, BaseComm {

    private static final String TAG = "Runtime_AndroidBle";
    private Ble ble;
    private BtleCallback mBtleCallback;
    private Context mContext;

    public AndroidBle(Context context, BtleCallback btleCallback) {
        mContext = context;
        ble = new Ble(context, this);
        mBtleCallback = btleCallback;
        mapDataCallBack = new ConcurrentHashMap<String, BaseCommProtocol>();
        mapDataContinueCallBack = new ConcurrentHashMap<String, BaseCommProtocol>();
    }

    @SuppressLint("NewApi")
    @Override
    public void destory() {
    }

    @Override
    public void onCharacteristicChanged(BluetoothDevice device, byte[] data, UUID uuid) {
        String mac = device.getAddress().replace(":", "");
        String uuidStr = uuid.toString();
        Log.p(TAG, Log.Level.VERBOSE, "onCharacteristicChanged", mac, ByteBufferUtil.Bytes2HexString(data));

        if (uuidStr.equals(BleConfig.UUID_BTM_READ_DATA_CHARACTERISTIC) ||
                uuidStr.equals(BleConfig.UUID_BP_RECEIVE) || uuidStr.equals(BleConfig.UUID_BP_RECEIVE_iHealth)
                || uuidStr.equals(BleConfig.UUID_BG_RECEIVE) || uuidStr.equals(BleConfig.UUID_BG_RECEIVE_CONTENT)
                || uuidStr.equals(BleConfig.UUID_BG_SEND_AND_RECEIVE)
                || uuidStr.equals(BleConfig.UUID_PO_RECEIVE) || uuidStr.equals(BleConfig.UUID_PO_RECEIVE_CONTINUOUS)
                || uuidStr.equals(BleConfig.UUID_PO_SEND_AND_RECEIVE)
                || uuidStr.equals(BleConfig.UUID_HS_RECEIVE) || uuidStr.equals(BleConfig.UUID_HS_RECEIVE)
                || uuidStr.equals(BleConfig.UUID_BS_RECEIVE) || uuidStr.equals(BleConfig.UUID_BS_RECEIVE)) {
            mapDataContinueCallBack.get(mac).unPackageDataUuid(uuidStr, data);
        } else {
            mapDataCallBack.get(mac).unPackageData(data);
        }

    }

    @Override
    public void onCharacteristicRead(BluetoothDevice device, byte[] arg0, UUID arg1, int arg2) {
        String mac = device.getAddress().replace(":", "");
        String uuidStr = arg1.toString();
        if (uuidStr.equals(BleConfig.UUID_BP_READ)
                || uuidStr.equals(BleConfig.UUID_BG_READ)
                || uuidStr.equals(BleConfig.UUID_PO_READ)
                || uuidStr.equals(BleConfig.UUID_HS_READ)
                || uuidStr.equals(BleConfig.UUID_BS_READ)) {
            Log.p(TAG, Log.Level.VERBOSE, "onCharacteristicRead", mac, ByteBufferUtil.Bytes2HexString(arg0));
            mapDataContinueCallBack.get(mac).unPackageDataUuid(uuidStr, arg0);
        } else {
            mBtleCallback.onCharacteristicRead(arg0, arg1, arg2);
        }

    }

    @Override
    public void onConnectionStateChange(BluetoothDevice arg0, int arg1, int arg2) {
        mBtleCallback.onConnectionStateChange(arg0, arg1, arg2);
    }

    @Override
    public void onDescriptorRead(byte[] arg0, UUID arg1, int arg2) {

    }

    @Override
    public void onDescriptorWrite() {

    }

    @Override
    public void onScanResult(BluetoothDevice arg0, int arg1, byte[] arg2) {
        ScanRecord scanRecord = ScanRecord.parseFromBytes(arg2);
        List<ParcelUuid> uuids = scanRecord.getServiceUuids();
        if (uuids != null) {
            String uuid = "";
            for (ParcelUuid pu : uuids) {
                uuid += " " + pu.toString();
            }
            //20160726 jing 提取广播包中的自定义内容
            Map<String, Object> manufactorData = new HashMap<>();
            if (uuid.contains(BleConfig.UUID_BP_SERVICE)) {
                SparseArray<byte[]> sparseArray = scanRecord.getManufacturerSpecificData();
                for (int i = 0; i < sparseArray.size(); i++) {
                    byte[] specificData = sparseArray.valueAt(i);
                    if (specificData.length >= 20) {
//                        Log.v(TAG,"getManufacturerSpecificData:" + ByteBufferUtil.Bytes2HexString(specificData)  + "  key:"+ sparseArray.keyAt(i));
                        byte[] moduleBuffer = ByteBufferUtil.bufferCut(specificData, specificData.length - 20, 16);
                        String module;
                        try {
                            module = new String(ByteBufferUtil.bufferCut(moduleBuffer, 0, searchByte(moduleBuffer, (byte) 0)), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            module = "";
                        }
                        manufactorData.put(iHealthDevicesIDPS.MODENUMBER, module);
                        manufactorData.put(iHealthDevicesIDPS.SERIALNUMBER, ByteBufferUtil.Bytes2HexString(specificData, specificData.length - 4, specificData.length));
                        break;
                    }
                }
            }
            mBtleCallback.onScanResult(arg0, arg1, uuid, manufactorData);
        }

    }

    private int searchByte(byte[] data, byte value) {
        int size = data.length;
        for (int i = 0; i < size; ++i) {
            if (data[i] == value) {
                return i;
            }
        }
        return data.length;
    }

    @Override
    public void onServicesDiscovered(BluetoothDevice arg0, List<UUID> arg1, int arg2) {
        mBtleCallback.onServicesDiscovered(arg0, arg1, arg2);
    }

    @Override
    public void sendData(String mac, byte[] data) {
        Log.p(TAG, Log.Level.VERBOSE, "_______________sendData", mac, ByteBufferUtil.Bytes2HexString(data));
        String address = addColon(mac);
        ble.sendData(address, data);
    }

    private String addColon(String address) {
        if (address.length() == 12) {
            return address;
        } else {
            byte[] mac = ByteBufferUtil.hexStringToByte(address);
            return ByteBufferUtil.mac2Address(mac);
        }
    }

    @Override
    public void scan(boolean b) {
        try {
            ble.scan(b);
        } catch (Exception e) {
            Log.e(TAG, "scan() -- Exception: " + e);
        }

    }

    @Override
    public boolean connectDevice(String address) {
        return ble.connectDevice(address);
    }

    @Override
    public void getService(String mac, String comm, String trans, String rece, String idps, boolean needIndication) {
        Log.p(TAG, Log.Level.VERBOSE, "getService", mac, comm, trans, rece, idps, needIndication);
        if (comm != null && rece != null && idps != null) {
            if (trans == null) {
                ble.getCommService(mac, UUID.fromString(comm), null,
                        UUID.fromString(rece), UUID.fromString(idps), needIndication);
            } else {
                ble.getCommService(mac, UUID.fromString(comm), UUID.fromString(trans),
                        UUID.fromString(rece), UUID.fromString(idps), needIndication);
            }

        } else {
            ble.disconnect(mac);
        }

    }

    @Override
    public void onServicesObtain() {

    }

    @Override
    public void onServicesObtain(UUID uuid, BluetoothDevice device, String para) {
        Log.p(TAG, Log.Level.VERBOSE, "onServicesObtain", uuid, device.getAddress().replace(":", ""), para);
        //判断使能是单独使用，还是放在连接流程中使用
        String uuidStr = uuid.toString();
        if (uuidStr.equals(BleConfig.UUID_BP_RECEIVE) || uuidStr.equals(BleConfig.UUID_BP_RECEIVE_iHealth)
                || uuidStr.equals(BleConfig.UUID_BG_RECEIVE) || uuidStr.equals(BleConfig.UUID_BG_RECEIVE_CONTENT)
                || uuidStr.equals(BleConfig.UUID_BG_SEND_AND_RECEIVE)
                || uuidStr.equals(BleConfig.UUID_PO_RECEIVE) || uuidStr.equals(BleConfig.UUID_PO_RECEIVE_CONTINUOUS)
                || uuidStr.equals(BleConfig.UUID_PO_SEND_AND_RECEIVE)
                || uuidStr.equals(BleConfig.UUID_HS_RECEIVE) || uuidStr.equals(BleConfig.UUID_HS_RECEIVE)
                || uuidStr.equals(BleConfig.UUID_BS_RECEIVE) || uuidStr.equals(BleConfig.UUID_BS_RECEIVE)) {
            //返回ins
            String mac = device.getAddress().replace(":", "");
            mapDataContinueCallBack.get(mac).unPackageDataUuid(uuidStr, null);

        } else {
            //返回 iHealthDeviceManager
            mBtleCallback.onServicesObtain();
        }
    }

    @Override
    public void Obtain(UUID uuid) {
        ble.readCharacteristic(uuid);
    }

    @Override
    public boolean Obtain(String mac, UUID serviceUuid, UUID charactUuid) {
        return ble.readCharacteristicExtra(mac, serviceUuid, charactUuid);
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void disconnect(String mac) {
        ble.disconnect(addColon(mac));
    }

    private Map<String, BaseCommProtocol> mapDataCallBack;
    private Map<String, BaseCommProtocol> mapDataContinueCallBack;

    @Override
    public void addCommNotify(String mac, BaseCommProtocol dataCallBack) {
        mapDataCallBack.put(mac, dataCallBack);
    }

    @Override
    public BaseComm getBaseComm() {
        return this;
    }

    @Override
    public void addCommNotify(BaseCommProtocol dataCallBack) {

    }

    /*
     * (non-Javadoc)
     * @see com.ihealth.communication.base.comm.BaseComm#sendData(java.lang.String,
     * java.lang.String, byte[])
     */
    @Override
    public void sendData(String mac, String deviceIP, byte[] data) {

    }

    @Override
    public void onRssi(int arg0) {

    }

    public void refresh(String mac) {
        ble.refresh(mac);
    }

    @Override
    public void addCommContinueNotify(String mac, BaseCommProtocol dataCallBack) {
        mapDataContinueCallBack.put(mac, dataCallBack);
    }

    @Override
    public void onCharacteristicWrite(BluetoothDevice device, UUID uuid, int status) {
        String mac = device.getAddress().replace(":", "");
        if (mapDataCallBack.get(mac) != null) {
            mapDataCallBack.get(mac).packageDataFinish();
        } else {
            Log.v(TAG, "onCharacteristicWrite success");
        }
    }

    @Override
    public void removeCommNotify(String mac) {

    }

    @Override
    public void removeCommNotify(BaseCommProtocol dataCallBack) {

    }

    @Override
    public Context getContext() {
        return mContext;
    }


}
