package com.ihealth.communication.cloud.data;

import android.content.Context;
import android.content.SharedPreferences.Editor;
//import com.ihealth.communication.utils.Log;
import android.content.SharedPreferences;

public class BP_InAuthor {

	public Context context;
	private boolean timerIsStart = false;

	private static final BP_InAuthor INSTANCE = new BP_InAuthor();

	public static BP_InAuthor getInstance() {
		return INSTANCE;
	}
	public BP_InAuthor() {

	}
	public void initAuthor(Context context,String un){
		//取当前用户配置信息-由用户登录SDK验证获得
		SharedPreferences userInfo = context.getSharedPreferences(un+"userinfo", 0);
		String email = userInfo.getString("email", "");
		String apiName = userInfo.getString("apiName", "");
		String Host = userInfo.getString("Host", "");
		String accessToken = userInfo.getString("accessToken", "");
		String refreshToken = userInfo.getString("refreshToken", "");
		String client_id = userInfo.getString("client_id", "");
		String client_secret = userInfo.getString("client_secret", "");

		SharedPreferences sp = context.getSharedPreferences("jiuan.sdk.author", 0);
		Editor edit = sp.edit();
		edit.putString("email", email);
		edit.putString("apiName", apiName);
		edit.putString("Host", Host);
		edit.putString("accessToken", accessToken);
		edit.putString("refreshToken", refreshToken);
		edit.putString("client_id", client_id);
		edit.putString("client_secret", client_secret);
		edit.commit();

		this.context = context;
	}

	//BP_SDK 数据上传流程入口
	public void run(){

		//20140609 modify wz 将第三方注册放入到Timer中
		//解决 无网络访问情况下,开始无网络,之后开启网络的情况处理
		if(!timerIsStart){
			BP_Up timer = new BP_Up(this.context);
			timer.Start_timer();
			timerIsStart =true;
		}

	}

}
