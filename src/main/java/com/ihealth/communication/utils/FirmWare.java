package com.ihealth.communication.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirmWare {
	
	private byte[] inblocknumber;   					// 续传块编号
	private byte[] softwareVersionNumber; 				// 软件总版本号
	private byte[] softwareTotalLenght; 				// 软件总长度
	private List<byte[]> softwareLenList = new ArrayList<byte[]>();
	private List<byte[]> softwareVersionList = new ArrayList<byte[]>();
	private List<byte[]> crcList = new ArrayList<byte[]>();
	
	public FirmWare(byte[] inblocknumber, byte[] softwareVersionNumber,
			byte[] softwareTotalLenght, List<byte[]> softwareLenList,
			List<byte[]> softwareVersionList, List<byte[]> crcList) {
		super();
		this.inblocknumber = inblocknumber;
		this.softwareVersionNumber = softwareVersionNumber;
		this.softwareTotalLenght = softwareTotalLenght;
		this.softwareLenList = softwareLenList;
		this.softwareVersionList = softwareVersionList;
		this.crcList = crcList;
	}
	
	public FirmWare() {
		super();
		softwareLenList.clear();
		softwareVersionList.clear();
		crcList.clear();
	}

	public byte[] getInblocknumber() {
		return inblocknumber;
	}

	public byte[] getSoftwareVersionNumber() {
		return softwareVersionNumber;
	}

	public byte[] getSoftwareTotalLenght() {
		return softwareTotalLenght;
	}

	public List<byte[]> getSoftwareLenList() {
		return softwareLenList;
	}

	public List<byte[]> getSoftwareVersionList() {
		return softwareVersionList;
	}
	
	public List<byte[]> getCrcList(){
		return crcList;
	}

	public void setInblocknumber(byte[] inblocknumber) {
		this.inblocknumber = inblocknumber;
	}

	public void setSoftwareVersionNumber(byte[] softwareVersionNumber) {
		this.softwareVersionNumber = softwareVersionNumber;
	}

	public void setSoftwareTotalLenght(byte[] softwareTotalLenght) {
		this.softwareTotalLenght = softwareTotalLenght;
	}

	public void setSoftwareLenList(byte[] softwareLen) {
		softwareLenList.add(softwareLen);
	}

	public void setSoftwareVersionList(byte[] softwareVersion) {
		softwareVersionList.add(softwareVersion);
	}

	public void setCrcList(byte[] crc) {
		crcList.add(crc);
	}

	@Override
	public String toString() {
		return "FirmWare{" +
				"inblocknumber=" + Arrays.toString(inblocknumber) +
				", softwareVersionNumber=" + Arrays.toString(softwareVersionNumber) +
				", softwareTotalLenght=" + Arrays.toString(softwareTotalLenght) +
				", softwareLenList=" + softwareLenList +
				", softwareVersionList=" + softwareVersionList +
				", crcList=" + crcList +
				'}';
	}
}
