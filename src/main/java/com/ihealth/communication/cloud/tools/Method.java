package com.ihealth.communication.cloud.tools;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ihealth.communication.cloud.data.AM_InAuthor;
import com.ihealth.communication.cloud.data.ActivityData;
import com.ihealth.communication.cloud.data.DataBaseConstants;
import com.ihealth.communication.cloud.data.DataBaseTools;
import com.ihealth.communication.cloud.data.Data_AM_Activity;
import com.ihealth.communication.cloud.data.Data_AM_ActivityDayReport;
import com.ihealth.communication.cloud.data.Data_AM_ActivitySummary;
import com.ihealth.communication.cloud.data.Data_AM_Sleep;
import com.ihealth.communication.cloud.data.Data_AM_SleepSectionReport;
import com.ihealth.communication.cloud.data.Data_TB_Swim;
import com.ihealth.communication.cloud.data.Data_TB_SwimSection;
import com.ihealth.communication.cloud.data.Data_TB_Workout;
import com.ihealth.communication.cloud.data.Make_Data_Util;
import com.ihealth.communication.cloud.data.SleepData;
import com.ihealth.communication.cloud.data.Summary_Sleep;
import com.ihealth.communication.cloud.data.Summary_WorkOut;
import com.ihealth.communication.control.AmProfile;
import com.ihealth.communication.utils.ByteBufferUtil;
import com.ihealth.communication.utils.MD5;
import com.ihealth.communication.utils.PublicMethod;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.ihealth.communication.utils.Log;


public class Method {

	private static final String TAG = "Method";
	/**
	 * @return 返回当前时区
	 */
	public static float getTimeZone() {
		float timeZone = 0;

		float timeZoneDstSaving = (float) TimeZone.getDefault().getDSTSavings() / (float) 3600000;
//		Log.v("UpLoadBg", " timeZoneDstSaving = " + timeZoneDstSaving);

		boolean isDst = TimeZone.getDefault().inDaylightTime(new Date());
//		Log.v("UpLoadBg", " aa = " + isDst);

		float timeZoneUTC = (float) TimeZone.getDefault().getRawOffset() / (float) 3600000;
//		Log.v("UpLoadBg", " timeZoneUTC = " + timeZoneUTC);

		timeZone = timeZoneUTC + ((isDst) ? timeZoneDstSaving : 0);
//		Log.v("UpLoadBg", " timeZone = " + timeZone);

		return timeZone;
	}
	
	/**
	 * 取时间戳
	 */
	public static long getTS() {
		long val = System.currentTimeMillis() / 1000;
		return val;
	}
	
	/**
     * 生日格式转换成可以变成long的格式 2010-3-3-------20100303
     */
    public static long BirthdayToLong(String Birthday) {
        long BirthLong = 0;

        SimpleDateFormat sdfResouce = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdfResouce.setTimeZone(TimeZone.getDefault());

        Date fromData = new Date();
        try {
            fromData = sdfResouce.parse(Birthday + " 00:00:00");
            BirthLong = fromData.getTime() / 1000;
        } catch (Exception e) {
            Log.e("aa", "getDefaultTimerStr Exception ");
            e.printStackTrace();
        }

        return BirthLong;
    }
	/**
	 * 通过时间，设备ID等信息算出数据ID
	 * 
	 * @param macId
	 * @param Ts
	 * @param bgValue
	 * @return
	 */
	public static String getBgDataId(String macId, long Ts, int bgValue) {
		String result = "";
		result = macId + Ts + bgValue + "00000000";
		if (macId == null) {
			result = "BG5" + Ts + bgValue + "00000000";
		}
		result = ByteBufferUtil.Bytes2HexString(MD5.md5(result));
		return result;

	}
	
