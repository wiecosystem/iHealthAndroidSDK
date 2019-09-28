package com.ihealth.androidbg.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.ihealth.communication.utils.Log;

import com.ihealth.androidbg.audio.BG1.BG1_Command_Interface_Subject;
import com.ihealth.communication.ins.GenerateKap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by zhaoyongguang on 15/8/3.
 */

public class TunnerThread {

    private final static String TAG = TunnerThread.class.getSimpleName();
    private static int SAMPLE_RATE = 44100;
    private final static int BUFFERSIZE = 1024;//初始读取的buffer长度
    private byte[] mBufferAll = new byte[1024 + 1024];
    private boolean ISRECORDING = true;
    private AudioRecord mAudioRecord = null;
    private ArrayList<Integer> order = null;
    private byte[] receiveOrder = null;//接收到的数据－－转化成字节数组
    public BG1_Command_Interface_Subject msgSubject;//监听者模式
    private int[] freTimes = null;
    private LinkedHashMap<Integer, Integer> times = null;
    private boolean stop = false;//控制线程停止(当耳机拔出或者需要切换到另外一个线程的时候)
    private boolean is1307 = false;

    public TunnerThread() {

        initAudioRecord(3);
        mBufferAll = new byte[1024 + 1024];
        msgSubject = new BG1_Command_Interface_Subject();

        Tunnerthread512 tt512 = new Tunnerthread512();
        tt512.start();

    }

    public void set1307(boolean b) {
        this.is1307 = b;
    }

    public void close() {
        is1307 = false;
        stop = true;
        ISRECORDING = false;
        if (mAudioRecord != null) {
            Log.v(TAG, "stop current communication");
            try {
                mAudioRecord.release();
                mAudioRecord = null;
            } catch (Exception e) {
                Log.w(TAG, "Exception ---> " + e);
            } finally {
                mAudioRecord = null;
            }
        }
    }

    /**
     * 512窗口，256偏移量
     *
     * @author zhaoyongguang
     */
    class Tunnerthread512 extends Thread {

        public Tunnerthread512() {
            super();
        }

        @Override
        public void run() {
            super.run();
            try {
                if (mAudioRecord != null && mAudioRecord.getState() == 1) {
                    mAudioRecord.startRecording();
                    while (ISRECORDING) {
                        final byte[] bufferRead = new byte[BUFFERSIZE];
                        if (mAudioRecord.read(bufferRead, 0, BUFFERSIZE) > 0) {
                            if (bufferRead.length == 1024) {
                                System.arraycopy(mBufferAll, 1024, mBufferAll, 0, 1024);
                                System.arraycopy(bufferRead, 0, mBufferAll, 1024, 1024);
                                byte[] msg = cutFre_512(mBufferAll);
                                if (msg != null && msg.length >= 3) {
                                    Log.v(TAG, "this command =" + Bytes2HexString(msg, msg.length));
                                    msgSubject.notifyBytes(msg);
                                    receiveOrder = null;
                                }
                            } else {
                                Log.w(TAG, "bufferRead.length = " + bufferRead.length);
                            }
                        }
                        if (stop) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "record Exception = " + e);
            }
        }
    }

    private boolean initAudioRecord(int count) {
        Log.v(TAG, "initAudioRecord count ---> " + (4 - count));
        try {
            if (mAudioRecord != null) {
                Log.v(TAG, "mAudioRecord.getState() ---> " + mAudioRecord.getState());
                if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    return true;
                }
                try {
                    mAudioRecord.release();
                    mAudioRecord = null;
                } catch (Exception e) {
                    Log.w(TAG, "AudioRecord Exception ---> " + e);
                } finally {
                    mAudioRecord = null;
                }
            }

            int min_buffer_size = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            Log.v(TAG, "AudioRecord min_buffer_size ---> " + min_buffer_size);
            mAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    min_buffer_size * 2);


