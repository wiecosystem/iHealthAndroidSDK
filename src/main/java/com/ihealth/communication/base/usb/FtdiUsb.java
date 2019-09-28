package com.ihealth.communication.base.usb;

import android.content.Context;
import com.ihealth.communication.utils.Log;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.ihealth.communication.base.comm.BaseComm;
import com.ihealth.communication.base.protocol.BaseCommProtocol;
import com.ihealth.communication.utils.ByteBufferUtil;

import java.util.LinkedList;
import java.util.Queue;

public class FtdiUsb implements BaseComm{

	private D2xxManager ftD2xx = null;
	private FT_Device ftDev = null;
	private int DevCount = -1;
	private int currentIndex = -1;

	public readThread readthread;

	public byte[] readBuffer;
	public int readBufferlen;

	public boolean datareceived = false;
	public boolean READ_ENABLE = false;
	public boolean accessory_attached = false;

	public Context global_context;
	
	private int quencesequenceID;

	int baudRate = 57600; /* baud rate */
	byte stopBit = 1; /* 1:1stop bits, 2:2 stop bits */
	byte dataBit = 8; /* 8:8bit, 7: 7bit */
	byte parity = 0; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
	byte flowControl = 0; /* 0:none, 1: flow control(CTS,RTS) */
	int portNumber = 1; /* port number */
	private BaseCommProtocol commProtocol;
	private Context mContext;
	public int iavailable = 0;
	public static final int readLength = 512;
	private byte[] readData = new byte[readLength];
	public boolean uart_configured = false;
	public boolean bReadThreadGoing = false;
	/* constructor */
	public FtdiUsb(Context context) {
		super();
		this.mContext = context;
		global_context = context;
		try {
			ftD2xx = D2xxManager.getInstance(global_context);
		} catch (D2xxManager.D2xxException ex) {
			ex.printStackTrace();
		}
		SetupD2xxLibrary();
	}

