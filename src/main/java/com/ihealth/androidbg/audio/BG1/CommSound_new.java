package com.ihealth.androidbg.audio.BG1;

import android.os.SystemClock;

import com.ihealth.communication.utils.Log;

import com.ihealth.androidbg.audio.AudioTrackManager;
import com.ihealth.androidbg.audio.TransToneData;
import com.ihealth.androidbg.audio.TunnerThread;

/**
 * 这个类完成封装数据，发送数据，接收数据，解包数据
 */
public class CommSound_new implements BG1_Command_Interface {

    private static final String TAG = CommSound_new.class.getSimpleName();
    public AudioTrackManager audio;
    private int commandSequenceNum;  // 全局统一的命令顺序号
    private byte[] receiveOrder;

    public CommSound_new(TunnerThread tunner) {
        audio = new AudioTrackManager();
        audio.initManager();
        commandSequenceNum = 0;//顺序ID
        tunner.msgSubject.detach(this);
        tunner.msgSubject.attach(this);
    }

    /**
     * TODO 封装需要发送的数据
     *
     * @param stateID 状态ID，
     * @param data    需要发送的数据
     * @return
     */
    private byte[] packageCommand(byte stateID, byte[] data) {
        if (data != null) {
            byte[] commandSend = new byte[data.length + 3];
            commandSend[1] = stateID; // 状态ID
            commandSend[2] = (byte) (commandSequenceNum); // 顺序ID
            commandSequenceNum += 2;
            int tempTotal = 0; // 校验和
            tempTotal = commandSend[1] + commandSend[2];
            for (int j = 0; j < data.length; j++) {
                commandSend[j + 3] = data[j];
                tempTotal += commandSend[j + 3];
            }
            byte total = (byte) ((byte) ((tempTotal & 0xF0) >> 4) + (byte) (tempTotal & 0x0F));
            commandSend[0] = (byte) ((byte) (total << 4) + (byte) (data.length & 0x0F));
            return commandSend;
        } else {
            return null;
        }
    }

    /*
    * 1305+补偿信号
    * AudioTrack初始化时调用一次，可优化第一次下行信号，避免下位机重发
    * S7 & MI4
    * 20160602_zhao
    * */
    public void sendCommand() {
        int tone[] = new int[]{1000, 1000, 1000, 1000};
        audio.play(tone);
    }

