
package com.ihealth.communication.control;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.smartlinklib.ModuleInfo;
import com.example.smartlinklib.SmartLinkManipulator;
import com.example.smartlinklib.SmartLinkManipulator.ConnectCallBack;
import com.ihealth.communication.cloud.CommCloudHS6;
import com.ihealth.communication.cloud.data.Date_TB_HS6MeasureResult;
import com.ihealth.communication.cloud.data.UserNetData;
import com.ihealth.communication.cloud.tools.AppsDeviceParameters;
import com.ihealth.communication.cloud.tools.Method;
import com.ihealth.communication.manager.iHealthDeviceHs6Callback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;

/**
 * Public API for HS6.
 * <p/>
 * Before using these methods,you need to call
 * {@link iHealthDevicesManager#sdkUserInAuthor(Context, String, String, String, int)} for
 * permission.
 */
public class HS6Control {

    /**
     * Callback indicating bind user to the scale
     */
    public static final String ACTION_HS6_BIND = "hs6_bind";

    /**
     * The result of bind HS6 device. 1 bind success; 2 the scale has no empty position ;3 bind fail
     */
    public static final String BIND_HS6_RESULT = "bindResult";

    /**
     * The model of HS6
     */
    public static final String HS6_MODEL = "model";


    /**
     * The user position in the HS6 range from 1 to 10,  -1 if bind false
     */
    public static final String HS6_POSITION = "position";

    /**
     * Whether has setted wifi.1 setted ;0 not
     */
    public static final String HS6_SETTED_WIFI = "settedWifi";

    /**
     * The return data of bind user
     */
    public static final String HS6_BIND_EXTRA = "bindData";

    /**
     * Callback indicating the error
     */
    public static final String ACTION_HS6_ERROR = "hs6_error";
    /**
     * The error code: 1 user information upload error; 2 bind error
     */
    public static final String HS6_ERROR = "hs6_error";

    /**
     * Callback indicating unbind of HS6
     */
    public static final String ACTION_HS6_UNBIND = "hs6_unbind";
    /**
     * the result of unBind,it's boolean,true set success,otherwise false
     */
    public static final String HS6_UNBIND_RESULT = "unBind";

    /**
     * The result of specified bind,it's boolean,true set success,otherwise false
     */
    public static final String HS6_SPECIFIED_BIND_RESULT = "specifiedBind_result";

    /**
     * Callback indicating the setting wifi of HS6
     */
    public static final String ACTION_HS6_SETWIFI = "hs6_setwifi";

    /**
     * The result of set wifi,it's a boolean,true set wifi success,otherwise false
     */
    public static final String SETWIFI_RESULT = "setWifiResult";


    /**
     * Callback indicating the getting token of HS6
     */
    public static final String ACTION_HS6_GET_TOKEN = "hs6_get_token";

    /**
     * The result of get token,it's a JSONObject
     */
    public static final String GET_TOKEN_RESULT = "getTokenResult";

    /**
     * Callback indicating set unit to the device
     */
    public static final String ACTION_HS6_SET_UNIT = "hs6_set_unit";

    /**
     * The result of set unit,it's boolean,true set success,otherwise false
     */
    public static final String SET_UNIT_RESULT = "setUnitResult";

    /**
     * The unit of weight, Kg
     */
    public static final int Unit_Kg = 0;
    /**
     * The unit of weight, Kg
     */
    public static final int Unit_Lbs = 1;
    /**
     * The unit of weight, Kg
     */
    public static final int Unit_St = 2;

    private String userName;
    private String type;
    private Context mContext;
    private String TAG = "HS6Control";
    private iHealthDeviceHs6Callback mHs6Callback;

    /**
     * A construct of HS6Control
     *
     * @param userName    the unique ID for user
     * @param context     Context
     * @param type        the type of iHealth devices
     * @param hs6Callback all the information or data of these methods are returnned by
     *                    iHealthDeviceHs6Callback
     * @hide
     */
    public HS6Control(String userName, Context context, String type, iHealthDeviceHs6Callback hs6Callback) {
        Log.p(TAG, Log.Level.INFO, "HS6Control_Constructor", userName, context, type);
        this.mContext = context;
        mHs6Callback = hs6Callback;
        this.userName = userName;
        this.type = type;
    }

