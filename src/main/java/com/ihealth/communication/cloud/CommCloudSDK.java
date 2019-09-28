
package com.ihealth.communication.cloud;

import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ihealth.communication.cloud.tools.AppIDFactory;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.cloud.tools.HttpsPost;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Debug;
import android.telephony.TelephonyManager;
//import com.ihealth.communication.utils.Log;
import com.ihealth.communication.utils.Log;

public class CommCloudSDK {

    private static final String TAG = "CommCloudSDK";
    private static final String TAG1 = "HS5wifi";

    boolean debug = false;

    private static final String Sv_UserSign = "b3118e4fa5204f6dba1e2f1723270747";
    private static final String Sv_UserAuthorization = "3342965be24349a5860cec3201f1b890";
    private static final String Sv_token_refresh = "9e4a5f26773e4d8a87ce2b83fa2641b3";
    private static final String Sv_UserRegister = "4e4d39ad421e47dea254a86d91e673e3";
    private static final String Sv_UserExistForThird = "828811c6b7494c7b8c34b101a95f7877";
    private static final String Sv_UserCombine = "2fb61340b51445daa7670d781b0a7cc5";
    private static final String Sv_AuthorizationDownload = "03d73ec7a59e4c0ead951e6b798377fc";
    private static final String SC = "7c789858c0ec4ebf8189ebb14b6730a5";//5.0地址SC
    private static final String PATH = "/api5/";
    
    private Context context;
    public String messageReturn = "";
    public int result = 0;
    public long TS = 0;
    public float resultMessage = 0;
    public int queueNum = 0;
    String QueueNum = "111111";// 服务器写死的111111

    public CommCloudSDK(Context context) {
        this.context = context;
    }