    /**
     * 将封装好的指令发送出去(5遍重发)
     *
     * @param stateID 状态ID
     * @param command 需要发送的数据
     * @param id      指令标识，用于接收预期结果
     */
    public byte[] sendCommand(byte stateID, byte[] command, int id) {
        int count = 5;//重发次数
        byte[] myCommand = packageCommand(stateID, command);
        byte[] recData = null;
        int[] tone = TransToneData.transDataToTone((TransToneData.getDataByOrder(myCommand)));
        int timer_current = (int) (((myCommand.length + 1) * 11.6 + 440.8) * 1.5);//750;//(42 * 11.6 * 1.5)
        int timer = timer_current;
        if (myCommand.length != 0) {
            while (AudioTrackManager.inCommunication && count-- != 0) {
                Log.v(TAG, "------------------------------------------------------ >");
                Log.v(TAG, "| sendCommand ----------> " + Bytes2HexString(myCommand, myCommand.length));
                Log.v(TAG, "------------------------------------------------------ >");
                audio.play(tone);
                timer = timer_current;
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
     * TODO 接收声音指令
     */
    public byte[] receiveCommand(int id) {
        if (receiveOrder != null && receiveOrder.length > 3) {
            byte[] data = unPackageCommand(receiveOrder, id);//返回
            receiveOrder = null;//取完数以后－－把接收数组初始化
            return data;
        } else {
            return null;
        }
    }

    private static final int ID_APPId = 2;// APP Id -- > 返回idps信息
    private static final int ID_BottleId = 3;// bottle Id 和 固化信息版本 -- > 返回需要发送的code情况
    private static final int ID_IDEN_31 = 42;// 认证第一包
    private static final int ID_IDEN_32 = 41;// 认证第二包
    private static final int ID_IDEN_33 = 40;// 认证第三包
    private static final int ID_Code_31 = 52;// 3-1
    private static final int ID_Code_32 = 51;// 3-2
    private static final int ID_Code_33 = 50;// 3-3
    private static final int ID_Code_71 = 66;// 7-1
    private static final int ID_Code_72 = 65;// 7-2
    private static final int ID_Code_73 = 64;// 7-3
    private static final int ID_Code_74 = 63;// 7-4
    private static final int ID_Code_75 = 62;// 7-5
    private static final int ID_Code_76 = 61;// 7-6
    private static final int ID_Code_77 = 60;// 7-7


    /**
     * TODO 将接收到的指令解包
     *
     * @param receiveCommand 指令
     * @param id             指令标识
     */
    public byte[] unPackageCommand(byte[] receiveCommand, int id) {

        if (receiveCommand == null) {
            Log.v(TAG, "command null");
            return null;
        }
        Log.v(TAG, "Unpackage start = " + Bytes2HexString(receiveCommand, receiveCommand.length));
        int lengthReceive = receiveCommand.length;
        if (lengthReceive < 3) {
            return null;
        }

        byte[] dataReceive = receiveCommand;

        int lenPackage = dataReceive[0] & 0x0F;
        if (lenPackage + 3 != lengthReceive) {//长度错误
            Log.v(TAG, "Unpackage length error");
            if (!AudioTrackManager.isR2017) {
                return null;
            }
        }
        switch (id) {
            case ID_APPId:
                if (lengthReceive != 13 || dataReceive[3] != (byte) 0xD0) {
                    Log.v(TAG, "APP Id back error");
                    if (!AudioTrackManager.isR2017) {
                        return null;
                    }
                }
                break;
            case ID_BottleId:
                if (lengthReceive != 4) {
                    Log.v(TAG, "bottle Id back error");
                    return null;
                }
                break;
            case ID_IDEN_31:
                if (lengthReceive != 18 || dataReceive[3] != (byte) 0xFB) {
                    Log.v(TAG, "iden-1 back error");
                    return null;
                }
                break;

            case ID_IDEN_32:
                if (lengthReceive != 13) {
                    Log.v(TAG, "iden-2 back error");
                    return null;
                }
                break;

            case ID_IDEN_33:
                if (lengthReceive != 4 || (dataReceive[3] != (byte) 0xFD && dataReceive[3] != (byte) 0xFE)) {
                    Log.v(TAG, "iden-3 back error");
                    return null;
                } else {
                    Log.v(TAG, "iden success ------");
                }
                break;

            case ID_Code_31:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x50) {
                    Log.v(TAG, "3-1 back error");
                    return null;
                }
                break;
            case ID_Code_32:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x50) {
                    Log.v(TAG, "3-2 back error");
                    return null;
                }
                break;
            case ID_Code_33:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x50) {
                    Log.v(TAG, "3-3 back error");
                    return null;
                }
                break;
            case ID_Code_71:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x25) {
                    Log.v(TAG, "7-1 back error");
                    return null;
                }
                break;
            case ID_Code_72:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x25) {
                    Log.v(TAG, "7-2 back error");
                    return null;
                }
                break;
            case ID_Code_73:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x25) {
                    Log.v(TAG, "7-3 back error");
                    return null;
                }
                break;
            case ID_Code_74:
                if (lengthReceive != 4) {//|| dataReceive[3] != (byte) 0x25
                    Log.v(TAG, "7-4 back error");
                    if (!AudioTrackManager.isR2017) {
                        return null;
                    }
                }
                break;
            case ID_Code_75:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x25) {
                    Log.v(TAG, "7-5 back error");
                    return null;
                }
                break;
            case ID_Code_76:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x25) {
                    Log.v(TAG, "7-6 back error");
                    if (!AudioTrackManager.isR2017) {
                        return null;
                    }
                }
                break;
            case ID_Code_77:
                if (lengthReceive != 4 || dataReceive[3] != (byte) 0x25) {
                    Log.v(TAG, "7-7 back error");
                    return null;
                }
                break;
        }

        byte tempSum = (byte) ((dataReceive[0] >> 4) & 0x0F);
        byte total = 0;
        total = (byte) (dataReceive[1] + dataReceive[2]);
        byte[] data = new byte[dataReceive.length - 3];
        for (int i = 0; i < dataReceive.length - 3; i++) {
            data[i] = dataReceive[i + 3];
            total += (byte) data[i];
        }
        Log.v(TAG, "total = " + Bytes2HexString(new byte[]{total}, 1));
        byte sum = (byte) ((byte) ((byte) ((total & 0xF0) >> 4) + (byte) (total & 0x0F)) & 0x0F);
        Log.v(TAG, "sum = " + Bytes2HexString(new byte[]{sum}, 1));
        if (sum != tempSum) {// 和校验
            Log.v(TAG, "Unpackage sum error id = " + id);
            if (!AudioTrackManager.isR2017) {
                return null;
            }
        }
        Log.v(TAG, "Unpackage success");
        commandSequenceNum = receiveCommand[2] + 1;//赋值 顺序ID
        return data;
    }

    /**
     * 生成并发送ACK
     *
     * @param
     * @return
     */
    public void sendACK(byte stateID, byte commandID) {
        byte[] commandACK = new byte[4];
        commandACK[1] = stateID;   // 状态ID
        commandACK[2] = (byte) (commandSequenceNum + 1);// 顺序ID
        commandACK[3] = commandID; //命令ID
        byte tempTotal = (byte) (commandACK[1] + commandACK[2] + commandACK[3]); //和
        byte total = (byte) ((byte) ((tempTotal & 0xF0) >> 4) + (byte) (tempTotal & 0x0F));
        commandACK[0] = (byte) ((byte) (total << 4) + (byte) (1 & 0x0F));// 长度／和校验
        Log.v(TAG, "*********************************************************** ");
        Log.v(TAG, "* send ack ----------> " + Bytes2HexString(commandACK, commandACK.length));
        Log.v(TAG, "*********************************************************** ");
        int[] tone = null;
        tone = TransToneData.transDataToTone((TransToneData.getDataByOrder((commandACK))));
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