	//时间数据处理
	public static long String2TS(String dateStr){
		long ret = -1;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");  
		Date date;
		try {
			date = sdf.parse(dateStr);
			ret = date.getTime();
			ret = ret / 1000;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	/**
	 * @param data
	 * @return 把AM的运动数据转换成JSON格式字符串
	 */
//	public static String changeActivityData2Json(Context con,String userName,String mac, String type, boolean is4byte, byte[] data) {
//		
//		String str_date_get = "";//取出来的日期
//		String str_steps_get = "";//取出来的步数
//		String str_calorie_get = "";//取出来的卡路里
//		String str_date_set = "";//存储的日期
//		String str_steps_set = "";//存储的步数
//		String str_calorie_set = "";//存储的卡路里
//		//
//		int calorie = 0;//卡路里－－5分钟
//		int steps = 0;//步数－－5分钟
//		int stepLength = 0;//步长
//		//取数据
//		String getlastdata = getLastActivityDataSharedPreference(con,mac);
//		if(!getlastdata.replace("#", "").replace("-", "").equals("")){
//			str_date_get = getlastdata.split("#")[0];
//			str_steps_get = getlastdata.split("#")[1].split("-")[0];
//			str_calorie_get = getlastdata.split("#")[1].split("-")[1];
//		}
//		// 解析每一段数据		
//		ArrayList<ActivityData> list_ad = new ArrayList<ActivityData>();
//		if (data != null && data.length > 6) {
//			String date = ((int) (data[0] & 0xFF) + 2000) + "-" + (int) (data[1] & 0xFF) + "-" + (int) (data[2] & 0xFF) + " ";
//			stepLength = (int)(data[3] & 0xFF);//步长
//			String time =getSuitableTime((int)(data[6 + 0] & 0xFF) + ":" + (int) (data[6 + 1] & 0xFF));
//			int gapCalorie = -1;
//			int gapSteps = -1;
//			if(!str_date_get.equals("")){
//				if((date.trim().equals(str_date_get.split(" ")[0]))&&(PublicMethod.String2TS(getAfterTime((date+time),0))-PublicMethod.String2TS(str_date_get)) < 900){
//					gapSteps = Integer.parseInt(str_steps_get);
//					gapCalorie = Integer.parseInt(str_calorie_get);
//				}				
//			}
//			if (is4byte) {
//				int num_activity = (data.length - 6) / 8;
//				for (int j = 0; j < num_activity; j++) {
//					ActivityData ad = new ActivityData();
//					ad.setTime(getAfterTime((date+time),j));
//					ad.setSteps((int) (data[6 + 2 + j * 8] & 0xFF) * 256 * 256 * 256 + (int) (data[6 + 3 + j * 8] & 0xFF) * 256 * 256 +
//							(int) (data[6 + 4 + j * 8] & 0xFF) * 256 + (int) (data[6 + 5 + j * 8] & 0xFF));
//					ad.setCalorie((int) (data[6 + 6 + j * 8] & 0xFF) * 256 + (int) (data[6 + 7 + j * 8] & 0xFF));
//					list_ad.add(ad);
//					//计算实时卡路里和步长
//					if(gapCalorie!=-1){
//						calorie = ad.getCalorie() - gapCalorie;
//						gapCalorie = ad.getCalorie();
//					}else{
//						calorie = 0;
//						gapCalorie = ad.getCalorie();
//					}
//					if(gapSteps!=-1){
//						steps = ad.getSteps() - gapSteps;
//						gapSteps = ad.getSteps();
//					}else{
//						steps = 0;
//						gapSteps = ad.getSteps();
//					}
//					//添加数据库－－运动5分钟数据
//					Data_AM_Activity SingleData = Make_Data_Util.makeDataSingleAMA(userName,mac,type,ad,calorie,steps,stepLength);
//					DataBaseTools sdk_db = new DataBaseTools(con);
//					Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_ACTIVITY, SingleData);
//				}
//			}else {
//				int num_activity = (data.length - 6) / 6;
//				for (int j = 0; j < num_activity; j++) {
//					ActivityData ad = new ActivityData();
//					ad.setTime(getAfterTime((date+time),j));
//					ad.setSteps((int) (data[6 + 2 + j * 6] & 0xFF) * 256 + (int) (data[6 + 3 + j * 6] & 0xFF));
//					ad.setCalorie((int) (data[6 + 4 + j * 6] & 0xFF) * 256 + (int) (data[6 + 5 + j * 6] & 0xFF));
//					list_ad.add(ad);
//					//计算实时卡路里和步长
//					if(gapCalorie!=-1){
//						calorie = ad.getCalorie() - gapCalorie;
//						gapCalorie = ad.getCalorie();
//					}else{
//						calorie = 0;
//						gapCalorie = ad.getCalorie();
//					}
//					if(gapSteps!=-1){
//						steps = ad.getSteps() - gapSteps;
//						gapSteps = ad.getSteps();
//					}else{
//						steps = 0;
//						gapSteps = ad.getSteps();
//					}
//					//添加数据库－－运动5分钟数据
//					Data_AM_Activity SingleData = Make_Data_Util.makeDataSingleAMA(userName,mac,type,ad,calorie,steps,stepLength);
//					DataBaseTools sdk_db = new DataBaseTools(con);
//					Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_ACTIVITY, SingleData);
//				}
//			}
//		}
//		JSONArray array_workout = new JSONArray();
//		JSONObject object = new JSONObject();
//		for(int j = 0;j<list_ad.size();j++){
//			JSONObject stoneObject = new JSONObject();
//			try {
//				stoneObject.put("time", list_ad.get(j).getTime() + "");
//				stoneObject.put("steps", list_ad.get(j).getSteps() + "");
//				stoneObject.put("calorie", list_ad.get(j).getCalorie() + "");
//				str_date_set = list_ad.get(j).getTime() + "";
//				str_steps_set = list_ad.get(j).getSteps() + "";
//				str_calorie_set = list_ad.get(j).getCalorie() + "";
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			array_workout.put(stoneObject);
//		}
//		//存数据
//		String setlastdata = str_date_set +"#"+ str_steps_set +"-"+ str_calorie_set;
//		setLastActivityDataSharedPreference(con,mac,setlastdata);
//		//添加数据库--运动日报表
//		if(!str_calorie_set.equals("")){
//			Data_AM_ActivityDayReport SingleData = Make_Data_Util.makeDataSingleAMADR(userName,mac,type,Integer.parseInt(str_calorie_set),Integer.parseInt(str_steps_set),stepLength,PublicMethod.String2TS(setlastdata));
//			DataBaseTools sdk_db = new DataBaseTools(con);
//			Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_ACTIVITYREPORT, SingleData);
//		}
//		//timer
//		AM_InAuthor sdk_InAuthor = new AM_InAuthor();
//		sdk_InAuthor.initAuthor(con, userName);
//		sdk_InAuthor.run();
//		try {
//			object.putOpt("ActivityData", array_workout);
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return object.toString();
//	}
	/**
	 * 解析运动数据，但是不规整，把原始数据上云
	 * @param con
	 * @param userName
	 * @param mac
	 * @param type
	 * @param o 传给用户的json格式运动数据
	 */
	public static void parseActivityDataJson(Context con, String userName, String mac, String type, JSONObject o) {
		//声明变量
		JSONArray activityDataArray = null;	//全部运动数据的jsonArray
		JSONObject eachActivityDataObject = null;	//每一段运动数据的object
		JSONArray eachActivityDataArray = null;	//每一段运动数据的array
		
		String str_date_get = "";//取出来的日期
		String str_steps_get = "";//取出来的步数
		String str_calorie_get = "";//取出来的卡路里
		String str_date_set = "";//存储的日期
		String str_steps_set = "";//存储的步数
		String str_calorie_set = "";//存储的卡路里
		//
		int calorie = 0;//卡路里－－5分钟
		int steps = 0;//步数－－5分钟
		int stepLength = 0;//步长
		
		
		if (o != null) {
			JSONObject activityData = o;
			try {
				activityDataArray = activityData.getJSONArray(AmProfile.SYNC_ACTIVITY_DATA_AM);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//对所有的运动数据进行循环
			for (int i = 0; i < activityDataArray.length(); i++) {
				try {
					eachActivityDataObject = (JSONObject) activityDataArray.get(i);
					eachActivityDataArray = eachActivityDataObject.getJSONArray(AmProfile.SYNC_ACTIVITY_EACH_DATA_AM);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(eachActivityDataArray != null && eachActivityDataArray.length()>0) {
					//取数据
					String getlastdata = getLastActivityDataSharedPreference(con,mac);
					if(getlastdata!=null&&!getlastdata.replace("#", "").replace("-", "").equals("")){
						str_date_get = getlastdata.split("#")[0];
						str_steps_get = getlastdata.split("#")[1].split("-")[0];
						str_calorie_get = getlastdata.split("#")[1].split("-")[1];
					}

					int gapCalorie = -1;
					int gapSteps = -1;

					String date = null;
					try {
						date = ((JSONObject) eachActivityDataArray.get(0)).getString(AmProfile.SYNC_ACTIVITY_DATA_TIME_AM).split(" ")[0];
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					//同一天的运动数据
					if(date!=null&&!str_date_get.equals("")){
						if((date.trim().equals(str_date_get.split(" ")[0]))){
							gapSteps = Integer.parseInt(str_steps_get);
							gapCalorie = Integer.parseInt(str_calorie_get);
						}
					}

					JSONObject fiveMinDataObject = null;
					//对每一个5分钟数据进行循环
					for (int j = 0; j < eachActivityDataArray.length(); j++) {
						try {
							fiveMinDataObject = (JSONObject) eachActivityDataArray.get(j);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						ActivityData ad = new ActivityData();
						try {
							ad.setTime(fiveMinDataObject.getString(AmProfile.SYNC_ACTIVITY_DATA_TIME_AM));
							ad.setSteps(Integer.parseInt(fiveMinDataObject.getString(AmProfile.SYNC_ACTIVITY_DATA_STEP_AM)));
							ad.setCalorie(Integer.parseInt(fiveMinDataObject.getString(AmProfile.SYNC_ACTIVITY_DATA_CALORIE_AM)));
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//计算实时卡路里和步长
						if(gapCalorie!=-1&&((ad.getCalorie() - gapCalorie) >= 0)){
							calorie = ad.getCalorie() - gapCalorie;
							gapCalorie = ad.getCalorie();
						}else{
							calorie = 0;
							gapCalorie = ad.getCalorie();
						}
						if(gapSteps!=-1&&((ad.getSteps() - gapSteps) >= 0)){
							steps = ad.getSteps() - gapSteps;
							gapSteps = ad.getSteps();
						}else{
							steps = 0;
							gapSteps = ad.getSteps();
						}
						//添加数据库－－运动5分钟数据
						Data_AM_Activity SingleData = Make_Data_Util.makeDataSingleAMA(userName,mac,type,ad,calorie,steps,stepLength);
						DataBaseTools sdk_db = new DataBaseTools(con);
						Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_ACTIVITY, SingleData);
					}

					try {
						str_date_set = ((JSONObject) eachActivityDataArray.get(eachActivityDataArray.length()-1)).getString(AmProfile.SYNC_ACTIVITY_DATA_TIME_AM);
						str_steps_set = ((JSONObject) eachActivityDataArray.get(eachActivityDataArray.length()-1)).getString(AmProfile.SYNC_ACTIVITY_DATA_STEP_AM);
						str_calorie_set = ((JSONObject) eachActivityDataArray.get(eachActivityDataArray.length()-1)).getString(AmProfile.SYNC_ACTIVITY_DATA_CALORIE_AM);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//存数据
					String setlastdata = str_date_set +"#"+ str_steps_set +"-"+ str_calorie_set;
					setLastActivityDataSharedPreference(con,mac,setlastdata);
					//添加数据库--运动日报表
					if(!str_calorie_set.equals("")){
						Data_AM_ActivityDayReport SingleData = Make_Data_Util.makeDataSingleAMADR(userName,mac,type,Integer.parseInt(str_calorie_set),Integer.parseInt(str_steps_set),stepLength,PublicMethod.String2TS(setlastdata));
						DataBaseTools sdk_db = new DataBaseTools(con);
						Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_ACTIVITYREPORT, SingleData);
					}
				}else {
					Log.i(TAG, "第"+i+"段运动数据为空");
				}

			}
			
			//timer
			AM_InAuthor sdk_InAuthor = new AM_InAuthor();
			sdk_InAuthor.initAuthor(con, userName);
			sdk_InAuthor.run();
		}
	}
	
	/**
	 * 保存
	 * LastActivityData
	 */
	private static void setLastActivityDataSharedPreference(Context context,String mac, String data) {
		SharedPreferences sp = context.getSharedPreferences("LastActivityData", Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString(mac, data);
		editor.commit();
	}
	/**
	 * 读取
	 * LastActivityData
	 */
	private static String getLastActivityDataSharedPreference(Context context,String mac) {
		SharedPreferences sp = context.getSharedPreferences("LastActivityData", Context.MODE_PRIVATE);
		return sp.getString(mac, "");
	}
	
	/**
	 * @param data
	 * @return 把AM的睡眠数据转换成JSON格式字符串
	 */
//	public static String changeSleepData2Json(Context con,String userName,String mac,String type, List<byte[]> dataList){
//		//存储所有的睡眠段表
//		ArrayList<Object> amSleepSectionsList = new ArrayList<Object>();
//		//5分钟数据list
//		ArrayList<Object> amSleep5MinList = new ArrayList<Object>();
//		
//		//对每段睡眠数据循环处理
//		for (byte[] data : dataList) {
//			boolean needPinDuan = false;
//			String startTime = "";//开始入睡时间"YY-MM-DD HH:MM:SS"
//			String endTime = "";//睡眠结束时间"YY-MM-DD HH:MM:SS"
//			int fallSleep = 0;//入睡时长－－记录的是段数，每段5分钟
//			int sleep = 0;    //浅睡时长－－记录的是段数，每段5分钟
//			int deepSleep = 0;//深睡时长－－记录的是段数，每段5分钟
//			int awakenTimes = 0;//醒来的次数
//			int awake = 0;      //醒来的时长－－记录的是段数，每段5分钟
//			String timeSectionId = "";//睡眠段ID
//			//取数据
//			String getlastdata = getLastSectionDataSharedPreference(con,mac);
//			ArrayList<SleepData> list_sd = new ArrayList<SleepData>();
//			// 解析每一段数据
//			if (data != null && data.length > 8) {
//				String date = ((int) (data[0] & 0xFF) + 2000) + "-" + (int) (data[1] & 0xFF) + "-" + (int) (data[2] & 0xFF) + " ";
//				String time = getSuitableTime((int) (data[3] & 0xFF) + ":" + + (int) (data[4] & 0xFF));
//				int num_sleep = (data.length - 8) / 1;
//				if(getlastdata.equals("")){//没有缓存的section
//					needPinDuan = false;
//					startTime = "";
//					endTime = "";
//					fallSleep = 0;
//					sleep = 0;
//					deepSleep = 0;
//					awakenTimes = 0;
//					awake = 0;
//					timeSectionId = "";
//				}else{//有section
//					String[] dataShare = getlastdata.split("#");
//					endTime = dataShare[1];
//					if((PublicMethod.String2TS(getAfterTime((date+time),0))-PublicMethod.String2TS(endTime)) < 900){//需要拼段
//						needPinDuan = true;
//						startTime = dataShare[0];
//						endTime = dataShare[1];
//						fallSleep = Integer.parseInt(dataShare[2]);
//						sleep = Integer.parseInt(dataShare[3]);
//						deepSleep = Integer.parseInt(dataShare[4]);
//						awakenTimes = Integer.parseInt(dataShare[5]);
//						awake = Integer.parseInt(dataShare[6]);
//						timeSectionId = dataShare[7];
//					}else{//不需要拼段
//						needPinDuan = false;
//						startTime = "";
//						endTime = "";
//						fallSleep = 0;
//						sleep = 0;
//						deepSleep = 0;
//						awakenTimes = 0;
//						awake = 0;
//						timeSectionId = "";
//					}
//				}
////				Log.i("startTime",startTime);
////				Log.i("endTime",endTime);
////				Log.i("fallSleep",fallSleep+"");
////				Log.i("sleep",sleep+"");
////				Log.i("deepSleep",deepSleep+"");
////				Log.i("awakenTimes",awakenTimes+"");
////				Log.i("awake",awake+"");
////				Log.i("timeSectionId",timeSectionId+"");
//				//处理section数据
//				if(!needPinDuan){
//					startTime = getAfterTime((date+time),0);//开始时间
//				}
//				endTime = getAfterTime((date+time), num_sleep);//结束时间
//				if(!needPinDuan){
//					timeSectionId = mac + PublicMethod.String2TS(startTime) + PublicMethod.String2TS(endTime);//section id
//				}
//				boolean isFallSleepData = true;//是否是入睡数据0
//				int temp = 0;//睡眠数据缓存
//				for (int j = 0; j < num_sleep; j++) {
//					SleepData sd = new SleepData();
//					sd.setTime(getAfterTime((date+time),j));
//					sd.setGrade((int) (data[8 + j] & 0xFF));
//					list_sd.add(sd);
//					//计算入睡时间
//					if(!needPinDuan){
//						if(isFallSleepData){
//							if((int) (data[8 + j] & 0xFF) == 0){
//								fallSleep += 1;
//							}else{
//								isFallSleepData = false;
//							}
//						}					
//					}
//					//浅睡时间
//					if((int) (data[8 + j] & 0xFF) == 1){
//						sleep += 1;
//					}
//					//深睡时间
//					if((int) (data[8 + j] & 0xFF) == 2){
//						deepSleep += 1;
//					}
//					//醒来次数
//					if((int) (data[8 + j] & 0xFF) == 0 && temp != 0){
//						awakenTimes += 1;
//					}
//					temp = (int) (data[8 + j] & 0xFF);
//					//醒来时间
//					if(!isFallSleepData && (int) (data[8 + j] & 0xFF) == 0){
//						awake += 1;
//					}
//					//添加数据库－－睡眠5分钟数据
//					Data_AM_Sleep SingleData = Make_Data_Util.makeDataSingleAMS(userName,mac,type,(int) (data[8 + j] & 0xFF),timeSectionId,getAfterTime((date+time),j));
//					DataBaseTools sdk_db = new DataBaseTools(con);
//					Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_SLEEP, SingleData);
//				}
//			}
//			JSONArray array_sleep = new JSONArray();
//			JSONObject object = new JSONObject();
//			for(int j = 0;j<list_sd.size();j++){
//				JSONObject stoneObject = new JSONObject();
//				try {
//					stoneObject.put("time", list_sd.get(j).getTime() + "");
//					stoneObject.put("grade", list_sd.get(j).getGrade() + "");
//				} catch (JSONException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				array_sleep.put(stoneObject);
//			}
//			try {
//				amSleep5MinList.add(object.putOpt("SleepData", array_sleep));
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			//存数据
//			String setlastdata =  startTime+"#"+endTime+"#"+fallSleep+"#"+sleep+"#"+deepSleep+"#"+awakenTimes+"#"+awake +"#"+timeSectionId ;
//			setLastSectionDataSharedPreference(con,mac,setlastdata);
//			//将每段睡眠段表添加进list
//			Data_AM_SleepSectionReport SingleData = Make_Data_Util.makeDataSingleAMSSR(userName,mac,type,awake,deepSleep,fallSleep,sleep,awakenTimes,startTime,endTime,timeSectionId);
//			amSleepSectionsList.add(SingleData);
//		}
//		//睡眠段表判重
//		ArrayList<Object> noRepeatSleepSectionList = PublicMethod.isRepeat_Sleep(amSleepSectionsList);
//		//判重后的睡眠段表循环加入待上云数据库
//		DataBaseTools sdk_db = new DataBaseTools(con);
//		for (int i = 0; i < noRepeatSleepSectionList.size(); i++) {
//			Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_SLEEPREPORT, noRepeatSleepSectionList.get(i));
//		}
//		//timer
//		AM_InAuthor sdk_InAuthor = new AM_InAuthor();
//		sdk_InAuthor.initAuthor(con, userName);
//		sdk_InAuthor.run();
//		return amSleep5MinList.toString();		
//	}
	
	/**
	 * 解析睡眠数据但不规整，把数据上云
	 * @param con
	 * @param userName
	 * @param mac
	 * @param type
	 * @param o 传给用户的json格式睡眠数据
	 */
	public static void parseSleepDataJson(Context con, String userName, String mac, String type, JSONObject o) {
		//声明变量
		JSONArray sleepDataArray = null;	//全部运动数据的jsonArray
		JSONObject eachSleepDataObject = null;	//每一段运动数据的object
		JSONArray eachSleepDataArray = null;	//每一段运动数据的array
		
		//存储所有的睡眠段表
		ArrayList<Object> amSleepSectionsList = new ArrayList<Object>();
		//5分钟数据list
		ArrayList<Object> amSleep5MinList = new ArrayList<Object>();
		
		if (o != null) {
			JSONObject sleepDataObject = o;
			try {
				sleepDataArray = sleepDataObject.getJSONArray(AmProfile.SYNC_SLEEP_DATA_AM);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//对所有的睡眠数据进行循环
			for (int i = 0; i < sleepDataArray.length(); i++) {
					
				String startTime = "";//开始入睡时间"YY-MM-DD HH:MM:SS"
				String endTime = "";//睡眠结束时间"YY-MM-DD HH:MM:SS"
				int fallSleep = 0;//入睡时长－－记录的是段数，每段5分钟
				int sleep = 0;    //浅睡时长－－记录的是段数，每段5分钟
				int deepSleep = 0;//深睡时长－－记录的是段数，每段5分钟
				int awakenTimes = 0;//醒来的次数
				int awake = 0;      //醒来的时长－－记录的是段数，每段5分钟
				String timeSectionId = "";//睡眠段ID
				try {
					eachSleepDataObject = (JSONObject) sleepDataArray.get(i);
					eachSleepDataArray = eachSleepDataObject.getJSONArray(AmProfile.SYNC_SLEEP_EACH_DATA_AM);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (eachSleepDataArray != null && eachSleepDataArray.length() > 0) {
					String date = null;
					String time = null;
					try {
						date = ((JSONObject) eachSleepDataArray.get(0)).getString(AmProfile.SYNC_SLEEP_DATA_TIME_AM).split(" ")[0];
						time = ((JSONObject) eachSleepDataArray.get(0)).getString(AmProfile.SYNC_SLEEP_DATA_TIME_AM).split(" ")[1];
						startTime = ((JSONObject) eachSleepDataArray.get(0)).getString(AmProfile.SYNC_SLEEP_DATA_TIME_AM);
						endTime = ((JSONObject) eachSleepDataArray.get(eachSleepDataArray.length() - 1)).getString(AmProfile.SYNC_SLEEP_DATA_TIME_AM);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					timeSectionId = mac + PublicMethod.String2TS(startTime) + PublicMethod.String2TS(endTime);

					JSONObject fiveMinDataObject = null;
					boolean isFallSleepData = true;//是否是入睡数据0
					int temp = 0;//睡眠数据缓存
					//对每一个5分钟数据进行循环
					for (int j = 0; j < eachSleepDataArray.length(); j++) {
						try {
							fiveMinDataObject = (JSONObject) eachSleepDataArray.get(j);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
						}
						SleepData sd = new SleepData();
						try {
							sd.setTime(fiveMinDataObject.getString(AmProfile.SYNC_SLEEP_DATA_TIME_AM));
							sd.setGrade(Integer.parseInt(fiveMinDataObject.getString(AmProfile.SYNC_SLEEP_DATA_LEVEL_AM)));
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(isFallSleepData){
							if(sd.getGrade() == 0){
								fallSleep += 1;
							}else{
								isFallSleepData = false;
							}
						}
						//浅睡时间
						if(sd.getGrade() == 1){
							sleep += 1;
						}
						//深睡时间
						if(sd.getGrade() == 2){
							deepSleep += 1;
						}
						//醒来次数
						if(sd.getGrade() == 0 && temp != 0){
							awakenTimes += 1;
						}
						temp = sd.getGrade();
						//醒来时间
						if(!isFallSleepData && sd.getGrade() == 0){
							awake += 1;
						}

						//添加数据库－－睡眠5分钟数据
						Data_AM_Sleep SingleData = Make_Data_Util.makeDataSingleAMS(userName,mac,type,sd.getGrade(),timeSectionId,sd.getTime());
						DataBaseTools sdk_db = new DataBaseTools(con);
						Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_SLEEP, SingleData);
					}
					//睡眠段表
					Data_AM_SleepSectionReport SingleData = Make_Data_Util.makeDataSingleAMSSR(userName,mac,type,awake,deepSleep,fallSleep,sleep,awakenTimes,startTime,endTime,timeSectionId);
					DataBaseTools sdk_db = new DataBaseTools(con);
					Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_SLEEPREPORT, SingleData);
				} else {
					Log.i(TAG, "第"+i+"段睡眠数据为空");
				}

			}
			//timer
			AM_InAuthor sdk_InAuthor = new AM_InAuthor();
			sdk_InAuthor.initAuthor(con, userName);
			sdk_InAuthor.run();
		}
	}
	
	/**
	 * 保存
	 * LastSectionData
	 */
	private static void setLastSectionDataSharedPreference(Context context,String mac, String data) {
		SharedPreferences sp = context.getSharedPreferences("LastSectionData", Context.MODE_PRIVATE);
		Editor editor = sp.edit();
		editor.putString(mac, data);
		editor.commit();
	}
	/**
	 * 读取
	 * LastSectionData
	 */
	private static String getLastSectionDataSharedPreference(Context context,String mac) {
		SharedPreferences sp = context.getSharedPreferences("LastSectionData", Context.MODE_PRIVATE);
		return sp.getString(mac, "");
	}
	
//	public static String changeStageReport2Json(Context context, String userName, String mac, String type, byte[] data) {
//        ArrayList<Summary_WorkOut> list_workout = new ArrayList<Summary_WorkOut>();
//        ArrayList<Summary_Sleep> list_sleep = new ArrayList<Summary_Sleep>();
//        if (data != null && data.length > 0) {
//            int num_stage = data.length / 17;
//            for (int i = 0; i < num_stage; i++) {
//                if (data[0 + i * 17] == (byte) 0x01) {// work out
//                    Summary_WorkOut wo = new Summary_WorkOut();
//                    wo.setId(1);
//                    wo.setOverTime("20" + (int) (data[1 + i * 17] & 0xFF) + "-" + (int) (data[2 + i * 17] & 0xFF) + "-"
//                            + (int) (data[3 + i * 17] & 0xFF) + " " + (int) (data[4 + i * 17] & 0xFF) + ":"
//                            + (int) (data[5 + i * 17] & 0xFF));
//                    wo.setUseTime((int) (data[6 + i * 17] & 0xFF) * 256 + (int) (data[7 + i * 17] & 0xFF));
//                    wo.setSteps((int) (data[8 + i * 17] & 0xFF) * 256 + (int) (data[9 + i * 17] & 0xFF));
//                    wo.setDistance(Float.parseFloat((int) (data[10 + i * 17] & 0xFF) + "."
//                            + (int) (data[11 + i * 17] & 0xFF)));
//                    wo.setCalorie((int) (data[12 + i * 17] & 0xFF) * 256 + (int) (data[13 + i * 17] & 0xFF));
//                    list_workout.add(wo);
//                } else if (data[0 + i * 17] == (byte) 0x02) {// sleep
//                    Summary_Sleep ss = new Summary_Sleep();
//                    ss.setId(2);
//                    ss.setOverTime("20" + (int) (data[1 + i * 17] & 0xFF) + "-" + (int) (data[2 + i * 17] & 0xFF) + "-"
//                            + (int) (data[3 + i * 17] & 0xFF) + " " + (int) (data[4 + i * 17] & 0xFF) + ":"
//                            + (int) (data[5 + i * 17] & 0xFF));
//                    ss.setUseTime((int) (data[6 + i * 17] & 0xFF) * 256 + (int) (data[7 + i * 17] & 0xFF));
//                    ss.setSleepEfficiency((Float
//                            .parseFloat(((int) (data[8 + i * 17] & 0xFF) * 256 + (int) (data[9 + i * 17] & 0xFF)) + "") / 10));
//                    ss.setIsDelayFifty((int) (data[12 + i * 17] & 0xFF));
//                    list_sleep.add(ss);
//                }
//            }
//        }
//        JSONArray array_workout = new JSONArray();
//        JSONArray array_sleep = new JSONArray();
//        JSONObject object = new JSONObject();
//        for (int j = 0; j < list_workout.size(); j++) {
//            JSONObject stoneObject = new JSONObject();
//            try {
//                stoneObject.put("id", list_workout.get(j).getId() + "");
//                stoneObject.put("overTime", list_workout.get(j).getOverTime() + "");
//                stoneObject.put("useTime", list_workout.get(j).getUseTime() + "");
//                stoneObject.put("steps", list_workout.get(j).getSteps() + "");
//                stoneObject.put("distance", list_workout.get(j).getDistance() + "");
//                stoneObject.put("calorie", list_workout.get(j).getCalorie() + "");
//            } catch (JSONException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            array_workout.put(stoneObject);
//        }
//        for (int j = 0; j < list_sleep.size(); j++) {
//            JSONObject stoneObject = new JSONObject();
//            try {
//                stoneObject.put("id", list_sleep.get(j).getId() + "");
//                stoneObject.put("overTime", list_sleep.get(j).getOverTime() + "");
//                stoneObject.put("useTime", list_sleep.get(j).getUseTime() + "");
//                stoneObject.put("sleepEfficiency", list_sleep.get(j).getSleepEfficiency() + "");
//                stoneObject.put("isDelayFifty", list_sleep.get(j).getIsDelayFifty() + "");
//            } catch (JSONException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            array_sleep.put(stoneObject);
//        }
//        try {
//            object.putOpt("stageWorkOutData", array_workout);
//            object.putOpt("stageSleepData", array_sleep);
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        // 添加数据库
//        for (int i = 0; i < list_workout.size(); i++) {
//            Data_AM_ActivitySummary SingleData = Make_Data_Util.makeDataSingleAMAS(userName, mac, type, list_workout.get(i));
//            DataBaseTools sdk_db = new DataBaseTools(context);
//            Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_AM_ACTIVITYSUMMARY, SingleData);
//        }
//        // timer
//        AM_InAuthor sdk_InAuthor = new AM_InAuthor();
//        sdk_InAuthor.initAuthor(context, userName);
//        sdk_InAuthor.run();
//        return object.toString();
//    }
	
	/**
	 * 解析阶段性数据但不规整，上云
	 * @param con
	 * @param userName
	 * @param mac
	 * @param type
	 * @param o 传给用户的阶段性数据
	 */
	public static void parseStageDataJson(Context con, String userName, String mac, String type, JSONObject o) {
		JSONArray stageDataArray = null;
		
		if (o != null) {
			JSONObject stageDataObject = o;
			try {
				stageDataArray = stageDataObject.getJSONArray(AmProfile.SYNC_STAGE_DATA_AM);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/* 游泳段表变量 */
			Data_TB_SwimSection data_TB_SwimSection = new Data_TB_SwimSection();
			int spendTime_All = 0; //段总耗时
			float swimCalories_All = 0; //段总卡路里
			int thrashTimes_All = 0; //段总划水次数
			int spendTime_BackStroke = 0; //段仰泳耗时
			int spendTime_BreastStroke = 0;//段蛙泳耗时
			int spendTime_FreeStroke = 0;//段自由泳耗时
			int spendTime_Unrecognized = 0;//段未识别耗时
			int tripCount = 0; //段总程数


			JSONObject jsonObject = null;
			for (int i = 0; i < stageDataArray.length(); i++) {
				try {
					jsonObject = (JSONObject) stageDataArray.get(i);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				String dataType = null;
				try {
					dataType =  (String) jsonObject.get(AmProfile.SYNC_STAGE_DATA_TYPE_AM);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//workout 容器
				Summary_WorkOut wo = new Summary_WorkOut();
				//sleep summary 容器
				Summary_Sleep ss = new Summary_Sleep();
				//swim 容器
				Data_TB_Swim swim = new Data_TB_Swim();
				switch (dataType) {
				case AmProfile.SYNC_STAGE_DATA_TYPE_WORKOUT_AM:		//workout
					wo.setId(1);
					try {
						wo.setUseTime(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_USED_TIME_AM));
						wo.setOverTime(jsonObject.getString(AmProfile.SYNC_STAGE_DATA_STOP_TIME_AM));
						wo.setSteps(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_WORKOUT_STEP_AM));
						wo.setCalorie(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_CALORIE_AM));
						wo.setDistance(Float.parseFloat(jsonObject.getString(AmProfile.SYNC_STAGE_DATA_DISTANCE_AM)));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String workOutdataID = null;
					try {
						long startTime = PublicMethod.String2TS(jsonObject.getString(AmProfile.SYNC_STAGE_DATA_STOP_TIME_AM)) - PublicMethod.String2TS(String.valueOf(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_USED_TIME_AM)));
						workOutdataID = mac + startTime + PublicMethod.String2TS(jsonObject.getString(AmProfile.SYNC_STAGE_DATA_STOP_TIME_AM));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					Data_TB_Workout SingleData = Make_Data_Util.makeDataSingleAMWorkOut(workOutdataID, wo.getUseTime(), wo.getSteps(), (int) wo.getDistance(), wo.getCalorie(), type, mac, userName); //mac+start+end
		            DataBaseTools sdk_db = new DataBaseTools(con);
		            Boolean addData = sdk_db.addData(DataBaseConstants.TABLE_TB_WORKOUT, SingleData);
		            Log.i(TAG, "workout stage data up cloud result is "+addData);
					break;

				case AmProfile.SYNC_STAGE_DATA_TYPE_SLEEP_AM:		//sleep
					ss.setId(2);
					try {
						ss.setOverTime(jsonObject.getString(AmProfile.SYNC_STAGE_DATA_STOP_TIME_AM));
						ss.setUseTime(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_USED_TIME_AM));
						ss.setSleepEfficiency((float)jsonObject.getDouble(AmProfile.SYNC_STAGE_DATA_SLEEP_EFFICIENCY_AM));
					} catch (JSONException e) {
						// TODO: handle exception
					}
					break;
					
				case AmProfile.SYNC_STAGE_DATA_TYPE_SWIM_AM:		//swim
					tripCount++; //程数+1
					try {
						swim.setSwim_Calories(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_CALORIE_AM));
						swim.setSwim_PullTimes(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_SWIM_PULL_TIMES_AM));
						swim.setSwim_Cycles(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_SWIM_TURNS_AM));
						swim.setSwim_Storke(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_SWIM_STROKE_AM));
						swim.setSwim_Distance(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_SWIMPOOL_LENGTH_AM));
						swim.setSwim_endtime(PublicMethod.String2TS(jsonObject.getString(AmProfile.SYNC_STAGE_DATA_STOP_TIME_AM)));
						swim.setSwim_SpendMinutes(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_USED_TIME_AM));

						swim.setSwim_CutInTimeDif(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_SWIM_CUTINDIF_AM));
						swim.setSwim_CutOutTimeDif(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_SWIM_CUTOUTDIF_AM));
						swim.setSwim_ProcessFlag(jsonObject.getInt(AmProfile.SYNC_STAGE_DATA_SWIM_PROCESSFLAG_AM));

						/* 游泳详细数据上云开始 */
						long startTime = swim.getSwim_endtime() - swim.getSwim_SpendMinutes();
						String swimDataID = mac + startTime + swim.getSwim_endtime();
						Data_TB_Swim singleData = Make_Data_Util.makeDataSingleAMSwim(userName, swimDataID, mac, type, swim.getSwim_Calories(), swim.getSwim_PullTimes(), swim.getSwim_Cycles(),
								swim.getSwim_Stroke(), swim.getSwim_Distance(), swim.getSwim_endtime(), swim.getSwim_SpendMinutes(), swim.getSwim_CutInTimeDif(), swim.getSwim_CutOutTimeDif(), swim.getSwim_ProcessFlag());
						DataBaseTools sdk_db_swim = new DataBaseTools(con);
						Boolean addDataResult = sdk_db_swim.addData(DataBaseConstants.TABLE_TB_SWIM, singleData);
						Log.i(TAG, "swim stage data up cloud result is "+addDataResult);
						/* 游泳详细数据上云结束 */

					} catch (JSONException e) {
						e.printStackTrace();
					}

					/* 统计游泳段表数据 */
//					int swimStroke = swim.getSwim_Stroke();
//					int swimSpendTime = swim.getSwim_SpendMinutes();
//					float swimCalories = swim.getSwim_Calories();
//					int swimPullTimes = swim.getSwim_PullTimes();
//					switch (swimStroke) {
//						case 0:	//自由泳
//							spendTime_FreeStroke += swimSpendTime;
//							Log.i(TAG, "自由泳spendTime_FreeStroke =" + spendTime_FreeStroke);
//							break;
//						case 1:	//蛙泳
//							spendTime_BreastStroke += swimSpendTime;
//							Log.i(TAG, "蛙泳spendTime_BreastStroke =" + spendTime_BreastStroke);
//							break;
//						case 2:	//仰泳
//							spendTime_BackStroke += swimSpendTime;
//							Log.i(TAG, "仰泳spendTime_BackStroke =" + spendTime_BackStroke);
//							break;
//						case 5:	//未识别
//							spendTime_Unrecognized += swimSpendTime;
//							Log.i(TAG, "未识别spendTime_Unrecognized =" + spendTime_Unrecognized);
//							break;
//						default:
//							break;
//					}

//					spendTime_All += swimSpendTime;//总用时
//					swimCalories_All += swimCalories;//总卡路里
//					thrashTimes_All += swimPullTimes;//总划水次数
//					Log.i(TAG, "spendTime_All =" + spendTime_All);
//					Log.i(TAG, "swimCalories_All =" + swimCalories_All);
//					Log.i(TAG, "thrashTimes_All =" + thrashTimes_All);
//
//					int flag = swim.getSwim_ProcessFlag();
//					if (flag == 2 || flag == 3) {
//						int cutInTime = swim.getSwim_CutInTimeDif();
//						long sectionStartTime = swim.getSwim_endtime() - spendTime_All - cutInTime;
//						int cutOutTime = swim.getSwim_CutOutTimeDif();
//						long sectionEndTime = swim.getSwim_endtime() + cutOutTime;
//
//						//初始化段表数据结构
//						data_TB_SwimSection.setSwimSection_StartTime(sectionStartTime);
//						data_TB_SwimSection.setSwimSection_Endtime(sectionEndTime);
//
//						data_TB_SwimSection.setSwimSection_PoolLength(swim.getSwim_Distance());
//						data_TB_SwimSection.setSwimSection_SpendTimeBackStroke(spendTime_BackStroke);
//						data_TB_SwimSection.setSwimSection_SpendTimeBreastStroke(spendTime_BreastStroke);
//						data_TB_SwimSection.setSwimSection_SpendTimeFreeStroke(spendTime_FreeStroke);
//						data_TB_SwimSection.setSwimSection_SpendTimeUnrecognized(spendTime_Unrecognized);
//
//						data_TB_SwimSection.setSwimSection_SumCalories(swimCalories_All);
//						data_TB_SwimSection.setSwimSection_SumThrashTimes(thrashTimes_All);
//						data_TB_SwimSection.setSwimSection_SumTripCount(tripCount);
//						data_TB_SwimSection.setSwimSection_SwimCoastTime(spendTime_All);
//
//						String swimSectionDataID = mac + sectionStartTime + sectionEndTime;
//
//						Data_TB_SwimSection swimSectionSingleData = Make_Data_Util.makeDataSingleAMSwimSection(userName, swimSectionDataID, mac, type, data_TB_SwimSection);
//						DataBaseTools sdk_db_swimSection = new DataBaseTools(con);
//						Boolean addDataResult = sdk_db_swimSection.addData(DataBaseConstants.TABLE_TB_SWIMSECTION, swimSectionSingleData);
//						Log.i(TAG, "swim section data up cloud result is "+addDataResult);
//						if (addDataResult) {
//							spendTime_All = 0; //段总耗时
//							swimCalories_All = 0; //段总卡路里
//							thrashTimes_All = 0; //段总划水次数
//							spendTime_BackStroke = 0; //段仰泳耗时
//							spendTime_BreastStroke = 0;//段蛙泳耗时
//							spendTime_FreeStroke = 0;//段自由泳耗时
//							spendTime_Unrecognized = 0;//段未识别耗时
//							tripCount = 0; //段总程数
//						}
//					}
					break;
				default:
					break;
				}
			}
			// timer
	        AM_InAuthor sdk_InAuthor = new AM_InAuthor();
	        sdk_InAuthor.initAuthor(con, userName);
	        sdk_InAuthor.run();
		}
	}

	/**
	 * 测量日期转换Long转正常日期 毫秒数转为2010-3-3 11:11:00
	 */
	public static String LongToDate(Long Date) {
		String birthStr = "1987-01-01 00:00:00";

		Date ori = new Date(Date * 1000);
		SimpleDateFormat sdfResouce = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		sdfResouce.setTimeZone(TimeZone.getDefault());

		try {
			birthStr = sdfResouce.format(ori);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return birthStr;
	}

	/**
	 * 测量日期转换成可以变成long的格式 2010-3-3 11:11:00转为毫秒数
	 */
	public static long DateToLong(String Date) {
		long DateLong = 0;

		SimpleDateFormat sdfResouce = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		sdfResouce.setTimeZone(TimeZone.getDefault());

		Date fromData = new Date();
		try {
			fromData = sdfResouce.parse(Date);
			DateLong = fromData.getTime() / 1000;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return DateLong;
	}
}
