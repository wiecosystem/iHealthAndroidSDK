package com.ihealth.communication.cloud.data;

import java.net.SocketTimeoutException;
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.telephony.TelephonyManager;
import com.ihealth.communication.utils.Log;

public class HS_CommCloud {

	private static boolean isDebug = false;
	public static final String TAG = "CommCloudHS"; 
	
	Context context;
	private String QueueNum = "111111";
	public static final String SV_weight_upload = "6695adca89834f1794cc02ac1ff7c7fc";
	private static final String SV_weight_download = "038e0b1af7794472a7e663d27aa59573";
	
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
	public HS_CommCloud(Context context) {
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
	 * 体称数据上传
	 * @param userName			用户名
	 * @param VerifyToken		Token
	 * @param hsArr				体称数据
	 * @param inputHost			传入的host  当前最适的Host
	 * @return	ReturnHSData    HS接口结果类
	 * @throws SocketTimeoutException
	 */
	public HS_ReturnData weight_upload(String userName, String VerifyToken, ArrayList<Data_HS_Result> hsArr, String inputHost
					) throws ConnectTimeoutException, SocketTimeoutException {

		HS_ReturnData hsUploadReturn = new HS_ReturnData();
		// 编辑发送参数
		Map<String, String> weight_uploadParams = new HashMap<String, String>();
		weight_uploadParams.put("sc", AppsDeviceParameters.SC);
		weight_uploadParams.put("sv", SV_weight_upload);
		weight_uploadParams.put("AppVersion", AppsDeviceParameters.APP_VERSION);
		weight_uploadParams.put("AppGuid", getAppID());
		weight_uploadParams.put("PhoneOS", android.os.Build.VERSION.RELEASE);
		weight_uploadParams.put("PhoneName", android.os.Build.MODEL);
		weight_uploadParams.put("PhoneID", getDeviceID());
		weight_uploadParams.put("PhoneLanguage", Locale.getDefault().getLanguage());
		weight_uploadParams.put("PhoneRegion", Locale.getDefault().getCountry());
		weight_uploadParams.put("QueueNum", "1");
		weight_uploadParams.put("Token", "");

		// 编辑参数UploadData
		JSONArray jsonWTUArr = new JSONArray();
		try {
			JSONObject jsonWTUData;
			String weather;
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
				jsonWTUData.put("BMI", hsArr.get(i).getBMI());
				jsonWTUData.put("BoneValue", hsArr.get(i).getBoneValue());
				jsonWTUData.put("DCI", hsArr.get(i).getDCI());
				jsonWTUData.put("FatValue", hsArr.get(i).getFatValue());
				jsonWTUData.put("MuscaleValue", hsArr.get(i).getMuscaleValue());
				jsonWTUData.put("MeasureType", hsArr.get(i).getMeasureType());
				jsonWTUData.put("WaterValue", hsArr.get(i).getWaterValue());
				jsonWTUData.put("WeightValue", hsArr.get(i).getWeightValue());
				jsonWTUData.put("MeasureTime", hsArr.get(i).getMeasureTime());
				jsonWTUData.put("Note", hsArr.get(i).getNote());
				jsonWTUData.put("VisceraFatLevel", hsArr.get(i).getVisceraFatLevel());
				jsonWTUData.put("MechineType", hsArr.get(i).getMechineType());
				jsonWTUData.put("MechineDeviceID", hsArr.get(i).getMechineDeviceID());
//5.0云协议新增
				jsonWTUData.put("NoteTS", hsArr.get(i).getNoteTS());
				jsonWTUData.put("Mood", hsArr.get(i).getMood());
				jsonWTUData.put("Activity", hsArr.get(i).getActivity());
				weather = hsArr.get(i).getTemp() +","+hsArr.get(i).getHumidity() +","+hsArr.get(i).getVisibility() +"," +hsArr.get(i).getWeather();
				jsonWTUData.put("Weather", weather);
				jsonWTUArr.put(i, jsonWTUData);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		weight_uploadParams.put("Un", userName);
		weight_uploadParams.put("VerifyToken", VerifyToken);
		weight_uploadParams.put("UploadData", jsonWTUArr.toString());

		String webAddress = inputHost + AppsDeviceParameters.path + "weight_upload.htm";//流程中调用接口
		if(isDebug) Log.i(TAG, "体重数据上传 = " + weight_uploadParams.toString());
		HttpsPost ht = new HttpsPost(context);
		
		try {
			messageReturn = ht.requireClass(webAddress, weight_uploadParams, "UTF-8");
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
			
			if(isDebug) Log.i(TAG, "体重上传返回 = " + resultMessage);
			
			if (resultMessage == 100.0) {
				return hsUploadReturn;
			} else if(resultMessage == 208.0){//服务器地址不正确
				JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
				JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();
				String regionHost = jsonWebListOut.getString("RegionHost");
				
				if(isDebug) Log.i(TAG, "hs_upload返回208,拿到regionHost = " + regionHost);
				
				HS_ReturnData hsUploadReturn1 = weight_upload(un, VerifyToken, hsArr, regionHost);
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
					HS_ReturnData bpUploadReturn1 = weight_upload(un, accessToken, hsArr,host);
					
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
				HS_ReturnData regReturnData = new HS_ReturnData();
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
						HS_ReturnData hsUploadReturn1 = weight_upload(un, accessToken, hsArr, inputHost);
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