    private boolean getDevicePermisson(String userName, String type) {
        SharedPreferences sharedPreferences = mContext
                .getSharedPreferences(userName + "userinfo", Context.MODE_PRIVATE);
        String userPermission = sharedPreferences.getString("apiName", null);
        if (userPermission == null)
            return false;
        String am1 = "OpenApiActivity";
        String am2 = "OpenApiSleep";
        String po = "OpenApiSpO2";
        String hs = "OpenApiWeight";
        String bp = "OpenApiBP";
        String bg = "OpenApiBG";
        String[] test = null;
        if (iHealthDevicesManager.TYPE_AM3.equals(type)
                || iHealthDevicesManager.TYPE_AM3S.equals(type)
                || iHealthDevicesManager.TYPE_AM4.equals(type)) {
            test = new String[]{
                    am1, am2
            };

        } else if (iHealthDevicesManager.TYPE_BG1.equals(type)
                || iHealthDevicesManager.TYPE_BG5.equals(type)) {
            test = new String[]{
                    bg
            };

        } else if (iHealthDevicesManager.TYPE_BP3L.equals(type)
                || iHealthDevicesManager.TYPE_BP3M.equals(type)
                || iHealthDevicesManager.TYPE_BP5.equals(type)
                || iHealthDevicesManager.TYPE_BP7.equals(type)
                || iHealthDevicesManager.TYPE_BP7S.equals(type)) {
            test = new String[]{
                    bp
            };

        } else if (iHealthDevicesManager.TYPE_HS3.equals(type)
                || iHealthDevicesManager.TYPE_HS4.equals(type)
                || iHealthDevicesManager.TYPE_HS4S.equals(type)
                || iHealthDevicesManager.TYPE_HS5.equals(type)
                || iHealthDevicesManager.TYPE_HS6.equals(type)
                || iHealthDevicesManager.TYPE_HS5_BT.equals(type)) {
            test = new String[]{
                    hs
            };

        } else if (iHealthDevicesManager.TYPE_PO3.equals(type)) {
            test = new String[]{
                    po
            };
        } else {
            return false;
        }

        for (String string : test) {
            if (userPermission.contains(string)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set wifi to the scale.
     *
     * @param ssid     the name of net
     * @param password the password of the net
     * @return boolean whether has permission of using the method,false you need to get the
     * permission firstly.The result of set wifi is returnned by
     * {@link iHealthDeviceHs6Callback#setWifiNotify(String, String, String)} and its'
     * action is {@link this#ACTION_HS6_SETWIFI}
     */
    public boolean setWifi(String ssid, String password) {
        Log.p(TAG, Log.Level.INFO, "setWifi", ssid, password);
        if (getDevicePermisson(userName, type) || !AppsDeviceParameters.isUpLoadData) {
            SmartLinkManipulator sm = SmartLinkManipulator.getInstence();
            try {
                sm.setConnection(ssid, password, mContext);
            } catch (Exception e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            sm.Startconnection(callBack);
            return true;
        } else {
            return false;
        }

    }

    /**
     * The callback of the set wifi
     */
    private ConnectCallBack callBack = new ConnectCallBack() {

        @Override
        public void onConnectTimeOut() {
            Log.p(TAG, Log.Level.INFO, "onConnectTimeOut");
            JSONObject mJsonObject = new JSONObject();
            try {
                mJsonObject.put(SETWIFI_RESULT, false);
            } catch (JSONException e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            if (mHs6Callback != null) {
                mHs6Callback.setWifiNotify(type, ACTION_HS6_SETWIFI, mJsonObject.toString());
            } else {
            }

        }

        @Override
        public void onConnectOk() {
            Log.p(TAG, Log.Level.INFO, "onConnectOk");
            JSONObject mJsonObject = new JSONObject();
            try {
                mJsonObject.put(SETWIFI_RESULT, true);
            } catch (JSONException e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            if (mHs6Callback != null) {
                mHs6Callback.setWifiNotify(type, ACTION_HS6_SETWIFI, mJsonObject.toString());
            } else {
            }
        }

        @Override
        public void onConnect(ModuleInfo arg0) {
            Log.p(TAG, Log.Level.INFO, "onConnect", arg0);
        }
    };

    private Date_TB_HS6MeasureResult parseCode(String[] split) {
        String model_Str = "";
        String mac_Str = "";
        String LogicVer_Str = "";
        for (int i = 0; i < split.length; i++) {
            String str = split[i];
            String str_Lower = "";
            str_Lower = str.toLowerCase();
            if (str_Lower.contains("model")) {
                String[] model_temp = str.split(":");
                model_Str = model_temp[1];
            } else if (str_Lower.contains("mac")) {
                String[] mac_temp = str.split(":");
                mac_Str = mac_temp[1];
            } else if (str_Lower.contains("logicver")) {
                String[] logicVer_temp = str.split(":");
                LogicVer_Str = logicVer_temp[1];
            }
        }
        Date_TB_HS6MeasureResult mHS6MeasureResult = new Date_TB_HS6MeasureResult();
        mHS6MeasureResult.setModel(model_Str);
        mHS6MeasureResult.setMAC(mac_Str);
        long nowts = System.currentTimeMillis() / 1000;
        mHS6MeasureResult.setTS(nowts);
        mHS6MeasureResult.setLatestVersion(LogicVer_Str);
        return mHS6MeasureResult;
    }

    /**
     * Bind the user and scale together,after bind success user's weight datas can be transmitted to
     * the cloud .And this method is a time consuming operation that
     * cannot be called in the main thread.
     *
     * @param birthday     format like yyyy-MM-dd HH:mm:ss
     * @param height       the unit is cm
     * @param weight       the unit is kg
     * @param isSporter    is sporter; 2 is not ;3 unknown
     * @param gender       0 is male ;1 is female
     * @param serialNumber the mac address of the scale
     * @return boolean whether has permission of using the method,false you need to get the
     * permission firstly.The result of binding user is returnned by
     * {@link iHealthDeviceHs6Callback#onNotify(String, String, String, String)} and its'
     * action is {@link #ACTION_HS6_BIND}
     */
    public boolean bindDeviceHS6(String birthday, float weight, int height, int isSporter, int gender, String serialNumber) {
        Log.p(TAG, Log.Level.INFO, "bindDeviceHS6", birthday, weight, height, isSporter, gender, serialNumber);
        if (getDevicePermisson(userName, type)) {
//            String[] split = decode.split(" ");
//            Date_TB_HS6MeasureResult mHs6MeasureResult = parseCode(split);
            Date_TB_HS6MeasureResult mHs6MeasureResult = new Date_TB_HS6MeasureResult();
            mHs6MeasureResult.setMAC(serialNumber);
            mHs6MeasureResult.setModel("HS6");
            long nowts = System.currentTimeMillis() / 1000;
            mHs6MeasureResult.setTS(nowts);
            mHs6MeasureResult.setPosition(0);

            final ArrayList<Date_TB_HS6MeasureResult> hs6Arr = new ArrayList<Date_TB_HS6MeasureResult>();
            hs6Arr.add(mHs6MeasureResult);
            CommCloudHS6 comm = new CommCloudHS6(userName, mContext);
            JSONObject mJsonObject = new JSONObject();
            JSONArray mJsonArray = new JSONArray();
            try {
                UserNetData userNetData = this.setUserNetData(birthday, weight, height, isSporter, gender);
                int user_upload = comm.user_upload(userName, userNetData);
                if (user_upload == 100) { // 用户信息上传成功
                    boolean user_netdevice_Bind = comm.User_netdevice_Bind(hs6Arr);
                    if (user_netdevice_Bind) {
                        ArrayList<Date_TB_HS6MeasureResult> HS6Arr = comm.Bind_HS6ReturnArr;
                        if (HS6Arr != null && HS6Arr.size() > 0) {
                            for (int i = 0; i < HS6Arr.size(); i++) {
                                JSONObject mJsonObject2 = new JSONObject();
                                int result = HS6Arr.get(i).getAction();
                                if (result == 1 || result == 2) {
                                    mJsonObject2.put(BIND_HS6_RESULT, result);
                                } else {
                                    mJsonObject2.put(BIND_HS6_RESULT, 3);
                                }
                                mJsonObject2.put(HS6_MODEL, mHs6MeasureResult.getModel());
                                mJsonObject2.put(HS6_POSITION, HS6Arr.get(i).getPosition());
                                mJsonObject2.put(HS6_SETTED_WIFI, HS6Arr.get(i).getSetWifi());
                                mJsonArray.put(mJsonObject2);
                            }
                            mJsonObject.put(HS6_BIND_EXTRA, mJsonArray);
                            mHs6Callback
                                    .onNotify(mHs6MeasureResult.getMAC(), type, ACTION_HS6_BIND, mJsonObject.toString());
                        } else {
                            mJsonObject.put(HS6_ERROR, 2);
                            mHs6Callback.onNotify(mHs6MeasureResult.getMAC(), type, ACTION_HS6_ERROR,
                                    mJsonObject.toString());
                        }
                    } else {
                        mJsonObject.put(HS6_ERROR, 2);
                        mHs6Callback.onNotify(mHs6MeasureResult.getMAC(), type, ACTION_HS6_ERROR,
                                mJsonObject.toString());
                    }
                } else {
                    mJsonObject.put(HS6_ERROR, 1);
                    mHs6Callback.onNotify(mHs6MeasureResult.getMAC(), type, ACTION_HS6_ERROR,
                            mJsonObject.toString());
                }

            } catch (Exception e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * @param birthday  format "yyyy-MM-dd HH:mm:ss"
     * @param height    unit is cm
     * @param isSporter 1 is sporter,2 not, 3 unknow
     * @param gender    0 male,1 female
     * @return UserNetData
     */
    private UserNetData setUserNetData(String birthday, float weight, int height, int isSporter, int gender) {
        UserNetData usernetdata = new UserNetData();
        usernetdata.setID(0);
        usernetdata.setBirthday(Method.BirthdayToLong(birthday));
        String[] mail = new String[]{
                "", "", "", "", "", "", "", "", "", ""
        };
        usernetdata.setEmail(mail);
        usernetdata.setGender(gender);
        usernetdata.setIsSporter(isSporter);
        usernetdata.setHeight(height);
        usernetdata.setWeight(weight);
        return usernetdata;
    }

    /**
     * unBind the user and scale,and this method is a time consuming operation that cannot be called
     * in the main thread.
     *
     * @param serialNumber the mac address of scale
     * @return boolean whether has permission of using the method,false you need to get the
     * permission firstly.The result of unbinding user is returnned by
     * {@link iHealthDeviceHs6Callback#onNotify(String, String, String, String)} and its'
     * action is {@link #ACTION_HS6_UNBIND}
     */
    public boolean unBindDeviceHS6(String serialNumber) {
        Log.p(TAG, Log.Level.INFO, "unBindDeviceHS6", serialNumber);
        if (getDevicePermisson(userName, type)) {
            Date_TB_HS6MeasureResult mHs6MeasureResult = new Date_TB_HS6MeasureResult();
            mHs6MeasureResult.setMAC(serialNumber);
            mHs6MeasureResult.setModel("HS6");
            long nowts = System.currentTimeMillis() / 1000;
            mHs6MeasureResult.setTS(nowts);

            final ArrayList<Date_TB_HS6MeasureResult> hs6Arr = new ArrayList<Date_TB_HS6MeasureResult>();
            hs6Arr.add(mHs6MeasureResult);
            CommCloudHS6 comm = new CommCloudHS6(userName, mContext);
            JSONObject mJsonObject = new JSONObject();
            try {
                boolean user_netdevice_unBind = comm.User_netdevice_Unbind(hs6Arr);
                mJsonObject.put(HS6_UNBIND_RESULT, user_netdevice_unBind);
                mHs6Callback.onNotify(mHs6MeasureResult.getMAC(), type, ACTION_HS6_UNBIND, mJsonObject.toString());
            } catch (Exception e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get AccessToken of HS6 user,and this method is a time consuming operation that cannot be called
     * in the main thread.
     * After get AccessToken, you can call openApi(http://developer.ihealthlabs.com) to pull data form iHealth cloud.
     *
     * @param client_id     the identification of the SDK
     * @param client_secret the identification of the SDK
     * @param username      the identification of the user
     * @param client_para   a random string,return back without change
     * @return boolean whether has permission of using the method,false you need to get the
     * permission firstly.
     * <p> The result of get token is return by
     * {@link iHealthDeviceHs6Callback#onNotify(String, String, String, String)} and its'
     * action is {@link #ACTION_HS6_GET_TOKEN}.
     * <p> eg.
     * <p> {"APIName":"OpenApiActivity OpenApiBG OpenApiBP OpenApiFood OpenApiSleep OpenApiSpO2 OpenApiSport
     * OpenApiUserInfo OpenApiWeight",
     * <p> "AccessToken":"9fuIPl3Bo6lqJfbYjXFjuPnNwNqVfxjiUE7cMCZSjrX22RJSoKf28jtIhI0v86wjV5GJ21bc6LvMNbfYG0QsZ7cYuUSO0EkaiFTST*GcjZvvTKxfEOmhQTfLXTXYAOAwCoXlEs0DRqJaHZU5JS30ssyLNlqADPV9dlvWZitQmIfXjF6CSZM2SuRCD*bbbrqtwBsn*sC24OEoQCRpDau6wQ",
     * <p> "Expires":3672000,
     * <p>  "RefreshToken":"9fuIPl3Bo6lqJfbYjXFjuPnNwNqVfxjiUE7cMCZSjrX22RJSoKf28jtIhI0v86wjV5GJ21bc6LvMNbfYG0QsZ2TcvTQNInn85XdPIJRIe-9zB-eaY5utBVKmtLjJdEEmBlx5le5mT6oF7WBwVkwx*CUpSsdgUcyE3mG3FJnSHlajogaUSUgvMgmgUaVEMYzv4pcbCUltGNAMqJt5wwvBZA",
     * <p>  "RefreshTokenExpires":31536000,
     * <p>  "UUID":"c27fcbca44314786a49c0b092fef3cd2    ",
     * <p> "UserID":"51027f3e09a14a55917e687c628a0f13",
     * <p> "UserRegion":"https:\/\/api.ihealthlabs.com.cn:8443",
     * <p> "client_para":"random_str"}
     */
    public boolean getToken(String client_id, String client_secret,
                            String username, String client_para) {
        Log.p(TAG, Log.Level.INFO, "getToken", client_id, client_secret, username, client_para);
        if (getDevicePermisson(username, type)) {
            CommCloudHS6 comm = new CommCloudHS6(username, mContext);
            JSONObject mJsonObject = new JSONObject();
            try {
                String tokenResult = comm.sdk_get_token(client_id, client_secret, username, client_para);
                JSONTokener jsonTokener = new JSONTokener(tokenResult);
                mJsonObject.put(GET_TOKEN_RESULT, (JSONObject) jsonTokener.nextValue());
                mHs6Callback.onNotify("", type, ACTION_HS6_GET_TOKEN, mJsonObject.toString());
            } catch (Exception e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * set unit of HS6,and this method is a time consuming operation that cannot be called
     * in the main thread.
     *
     * @param username the identification of the user
     * @param unitType the unit type
     *                 <p>0 Kg
     *                 <p>1 lbs
     *                 <p>2 st
     * @return boolean whether has permission of using the method,false you need to get the
     * permission firstly.The result of get token is returnned by
     * {@link iHealthDeviceHs6Callback#onNotify(String, String, String, String)} and its'
     * action is {@link #ACTION_HS6_SET_UNIT}
     */
    public boolean setUnit(String username, int unitType) {
        Log.p(TAG, Log.Level.INFO, "setUnit", username, unitType);
        if (getDevicePermisson(username, type)) {
            CommCloudHS6 comm = new CommCloudHS6(username, mContext);
            JSONObject mJsonObject = new JSONObject();
            try {
                boolean result = false;
                if (comm.sync_unit(username, unitType) == 100) {
                    result = true;
                }
                mJsonObject.put(SET_UNIT_RESULT, result);
                mHs6Callback.onNotify("", type, ACTION_HS6_SET_UNIT, mJsonObject.toString());
            } catch (Exception e) {
                Log.p(TAG, Log.Level.WARN, "Exception", e.getMessage());
            }
            return true;
        } else {
            return false;
        }

    }
}
