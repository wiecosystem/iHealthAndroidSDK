
package com.ihealth.communication.base.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.comm.BaseCommCallback;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.ByteBufferUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BtCommThread extends Thread implements BaseComm {

    private static final String TAG = "Runtime_BtCommThread";
    private BluetoothDevice mDevice = null;
    private BluetoothSocket mSocket = null;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private int mReadBufferLen;
    private byte[] mReadBuffer;
    private Context mContext;
    private BtUnpackageData mBtUnpackageData;
    private BaseCommCallback mBaseCommCallback;
    private String mType;
    public static boolean ioException = false;

    public BtCommThread(Context context, BluetoothDevice device, String type, BluetoothSocket socket,
                        BaseCommCallback baseCommCallback) throws IOException {
        this.mContext = context;
        this.mDevice = device;
        this.mSocket = socket;
        this.mType = type;
        this.mBaseCommCallback = baseCommCallback;
        this.mInputStream = socket.getInputStream();
        this.mOutputStream = socket.getOutputStream();
        mReadBuffer = new byte[256];
        mBtUnpackageData = new BtUnpackageData();
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (mInputStream == null) {
                    break;
                }
                mReadBufferLen = mInputStream.read(mReadBuffer);
                if (mReadBufferLen > 0) {
                    Log.p(TAG, Log.Level.VERBOSE,"Read", ByteBufferUtil.Bytes2HexString(mReadBuffer, mReadBufferLen));
                    mBtUnpackageData.addReadUsbData(mReadBuffer, mReadBufferLen);
                }
                ioException = false;
            } catch (Exception e) {
                ioException = true;
                String mac = mDevice.getAddress().replace(":", "");
                mBaseCommCallback.onConnectionStateChange(mac, mType, iHealthDevicesManager.DEVICE_STATE_DISCONNECTED, 0, null);
                Log.w(TAG,"Read() -- Exception: " + e);
                break;
            }
        }
    }

    private void close() {
        try {
            if (this.mSocket != null) {
                this.mSocket.close();
            }
            this.mInputStream = null;
            this.mOutputStream = null;
            this.mSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.mInputStream = null;
            this.mOutputStream = null;
            this.mSocket = null;
        }
    }

    @Override
    public void sendData(String mac, byte[] data) {
        try {
            Log.p(TAG, Log.Level.VERBOSE,"sendData",mac,ByteBufferUtil.Bytes2HexString(data, data.length));
            //防止断开后 发送数据导致NPE
            if (mOutputStream != null) {
                this.mOutputStream.write(data);
                this.mOutputStream.flush();
            }
        } catch (IOException e) {
            Log.w(TAG,"sendData() -- IOException: " + e);
        }
    }

    @Override
    public void disconnect() {
        close();
    }

    // @Override
    // public void addCommNotify(NewDataCallback dataCallBack) {
    // btNotify.attach(dataCallBack);
    // }

    @Override
    public void disconnect(String mac) {
        // TODO Auto-generated method stub
        close();
    }

    @Override
    public void addCommNotify(String mac, BaseCommProtocol dataCallBack) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addCommNotify(BaseCommProtocol dataCallBack) {
        mBtUnpackageData.addBtCommProtocol(dataCallBack);
    }

    /*
     * (non-Javadoc)
     * @see com.ihealth.communication.base.comm.BaseComm#sendData(java.lang.String,
     * java.lang.String, byte[])
     */
    @Override
    public void sendData(String mac, String deviceIP, byte[] data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addCommContinueNotify(String mac, BaseCommProtocol dataCallBack) {

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
