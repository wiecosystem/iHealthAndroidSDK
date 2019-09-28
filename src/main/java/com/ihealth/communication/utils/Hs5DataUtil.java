/**
 * @title
 * @Description
 * @author
 * @date 2015年5月19日 下午8:36:23 
 * @version V1.0  
 */

package com.ihealth.communication.utils;

import java.math.BigDecimal;

import com.ihealth.communication.cloud.tools.AppsDeviceParameters;

import android.R.integer;
import com.ihealth.communication.utils.Log;

/**
 * @ClassName: Hs5DataUtil
 * @Description: TODO
 * @author gaonana
 * @date 2015年5月19日 下午8:36:23
 */
public class Hs5DataUtil {
    private static final String TAG = "Hs5DataUtil----";
    private static final String TAG1 = "HS5Wifi";

    public static int[] parseData(byte[] returnData) {
        int year = returnData[0] & 0xFF;
        int month = (returnData[4] & 0xF0) >> 4;
        int day = ((returnData[3] & 0x80) >> 7) * 16 + ((returnData[2] & 0x80) >> 7)
                * 8 + ((returnData[1] & 0xE0) >> 5);
        int hour = returnData[1] & 0x1F;
        int minute = returnData[2] & 0x7F;
        int second = returnData[3] & 0x7F;
        int weightR = ((int) (returnData[4] & 0x0F)) * 256
                + ((int) returnData[5] & 0xFF);
        int fat = ((int) (returnData[6] & 0xFF)) * 256 + ((int) returnData[7] & 0xFF);
        int water = ((int) (returnData[8] & 0xFF)) * 256
                + (int) (returnData[9] & 0xFF);
        int muscle = ((int) (returnData[10] & 0xFF)) * 256
                + (int) (returnData[11] & 0xFF);
        int skeleton = returnData[12] & 0xFF;
        int vFatLevel = returnData[13] & 0xFF;
        int DCI = ((int) (returnData[14] & 0xFF)) * 256 + ((int) returnData[15] & 0xFF);

        int[] result = new int[] {
                year, month, day, hour, minute, second,
                weightR, fat, water, muscle, skeleton, vFatLevel, DCI
        };
        Log.v(TAG1, TAG + "Time====" + year + "-" + month + "-" + day + " "
                + hour + ":" + minute + ":" + second);
        Log.v(TAG1, TAG + "Composition====" + weightR + "-" + fat + "-" + water
                + " " + muscle + ":" + skeleton + ":" + vFatLevel + ":"
                + DCI);
        return result;
    }

    public static String getDateStr(int[] data) {
        int year = data[0] + 2000;
        int month = data[1];
        int day = data[2];
        int hh = data[3];
        int mm = data[4];
        int ss = data[5];
        String yearStr = String.valueOf(year);
        String monthStr = "";
        if (month < 10) {
            monthStr = "0" + String.valueOf(month);
        } else {
            monthStr = String.valueOf(month);
        }
        String dayStr = "";
        if (day < 10) {
            dayStr = "0" + String.valueOf(day);
        } else {
            dayStr = String.valueOf(day);
        }
        String hourStr = "";
        if (hh < 10) {
            hourStr = "0" + String.valueOf(hh);
        } else {
            hourStr = String.valueOf(hh);
        }
        String minsStr = "";
        if (mm < 10) {
            minsStr = "0" + String.valueOf(mm);
        } else {
            minsStr = String.valueOf(mm);
        }
        String sedStr = "";
        if (ss < 10) {
            sedStr = "0" + String.valueOf(ss);
        } else {
            sedStr = String.valueOf(ss);
        }
        String str = yearStr + "-" + monthStr + "-" + dayStr + " " + hourStr + ":" + minsStr + ":" + sedStr;
        return str;
    }



    /**
     * 取时间戳
     */
    public static long getTS() {
        long val = System.currentTimeMillis() / 1000;
        return val;
    }

}
