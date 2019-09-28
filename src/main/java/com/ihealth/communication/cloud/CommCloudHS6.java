
package com.ihealth.communication.cloud;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ihealth.communication.cloud.data.Data_TB_Unit;
import com.ihealth.communication.cloud.data.Date_TB_HS6MeasureResult;
import com.ihealth.communication.cloud.data.UserNetData;
import com.ihealth.communication.cloud.tools.AppIDFactory;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.cloud.tools.HttpPost;
import com.ihealth.communication.cloud.tools.HttpsPost;
import com.ihealth.communication.cloud.tools.Method;

import android.content.Context;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import com.ihealth.communication.utils.Log;

/**
 * HS6 云接口 包含:
 * 0 用户信息上云 user_upload；
 * 1 联网设备绑定 User_netdevice_Bind
 * 2 联网设备解绑 User_netdevice_Unbind
 * 3 同步单位  sync_unit；
 * 4 HS6拉取数据  openapiv2/user/sdk_get_token.json
 *
 * @author brave
 */
public class CommCloudHS6 {

    private static final String TAG = "CommCloudHS6";

    private Context context;
    public String messageReturn = ""; // 全部的返回
    public int result = 0;
    public long TS = 0; // 返回的TS
    public float resultMessage = 0; // 100 : 成功

    public ArrayList<Date_TB_HS6MeasureResult> Bind_HS6ReturnArr;// 返回
    public ArrayList<Date_TB_HS6MeasureResult> Unbind_HS6ReturnArr;// 返回

    private static final String SV_USER_NETDEVICE_BIND = "af7593f2e0744df2ab05f053d08f4dbf";
    private static final String SV_USER_NETDEVICE_UNBIND = "dd603c07bff9428280e0c7452b48a79e";
    private static final String SV_user_upload = "cec7c99b534049de90b211ac7f4e90c5";
    private static final String SV_sync_unit = "f62995e6922547e294d11f7218a91383";

    private final String SC = "7c789858c0ec4ebf8189ebb14b6730a5";// 5.0地址SC
    private String host;
    private String un;
    private String accessToken;
    private static String path = "/api5/";// 地址路径

    public userUpload_Return returnData = null; // ID 、iHealthId

    // 构造
    public CommCloudHS6(String username, Context context) {
        super();
        // TODO Auto-generated constructor stub
        this.context = context;
        un = username;
        accessToken = context.getSharedPreferences(un + "userinfo", 0).getString("accessToken", "");// 获取最优服务器
        host = context.getSharedPreferences(un + "userinfo", 0).getString("Host", "");// 获取最优服务器
        if ("".equals(host)) {
            host = AppsDeviceParameters.webSite;
        }
    }

