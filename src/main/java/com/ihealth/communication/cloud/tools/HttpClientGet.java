package com.ihealth.communication.cloud.tools;

import android.os.Environment;
import android.os.SystemClock;
import com.ihealth.communication.utils.Log;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class HttpClientGet {

	private final AllowAllHostnameVerifier HOSTNAME_VERIFIER = new AllowAllHostnameVerifier();

	private X509TrustManager xtm = new X509TrustManager() {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	};

	private X509TrustManager[] xtmArray = new X509TrustManager[] { xtm };

	String paths = "";
	Map<String, String> paramss = null;
	String encodings = "";
	String RetBak = "";
	boolean isRe = true;
	int isStop = 0;

	// 增加捕获超时异常
	public String sendPOSTRequestForInputStream(String path, Map<String, String> params, String encoding, final int type) throws ConnectTimeoutException {

		paths = path;
		paramss = params;
		encodings = encoding;
		RetBak = "";
		isRe = true;
		isStop = 0;

		class acceptThread extends Thread {
			Map<String, String> paramsa = null;

			public acceptThread(Map<String, String> params) {
				this.paramsa = params;
			}

//			private HttpsURLConnection conn = null;
			private HttpURLConnection conn = null;
			InputStream in = null;

			FileOutputStream fout = null;

			@Override
			public void run() {
				String bakn = "";
				try {

					StringBuilder entityBuilder = new StringBuilder("");
					if (paramsa != null && !paramss.isEmpty()) {
						for (Map.Entry<String, String> entry : paramss.entrySet()) {
							entityBuilder.append(entry.getKey()).append('=');
							entityBuilder.append(URLEncoder.encode(entry.getValue(), encodings));
							entityBuilder.append('&');
						}
						entityBuilder.deleteCharAt(entityBuilder.length() - 1);
					}

					String httpurl = paths + entityBuilder.toString();
					Log.i("AMDownLoad", "httpurl_GET" + httpurl);
					URL url = new URL(httpurl);
					conn = (HttpURLConnection) url.openConnection();
//					conn = (HttpsURLConnection) url.openConnection();
					if (conn instanceof HttpsURLConnection) {
						// Trust all certificates
						SSLContext context = SSLContext.getInstance("TLS");
						context.init(new KeyManager[0], xtmArray, new SecureRandom());
						SSLSocketFactory socketFactory = context.getSocketFactory();
//						conn.setSSLSocketFactory(socketFactory);
//						sconn.setHostnameVerifier(HOSTNAME_VERIFIER);
					}

					conn.setConnectTimeout(15000);
					conn.setReadTimeout(15000);
					conn.setDoInput(true);
					conn.setRequestMethod("GET");
					// conn.setRequestProperty("Content-Type", "text/html");
					conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					conn.setRequestProperty("Accept-Charset", encodings);
					conn.setRequestProperty("contentType", encodings);
					in = conn.getInputStream();
					try {
						// 放入SD卡根目录下的文件夹下，要先mkdir创建文件夹，获取文件夹对象之后 在创建文件
						//File path = new File(Method.getSDPath() + "/iHealth/");
						File path = new File(getSDPath() + "/iHealth/");
						if (!path.exists()) {
							path.mkdirs();
						}

//						Log.e("下载固件信息", "type = " + ((type == 0) ? "AMDownload.txt" : "AMIndex.txt"));
						File file = null;
						if (type == 0) {
							file = new File(path, "AMDownload.txt");
							if(file.exists()){
								file.delete();
							}
						} else {
							file = new File(path, "AMIndex.txt");
							if(file.exists()){
								file.delete();
							}
						}

						FileOutputStream fout = new FileOutputStream(file);

						int num = 0;
						byte[] resu = new byte[1024]; 
						StringBuffer sb = new StringBuffer(); // 同步
						while ((num = in.read(resu)) != -1) {
							String aaaaaaa = new String(resu, 0, num);
							fout.write(resu, 0, num);
							sb.append(aaaaaaa);

						}
						RetBak = sb.toString();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							if (fout != null) {
								fout.close();
							}
						} catch (Exception e2) {
							e2.printStackTrace();
							Log.e("CLOUD", "fout类中释放资源出错");
						}
					}

//					Log.e("Return", RetBak);
				} catch (Exception e) {
					e.printStackTrace();
					RetBak = "";

				} finally {
					try {
						if (in != null) {
							in.close();
						}
						if (conn != null) {
							conn.disconnect();
						}
					} catch (IOException e) {
						e.printStackTrace();
						Log.e("CLOUD", "conn类中释放资源出错");
					}
				}
				isRe = false;
			}
		}

		acceptThread abc = new acceptThread(params);
		abc.start();

		while (isRe) {
			SystemClock.sleep(1);
			isStop++;
			if (isStop > 120000) {
				break;
			}
		}
		return RetBak;
	}
	
    /**
     * 获取sd卡根目录
     */
    public static String getSDPath() {
        File sdDir = null;
        sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
        return sdDir.toString();
    }
}
