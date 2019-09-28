package com.ihealth.androidbg.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import com.ihealth.communication.utils.Log;


public class AudioTrackManager {

    private final static String TAG = AudioTrackManager.class.getSimpleName();
    private static final int sampleRate = 44100;
    private static final int audioTrackTxStreamType = AudioManager.STREAM_MUSIC;
    private static final int audioTrackTxChannel = AudioFormat.CHANNEL_OUT_MONO;
    private static final int audioTrackTxFormat = AudioFormat.ENCODING_PCM_16BIT;
    private static final int audioTrackTxMode = AudioTrack.MODE_STREAM;
    private static final short max_short = Short.MAX_VALUE;

    private AudioTrack audioTrack = null;
    private short[] dataSend = null;
    private boolean isNeedStop = false;
    private int times = 4;

    public AudioTrackManager() {
    }

    /**
     * some specific process
     */
    public static boolean isR2017 = false;//解析偏差问题，345680
    public static boolean inCommunication = false;

    /**
     * TODO 初始化 AudioTrack
     *
     * @hide
     */
    public void initManager() {
        try {
            if (Build.MODEL.toUpperCase().contains("GT-S7390")
                    || Build.MODEL.toUpperCase().contains("GT-S7562")) {  //||Build.MODEL.toUpperCase().contains("LG-F180L") //录音异常
                this.isNeedStop = true;
            } else if (Build.MODEL.toUpperCase().contains("SM-G935")
                    || Build.MODEL.toUpperCase().contains("SM-G930")
                    || Build.PRODUCT.toUpperCase().contains("S7")) {
                times = 7;
            } else if (Build.MODEL.equalsIgnoreCase("R2017")) {
                this.isNeedStop = true;
                isR2017 = true;
            }

            int min_buffer_size = AudioTrack.getMinBufferSize(
                    sampleRate,
                    audioTrackTxChannel,
                    audioTrackTxFormat);
            Log.v(TAG, "AudioTrack min_buffer_size ---> " + min_buffer_size);
            Log.v(TAG, "AudioTrack.getMaxVolume() ---> " + AudioTrack.getMaxVolume());
            audioTrack = new AudioTrack(
                    audioTrackTxStreamType,
                    sampleRate,
                    audioTrackTxChannel,
                    audioTrackTxFormat,
                    min_buffer_size * 2,
                    audioTrackTxMode);
            audioTrack.setStereoVolume(0.0f, AudioTrack.getMaxVolume());
        } catch (Exception e) {
            Log.w(TAG, "initAudioTrack Exception ---> " + e);
        }
    }

    /**
     * TODO 发送声音
     *
     * @param rates
     */
    public void play(int[] rates) {

        dataSend = new short[256 * 60];//初始化0

        //头补偿
        double x_h = 2.0 * Math.PI * 1000.0 / 44100;
        for (int j = 0; j < 256 * times; j++) {
            dataSend[j] = (short) (max_short * Math.sin(x_h * j));
        }
        //有效数据
        for (int i = 0; i < rates.length; i++) {
            double x = 2.0 * Math.PI * rates[i] / 44100;
            for (int j = 0; j < 256; j++) {
                dataSend[j + 256 * (i + times)] = (short) (max_short * Math.sin(x * j));
            }
        }
        //尾补偿 honor 5C
        for (int i = 0; i < 256 * 2; i++) {
            dataSend[i + 256 * (times + rates.length)] = (short) (max_short * Math.sin(x_h * i));
        }

        if (audioTrack == null) {
            initManager();
        }
        if (audioTrack != null && audioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            audioTrack.play();
            audioTrack.write(dataSend, 0, dataSend.length);
            //小米Note Y51 S6 7-1 3-1 no need stop
            if (isNeedStop) {
                audioTrack.stop();
            }

            try {
                audioTrack.flush();
            } catch (Exception e) {
                Log.w(TAG, "audioTrack.flush Exception ---> " + e);
                //java.lang.IllegalStateException: Unable to retrieve AudioTrack pointer for flush()
                //at android.media.AudioTrack.native_flush(Native Method)
            }

        } else {
            Log.w(TAG, "audioTrack == null cannot send wave to BG1");
        }
    }

    /**
     * TODO 停止
     */
    public void stop() {
        if (audioTrack != null) {
            try {
                audioTrack.release();
                audioTrack = null;
            } catch (IllegalStateException e) {
                Log.w(TAG, "IllegalStateException ----> " + e);
            } finally {
                audioTrack = null;
            }
        }
    }
}

