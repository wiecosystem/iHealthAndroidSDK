package com.ihealth.communication.cloud.tools;

import android.content.Context;
import android.content.SharedPreferences;
import com.ihealth.communication.utils.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLCustomSocketFactory {

    private static final String TAG = "SSLCustomSocketFactory";

    private static final String KEY_PASS04 = "ELPWfWdA";
    private static final String FILE04 = "idscertificate.p12";

    public SSLCustomSocketFactory(KeyStore trustStore) throws Throwable {
        super();
    }

    public static SSLSocketFactory getSocketFactory04(Context context) {
        SharedPreferences sp = context.getSharedPreferences("IHCertificateFileInfo", Context.MODE_PRIVATE);
        String filePath = sp.getString("cert_path", FILE04);
        String password = sp.getString("cert_password", KEY_PASS04);
//        Log.i(TAG, "filePath = " + filePath);
//        Log.i(TAG, "password = " + password);
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");//KeyStore.getDefaultType() //BKS-V1
            InputStream ins = new FileInputStream(filePath);
            ks.load(ins, password.toCharArray());
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(ks, password.toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();
            sslcontext.init(keyManagers, null, null);

            SSLSocketFactory factory = sslcontext.getSocketFactory();

            return factory;
        } catch (FileNotFoundException e) {
            Log.i(TAG, "fnf" + e.getMessage());
            return getSocketFactoryAll();
        } catch (Throwable e) {
            Log.i(TAG, "throwable" + e.getMessage());
        }
        return (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    public static SSLSocketFactory getSocketFactory03(Context context, String otp) {

        try {
            KeyStore ks = null;
            ks = KeyStore.getInstance("PKCS12");
            FileInputStream fins = new FileInputStream(context.getFileStreamPath("03cert.p12"));
            ks.load(fins, otp.toCharArray());
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(ks, KEY_PASS04.toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();
            sslcontext.init(keyManagers, null, null);

            SSLSocketFactory factory = sslcontext.getSocketFactory();

            Log.e("", "getSocketFactory success");
            return factory;
        } catch (Throwable e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
        Log.e("", "getSocketFactory failed");

        return (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    public static SSLSocketFactory getSocketFactoryAll() {
        X509TrustManager xtm = new X509TrustManager() {
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
        X509TrustManager[] xtmArray = new X509TrustManager[]{xtm};
        SSLContext context;
        try {
            context = SSLContext.getInstance("TLS");
            context.init(new KeyManager[0], xtmArray, new SecureRandom());
            SSLSocketFactory socketFactory = context.getSocketFactory();

            return socketFactory;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return (SSLSocketFactory) SSLSocketFactory.getDefault();
    }


    public static class MyX509TrustManager implements X509TrustManager {
        /*
         * The default X509TrustManager returned by SunX509.  We'll delegate
         * decisions to it, and fall back to the logic in this class if the
         * default X509TrustManager doesn't trust it.
         */
        X509TrustManager sunJSSEX509TrustManager;

        MyX509TrustManager(Context context, String otp) throws Exception {
            // create a "default" JSSE X509TrustManager.
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(context.getFileStreamPath("03cert.p12")), otp.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(ks);
            TrustManager tms[] = tmf.getTrustManagers();
            /*
             * Iterate over the returned trustmanagers, look
			 * for an instance of X509TrustManager.  If found, 
			 * use that as our "default" trust manager. 
			 */
            for (int i = 0; i < tms.length; i++) {
                if (tms[i] instanceof X509TrustManager) {
                    sunJSSEX509TrustManager = (X509TrustManager) tms[i];
                    return;
                }
            }
            /*
             * Find some other way to initialize, or else we have to fail the
			 * constructor. 
			 */
            throw new Exception("Couldn't initialize");
        }

        /*
         * Delegate to the default trust manager.
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                sunJSSEX509TrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException excep) {
                // do any special handling here, or rethrow exception.
            }
        }

        /*
         * Delegate to the default trust manager.
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                sunJSSEX509TrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException excep) {
                /*
                 * Possibly pop up a dialog box asking whether to trust the
				 * cert chain. 
				 */
            }
        }

        /*
         * Merely pass this through.
         */
        public X509Certificate[] getAcceptedIssuers() {
            return sunJSSEX509TrustManager.getAcceptedIssuers();
        }
    }
}
