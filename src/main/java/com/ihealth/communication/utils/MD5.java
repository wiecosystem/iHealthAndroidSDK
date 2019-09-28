/**
 * @title
 * @Description
 * @author
 * @date 2015年7月3日 上午10:10:42 
 * @version V1.0  
 */

package com.ihealth.communication.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ihealth.communication.utils.Log;


/**
 * @ClassName: MD5
 * @Description: TODO
 * @author gaonana
 * @date 2015年7月3日 上午10:10:42
 */
public class MD5 {

    public static byte[] md5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }
//        Log.i("hash", ByteBufferUtil.Bytes2HexString(hash));
        return hash;
    }
    public static String md5String(String string) {
      return ByteBufferUtil.Bytes2HexString(md5(string));
    }
}
