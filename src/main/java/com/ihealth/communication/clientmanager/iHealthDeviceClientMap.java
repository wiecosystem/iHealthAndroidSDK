package com.ihealth.communication.clientmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ihealth.communication.manager.iHealthDevicesCallback;


/**
 * 
 * @hide
 */
public class iHealthDeviceClientMap {

	private static final String TAG = "ihealthDeviceManager";
	private Map<Integer, iHealthDevicesCallback> mapClient = new ConcurrentHashMap<Integer, iHealthDevicesCallback>();
	private Map<Integer, String> mapClientFilter = new ConcurrentHashMap<Integer, String>();
	private int clientIdCount = 0;
	private final Object mStateLock = new Object();
	
	public int add(iHealthDevicesCallback miHealthDevicesCallback) {
		synchronized(mStateLock){
			clientIdCount += 1;
			mapClient.put(clientIdCount, miHealthDevicesCallback);
			return clientIdCount;
		}
	}

	public void remove(int clientId) {
		synchronized (mStateLock) {
			   mapClient.remove(clientId);
			   mapClientFilter.remove(clientId);
		}
	}
	
	public void clear() {
		synchronized (mStateLock) {
			clientIdCount = 0;
			mapClient.clear();
			mapClientFilter.clear();
		}
	}

	public iHealthDevicesCallback getCallback(int clientId){
		return mapClient.get(clientId);
	}
	
	public List<iHealthDevicesCallback> getCallbacklist(String mac, String type) {
		List<iHealthDevicesCallback> list = new ArrayList<iHealthDevicesCallback>();
		for (int key : mapClient.keySet()) {
			String filterStr = mapClientFilter.get(key);
			if(filterStr == null){
				list.add(mapClient.get(key));
			}else{
				if(filterStr.contains(mac) || filterStr.contains(type)){
					list.add(mapClient.get(key));
				}
			}
			
		}
		return list;
	}
	
	public List<iHealthDevicesCallback> getCallbacklist_All() {
		List<iHealthDevicesCallback> list = new ArrayList<iHealthDevicesCallback>();
		for (int key : mapClient.keySet()) {
			list.add(mapClient.get(key));
		}
		return list;
	}
	
	public void addCallbackFilter(int clientId, String[] filterStr){
		String newFilterStr = null;
		for (int i = 0; i < filterStr.length; i++) {
			newFilterStr += filterStr[i];
		}
		String oldFilterStr = mapClientFilter.get(clientId);
		if(oldFilterStr != null){
			mapClientFilter.put(clientId, oldFilterStr + newFilterStr);
		}else{
			mapClientFilter.put(clientId, newFilterStr);
		}
	}
}