    /**
     * 登录接口 author_wz 2014年5月22日 下午1:29:20
     * 
     * @throws Exception
     * @throws SocketTimeoutException
     * @throws ConnectTimeoutException
     */
    public ReturnDataUser UserSign(String client_id, String client_secret,
            String username, String host) throws Exception {
        ReturnDataUser returnData = new ReturnDataUser();

        Map<String, String> Params = new HashMap<String, String>();
        Params.put("sc", SC);
        Params.put("sv", Sv_UserSign);
        Params.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        Params.put("AppGuid", getAppID());
        Params.put("PhoneOS", "android" + android.os.Build.VERSION.RELEASE);
        Params.put("PhoneName", android.os.Build.MODEL);
        Params.put("PhoneID", getDeviceID());
        Params.put("PhoneLanguage", Locale.getDefault().getLanguage());
        Params.put("PhoneRegion", Locale.getDefault().getCountry());
        Params.put("QueueNum", QueueNum);
        Params.put("Token", "");

        Params.put("client_id", client_id);
        Params.put("client_secret", client_secret);
        Params.put("username", username);
        Params.put("hash", MD5(client_id + "ihealth_API-SDK" + QueueNum));

        String webAddress = host + PATH + "UserSign.htm";

        HttpsPost ht = new HttpsPost(context);

        messageReturn = ht.requireClass(webAddress, Params, "UTF-8");

        if (messageReturn.length() == 0)
            return returnData;

        try {
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();

            this.result = jsonORegist.getInt("Result");
            this.TS = Long.parseLong(jsonORegist.getString("TS"));
            this.resultMessage = Float.parseFloat(jsonORegist.getString("ResultMessage"));
            this.queueNum = jsonORegist.getInt("QueueNum");

            returnData.setResultCode(jsonORegist.getString("ResultMessage"));
            // ID 字段为预留，服务器端暂不发送
            JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
            JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();

            if (resultMessage == 100.0) {
                String apiName = jsonWebListOut.getString("APIName");
                String AccessToken = jsonWebListOut.getString("AccessToken");
                long Expires = jsonWebListOut.getLong("Expires");
                String RefreshToken = jsonWebListOut.getString("RefreshToken");
                int userId = Integer.parseInt(jsonWebListOut.getString("ID"));// 配置用户ID
                SharedPreferences.Editor editor = context.getSharedPreferences("ihealth_userid", Context.MODE_PRIVATE).edit();
                editor.putInt(username, userId);
                returnData.setId(Integer.parseInt(jsonWebListOut.getString("ID"))); 
                if (debug) {
                    Log.i(TAG1, "apiName = " + apiName);
                    Log.i(TAG1, "AccessToken = " + AccessToken);
                    Log.i(TAG1, "Expires = " + Expires);
                    Log.i(TAG1, "RefreshToken = " + RefreshToken);
                }
                returnData.setApiName(apiName);
                returnData.setAccessToken(AccessToken);
                returnData.setExpires(Expires);
                returnData.setRefreshToken(RefreshToken);
                UserCheckSDK.saveUserInfo(context, username, null, host, null, null, null, null, returnData.getId());

                return returnData;
            } else if (resultMessage == 208.0) {
                String regionHost = jsonWebListOut.getString("RegionHost");
                // Log.i(TAG, "UserSign返回208,拿到regionHost = " + regionHost);

                ReturnDataUser returnData1 = UserSign(client_id, client_secret, username, regionHost);
                if ("100".equals(returnData1.getResultCode())) {// 登录成功
                    // 接口中保存生效的Host,外面不保存
                    UserCheckSDK.saveUserInfo(context, username, null, regionHost, null, null, null, null, returnData1.getId());
                    // Log.i(TAG, "重新调用接口成功!->保存regionHost到本地");
                    return returnData1;
                } else { // 登录失败
                    return returnData1;
                }
            } else {
                return returnData;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return returnData;
        }
    }

    /**
     * 刷新令牌接口 author_wz 2014年5月22日 下午1:29:20
     */
    public ReturnDataUser token_refresh(String tokenRefresh, String username,
            String host) {
        // Log.i("BP_SDK", "调用token_refresh");

        ReturnDataUser returnData = new ReturnDataUser();
        Map<String, String> Params = new HashMap<String, String>();

        Params.put("sc", SC);
        Params.put("sv", Sv_token_refresh);
        Params.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        Params.put("AppGuid", getAppID());
        Params.put("PhoneOS", "android" + android.os.Build.VERSION.RELEASE);
        Params.put("PhoneName", android.os.Build.MODEL);
        Params.put("PhoneID", getDeviceID());
        Params.put("PhoneLanguage", Locale.getDefault().getLanguage());
        Params.put("PhoneRegion", Locale.getDefault().getCountry());
        Params.put("QueueNum", QueueNum);
        Params.put("Token", "");

        Params.put("TokenRefresh", tokenRefresh);
        Params.put("Un", username);

        String webAddress = host + PATH
                + "token_refresh.htm";
        // AppsDeviceParameters.Address + "token_refresh.ashx";

        HttpsPost ht = new HttpsPost(context);

        try {
            messageReturn = ht.requireClass(webAddress, Params, "UTF-8");
        } catch (ConnectTimeoutException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (SocketTimeoutException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (messageReturn.length() == 0)
            return returnData;

        String webString = "";
        // 分析收取的数据
        try {
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();
            this.result = jsonORegist.getInt("Result");
            this.TS = Long.parseLong(jsonORegist.getString("TS"));
            this.resultMessage = Float.parseFloat(jsonORegist
                    .getString("ResultMessage"));
            this.queueNum = jsonORegist.getInt("QueueNum");

            returnData.setResultCode(jsonORegist.getString("ResultMessage"));

            if (resultMessage == 100.0) {
                // ID 字段为预留，服务器端暂不发送
                JSONTokener jsonTokener = new JSONTokener(
                        jsonORegist.getString("ReturnValue"));
                JSONObject jsonWebListOut = (JSONObject) jsonTokener
                        .nextValue();

                String apiName = jsonWebListOut.getString("APIName");
                String accessToken = jsonWebListOut.getString("AccessToken");
                long expires = jsonWebListOut.getLong("Expires");
                String refreshToken = jsonWebListOut.getString("RefreshToken");

                returnData.setApiName(apiName);
                returnData.setAccessToken(accessToken);
                returnData.setExpires(expires);
                returnData.setRefreshToken(refreshToken);

                return returnData;
            } else {
                return returnData;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return returnData;
        }
    }

    /**
     * 注册接口 author_wz 2014年5月22日 下午1:29:20
     * 
     * @throws Exception
     * @throws SocketTimeoutException
     * @throws ConnectTimeoutException
     */
    public ReturnDataUser UserRegister(String client_id, String client_secret, String username, String host)
            throws Exception {

        ReturnDataUser returnData = new ReturnDataUser();

        // 编辑发送参数
        Map<String, String> Params = new HashMap<String, String>();
        Params.put("sc", SC);
        Params.put("sv", Sv_UserRegister);
        Params.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        Params.put("AppGuid", getAppID());
        Params.put("PhoneOS", "android" + android.os.Build.VERSION.RELEASE);
        Params.put("PhoneName", android.os.Build.MODEL);
        Params.put("PhoneID", getDeviceID());
        Params.put("PhoneLanguage", Locale.getDefault().getLanguage());
        Params.put("PhoneRegion", Locale.getDefault().getCountry());
        Params.put("QueueNum", QueueNum);
        Params.put("Token", "");

        Params.put("client_id", client_id);
        Params.put("client_secret", client_secret);
        Params.put("username", username);
        Params.put("hash", MD5(client_id + "ihealth_API-SDK" + QueueNum));// MD5（client_id+"ihealth_API-SDK"+query_number）

        String webAddress = host + PATH + "UserRegister.htm";
        HttpsPost ht = new HttpsPost(context);

        messageReturn = ht.requireClass(webAddress, Params, "UTF-8");

        if (messageReturn.length() == 0)
            return returnData;

        // 分析收取的数据
        try {
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();

            this.result = jsonORegist.getInt("Result");
            this.TS = Long.parseLong(jsonORegist.getString("TS"));
            this.resultMessage = Float.parseFloat(jsonORegist.getString("ResultMessage"));
            this.queueNum = jsonORegist.getInt("QueueNum");

            returnData.setResultCode(jsonORegist.getString("ResultMessage"));
            // ID 字段为预留，服务器端暂不发送
            JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
            JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();

            if (resultMessage == 100.0) {

                String apiName = jsonWebListOut.getString("APIName");
                String AccessToken = jsonWebListOut.getString("AccessToken");
                long Expires = jsonWebListOut.getLong("Expires");
                String RefreshToken = jsonWebListOut.getString("RefreshToken");

                returnData.setApiName(apiName);
                returnData.setAccessToken(AccessToken);
                returnData.setExpires(Expires);
                returnData.setRefreshToken(RefreshToken);

                returnData.setId(Integer.parseInt(jsonWebListOut.getString("ID")));
                UserCheckSDK.saveUserInfo(context, null, null, host, null, null, null, null, returnData.getId());

                return returnData;
            } else if (resultMessage == 208.0) {
                String regionHost = jsonWebListOut.getString("RegionHost");
                // Log.i(TAG, "UserRegister返回208,拿到regionHost = " + regionHost);

                ReturnDataUser returnData1 = UserExistForThird(client_id, client_secret, username, regionHost);
                if ("100".equals(returnData1.getResultCode())) {// 登录成功
                    // 保存最新的信息
                    UserCheckSDK.saveUserInfo(context, null, null, regionHost, null, null, null, null, returnData.getId());
                    // Log.i(TAG, "保存regionHost到本地");
                    return returnData1;
                } else { // 登录失败
                    return returnData1;
                }
            } else {
                return returnData;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return returnData;
        }
    }

    /**
     * 验证用户是否存在 author_wz 2014年5月22日 下午1:29:20
     * 
     * @throws Exception
     * @throws SocketTimeoutException
     * @throws ConnectTimeoutException
     */
    public ReturnDataUser UserExistForThird(String client_id, String client_secret,
            String username, String host) throws Exception {

        ReturnDataUser returnData = new ReturnDataUser();
        int Status = 0;
        int id = 0;

        // 编辑发送参数
        Map<String, String> Params = new HashMap<String, String>();
        Params.put("sc", SC);
        Params.put("sv", Sv_UserExistForThird);
        Params.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        Params.put("AppGuid", getAppID());
        Params.put("PhoneOS", "android" + android.os.Build.VERSION.RELEASE);
        Params.put("PhoneName", android.os.Build.MODEL);
        Params.put("PhoneID", getDeviceID());
        Params.put("PhoneLanguage", Locale.getDefault().getLanguage());
        Params.put("PhoneRegion", Locale.getDefault().getCountry());
        Params.put("QueueNum", QueueNum);
        Params.put("Token", "");

        Params.put("client_id", client_id);
        Params.put("client_secret", client_secret);
        Params.put("username", username);
        Params.put("hash", MD5(client_id + "ihealth_API-SDK" + QueueNum));// MD5（client_id+"ihealth_API-SDK"+query_number）

        String webAddress = host + PATH + "UserExistForThird.htm";

        HttpsPost ht = new HttpsPost(context);

        messageReturn = ht.requireClass(webAddress, Params, "UTF-8");

        if (messageReturn.length() == 0)
            return returnData;

        try {
            // messageReturn
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();

            this.result = jsonORegist.getInt("Result");
            this.TS = Long.parseLong(jsonORegist.getString("TS"));
            this.resultMessage = Float.parseFloat(jsonORegist
                    .getString("ResultMessage"));
            this.queueNum = jsonORegist.getInt("QueueNum");

            returnData.setResultCode(jsonORegist.getString("ResultMessage"));

            if(resultMessage == 225 || resultMessage == 224 || resultMessage == 223) {
                return returnData;
            }
            // ReturnValue
            JSONTokener jsonTokener = new JSONTokener(
                    jsonORegist.getString("ReturnValue"));
            JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();

            if (resultMessage == 100.0) {

                id = jsonWebListOut.getInt("ID");
                Status = jsonWebListOut.getInt("Status");
                returnData.setId(id);
                returnData.setStatus(Status);
                // 接口中保存生效的Host
                UserCheckSDK.saveUserInfo(context, username, null, host, null, null, null, null,id);
                return returnData;
            } else if (resultMessage == 208.0) {
                String regionHost = jsonWebListOut.getString("RegionHost");
                // Log.i(TAG, "UserSign返回208,拿到regionHost = " + regionHost);

                ReturnDataUser returnData1 = UserExistForThird(client_id,
                        client_secret, username, regionHost);
                if ("100".equals(returnData1.getResultCode())) {// 登录成功
                    // 保存最新的信息
                    UserCheckSDK.saveUserInfo(context, username, null, regionHost, null, null, null, null, returnData.getId());
                    // Log.i(TAG, "保存regionHost到本地");
                    return returnData1;
                } else { // 登录失败
                    return returnData1;
                }
            } else {
                return returnData;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return returnData;
        }
    }

    /**
     * 用户合并 author_wz 2014年5月22日 下午1:29:20
     * 
     * @param host
     * @throws Exception
     * @throws SocketTimeoutException
     * @throws ConnectTimeoutException
     */
    public ReturnDataUser UserCombine(String client_id, String client_secret,
            String username, String host) throws Exception {

        ReturnDataUser returnData = new ReturnDataUser();
        // 编辑发送参数
        Map<String, String> Params = new HashMap<String, String>();
        Params.put("sc", SC);
        Params.put("sv", Sv_UserCombine);
        Params.put("AppVersion", AppsDeviceParameters.APP_VERSION);
        Params.put("AppGuid", getAppID());
        Params.put("PhoneOS", "android" + android.os.Build.VERSION.RELEASE);
        Params.put("PhoneName", android.os.Build.MODEL);
        Params.put("PhoneID", getDeviceID());
        Params.put("PhoneLanguage", Locale.getDefault().getLanguage());
        Params.put("PhoneRegion", Locale.getDefault().getCountry());
        Params.put("QueueNum", QueueNum);
        Params.put("Token", "");

        Params.put("client_id", client_id);
        Params.put("client_secret", client_secret);
        Params.put("username", username);
        Params.put("hash", MD5(client_id + "ihealth_API-SDK" + QueueNum));// MD5（client_id+"ihealth_API-SDK"+query_number）

        String webAddress = host + PATH + "UserCombine.htm";

        HttpsPost ht = new HttpsPost(context);

        messageReturn = ht.requireClass(webAddress, Params, "UTF-8");

        if (messageReturn.length() == 0)
            return returnData;

        // 分析收取的数据
        try {
            JSONTokener jsonTParser = new JSONTokener(messageReturn);
            JSONObject jsonORegist = (JSONObject) jsonTParser.nextValue();
            this.result = jsonORegist.getInt("Result");
            this.TS = Long.parseLong(jsonORegist.getString("TS"));
            this.resultMessage = Float.parseFloat(jsonORegist.getString("ResultMessage"));
            this.queueNum = jsonORegist.getInt("QueueNum");

            returnData.setResultCode(jsonORegist.getString("ResultMessage"));
            JSONTokener jsonTokener = new JSONTokener(jsonORegist.getString("ReturnValue"));
            JSONObject jsonWebListOut = (JSONObject) jsonTokener.nextValue();

            if (resultMessage == 100.0) {
                // ID 字段为预留，服务器端暂不发送
                String apiName = jsonWebListOut.getString("APIName");
                String AccessToken = jsonWebListOut.getString("AccessToken");
                long Expires = jsonWebListOut.getLong("Expires");
                String RefreshToken = jsonWebListOut.getString("RefreshToken");

                returnData.setApiName(apiName);
                returnData.setAccessToken(AccessToken);
                returnData.setExpires(Expires);
                returnData.setRefreshToken(RefreshToken);
                returnData.setId(Integer.parseInt(jsonWebListOut.getString("ID")));
                
                // modify 外面已经保存了最新的Host
                return returnData;
            } else if (resultMessage == 208.0) {
                String regionHost = jsonWebListOut.getString("RegionHost");
                // Log.i(TAG, "UserCombine返回208,拿到regionHost = " + regionHost);

                ReturnDataUser returnData1 = UserCombine(client_id, client_secret, username, regionHost);
                if ("100".equals(returnData1.getResultCode())) {// 登录成功
                    // 接口中保存生效的Host,外面保存Token
                    UserCheckSDK.saveUserInfo(context, username, null, regionHost, null, null, null, null, returnData.getId());
                    // Log.i(TAG, "保存regionHost到本地");
                    return returnData1;
                } else { // 登录失败
                    return returnData1;
                }
            } else {
                return returnData;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return returnData;
        }
    }

    private String getDeviceID() {

        TelephonyManager telephonyManager = (TelephonyManager) this.context
                .getSystemService(Context.TELEPHONY_SERVICE);

        return (telephonyManager.getDeviceId() != null) ? telephonyManager
                .getDeviceId() : getAppID();
    }

    private String getAppID() {
        // String appID = getDeviceID();
        String appID = "appID";
        AppIDFactory appIDFactory = new AppIDFactory(context);
        appID = appIDFactory.getAppID();
        return appID;
    }

    /**
     * @param str
     * @return MD5加密算法 author_GYL 2013年11月21日 下午1:48:44
     */
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
        // Log.i("MD5",hexValue.toString());
        return hexValue.toString();
    }

}
