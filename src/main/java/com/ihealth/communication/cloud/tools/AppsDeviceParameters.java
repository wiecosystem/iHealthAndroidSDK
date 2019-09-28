package com.ihealth.communication.cloud.tools;

/**
 * User_SDK配置参数
 * @author brave
 *
 */
public class AppsDeviceParameters {

	//是否下载隐私条款
	//正式版置为true
	//Watch版置为false
	//往后只维护一版SDK，且新用户注册不再提示隐私条款，20150814_ZYG
	//public static boolean IsNeedDownloadPrivacy = false;
	
	public static final boolean isLog = false;//调试Log开关
    public static final boolean isOfficial = true;// 是否是正式服务器
    public static boolean isUpLoadData = false;//数据上云开关

	public static String webSite = "https://api.ihealthlabs.com:443";//接口正式服务器地址 .htm
	public static final String AddressCenter = "https://api.ihealthlabs.com:443/apicenter/"; //中心接口服务器地址-正式 .htm
	public static String path = "/api5/";//地址路径
	public static String Address = webSite + "/api5/"; //地址+路径
	
	//与实际输出版本保持一致
	public static final String APP_VERSION = "ASDK_2.3.0.29";//接口版本
	public static final String SC = "7c789858c0ec4ebf8189ebb14b6730a5";//5.0地址SC

	//指令超时时间
	public static final long Delay_Short = 2000;
	public static final long Delay_Medium = 4000;
	public static final long Delay_Long = 8000;
	public static final long Delay_LongLong = 16000;
}
