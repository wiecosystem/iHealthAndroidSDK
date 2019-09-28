/**
 * @title
 * @Description
 * @author
 * @date 2015年11月6日 下午1:43:31
 * @version V1.0
 */

package com.ihealth.communication.utils;

import static java.text.DateFormat.DEFAULT;
import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getDateTimeInstance;
import static java.text.DateFormat.getTimeInstance;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.ihealth.communication.cloud.data.Data_AM_SleepSectionReport;


import android.content.ContentResolver;
import android.content.Context;
import android.provider.ContactsContract;
import com.ihealth.communication.utils.Log;

/**
 * @author gaonana
 * @ClassName: PublicMethod
 * @Description: TODO
 * @date 2015年11月6日 下午1:43:31
 */
public class PublicMethod {
    /**
     * @param context
     * @param timeStr
     * @param id      （1，日期， 2，时间， 3日期+时间）
     * @return
     */
    public static String getDefaultTimerStr(Context context, String timeStr, int id) {
        String outPut = "";
        SimpleDateFormat sdfResouce;
        sdfResouce = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        sdfResouce.setTimeZone(TimeZone.getDefault());
        java.text.DateFormat df = null;
        switch (id) {
            // one of SHORT, MEDIUM, LONG, FULL, or DEFAULT.
            case 1:
                df = getDateInstance(DEFAULT, Locale.getDefault());
                break;
            case 2:
                df = getTimeInstance(SHORT, Locale.getDefault());
                break;
            case 3:
                df = getDateTimeInstance(java.text.DateFormat.FULL, SHORT, Locale.getDefault());
                break;
            case 4:
                df = getDateTimeInstance(java.text.DateFormat.LONG, SHORT, Locale.getDefault());
                break;
            case 5:
                df = getDateInstance(SHORT, Locale.getDefault());
                break;
            default:
                break;
        }

        SimpleDateFormat sdf = (SimpleDateFormat) df;
        Date fromData = new Date();
        try {
            fromData = sdfResouce.parse(timeStr);
            ContentResolver cv = context.getContentResolver();
            String strTimeFormat = android.provider.Settings.System.getString(cv,
                    android.provider.Settings.System.TIME_12_24);
            if (strTimeFormat != null) {
                if (strTimeFormat.equals("24")) {
                    SimpleDateFormat dstFormat = new SimpleDateFormat(sdf.toLocalizedPattern().replace("a", "")
                            .replace("h", "H"));
                    outPut = dstFormat.format(fromData);
                } else {
                    SimpleDateFormat dstFormat = new SimpleDateFormat(sdf.toLocalizedPattern());
                    outPut = sdf.format(fromData);
                }
            } else {
                SimpleDateFormat dstFormat = new SimpleDateFormat(sdf.toLocalizedPattern());
                outPut = sdf.format(fromData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outPut;
    }

    public static long String2TS(String dateStr) {
        long ret = -1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date;
        try {
            date = sdf.parse(dateStr);
            ret = date.getTime();
            ret = ret / 1000;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public static String TS2String(long TS) {
        String time = "";
        Date date = new Date();
        date.setTime(TS * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        time = sdf.format(date);
        return time;
    }

    public static ArrayList<Object> isRepeat_Sleep(ArrayList<Object> amSleepSectionsList) {
        ArrayList<Object> TestList1 = new ArrayList<Object>();
        for (int i = 0; i < amSleepSectionsList.size(); i++) {
            TestList1.add((Data_AM_SleepSectionReport) amSleepSectionsList.get(amSleepSectionsList.size() - 1 - i));
        }
        for (int i = 0; i < TestList1.size() - 1; i++) {
            for (int j = TestList1.size() - 1; j > i; j--) {
                if (((Data_AM_SleepSectionReport) TestList1.get(j)).getTimeSectionId().equals(
                        ((Data_AM_SleepSectionReport) TestList1.get(i)).getTimeSectionId())) {
                    TestList1.remove(j);
                }
            }
        }
        return TestList1;
    }

    /**
     * 将byte数组变成字符串
     *
     * @param b 待处理字符串
     * @return 产生的字符串
     */
    public static String Bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

    /**
     * get AM device data id
     *
     * @param MacId
     * @param resultData
     * @param TS
     * @return
     */
    public static String getAMDataID(String MacId, String resultData, long TS) {
        String mac = MacId.replaceAll(":", "");
        String DataId = mac + TS + resultData;
        if (null == mac) {
            DataId = TS + resultData;
        }
        return DataId;
    }

    /**
     * get BP device data id
     *
     * @param MacId
     * @param resultData
     * @param TS
     * @return
     */
    public static String getBPDataID(String MacId, String resultData, long TS) {
        String mac = MacId.replaceAll(":", "");
        String DataId = mac + resultData + TS;
        if (null == mac) {
            DataId = resultData + TS;
        }
        return DataId;
    }

    public static String getDataID(String MacId, String resultData, long TS) {
        String mac = MacId.replaceAll(":", "");
        String result = get2Value(Float.valueOf(resultData));
        String DataGetId = mac + TS + result + "00000000";
        if (null == mac) {
            DataGetId = TS + result + "00000000";
        }
        return DataGetId;
    }

    public static long getTs() {
        return System.currentTimeMillis() / 1000;
    }

    public static String get2Value(float valueData) {
        String result = "00";

        BigDecimal bg = new BigDecimal(valueData);
        int value = bg.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();

        if (value < 10) {
            result = "0" + value;
        } else {
            String aaa = value + "";
            if (aaa.length() == 2) {
                result = aaa;
            } else {
                result = aaa.substring(aaa.length() - 2, aaa.length());
            }
        }

        return result;
    }

    public static boolean isOneCode(String QRCode) {
        if (QRCode == null || QRCode.length() < 6) {
            return false;
        }
        return QRCode.substring(6).equalsIgnoreCase(MD5.md5String(QRCode.substring(0, 6)));
    }

    public static boolean isCtlCode(String QRCode) {
        if (QRCode == null || QRCode.length() < 6) {
            return false;
        }
        return QRCode.substring(0, QRCode.length() - 6).equalsIgnoreCase(MD5.md5String(QRCode.substring(QRCode.length() - 6)));
    }

}
