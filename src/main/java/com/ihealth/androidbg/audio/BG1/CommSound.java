package com.ihealth.androidbg.audio.BG1;

import android.os.SystemClock;

import com.ihealth.communication.utils.Log;

import com.ihealth.androidbg.audio.AudioTrackManager;
import com.ihealth.androidbg.audio.TransToneData;
import com.ihealth.androidbg.audio.TunnerThread;

/**
 * @author liujun
 *         这个类完成封装数据，发送数据，接收数据，解包数据
 * @time 2014-3-19
 */
public class CommSound implements BG1_Command_Interface {

    private static final String TAG = CommSound.class.getSimpleName();

    public AudioTrackManager audio;
    private int commandSequenceNum;  // 全局统一的命令顺序号
    private byte[] receiveOrder;

    public CommSound(TunnerThread tunner) {
        audio = new AudioTrackManager();
        audio.initManager();
        commandSequenceNum = 0;//顺序ID
        tunner.msgSubject.detach(this);
        tunner.msgSubject.attach(this);
    }

    /**
     * 2014-3-19
     * liujun
     * TODO 封装需要发送的数据－－加上头（4）和尾（1）
     *
     * @param stateID:状态ID，data:需要发送的数据
     * @return
     */
    private byte[] packageCommand(byte stateID, byte[] data) {
        if (data != null) {
            byte[] commandSend = new byte[data.length + 5];
            commandSend[0] = (byte) 0xB0;            // 命令头
            commandSend[1] = (byte) (data.length + 2); // 数据长度
            commandSend[2] = stateID; // 状态ID
            commandSend[3] = (byte) (commandSequenceNum); // 顺序ID
//			commandSequenceNum+=2;
            int tempTotal = 0; // 校验和
            tempTotal = commandSend[2] + commandSend[3];
            for (int j = 0; j < data.length; j++) {
                commandSend[j + 4] = data[j];
                tempTotal += commandSend[j + 4];
            }
            commandSend[data.length + 4] = (byte) tempTotal;
            return commandSend;
        } else {
            return null;
        }
    }

    /**
     * 2014-3-26
     * liujun
     * 发送握手指令－－不需要封装
     *
     * @param
     */
    public byte[] sendCommand(int id) {
        int count = 3;//重发次数
        byte[] recData = null;
        while (count-- != 0) {
            int tone[] = new int[]{16600, 16600, 16600, 16600, 16600, 16600, 16600, 16600};
            audio.play(tone);
            int timer = 1000;
            while (timer-- > 0) {
                recData = receiveCommand(id);
                if (recData == null) {
                    SystemClock.sleep(1);
                } else {
                    break;
                }
            }
            if (recData != null) {
                break;
            }
        }
        return recData;//没有得到返回数据
    }

    /**
     * 2014-3-19
     * liujun
     * 将封装好的指令发送出去(三遍重发)
     *
     * @param stateID:状态ID，command:需要发送的数据
     */
    public byte[] sendCommand(byte stateID, byte[] command, int id) {
        int count = 5;//重发次数
        byte[] myCommand = packageCommand(stateID, command);
        byte[] recData = null;
        int tone[] = TransToneData.transDataToTone((TransToneData.getDataByOrder(myCommand)));
        if (myCommand.length != 0) {
            while (AudioTrackManager.inCommunication && count-- != 0) {
                Log.v(TAG, "------------------------------------------------------ >");
                Log.v(TAG, "| sendCommand -----------> " + Bytes2HexString(myCommand, myCommand.length));
                Log.v(TAG, "------------------------------------------------------ >");
                audio.play(tone);
                int timer = 1000;
                while (AudioTrackManager.inCommunication && timer-- > 0) {
                    recData = receiveCommand(id);
                    if (recData == null) {
                        SystemClock.sleep(1);
                    } else {
                        break;
                    }
                }
                if (recData != null) {
                    break;
                }
            }
        }
        return recData;//没有得到返回数据
    }