	public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity,
			byte flowControl) {
		if (ftDev.isOpen() == false) {
			return;
		}
		// configure our port
		// reset to UART mode for 232 devices
		ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
		ftDev.setBaudRate(baud);
		switch (dataBits) {
		case 7:
			dataBits = D2xxManager.FT_DATA_BITS_7;
			break;
		case 8:
			dataBits = D2xxManager.FT_DATA_BITS_8;
			break;
		default:
			dataBits = D2xxManager.FT_DATA_BITS_8;
			break;
		}

		switch (stopBits) {
		case 1:
			stopBits = D2xxManager.FT_STOP_BITS_1;
			break;
		case 2:
			stopBits = D2xxManager.FT_STOP_BITS_2;
			break;
		default:
			stopBits = D2xxManager.FT_STOP_BITS_1;
			break;
		}

		switch (parity) {
		case 0:
			parity = D2xxManager.FT_PARITY_NONE;
			break;
		case 1:
			parity = D2xxManager.FT_PARITY_ODD;
			break;
		case 2:
			parity = D2xxManager.FT_PARITY_EVEN;
			break;
		case 3:
			parity = D2xxManager.FT_PARITY_MARK;
			break;
		case 4:
			parity = D2xxManager.FT_PARITY_SPACE;
			break;
		default:
			parity = D2xxManager.FT_PARITY_NONE;
			break;
		}

		ftDev.setDataCharacteristics(dataBits, stopBits, parity);

		short flowCtrlSetting;
		switch (flowControl) {
		case 0:
			flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
			break;
		case 1:
			flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
			break;
		case 2:
			flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
			break;
		case 3:
			flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
			break;
		default:
			flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
			break;
		}

		// TODO : flow ctrl: XOFF/XOM
		// TODO : flow ctrl: XOFF/XOM
		ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);

		uart_configured = true;
		
	}

	private static final String TAG = "uartInterface";
	public int createDeviceList()
	{
		if(ftD2xx == null){
			return DevCount;
		}
		int tempDevCount = ftD2xx.createDeviceInfoList(mContext);
		if (tempDevCount > 0){
			if( DevCount != tempDevCount ){
				DevCount = tempDevCount;
			}
		}else{
			Log.w(TAG, "createDeviceInfoList -- fail");
			DevCount = -1;
			currentIndex = -1;
		}
		return DevCount;
	}
	
	private int openIndex = 0;
	public void connectFunction() throws NullPointerException{
		int tmpProtNumber = openIndex + 1;

		if( currentIndex != openIndex ){
			if(null == ftDev){
				ftDev = ftD2xx.openByIndex(mContext, openIndex);
			}
			else{
				synchronized(ftDev){
					ftDev = ftD2xx.openByIndex(mContext, openIndex);
				}
			}
			uart_configured = false;
		}
		else{
			Log.w("usb", "Device port " + tmpProtNumber + " is already opened");
			return;
		}

		if(ftDev == null){
			Log.w("usb", "open device port("+tmpProtNumber+") NG, ftDev == null");
			return;
		}
			
		if (true == ftDev.isOpen()){
			currentIndex = openIndex;
			Log.v("usb", "open device port(" + tmpProtNumber + ") OK");
			if(false == bReadThreadGoing){
				readthread = new readThread();
				readthread.start();
				bReadThreadGoing = true;
			}
		}
		else {			
			Log.w("usb", "open device port(" + tmpProtNumber + ") NG");
		}
	}
	
	private class readThread  extends Thread
	{

		readThread(){
			this.setPriority(Thread.MIN_PRIORITY);
		}

		@Override
		public void run()
		{

			while(true == bReadThreadGoing)
			{
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
				}

				synchronized(ftDev)
				{
					iavailable = ftDev.getQueueStatus();				
					if (iavailable > 0) {
						if(iavailable > readLength){
							iavailable = readLength;
						}
						ftDev.read(readData, iavailable);
						Log.p(TAG, Log.Level.VERBOSE,"Read",ByteBufferUtil.Bytes2HexString(readData,iavailable));
						addReadUSBDATA(readData, iavailable);
					}
				}
			}
		}

	}
	
	 private void SetupD2xxLibrary () {

	    	if(!ftD2xx.setVIDPID(0x0403, 0xada1))
	    		Log.w("ftd2xx-java","setVIDPID Error");
	 }
	
	private Queue<Byte> readDataQueue = new LinkedList<Byte>();

	private void addReadUSBDATA(byte[] data, int count) {
		try {
		for (int i = 0; i < count; i++) {
			readDataQueue.offer(data[i]);
		}
			isFullCommand();
			isFullCommand();
			isFullCommand();
		} catch (Exception e) {
			Log.w(TAG, "uncaughtException:" + e.getCause().getMessage());
		}
	}

	private void isFullCommand() {
		int temp;
		byte[] datas = null;
		if (6 > readDataQueue.size()) {
			Log.w(TAG, "command length is not wrong");
			return;
		}
		int length = readDataQueue.peek() & 0xff;
		if (160 == length) {
			readDataQueue.poll();
		}
		temp = readDataQueue.peek() & 0xff;
		int len = temp + 3;
		if (readDataQueue.size() >= temp+2) {

			datas = new byte[len];
			datas[0] = (byte) 0xA0;
			for (int i = 1; i < len; i++) {
				Byte b = readDataQueue.poll().byteValue();
				if (null != b) {
					datas[i] = b;
				}
			}

		} else {
			Log.w(TAG, "This is not full command");
			return;
		}
		if(datas.length > 3){
			temp = (datas[3] & 0xff);
		}else{
			return;
		}
		if (quencesequenceID != temp) {
			quencesequenceID = temp;
		} else {
			Log.v(TAG, "Duplicate command");
			return;
		}
		readBuffer = new byte[datas.length];
		readBufferlen = datas.length;
		for (int i = 0; i < datas.length; i++) {
			readBuffer[i] = datas[i];
		}

		commProtocol.unPackageData(readBuffer);
	}

	public void disConnectFunction(){
		if(ftDev != null){
			if(true == ftDev.isOpen()){
				ftDev.close();
			}
		}
	}

	@Override
	public void sendData(String mac, byte[] data) {
		if(ftDev == null){
			return;
		}
		Log.p(TAG, Log.Level.VERBOSE,"sendData",mac,ByteBufferUtil.Bytes2HexString(data, data.length));
		ftDev.setLatencyTimer((byte) 16);
		ftDev.write(data, data.length);

	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		disConnectFunction();
	}

	@Override
	public void disconnect(String mac) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addCommNotify(String mac, BaseCommProtocol dataCallBack) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addCommNotify(BaseCommProtocol dataCallBack) {
		commProtocol = dataCallBack;
	}

    /* (non-Javadoc)
     * @see com.ihealth.communication.base.comm.BaseComm#sendData(java.lang.String, java.lang.String, byte[])
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