            if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                Log.v(TAG, "STATE_INITIALIZED ---> ");
                return true;
            } else {
                if (count > 1) {
                    return initAudioRecord(--count);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "initAudioRecord Exception ---> " + e);
            if (count > 1) {
                return initAudioRecord(--count);
            }
        }
        return false;
    }

    /**
     * 将1024个点按照窗口为1024的大小截取，并放入FFT进行频率计算，每次解析完成后，保持窗口大小不变并将其向后移动256个点继续解析，每1024个点可以解析出7个频率
     * 选取这9个频率中出现次数>2的频率作为有效频率(ios为连续4个相同频率，本FFT算法误差会大)
     *
     * @param point(1024个点 || 1024 + 1024个点)
     * @return 当解析出头频率时，将存储指令的缓存重新定义，当解析出尾频率时，存储该条指令结束，并解析该条指令，并给出返回值(byte[])，否则返回null
     */
    private byte[] cutFre(byte[] point) {
        byte[] temp;
        int[] fre = new int[4];
        int position = 0;
        times = new LinkedHashMap<Integer, Integer>();
        for (int i = 256; i < point.length - 1024 + 256; i += 256) {
            temp = new byte[1024];
            System.arraycopy(point, i, temp, 0, 1024);
            fre[position] = processSomeFre(temp);
            position++;
        }
        if (freTimes == null) {//第一次进入，把9个点全部复制进入缓存
            freTimes = new int[7];
            System.arraycopy(fre, 0, freTimes, 0, 4);
        } else {//第n次进入，将之前缓存中的前2个点删除，同时将这次的5个点加入缓存
            System.arraycopy(freTimes, 4, freTimes, 0, 3);
            System.arraycopy(fre, 0, freTimes, 3, 4);
        }
        for (int j : freTimes) times.put(j, times.containsKey(j) ? times.get(j) + 1 : 1);
        for (Map.Entry<Integer, Integer> e : times.entrySet()) {
            if ((e.getValue() >= 2 && e.getKey() > 500)) {
                if (e.getKey() == 906) {//是头频率
                    if ((order == null) || (order.size() == 0) || ((order != null) && (order.size() > 0) && (e.getKey().intValue() != order.get(order.size() - 1).intValue()))) {
                        order = new ArrayList<Integer>();
                        order.add(e.getKey());
                        receiveOrder = null;
                        break;
                    }
                } else if ((order != null) && (order.size() > 0) && (order.get(0).intValue() == 906) && (e.getKey().intValue() == 601)) {//是尾频率
                    order.add(e.getKey());
                    receiveOrder = getOrderByData(transToneToData(order));
                    order.remove(order);
                    order.clear();
                    freTimes = null;
                    return receiveOrder;
                } else if ((order != null) && (order.size() > 0) && (order.get(0).intValue() == 906) && (e.getKey().intValue() != order.get(order.size() - 1).intValue())) {//含有头频率
                    order.add(e.getKey());
                    break;
                } else {

                }
            }
        }
        return null;
    }

    /**
     * 为了每个频率点更好的解析，解析窗口改为512(实际窗口大小1024，另外512点补0)，偏移量为256，缓冲区1024+1024，这样每个频率点解析5次（3+2）（或者6次 2+2+2）
     * 取解析结果大于2次为有效频率。
     * 当解析出头频率时，将存储指令的缓存重新定义，当解析出尾频率时，存储该条指令结束，并解析该条指令，并给出返回值(byte[])，否则返回null
     *
     * @param point
     * @return
     * @author zhaoyongguang  2015年 7月22日 星期三 20时18分51秒 CST
     */
    private byte[] cutFre_512(byte[] point) {
        byte[] temp;
        int[] fre = new int[4];
        int position = 0;
        times = new LinkedHashMap<Integer, Integer>();
        for (int i = 256; i < point.length - 1024 + 256; i += 256) {
            temp = new byte[1024];
            System.arraycopy(point, i, temp, 0, 512);
            fre[position] = processSomeFre(temp);
            position++;
        }
        if (freTimes == null) {//第一次进入，把4个点全部复制进入缓存
            freTimes = new int[5];
            System.arraycopy(fre, 0, freTimes, 0, 4);
        } else {//第n次进入，将之前缓存中的前2个点删除，同时将这次的5个点加入缓存
            System.arraycopy(freTimes, 4, freTimes, 0, 1);
            System.arraycopy(fre, 0, freTimes, 1, 4);
        }
//        if(AppsDeviceParameters.isLog) {
//            Log.v(TAG, "-------------------------------------------------");
//            for (int m=0;m<freTimes.length;m++) {
//                Log.v(TAG, "freTimes[" + m + "] = " + freTimes[m]);
//            }
//            Log.v(TAG, "-------------------------------------------------");
//        }

        for (int j : freTimes) times.put(j, times.containsKey(j) ? times.get(j) + 1 : 1);
        for (Map.Entry<Integer, Integer> e : times.entrySet()) {
            if ((e.getValue() >= 2 && e.getKey() > 500)) {
                if (e.getKey() == 906) {//是头频率
                    order = new ArrayList<Integer>();
                    order.add(e.getKey());
                    receiveOrder = null;
                } else if ((order != null) && (order.size() > 0) && (order.get(0).intValue() == 906) && (e.getKey().intValue() == 601)) {//是尾频率
                    order.add(e.getKey());
                    receiveOrder = getOrderByData(transToneToData(order));
                    order.remove(order);
                    order.clear();
                    freTimes = null;
                    return receiveOrder;
                } else if ((order != null) && (order.size() > 0) && (order.get(0).intValue() == 906) && (e.getKey().intValue() != order.get(order.size() - 1).intValue())) {//含有头频率
                    order.add(e.getKey());
                }
            }
        }
        return null;
    }

    private GenerateKap mGenerateKap = new GenerateKap();

    private synchronized int processSomeFre(byte[] buffer) {
        double fre = mGenerateKap.getDataFromByteArray(buffer, SAMPLE_RATE) / 2;
        int myFre = getFreByReceive(fre);
        return myFre;
    }

    private int getFreByReceive(double fre) {
        int myFre = 0;
        if (is1307) {
            if (7500 > fre && fre >= 6597) {//174-154          0-21
                myFre = 6944;
            } else if (6597 > fre && fre >= 5966) {//153-139   1-15
                myFre = 6250;
            } else if (5966 > fre && fre >= 5445) {//138-127   2-12
                myFre = 5682;
            } else if (5445 > fre && fre >= 5008) {//126-117   3-10
                myFre = 5208;
            } else if (5008 > fre && fre >= 4636) {//116-108   4-9
                myFre = 4808;
            } else if (4636 > fre && fre >= 4315) {//107-101   5-7
                myFre = 4464;
            } else if (4315 > fre && fre >= 4020) {//100-94    6-7  97
                myFre = 4167;
            } else if (4020 > fre && fre >= 3840) {//93-90     7-4  91
                myFre = 3906;
            } else if (3840 > fre && fre >= 3550) {//89-83     8-7  86
                myFre = 3676;
            } else if (3550 > fre && fre >= 3268) {//82-76     9-7  79
                myFre = 3378;
            } else if (3268 > fre && fre >= 2951) {//75-69     A-7
                myFre = 3125;
            } else if (2951 > fre && fre >= 2639) {//68-62     B-7
                myFre = 2778;
            } else if (2639 > fre && fre >= 2408) {//61-56     C-6
                myFre = 2500;
            } else if (2408 > fre && fre >= 2098) {//55-49     D-7
                myFre = 2273;
            } else if (2098 > fre && fre >= 1806) {//48-42     E-7
                myFre = 1923;
            } else if (1806 > fre && fre >= 1482) {//41-35     F-7
                myFre = 1689;
            } else if (1482 > fre && fre >= 1091) {//34-26     X-9
                myFre = 1276;
            } else if (1091 > fre && fre >= 753) {//25-18      H-8
                myFre = 906;
            } else if (753 > fre && fre >= 500) {//17-12       T-6
                myFre = 601;
            }
        } else {
            if (10000 > fre && fre >= 8371) {
                myFre = 8929;
            } else if (8371 > fre && fre >= 7378) {
                myFre = 7813;
            } else if (7378 > fre && fre >= 6597) {
                myFre = 6944;
            } else if (6597 > fre && fre >= 5966) {
                myFre = 6250;
            } else if (5966 > fre && fre >= 5445) {
                myFre = 5682;
            } else if (5445 > fre && fre >= 5008) {
                myFre = 5208;
            } else if (5008 > fre && fre >= 4636) {
                myFre = 4808;
            } else if (4636 > fre && fre >= 4315) {
                myFre = 4464;
            } else if (4315 > fre && fre >= 3921) {
                myFre = 4167;
            } else if (3921 > fre && fre >= 3482) {
                myFre = 3676;
            } else if (3482 > fre && fre >= 3065) {
                myFre = 3289;
            } else if (3065 > fre && fre >= 2670) {
                myFre = 2841;
            } else if (2670 > fre && fre >= 2327) {
                myFre = 2500;
            } else if (2327 > fre && fre >= 1996) {
                myFre = 2155;
            } else if (1996 > fre && fre >= 1681) {
                myFre = 1838;
            } else if (1681 > fre && fre >= 1363) {
                myFre = 1524;
            } else if (1363 > fre && fre >= 1054) {
                myFre = 1202;
            } else if (1054 > fre && fre >= 753) {
                myFre = 906;
            } else if (753 > fre && fre >= 500) {
                myFre = 601;
            }
        }

        return myFre;
    }

    /**
     * TODO 把接收到的音頻数据转换成数据数组（收到什么样的音頻返回什么样的数据数组）
     *
     * @return
     */
    private int[] transToneToData(ArrayList<Integer> tone) {
        int data[] = new int[tone.size()];
        if (is1307) {
            for (int i = 0; i < tone.size(); i++) {
                if (tone.get(i) == 6944) {//0
                    data[i] = 0;
                } else if (tone.get(i) == 6250) {//1
                    data[i] = 1;
                } else if (tone.get(i) == 5682) {//2
                    data[i] = 2;
                } else if (tone.get(i) == 5208) {//3
                    data[i] = 3;
                } else if (tone.get(i) == 4808) {//4
                    data[i] = 4;
                } else if (tone.get(i) == 4464) {//5
                    data[i] = 5;
                } else if (tone.get(i) == 4167) {//6
                    data[i] = 6;
                } else if (tone.get(i) == 3906) {//7
                    data[i] = 7;
                } else if (tone.get(i) == 3676) {//8
                    data[i] = 8;
                } else if (tone.get(i) == 3378) {//9  3289 3378
                    data[i] = 9;
                } else if (tone.get(i) == 3125) {//A 2976 3125
                    data[i] = 10;
                } else if (tone.get(i) == 2778) {//B 2717 2778
                    data[i] = 11;
                } else if (tone.get(i) == 2500) {//C //2451  2500
                    data[i] = 12;
                } else if (tone.get(i) == 2273) {//D //2193  2232
                    data[i] = 13;
                } else if (tone.get(i) == 1923) {//E 1953 1923
                    data[i] = 14;
                } else if (tone.get(i) == 1689) {//F
                    data[i] = 15;
                } else if (tone.get(i) == 1276) {//X 1250 1330
                    data[i] = 16;
                } else if (tone.get(i) == 906) {//HEAD
                    data[i] = 17;
                } else if (tone.get(i) == 601) {//TAIL
                    data[i] = 18;
                }
            }
        } else {
            for (int i = 0; i < tone.size(); i++) {
                if (tone.get(i) == 8929) {//0
                    data[i] = 0;
                } else if (tone.get(i) == 7813) {//1
                    data[i] = 1;
                } else if (tone.get(i) == 6944) {//2
                    data[i] = 2;
                } else if (tone.get(i) == 6250) {//3
                    data[i] = 3;
                } else if (tone.get(i) == 5682) {//4
                    data[i] = 4;
                } else if (tone.get(i) == 5208) {//5
                    data[i] = 5;
                } else if (tone.get(i) == 4808) {//6
                    data[i] = 6;
                } else if (tone.get(i) == 4464) {//7
                    data[i] = 7;
                } else if (tone.get(i) == 4167) {//8
                    data[i] = 8;
                } else if (tone.get(i) == 3676) {//9
                    data[i] = 9;
                } else if (tone.get(i) == 3289) {//A
                    data[i] = 10;
                } else if (tone.get(i) == 2841) {//B
                    data[i] = 11;
                } else if (tone.get(i) == 2500) {//C
                    data[i] = 12;
                } else if (tone.get(i) == 2155) {//D
                    data[i] = 13;
                } else if (tone.get(i) == 1838) {//E
                    data[i] = 14;
                } else if (tone.get(i) == 1524) {//F
                    data[i] = 15;
                } else if (tone.get(i) == 1202) {//X
                    data[i] = 16;
                } else if (tone.get(i) == 906) {//HEAD
                    data[i] = 17;
                } else if (tone.get(i) == 601) {//TAIL
                    data[i] = 18;
                }
            }
        }

        return data;
    }

    /**
     * TODO 把整数数组解析成指令(包含X因素)
     *
     * @return
     */
    private byte[] getOrderByData(int[] data) {
        if (data.length % 2 == 0) {
            int temp = -1;
            byte[] order = new byte[(data.length - 2) / 2];
            for (int i = 1; i < data.length - 1; i += 2) {
                int num_gao = 0;
                int num_di = 0;
                if (data[i] == 16) {
                    num_gao = temp;
                } else {
                    num_gao = data[i];
                    temp = data[i];
                }
                if (data[i + 1] == 16) {
                    num_di = temp;
                } else {
                    num_di = data[i + 1];
                    temp = data[i + 1];
                }
                order[(i - 1) / 2] = (byte) ((num_gao * 16) + num_di);
            }
            return order;
        } else {
            int temp = -1;
            byte[] order = new byte[(data.length - 2) / 2 + 1];
            for (int i = 1; i < data.length - 1; i += 2) {
                int num_gao = 0;
                int num_di = 0;
                if (data[i] == 16) {
                    num_gao = temp;
                } else {
                    num_gao = data[i];
                    temp = data[i];
                }
                if (i == data.length - 2) {
                    num_di = 0;
                } else {
                    if (data[i + 1] == 16) {
                        num_di = temp;
                    } else {
                        num_di = data[i + 1];
                        temp = data[i + 1];
                    }
                }
                order[(i - 1) / 2] = (byte) ((num_gao * 16) + num_di);
            }
            return order;
        }
    }

    private short getShort(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }
        if (buf.length > 2) {
            throw new IllegalArgumentException("byte array size > 2 !");
        }
        short r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        }
        return r;
    }

    private String Bytes2HexString(byte[] b, int len) {
        String ret = "";
        for (int i = 0; i < len; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }
}