    /**
     * 2014-3-19
     * liujun
     * TODO 接收声音指令
     */
    public byte[] receiveCommand(int id) {
        if (receiveOrder != null) {
            byte[] data = unPackageCommand(receiveOrder, id);//返回
            receiveOrder = null;//取完数以后－－把接收数组初始化
            return data;
        } else {
            return null;
        }
    }

    /**
     * 2014-3-19
     * liujun
     * TODO 将接收到的指令解包－－去头（4）去尾（1）
     *
     * @param receiveCommand 指令
     * @param id             指令长度
     */
    public byte[] unPackageCommand(byte[] receiveCommand, int id) {
        if (receiveCommand == null) {
            return null;
        }
        int lengthReceive = receiveCommand.length;
        if (lengthReceive < 5) {
            return null;
        }
        byte[] dataReceive = receiveCommand;
        byte[] data = new byte[lengthReceive - 5];
        Log.v(TAG, "Unpackage command = " + Bytes2HexString(dataReceive, lengthReceive));
        if (dataReceive[0] != (byte) 0xA0) {//头命令不对
            Log.v(TAG, "Unpackage header error");
            return null;
        }
        int lenPackage = dataReceive[1];
        if (lenPackage + 3 != lengthReceive) {//长度错误
            Log.v(TAG, "Unpackage length error");
            return null;
        }
        if (dataReceive[4] != (byte) 0xA2) {//产品ID不对
            Log.v(TAG, "Unpackage product id error");
            return null;
        }
        byte sum = 0;
        sum = (byte) (dataReceive[2] + dataReceive[3] + dataReceive[4]);
        if (id == 0) {//握手
            if (lengthReceive != 7 || dataReceive[5] != (byte) 0x01) {
                Log.v(TAG, "handshake no match");
                return null;
            }
        }
        if (id == 1) {//模式
            if (lengthReceive != 22 || dataReceive[5] != (byte) 0x20) {
                Log.v(TAG, "model no match");
                return null;
            }
        }
        if (id == 2) {//APP Id
            if (lengthReceive != 7 || (dataReceive[5] != (byte) 0x2D && dataReceive[5] != (byte) 0x2E)) {
                Log.v(TAG, "APP Id no match");
                return null;
            }
        }
        if (id == 32) {//idps第一包
            if (lengthReceive != 22 || dataReceive[2] != (byte) 0x32 || dataReceive[5] != (byte) 0x3F) {
                Log.v(TAG, "idps 3-1 no match");
                return null;
            }
        }
        if (id == 31) {//idps第二包
            if (lengthReceive != 16 || dataReceive[2] != (byte) 0x31 || dataReceive[5] != (byte) 0x3F) {
                Log.v(TAG, "idps 3-2  no match");
                return null;
            }
        }
        if (id == 30) {//idps第三包
            if (lengthReceive != 28 || dataReceive[2] != (byte) 0x30 || dataReceive[5] != (byte) 0x3F) {
                Log.v(TAG, "idps 3-3 no match");
                return null;
            }
        }
        if (id == 43) {//认证第一包
            if (lengthReceive != 23 || dataReceive[2] != (byte) 0x22 || dataReceive[5] != (byte) 0xFB) {
                Log.v(TAG, "identify 4-1 no match");
                return null;
            }
        }
        if (id == 42) {//认证第二包
            if (lengthReceive != 23 || dataReceive[2] != (byte) 0x21 || dataReceive[5] != (byte) 0xFB) {
                Log.v(TAG, "identify 4-2 no match");
                return null;
            }
        }
        if (id == 41) {//认证第三包
            if (lengthReceive != 23 || dataReceive[2] != (byte) 0x20 || dataReceive[5] != (byte) 0xFB) {
                Log.v(TAG, "identify 4-3 no match");
                return null;
            }
        }
        if (id == 40) {//认证第四包
            if (lengthReceive != 7 || (dataReceive[5] != (byte) 0xFD && dataReceive[5] != (byte) 0xFE)) {
                Log.v(TAG, "identify 4-4 no match");

                return null;
            }
        }
        if (id == 51) {
            if (lengthReceive != 7 || dataReceive[5] != (byte) 0x50) {
                Log.v(TAG, "2-1 no match");
                return null;
            }
        }
        if (id == 50) {
            if (lengthReceive != 7 || dataReceive[5] != (byte) 0x50) {
                Log.v(TAG, "2-2 no match");
                return null;
            }
        }
        if (id == 65) {
            if (lengthReceive != 7 || dataReceive[2] != (byte) 0xA5 || dataReceive[5] != (byte) 0x25) {
                Log.v(TAG, "6-1 no match");
                return null;
            }
        }
        if (id == 64) {
            if (lengthReceive != 7 || dataReceive[2] != (byte) 0xA4 || dataReceive[5] != (byte) 0x25) {
                Log.v(TAG, "6-2 no match");
                return null;
            }
        }
        if (id == 63) {
            if (lengthReceive != 7 || dataReceive[2] != (byte) 0xA3 || dataReceive[5] != (byte) 0x25) {
                Log.v(TAG, "6-3 no match");
                return null;
            }
        }
        if (id == 62) {
            if (lengthReceive != 7 || dataReceive[2] != (byte) 0xA2 || dataReceive[5] != (byte) 0x25) {
                Log.v(TAG, "6-4 no match");
                return null;
            }
        }
        if (id == 61) {
            if (lengthReceive != 7 || dataReceive[2] != (byte) 0xA1 || dataReceive[5] != (byte) 0x25) {
                Log.v(TAG, "6-5 no match");
                return null;
            }
        }
        if (id == 60) {
            if (lengthReceive != 7 || dataReceive[2] != (byte) 0xA0 || dataReceive[5] != (byte) 0x25) {
                Log.v(TAG, "6-6 no match");
                return null;
            }
        }
        data = new byte[dataReceive.length - 5];
        for (int i = 0; i < dataReceive.length - 6; i++) {
            data[i] = dataReceive[i + 5];
            sum += (byte) data[i];
        }
        Log.v(TAG, "sum = " + Bytes2HexString(new byte[]{sum}, 1));
        if (sum != dataReceive[dataReceive.length - 1]) {//和校验
            Log.v(TAG, "Unpackage sum error");
            return null;
        }
        Log.v(TAG, "Unpackage success");
        return data;
    }

