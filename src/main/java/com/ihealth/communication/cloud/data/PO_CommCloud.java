package com.ihealth.communication.cloud.data;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ihealth.communication.cloud.CommCloudSDK;
import com.ihealth.communication.cloud.ReturnDataUser;
import com.ihealth.communication.cloud.UserCheckSDK;
import com.ihealth.communication.cloud.tools.AppIDFactory;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.cloud.tools.HttpsPost;
import com.ihealth.communication.cloud.tools.Method;
import com.ihealth.communication.utils.ByteBufferUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;
import com.ihealth.communication.utils.Log;

public class PO_CommCloud {

	private static boolean isDebug = false;
	public static final String TAG = "CommCloudHS";

	Context context;
	private String QueueNum = "111111";

	private static final String SV_uploadSyncTime = "8f75928e6bc9490bafea38be9d3e678c";
	private static final String SV_downloadSyncTime = "08f8480f0d2f4bd4bb63a23ceebeb45a";

	private static final String SV_po         = "46d171ad45fa41d88ee4af3257d67066";

	public String messageReturn = "";
	public int result = 0;
	public long TS = 0;
	public float resultMessage = 0;
	public int queueNum = 0;

	private String un;
	private String host;
	private String client_id;
	private String client_secret;
	/**
	 * 构造函数
	 */
	public PO_CommCloud(Context context) {
		if(isDebug)
			Log.i(TAG, "实例化sdk_AuthorTools,获取本地配置 un host");
		this.context = context;
		un = context.getSharedPreferences("jiuan.sdk.author", 0).getString("email", "jiuan");
		host = context.getSharedPreferences(un+"userinfo", 0).getString("Host", "");//获取最优服务器
		if("".equals(host)){
			host = AppsDeviceParameters.webSite;
		}
		client_id = context.getSharedPreferences(un+"userinfo", 0).getString("client_id", "");
		client_secret = context.getSharedPreferences(un+"userinfo", 0).getString("client_secret", "");
		if(isDebug){
			Log.i(TAG, "取得un = " + un);
			Log.i(TAG, "取得host = " + host);
		}

	}
	/**
	 * PO3同步时间上传
	 */
	public int uploadSyncTime(String mac, String inputHost) throws Exception {

		// 编辑发送参数
		Map<String, String> workout_uploadParams = new HashMap<String, String>();
		workout_uploadParams.put("sc", AppsDeviceParameters.SC);
		workout_uploadParams.put("sv", SV_uploadSyncTime);
		workout_uploadParams.put("AppVersion", AppsDeviceParameters.APP_VERSION);
		workout_uploadParams.put("AppGuid", getAppID());
		workout_uploadParams.put("PhoneOS", android.os.Build.VERSION.RELEASE);
		workout_uploadParams.put("PhoneName", android.os.Build.MODEL);
		workout_uploadParams.put("PhoneID", getDeviceID());
		workout_uploadParams.put("PhoneLanguage", Locale.getDefault().getLanguage());
		workout_uploadParams.put("PhoneRegion", Locale.getDefault().getCountry());
		workout_uploadParams.put("QueueNum", "1");
		workout_uploadParams.put("Token", "");

		// 编辑参数UploadData
		JSONArray jsonWTUArr = new JSONArray();
		try {
			JSONObject jsonWTUData;
			for (int i = 0; i < 1; i++) {
				jsonWTUData = new JSONObject();

				jsonWTUData.put("mDeviceId", mac);
				jsonWTUData.put("TimeOfAppSetLow", System.currentTimeMillis()/1000);
				Log.e(TAG,"上传同步时间"+ ByteBufferUtil.TS2String(System.currentTimeMillis()/1000));
				jsonWTUData.put("DeviceType", "PO3");
				jsonWTUData.put("TimeZone", Method.getTimeZone());

				jsonWTUArr.put(i, jsonWTUData);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		workout_uploadParams.put("UploadData", jsonWTUArr.toString());

		String webAddress = inputHost + "/api5/lowmachine_upload_time.htm";//流程中调用接口
		Log.i("", "数据上传 = " + workout_uploadParams.toString());

		try {
				HttpsPost ht = new HttpsPost(context);
				messageReturn = ht.requireClass(webAddress, workout_uploadParams, "UTF-8");


		} catch (UnknownHostException e) {
			return 101;
		} catch (SocketTimeoutException e) {
			return 102;
		}

		if (messageReturn.length() == 0){
			return 999;
		} else if (messageReturn.length() == 3) {
			return Integer.valueOf(messageReturn);//403 404 500
		}

		try {
			JSONTokener jsonTParser = new JSONTokener(messageReturn);
			JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();

			this.result = jsonORegist.getInt("Result");
			this.TS = Long.parseLong(jsonORegist.getString("TS"));
			this.resultMessage = Integer.parseInt(jsonORegist.getString("ResultMessage"));
			this.queueNum = jsonORegist.getInt("QueueNum");

			Log.i("", "上传返回 = " + resultMessage);

			if (resultMessage == 100) {
				return 100;
			}
			else if(resultMessage == 208){//服务器地址不正确
				JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
				JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();
				String regionHost = jsonWebListOut.getString("RegionHost");
				Log.i("", "hs_upload返回208,拿到regionHost = " + regionHost);
				return uploadSyncTime(mac, regionHost);
			} else {
				return (int) resultMessage;
			}
		} catch (JSONException e) {
			return 999;
		}
	}
	private long syncTime;
	public long getSyncTime(){
		return syncTime;
	}
	/**
	 * PO3同步时间下载
	 * no network,error code 101
	 * timeout,error code 102
	 */
	public int downloadSyncTime(String mac, String inputHost
	) throws  Exception {

		// 编辑发送参数
		Map<String, String> workout_uploadParams = new HashMap<String, String>();
		workout_uploadParams.put("sc", AppsDeviceParameters.SC);
		workout_uploadParams.put("sv", SV_downloadSyncTime);
		workout_uploadParams.put("AppVersion", AppsDeviceParameters.APP_VERSION);
		workout_uploadParams.put("AppGuid", getAppID());
		workout_uploadParams.put("PhoneOS", android.os.Build.VERSION.RELEASE);
		workout_uploadParams.put("PhoneName", android.os.Build.MODEL);
		workout_uploadParams.put("PhoneID", getDeviceID());
		workout_uploadParams.put("PhoneLanguage", Locale.getDefault().getLanguage());
		workout_uploadParams.put("PhoneRegion", Locale.getDefault().getCountry());
		workout_uploadParams.put("QueueNum", "1");
		workout_uploadParams.put("Token", "");

		workout_uploadParams.put("mDeviceId", mac);
		workout_uploadParams.put("DeviceType", "PO3");


		String webAddress = inputHost + "/api5/lowmachine_download_time.htm";//流程中调用接口
		Log.i("", "数据上传 = " + workout_uploadParams.toString());

		try {
				HttpsPost ht = new HttpsPost(context);
				messageReturn = ht.requireClass(webAddress, workout_uploadParams, "UTF-8");


		} catch (UnknownHostException e) {
			return 101;
		} catch (SocketTimeoutException e) {
			return 102;
		}

		if (messageReturn.length() == 0){
			return 999;
		} else if (messageReturn.length() == 3) {
			return Integer.valueOf(messageReturn);//403 404 500
		}

		try {
			JSONTokener jsonTParser = new JSONTokener(messageReturn);
			JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();

			this.result = jsonORegist.getInt("Result");
			this.TS = Long.parseLong(jsonORegist.getString("TS"));
			this.resultMessage = Integer.parseInt(jsonORegist.getString("ResultMessage"));
			this.queueNum = jsonORegist.getInt("QueueNum");

			Log.i("", "上传返回 = " + resultMessage);

			if (resultMessage == 100) {
				JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
				JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();
				syncTime = jsonWebListOut.getLong("TimeOfAppSetLow");
				return 100;
			} else if(resultMessage == 208){//服务器地址不正确
				JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
				JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();
				String regionHost = jsonWebListOut.getString("RegionHost");

				Log.i("", "hs_upload返回208,拿到regionHost = " + regionHost);

				return downloadSyncTime(mac, regionHost);
			} else { // 登录失败
				return (int) resultMessage;
			}
		} catch (JSONException e) {
			return 999;
		}
	}
	/**
	 * 运动数据上传
	 * @param userName			用户名
	 * @param VerifyToken		Token
	 * @param hsArr				数据
	 * @param inputHost			传入的host  当前最适的Host
	 * @return	Data    接口结果类
	 * @throws SocketTimeoutException
	 */
	public PO_ReturnData am_po_upload(String userName, String VerifyToken, ArrayList<Data_PO_Result> hsArr, String inputHost
			) throws ConnectTimeoutException, SocketTimeoutException {

		PO_ReturnData hsUploadReturn = new PO_ReturnData();
		// 编辑发送参数
		Map<String, String> activity_uploadParams = new HashMap<String, String>();
		activity_uploadParams.put("sc", AppsDeviceParameters.SC);
		activity_uploadParams.put("sv", SV_po);
		activity_uploadParams.put("AppVersion", AppsDeviceParameters.APP_VERSION);
		activity_uploadParams.put("AppGuid", getAppID());
		activity_uploadParams.put("PhoneOS", android.os.Build.VERSION.RELEASE);
		activity_uploadParams.put("PhoneName", android.os.Build.MODEL);
		activity_uploadParams.put("PhoneID", getDeviceID());
		activity_uploadParams.put("PhoneLanguage", Locale.getDefault().getLanguage());
		activity_uploadParams.put("PhoneRegion", Locale.getDefault().getCountry());
		activity_uploadParams.put("QueueNum", "1");
		activity_uploadParams.put("Token", "");

		// 编辑参数UploadData
		JSONArray jsonWTUArr = new JSONArray();
		try {
			JSONObject jsonWTUData;
			if(isDebug)
				Log.i(TAG, hsArr.size() + "");
			for (int i = 0; i < hsArr.size(); i++) {
				jsonWTUData = new JSONObject();

				jsonWTUData.put("ChangeType", hsArr.get(i).getChangeType());
				jsonWTUData.put("LastChangeTime", hsArr.get(i).getLastChangeTime());
				jsonWTUData.put("PhoneDataID", hsArr.get(i).getPhoneDataID());
				jsonWTUData.put("PhoneCreateTime", hsArr.get(i).getPhoneCreateTime());
				jsonWTUData.put("Lat", hsArr.get(i).getLat());
				jsonWTUData.put("Lon", hsArr.get(i).getLon());
				jsonWTUData.put("TimeZone", hsArr.get(i).getTimeZone());

				jsonWTUData.put("Activity", hsArr.get(i).getActivity());

				JSONArray jsonWAVEArr = new JSONArray();
				int wlLenght = hsArr.get(i).getWave().split("A").length;
				for (int j = 0; j < wlLenght; j++) {
					jsonWAVEArr.put(j, hsArr.get(i).getWave().split("A")[j]);
				}
				jsonWTUData.put("Wave", jsonWAVEArr);

				jsonWTUData.put("PR", hsArr.get(i).getPR());
				jsonWTUData.put("Result", hsArr.get(i).getResult());
				jsonWTUData.put("FlowRate", hsArr.get(i).getFlowrate());
				jsonWTUData.put("ResultSource", hsArr.get(i).getResultSource());

				jsonWTUData.put("Mood", hsArr.get(i).getMood());
				jsonWTUData.put("Weather", hsArr.get(i).getWeather());

				jsonWTUData.put("Note", hsArr.get(i).getNote());
				jsonWTUData.put("NoteTS", hsArr.get(i).getNoteTS());

				jsonWTUData.put("MeasureTime", hsArr.get(i).getMeasureTime());
				jsonWTUData.put("MechineType", hsArr.get(i).getMechineType());
				jsonWTUData.put("MechineDeviceID", hsArr.get(i).getMechineDeviceID());

				jsonWTUArr.put(i, jsonWTUData);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		activity_uploadParams.put("Un", userName);
		activity_uploadParams.put("VerifyToken", VerifyToken);
		activity_uploadParams.put("UploadData", jsonWTUArr.toString());

		String webAddress = inputHost + AppsDeviceParameters.path + "oxygen_upload.htm";//流程中调用接口
		if(isDebug) 
			Log.i(TAG, "数据上传 = " + activity_uploadParams.toString());
		HttpsPost ht = new HttpsPost(context);

		try {
			messageReturn = ht.requireClass(webAddress, activity_uploadParams, "UTF-8");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if (messageReturn.length() == 0){
			return hsUploadReturn;
		}
		try {
			JSONTokener jsonTParser = new JSONTokener(messageReturn);
			JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();

			this.result = jsonORegist.getInt("Result");
			this.TS = Long.parseLong(jsonORegist.getString("TS"));
			this.resultMessage = Float.parseFloat(jsonORegist.getString("ResultMessage"));
			this.queueNum = jsonORegist.getInt("QueueNum");

			hsUploadReturn.setResultMessage(jsonORegist.getString("ResultMessage"));

			if(isDebug) Log.i(TAG, "上传返回 = " + resultMessage);

			if (resultMessage == 100.0) {
				return hsUploadReturn;
			} else if(resultMessage == 208.0){//服务器地址不正确
				JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
				JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();
				String regionHost = jsonWebListOut.getString("RegionHost");

				if(isDebug) Log.i(TAG, "hs_upload返回208,拿到regionHost = " + regionHost);

				PO_ReturnData hsUploadReturn1 = am_po_upload(un, VerifyToken, hsArr, regionHost);
				if ("100".equals(hsUploadReturn1.getResultMessage())) {// 登录成功

					// 保存最新的信息
					UserCheckSDK.saveUserInfo(context, null, null, regionHost, null, null, null, null,-1);

					if(isDebug) Log.i(TAG, "保存regionHost到本地");

					host = regionHost;
					return hsUploadReturn1;
				} else { // 登录失败
					return hsUploadReturn1;
				} 
			}else if(resultMessage == 212.0){//Token失效->刷新

				if(isDebug) Log.i(TAG, "212->Token过期:刷新Token");

				//取得refreshToken
				SharedPreferences sharedPreferences = context.getSharedPreferences("jiuan.sdk.author", 0);
				String refreshToken = sharedPreferences.getString("refreshToken", "");

				//执行刷新
				CommCloudSDK commCloudSDK = new CommCloudSDK(context);
				ReturnDataUser token_refresh = commCloudSDK.token_refresh(refreshToken, un, host);

				if("100".equals(token_refresh.getResultCode())){//成功

					//保存后重新调用上传
					if(isDebug) Log.i(TAG, "重新调用hs_upload");
					String accessToken = token_refresh.getAccessToken();
					PO_ReturnData bpUploadReturn1 = am_po_upload(un, accessToken, hsArr,host);

					if("100".equals(bpUploadReturn1.getResultMessage())){

						if(isDebug) Log.i(TAG, "刷新Token后上传HS数据成功!保存最新Token到本地");
						//保存新的accessToken, refreshToken到本地
						//保存到jiuan.sdk.author
						refreshToken = token_refresh.getRefreshToken();
						Editor localEditor = sharedPreferences.edit();
						localEditor.putString("accessToken", accessToken);
						localEditor.putString("refreshToken", refreshToken);
						localEditor.commit();

						// 保存最新的信息-un+"userinfo"
						UserCheckSDK.saveUserInfo(context, null, null, null, accessToken, refreshToken, null, null,-1);

						return bpUploadReturn1;
					}else{
						return bpUploadReturn1;
					}
				}else{
					return hsUploadReturn;
				}
			} else if(resultMessage == 221.0){//Token失败->重新登录
				if(isDebug)
					Log.i(TAG, "221->Token验证失败->其他APP已刷新,需重新登录");
				PO_ReturnData regReturnData = new PO_ReturnData();
				try {
					CommCloudSDK commCloudSDK = new CommCloudSDK(context);
					ReturnDataUser userSign = commCloudSDK.UserSign(client_id, client_secret, un, inputHost);
					if("100".equals(userSign.getResultCode())){
						regReturnData.setResultMessage(userSign.getResultCode());
						regReturnData.setRegionHost(userSign.getRegionHost());
						regReturnData.setAccessToken(userSign.getAccessToken());
						regReturnData.setRefreshToken(userSign.getRefreshToken());
						regReturnData.setExpires(userSign.getExpires());
						if(isDebug)
							Log.i(TAG, "重新登录成功,重新调用hs_upload上传数据");
						String accessToken = regReturnData.getAccessToken();
						PO_ReturnData hsUploadReturn1 = am_po_upload(un, accessToken, hsArr, inputHost);
						if("100".equals(hsUploadReturn1.getResultMessage())){
							if(isDebug)
								Log.i(TAG, "再次上HS数据成功,保存最新Token信息到本地");

							//保存新的accessToken, refreshToken到本地
							//保存到jiuan.sdk.author
							SharedPreferences sharedPreferences = context.getSharedPreferences("jiuan.sdk.author", 0);
							Editor edit = sharedPreferences.edit();
							String refreshToken = regReturnData.getRefreshToken();
							edit.putString("accessToken", accessToken);
							edit.putString("refreshToken", refreshToken);
							edit.commit();

							// 保存最新的信息-un+"userinfo"
							UserCheckSDK.saveUserInfo(context, null, null, null, accessToken, refreshToken, null, null,-1);

							return hsUploadReturn1;
						}else{
							return hsUploadReturn1;
						}
					}else{
						return regReturnData;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				return hsUploadReturn;
			}
		} catch (JSONException e) {
			return hsUploadReturn;
		}
		return hsUploadReturn;
	}

	private String getDeviceID() {
		TelephonyManager telephonyManager = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
		return (telephonyManager.getDeviceId() != null) ? telephonyManager.getDeviceId() : getAppID();
	}

	private String getAppID() {
		String appID = "appID";
		AppIDFactory appIDFactory = new AppIDFactory(context);
		appID = appIDFactory.getAppID();
		return appID;
	}

}
