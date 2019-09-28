
package com.ihealth.communication.base.wifi;

import java.util.List;

import com.ihealth.communication.utils.Log;

import com.ihealth.communication.base.comm.BaseComm;

public class WifiSendThread implements Runnable {
    private static final String TAG1 = "HS5Wifi";
    private static final String TAG = "WifiSendThread--";
    private byte[] sendDatas;
    private String sendAddress;
    private String deviceIp;

    private BaseComm mBaseComm;

    public WifiSendThread(BaseComm comm) {
        this.mBaseComm = comm;
    }

    public void setData(String address, String deviceIp, byte[] sendDatas) {
        this.sendAddress = address;
        this.deviceIp = deviceIp;
        this.sendDatas = sendDatas;
    }

    @Override
    public void run() {
        mBaseComm.sendData(sendAddress, deviceIp,sendDatas);
    }

}