    /**
     * 生成并发送ACK
     *
     * @param
     * @return
     */
    public void sendACK(byte stateID, byte commandID) {
        byte[] commandACK = new byte[7];
        commandACK[0] = (byte) 0xB0; // 命令头
        commandACK[1] = (byte) (4); // 数据长度
        commandACK[2] = stateID;   // 状态ID
        commandACK[3] = (byte) (commandSequenceNum + 1);// 顺序ID
        commandACK[4] = (byte) (0xA2); // 产品标识
        commandACK[5] = commandID; //命令ID
        commandACK[6] = (byte) (commandACK[2] + commandACK[3] + commandACK[4] + commandACK[5]); //和
        Log.v("send ack", Bytes2HexString(commandACK, commandACK.length));
        int tone[] = TransToneData.transDataToTone((TransToneData
                .getDataByOrder((commandACK))));
        SystemClock.sleep(300);
        audio.play(tone);
    }

    /**
     * byte[]转十六进制的大写字符串
     *
     * @param b   待转字符串
     * @param len 长度
     */
    public static String Bytes2HexString(byte[] b, int len) {
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

    /**
     * 将指定字符串src，以每两个字符分割转换为16进制形式
     * 如："2B44EFD9" –> byte[]{0x2B, 0×44, 0xEF, 0xD9}
     *
     * @param src String
     * @return byte[]
     */
    private byte[] HexString2Bytes(String src) {
        byte[] ret = new byte[src.length() / 2];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < tmp.length / 2; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    /**
     * 将两个ASCII字符合成一个字节；
     * 如："EF"--> 0xEF
     *
     * @param src0 byte
     * @param src1 byte
     * @return byte
     */
    private byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0}))
                .byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1}))
                .byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    @Override
    public void msgBytes(byte[] msg) {
        this.receiveOrder = msg;
        Log.v(TAG, "Back Command <---------- " + Bytes2HexString(msg, msg.length));
    }

}
