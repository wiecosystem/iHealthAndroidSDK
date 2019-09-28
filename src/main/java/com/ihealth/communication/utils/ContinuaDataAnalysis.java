package com.ihealth.communication.utils;


/**
 * Created by jing on 16/9/4.
 */
public class ContinuaDataAnalysis {
    final private String TAG = this.getClass().getName();
    private byte[] continuaData;
    private int continuaDataLength = 0;
    private int currentPosition = 0;

    public ContinuaDataAnalysis(byte[] tempData, int length) {
        continuaData = tempData;
        continuaDataLength = length;
    }

    public byte read8ByteValue() {
        byte data = 0;
        int length = 1;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = tempBuf[0];

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public short readUInt8Value() {
        short data = 0;
        int length = 1;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = fabsByte(tempBuf[0]);

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public byte readSInt8Value() {
        byte data = 0;
        int length = 1;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = tempBuf[0];

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public short read16ByteValue() {
        short data = 0;
        int length = 2;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = (short) (fabsShort(tempBuf[0]) + tempBuf[1] * 256);

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public int read24ByteValue() {
        int data = 0;
        int length = 3;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = fabsShort(tempBuf[0]) + fabsShort(tempBuf[1]) * 256 + tempBuf[2] * 256 * 256;

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public int read32ByteValue() {
        int data = 0;
        int length = 4;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = fabsShort(tempBuf[0]) + fabsShort(tempBuf[1]) * 256 + fabsShort(tempBuf[2]) * 256 * 256 + tempBuf[3] * 256 * 256 * 256;

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public int readUInt16Value() {
        int data = 0;
        int length = 2;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = fabsShort(tempBuf[0]) + fabsShort(tempBuf[1]) * 256;

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public short readSInt16Value() {
        short data = 0;
        int length = 2;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = (short) (fabsShort(tempBuf[0]) + tempBuf[1] * 256);

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public long readUInt32Value() {
        long data = 0;
        int length = 4;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = fabsInt(tempBuf[0]) + fabsInt(tempBuf[1]) * 256 + fabsInt(tempBuf[2]) * 256 * 256 + fabsInt(tempBuf[3]) * 256 * 256 * 256;

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public int readSInt32Value() {
        int data = 0;
        int length = 4;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = (int) (fabsByte(tempBuf[0]) + fabsByte(tempBuf[1]) * 256 + fabsByte(tempBuf[2]) * 256 * 256 + tempBuf[3] * 256 * 256 * 256);

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }


    public float readSFloatValue() {
        float data = 0;
        int length = 2;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                int exponent = tempBuf[1] >> 4;
                int mantissa = fabsByte(tempBuf[0]) + (tempBuf[1] & 0x0F) * 256;

                data = (float) (mantissa * Math.pow(10, exponent));

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public float readFloatValue() {
        float data = 0;
        int length = 4;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                int exponent = tempBuf[3];
                int mantissa = fabsShort(tempBuf[0]) + fabsShort(tempBuf[1]) * 256 + tempBuf[2] * 256 * 256;

                data = (float) (mantissa * Math.pow(10, exponent));

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    /**
     * Format:yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public String readDateValue() {
        String data = "";
        int length = 7;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);
                int year = fabsShort(tempBuf[0]) + fabsShort(tempBuf[1]) * 256;
                int month = fabsByte(tempBuf[2]);
                int day = fabsByte(tempBuf[3]);
                int hour = fabsByte(tempBuf[4]);
                int min = fabsByte(tempBuf[5]);
                int second = fabsByte(tempBuf[6]);

                data = String.format("%d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, min, second);

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }


        return data;
    }

    public byte readNibbleValue() {
        byte data = 0;
        int length = 1;

        try {
            if ((currentPosition + length - 1) < continuaDataLength) {
                byte[] tempBuf = ByteBufferUtil.bufferCut(continuaData, currentPosition, length);

                data = tempBuf[0];

                currentPosition = currentPosition + length;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return data;
    }

    public short fabsByte(byte octet) {
        if (octet < 0) {
            return (short) (octet & 0xFF);
        } else {
            return octet;
        }
    }

    public int fabsShort(byte octet) {
        if (octet < 0) {
            return (int) (octet & 0xFF);
        } else {
            return octet;
        }
    }

    public long fabsInt(byte octet) {
        if (octet < 0) {
            return (long) (octet & 0xFF);
        } else {
            return octet;
        }
    }

}