    public String sdk_get_token(String client_id, String client_secret,
                                String username, String client_para) {

        if (!UserCheckSDK.isNetworkAvailable(context)) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("ErrorCode", 101);
                jsonObject.put("ErrorDescription", "No network");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject.toString();
        }
        try {
            // 编辑发送参数
            Map<String, String> sdk_get_tokenParams = new HashMap<String, String>();

            sdk_get_tokenParams.put("client_id", client_id);
            sdk_get_tokenParams.put("client_secret", client_secret);
            sdk_get_tokenParams.put("username", username);
            sdk_get_tokenParams.put("client_para", client_para);
            sdk_get_tokenParams.put("hash", MD5(client_para + "|" + username + "|" + client_secret + "|" + client_id));

            String webAddress = host + "/openapiv2/user/sdk_get_token.json";

            try {
                if (AppsDeviceParameters.isOfficial) {
                    HttpsPost ht = new HttpsPost(context);
                    messageReturn = ht.requireClass(webAddress, sdk_get_tokenParams, "UTF-8");
                } else {
                    HttpPost ht = new HttpPost();
                    messageReturn = ht.requireClass(webAddress, sdk_get_tokenParams, "UTF-8");
                }
            } catch (SocketTimeoutException e) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("ErrorCode", 102);
                    jsonObject.put("ErrorDescription", "Network timeout");
                } catch (JSONException e1) {
                    e.printStackTrace();
                }
                return jsonObject.toString();
            }

            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonObject = (JSONObject) jsonTParser.nextValue();
            if (!jsonObject.isNull("ErrorCode")) {
                int ErrorCode = jsonObject.getInt("ErrorCode");
                if (ErrorCode == 2005) {
                    host = jsonObject.getString("UserRegion");
                    return sdk_get_token(client_id, client_secret, username, client_para);
                }
            }
        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("Exception", "" + e);
            } catch (JSONException e2) {
                e.printStackTrace();
            }
            return jsonObject.toString();
        }
        return messageReturn;
    }

    /**
     * 仅仅更新称的单位
     *
     * @param username
     * @param unit
     * @throws SocketTimeoutException author_GYL 2013年11月21日 下午3:18:49
     */
    public int sync_unit(String username, int unit) throws Exception {

        // 编辑发送参数
        Map<String, String> sync_unitParams = new HashMap<String, String>();
        sync_unitParams.put("sc", AppsDeviceParameters.SC);
        sync_unitParams.put("sv", SV_sync_unit);
        sync_unitParams.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        sync_unitParams.put("AppGuid", getAppID());
        sync_unitParams.put("PhoneOS", android.os.Build.VERSION.RELEASE);
        sync_unitParams.put("PhoneName", android.os.Build.MODEL);
        sync_unitParams.put("PhoneID", getDeviceID());
        sync_unitParams.put("PhoneLanguage", Locale.getDefault().getLanguage());
        sync_unitParams.put("PhoneRegion", Locale.getDefault().getCountry());
        sync_unitParams.put("QueueNum", "1");
        sync_unitParams.put("Token", "");

        //参数Json
        JSONObject jsonUploadData = new JSONObject();
        try {
            jsonUploadData.put("Weight", unit + "");
            jsonUploadData.put("Weight_TS", Method.getTS() + "");

        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        //参数
        sync_unitParams.put("Un", username);
        sync_unitParams.put("VerifyToken", accessToken);
        sync_unitParams.put("UploadData", jsonUploadData.toString());

        String webAddress = host + "/api5/sync_unit.htm";


        try {
            if (AppsDeviceParameters.isOfficial) {
                HttpsPost ht = new HttpsPost(context);
                messageReturn = ht.requireClass(webAddress, sync_unitParams, "UTF-8");
            } else {
                HttpPost ht = new HttpPost();
                messageReturn = ht.requireClass(webAddress, sync_unitParams, "UTF-8");
            }
        } catch (UnknownHostException e) {
            return 101;
        } catch (SocketTimeoutException e) {
            return 102;
        }

        if (messageReturn.length() == 0) {
            return 999;
        } else if (messageReturn.length() == 3) {
            return Integer.valueOf(messageReturn);// 403 404 500
        }

        // 分析收取的数据
        try {
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonOSyncUnit = (JSONObject) jsonTParser.nextValue();
            this.result = jsonOSyncUnit.getInt("Result");
            this.TS = Long.parseLong(jsonOSyncUnit.getString("TS"));
            this.resultMessage = Float.parseFloat(jsonOSyncUnit.getString("ResultMessage"));

            if (resultMessage == 100.0) {
                // 获取ReturnValue的值
                JSONTokener jsonTokener = new JSONTokener(jsonOSyncUnit.getString("ReturnValue"));
                JSONObject msgValue = (JSONObject) jsonTokener.nextValue();

                return 100;
            } else {
                return (int) resultMessage;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return 999;
        }
    }

    /**
     * @param userName
     * @param ob
     * @return boolean 判断是否同步成功
     * @throws SocketTimeoutException author_GYL 2013年11月21日 下午3:18:49
     */
    public int user_upload(String userName, UserNetData ob)
            throws Exception {

        returnData = new userUpload_Return();

        // 编辑发送参数
        Map<String, String> user_uploadParams = new HashMap<String, String>();
        user_uploadParams.put("sc", AppsDeviceParameters.SC);
        user_uploadParams.put("sv", SV_user_upload);
        user_uploadParams.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        user_uploadParams.put("AppGuid", getAppID());
        user_uploadParams.put("PhoneOS", android.os.Build.VERSION.RELEASE);
        user_uploadParams.put("PhoneName", android.os.Build.MODEL);
        user_uploadParams.put("PhoneID", getDeviceID());
        user_uploadParams.put("PhoneLanguage", Locale.getDefault().getLanguage());
        user_uploadParams.put("PhoneRegion", Locale.getDefault().getCountry());
        user_uploadParams.put("QueueNum", "1");
        user_uploadParams.put("Token", "");

        // 编辑参数UploadData
        JSONObject jsonUploadData = new JSONObject();
        try {
            // 生成email数组
            JSONArray jsonAEmail = new JSONArray();
            for (int i = 0; i < ob.getEmail().length; i++) {
                jsonAEmail.put(i, ob.getEmail()[i]);
            }

            // 生成Logo
            JSONObject jsonLogoData = new JSONObject();
            try {
                jsonLogoData.put("Data", ob.logo.data);
                jsonLogoData.put("TS", ob.logo.TS);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }

            // 生成userinfo JSON
            jsonUploadData.put("ID", ob.getID());
            jsonUploadData.put("Birthday", ob.getBirthday());
            if (jsonAEmail != null) {
                jsonUploadData.put("Email", jsonAEmail);
            }
            jsonUploadData.put("Gender", ob.getGender());
            jsonUploadData.put("IsSporter", ob.getIsSporter());
            if (ob.getName() != null) {
                jsonUploadData.put("Name", ob.getName());
            }
            jsonUploadData.put("Height", ob.getHeight());
            jsonUploadData.put("Weight", ob.getWeight());
            if (ob.getNation() != null) {
                jsonUploadData.put("Nation", ob.getNation());
            }
            if (ob.getLanguage() != null) {
                jsonUploadData.put("Language", ob.getLanguage());
            }
            jsonUploadData.put("usecloud", ob.getUsecloud());
            jsonUploadData.put("TS", System.currentTimeMillis() / 1000);
            if (jsonLogoData != null) {
                jsonUploadData.put("Logo", jsonLogoData);
            }
            jsonUploadData.put("ActivityLevel", ob.getActivityLevel());
            jsonUploadData.put("TimeZone", Method.getTimeZone());
            if (ob.getUserNation() != null) {
                jsonUploadData.put("UserNation", ob.getUserNation());
            }
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        user_uploadParams.put("Un", userName);
        user_uploadParams.put("VerifyToken", accessToken);
        user_uploadParams.put("UploadData", jsonUploadData.toString());
        Log.v(TAG, user_uploadParams.toString() + "--ob.getUserNation()" + ob.getUserNation());

        String webAddress = host + "/userauthapi/user_upload.htm";
        try {
            if (AppsDeviceParameters.isOfficial) {
                HttpsPost ht = new HttpsPost(context);
                messageReturn = ht.requireClass(webAddress, user_uploadParams, "UTF-8");
            } else {
                HttpPost ht = new HttpPost();
                messageReturn = ht.requireClass(webAddress, user_uploadParams, "UTF-8");
            }
        } catch (UnknownHostException e) {
            return 101;
        } catch (SocketTimeoutException e) {
            return 102;
        }

        if (messageReturn.length() == 0) {
            return 999;
        } else if (messageReturn.length() == 3) {
            return Integer.valueOf(messageReturn);// 403 404 500
        }

        // 分析收取的数据
        try {
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonOSyncUnit = (JSONObject) jsonTParser.nextValue();
            this.result = jsonOSyncUnit.getInt("Result");
            this.TS = Long.parseLong(jsonOSyncUnit.getString("TS"));
            int resultMessage = Integer.parseInt(jsonOSyncUnit.getString("ResultMessage"));

            if (resultMessage == 100) {
                // 获取ReturnValue的值
                JSONTokener jsonTokener = new
                        JSONTokener(jsonOSyncUnit.getString("ReturnValue"));
                JSONObject msgValue = (JSONObject) jsonTokener.nextValue();

                returnData.ID = msgValue.getInt("ID");
                returnData.iHealthID = msgValue.getString("iHealthID");

                Log.v(TAG, "returnData.ID = " + returnData.ID);
                Log.v(TAG, "returnData.iHealthID = " + returnData.iHealthID);
            } else if (resultMessage == 212 || resultMessage == 221) {

                // 取得refreshToken
                String refreshToken = context.getSharedPreferences(un + "userinfo", 0).getString("refreshToken", "");// 获取最优服务器

                // 执行刷新
                CommCloudSDK commCloudSDK = new CommCloudSDK(context);
                ReturnDataUser token_refresh = commCloudSDK.token_refresh(refreshToken, un, host);

                if ("100".equals(token_refresh.getResultCode())) {// 成功

                    // 保存后重新调用上传
                    accessToken = token_refresh.getAccessToken();
                    return user_upload(userName, ob);

                } else {
                    return 999;
                }
            }
            return resultMessage;
        } catch (JSONException e) {
            e.printStackTrace();
            return 999;
        }
    }

    private class userUpload_Return {
        public int ID;
        String iHealthID;

        userUpload_Return() {
            ID = 0;
            iHealthID = new String();
        }
    }

    /**
     * 联网设备绑定
     *
     * @param hs6Arr
     * @return boolean
     * @throws SocketTimeoutException
     * @author liuyu
     * @modify zhaoyonguang 20141127
     */
    public boolean User_netdevice_Bind(ArrayList<Date_TB_HS6MeasureResult> hs6Arr)
            throws Exception {

        Bind_HS6ReturnArr = new ArrayList<Date_TB_HS6MeasureResult>();
        this.TS = 0;

        // 编辑发送参数
        Map<String, String> user_netdevice_bind = new HashMap<String, String>();
        user_netdevice_bind.put("sc", SC);
        user_netdevice_bind.put("sv", SV_USER_NETDEVICE_BIND);
        user_netdevice_bind.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        user_netdevice_bind.put("AppGuid", getAppID());
        user_netdevice_bind.put("PhoneOS", android.os.Build.VERSION.RELEASE);
        user_netdevice_bind.put("PhoneName", android.os.Build.MODEL);
        user_netdevice_bind.put("PhoneID", getDeviceID());
        user_netdevice_bind.put("PhoneLanguage", Locale.getDefault().getLanguage());
        user_netdevice_bind.put("PhoneRegion", Locale.getDefault().getCountry());
        user_netdevice_bind.put("QueueNum", "1");
        user_netdevice_bind.put("Token", "");

        // 编辑绑定设备信息参数 UploadData
        JSONArray jsonDeviceArr = new JSONArray();
        try {
            JSONObject jsonHS6Data;
            // 生成 JSON 对象
            for (int i = 0; i < hs6Arr.size(); i++) {
                jsonHS6Data = new JSONObject();
                jsonHS6Data.put("MAC", hs6Arr.get(i).getMAC());
                jsonHS6Data.put("Model", hs6Arr.get(i).getModel());
                // 替换指定位置
                jsonHS6Data.put("Position", hs6Arr.get(i).getPosition());
                jsonHS6Data.put("TS", hs6Arr.get(i).getTS());
                jsonDeviceArr.put(i, jsonHS6Data);
            }
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }
        user_netdevice_bind.put("Un", un);
        user_netdevice_bind.put("VerifyToken", accessToken);
        user_netdevice_bind.put("UploadData", jsonDeviceArr.toString());

        String webAddress = host + path + "user_netdevice_bind.htm";
        try {
            if (AppsDeviceParameters.isOfficial) {
                HttpsPost ht = new HttpsPost(context);
                messageReturn = ht.requireClass(webAddress, user_netdevice_bind, "UTF-8");
            } else {
                HttpPost ht = new HttpPost();
                messageReturn = ht.requireClass(webAddress, user_netdevice_bind, "UTF-8");
            }
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (messageReturn.length() == 0)
            return false;

        // 分析收取的数据
        try {
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();
            Log.v(TAG, "bind result = " + jsonORegist.toString());

            this.result = jsonORegist.getInt("Result");
            this.TS = Long.parseLong(jsonORegist.getString("TS"));
            this.resultMessage = Float.parseFloat(jsonORegist.getString("ResultMessage"));

            if (resultMessage == 100.0) {
                JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
                JSONArray msgValueArr = (JSONArray) jsonTokener.nextValue();
                int lenHS6Out = msgValueArr.length();
                if (lenHS6Out != 0) {

                    Date_TB_HS6MeasureResult hs6Obj;
                    for (int i = 0; i < lenHS6Out; i++) {
                        hs6Obj = new Date_TB_HS6MeasureResult();
                        JSONObject jsonHS6ItemOb = msgValueArr.optJSONObject(i);
                        if (jsonHS6ItemOb == null) {
                            Log.v(TAG, "jsonHS6ItemOb == null");

                        } else {
                            hs6Obj.setMAC(jsonHS6ItemOb.getString("MAC"));
                            hs6Obj.setStatus(jsonHS6ItemOb.getInt("Status"));
                            hs6Obj.setAction(jsonHS6ItemOb.getInt("Action"));
                            hs6Obj.setPosition(jsonHS6ItemOb.getInt("Position"));
                            /* 新添字段 zyg 2014年12月24日 星期三 09时18分17秒 CST */
                            hs6Obj.setSetWifi(jsonHS6ItemOb.getInt("SetWifi"));
                            // 20150828
                            try {
                                hs6Obj.setBindNum(jsonHS6ItemOb.getInt("BindNum"));
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                hs6Obj.setBindNum(0);
                            }
                            Bind_HS6ReturnArr.add(hs6Obj);
                        }
                    }
                }

                return true;
            } else if (resultMessage == 212 || resultMessage == 221) {
                // 取得refreshToken
                String refreshToken = context.getSharedPreferences(un + "userinfo", 0).getString("refreshToken", "");// 获取最优服务器

                // 执行刷新
                CommCloudSDK commCloudSDK = new CommCloudSDK(context);
                ReturnDataUser token_refresh = commCloudSDK.token_refresh(refreshToken, un, host);

                if ("100".equals(token_refresh.getResultCode())) {// 成功

                    // 保存后重新调用上传
                    accessToken = token_refresh.getAccessToken();
                    return User_netdevice_Bind(hs6Arr);

                } else {
                    return false;
                }

            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 联网设备解绑
     *
     * @param hs6Arr
     * @return boolean
     * @throws SocketTimeoutException
     * @author liuyu
     * @modify zhaoyonguang 20141127
     */
    public boolean User_netdevice_Unbind(ArrayList<Date_TB_HS6MeasureResult> hs6Arr)
            throws Exception {

        Unbind_HS6ReturnArr = new ArrayList<Date_TB_HS6MeasureResult>();
        this.TS = 0;

        // 编辑发送参数
        Map<String, String> user_netdevice_bind = new HashMap<String, String>();
        user_netdevice_bind.put("sc", AppsDeviceParameters.SC);
        user_netdevice_bind.put("sv", SV_USER_NETDEVICE_UNBIND);
        user_netdevice_bind.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        user_netdevice_bind.put("AppGuid", getAppID());
        user_netdevice_bind.put("PhoneOS", android.os.Build.VERSION.RELEASE);
        user_netdevice_bind.put("PhoneName", android.os.Build.MODEL);
        user_netdevice_bind.put("PhoneID", getDeviceID());
        user_netdevice_bind.put("PhoneLanguage", Locale.getDefault().getLanguage());
        user_netdevice_bind.put("PhoneRegion", Locale.getDefault().getCountry());
        user_netdevice_bind.put("QueueNum", "1");
        user_netdevice_bind.put("Token", "");

        // 编辑解除绑定设备信息参数 UploadData
        JSONArray jsonDeviceArr = new JSONArray();
        try {
            JSONObject jsonHS6Data;
            // 生成 JSON 对象
            Log.v(TAG, "Disbind HS6 size = " + hs6Arr.size());

            for (int i = 0; i < hs6Arr.size(); i++) {
                jsonHS6Data = new JSONObject();
                jsonHS6Data.put("MAC", hs6Arr.get(i).getMAC());
                jsonHS6Data.put("Model", hs6Arr.get(i).getModel());
                jsonHS6Data.put("TS", hs6Arr.get(i).getTS());
                jsonDeviceArr.put(i, jsonHS6Data);

            }

        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }

        user_netdevice_bind.put("Un", un);
        user_netdevice_bind.put("VerifyToken", accessToken);
        user_netdevice_bind.put("UploadData", jsonDeviceArr.toString());
        Log.v("", "disbind mac upload = " + user_netdevice_bind.toString());

        String webAddress = host + path + "user_netdevice_unbind.htm";
        try {
            if (AppsDeviceParameters.isOfficial) {
                HttpsPost ht = new HttpsPost(context);
                messageReturn = ht.requireClass(webAddress, user_netdevice_bind, "UTF-8");
            } else {
                HttpPost ht = new HttpPost();
                messageReturn = ht.requireClass(webAddress, user_netdevice_bind, "UTF-8");
            }
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (messageReturn.length() == 0)
            return false;

        // 分析收取的数据
        try {
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();
            Log.v(TAG, "disbind mac result = " + jsonORegist.toString());

            this.result = jsonORegist.getInt("Result");
            this.TS = Long.parseLong(jsonORegist.getString("TS"));
            this.resultMessage = Float.parseFloat(jsonORegist.getString("ResultMessage"));

            if (resultMessage == 100.0) {
                JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
                JSONArray msgValueArr = (JSONArray) jsonTokener.nextValue();
                int lenHS6Out = msgValueArr.length();
                if (lenHS6Out != 0) {

                    Date_TB_HS6MeasureResult hs6Obj;
                    for (int i = 0; i < lenHS6Out; i++) {
                        hs6Obj = new Date_TB_HS6MeasureResult();
                        JSONObject jsonHS6ItemOb = msgValueArr.optJSONObject(i);
                        if (jsonHS6ItemOb == null) {
                            Log.v(TAG, "jsonHS6ItemOb == null");

                        } else {
                            hs6Obj.setMAC(jsonHS6ItemOb.getString("MAC"));
                            hs6Obj.setStatus(jsonHS6ItemOb.getInt("Status"));
                            hs6Obj.setAction(jsonHS6ItemOb.getInt("Action"));
                            Unbind_HS6ReturnArr.add(hs6Obj);
                        }
                    }
                }

                return true;
            } else if (resultMessage == 212 || resultMessage == 221) {
                // 取得refreshToken
                String refreshToken = context.getSharedPreferences(un + "userinfo", 0).getString("refreshToken", "");// 获取最优服务器

                // 执行刷新
                CommCloudSDK commCloudSDK = new CommCloudSDK(context);
                ReturnDataUser token_refresh = commCloudSDK.token_refresh(refreshToken, un, host);

                if ("100".equals(token_refresh.getResultCode())) {// 成功
                    accessToken = token_refresh.getAccessToken();

                    return User_netdevice_Unbind(hs6Arr);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getAppID() {
        String appID = "appID";
        AppIDFactory appIDFactory = new AppIDFactory(context);
        appID = appIDFactory.getAppID();
        return appID;
    }

    private String getDeviceID() {
        TelephonyManager telephonyManager = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
        return (telephonyManager.getDeviceId() != null) ? telephonyManager.getDeviceId() : getAppID();
    }

    /**
     * @return 返回当前时区
     */
    private static float getTimeZone() {
        float timeZone = 0;

        float timeZoneDstSaving = (float) TimeZone.getDefault().getDSTSavings() / (float) 3600000;

        boolean isDst = TimeZone.getDefault().inDaylightTime(new Date());

        float timeZoneUTC = (float) TimeZone.getDefault().getRawOffset()
                / (float) 3600000;

        timeZone = timeZoneUTC + ((isDst) ? timeZoneDstSaving : 0);

        return timeZone;
    }

    private static String MD5(String str) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        char[] charArray = str.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }
        byte[] md5Bytes = md5.digest(byteArray);

        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = md5Bytes[i] & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}
