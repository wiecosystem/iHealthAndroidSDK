package com.ihealth.communication.cloud.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import android.content.Context;
import com.ihealth.communication.utils.Log;

public class HttpsPost {

    private static final String TAG = "HttpsPost";
    private HttpsURLConnection conn = null;
    private Context mContext = null;

    public HttpsPost(Context mContext) {
        this.mContext = mContext;
    }

    // 增加捕获超时异常
    public String requireClass(String path, Map<String, String> params, String encoding) throws Exception {

        String RetBak = "";

        InputStream inStream = null;
        OutputStream outStream = null;

        try {

            StringBuffer buffer = new StringBuffer();
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    buffer.append(entry.getKey()).append('=');
                    buffer.append(URLEncoder.encode(entry.getValue(), encoding));
                    buffer.append('&');
                }
                buffer.deleteCharAt(buffer.length() - 1);
            }

            URL url = new URL(path);
            conn = (HttpsURLConnection) url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                if (mContext != null) {
                    conn.setSSLSocketFactory(SSLCustomSocketFactory.getSocketFactory04(mContext));
                } else {
                    conn.setSSLSocketFactory(SSLCustomSocketFactory.getSocketFactoryAll());
                }
            }
            conn.setConnectTimeout(60000);// 60000
            conn.setReadTimeout(60000);// 60000
            conn.setRequestMethod("POST");
            conn.setDoInput(true);// 表示从服务器获取数据
            conn.setDoOutput(true);// 表示向服务器写数据

            String bSt = buffer.toString();
            Log.v(TAG,"Post" + "   " + path + " \n" + buffer.toString());
            byte[] entity = bSt.getBytes();

            // 表示设置请求体的类型是文本类型
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            // 获得上传信息的字节大小以及长度
            conn.setRequestProperty("Content-Length", String.valueOf(entity.length));
            // 获得输出流,向服务器输出数据
            outStream = conn.getOutputStream();
            outStream.write(entity);
            outStream.flush();

            inStream = conn.getInputStream();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = 0;
            byte[] resu = new byte[1024];
            while ((len = inStream.read(resu)) != -1) {
                baos.write(resu, 0, len);
            }
            baos.close();//内存输入流不需要CLOSE
            byte[] result = baos.toByteArray();//转化为byte数组
            RetBak = new String(result, encoding);

        } finally {
            try {
                if (outStream != null)
                    outStream.close();
                if (inStream != null)
                    inStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (null != conn) {
                conn.disconnect();
            }
        }

        Log.v(TAG, "Post result -->" + RetBak);

        return RetBak;
    }
}
