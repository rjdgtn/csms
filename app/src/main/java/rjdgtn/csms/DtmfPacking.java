package rjdgtn.csms;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Math.ceil;
import static java.lang.Math.min;

public class DtmfPacking {
    private static char[] intToSymbol = {'0', '1', '2', '3', '4', '5', '6', '7', '8'};
    private static int symbolToInt(char ch) {
        switch (ch) {
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            default: return -1;
        }
    }

    public static String[] multipack(byte[] data, int splitSize) {
        int blockNum = (int)ceil(data.length / (float)splitSize);
        String[] res = new String[blockNum+2];
        for (int i = 0; i < blockNum; i ++) {
            byte[] block = Arrays.copyOfRange(data, i * splitSize, min(data.length, (i+1) * splitSize));
            res[i+1] = pack(block);
        }

        res[0] = "*C#";
        res[blockNum+1] = "*D#";
        return res;

    }

    public static String pack(byte[] data) {
        String res = new String("*");
        //String origRes = new String();
        BitSet bitset = BitSet.valueOf(data);

        int checksum = 0;
        int prevVal = 8;
        int symbolsNum = (int)ceil(data.length * 8 / 3.0);
        for (int i = 0; i < symbolsNum; i++) {
            //0-7 - 3 bits
            int val = 0;
            if (bitset.get(i*3))
                val += 1;
            val = val << 1;
            if (bitset.get(i*3+1))
                val += 1;
            val = val << 1;
            if (bitset.get(i*3+2))
                val += 1;

            checksum = checksum ^ val;

            //origRes += intToSymbol[val];

            if (val >= prevVal) val++;

            res += intToSymbol[val];
            prevVal = val;
        }

        if (checksum >= prevVal) checksum++;
        res += intToSymbol[checksum];
        res += '#';

//        Log.d("MY CSMS:", origRes);

        return res;
    }

    public static byte[] unpack(String msg) {
        int overhead = 3;
        if (msg.length() < 3 + overhead) return null;
        if (msg.charAt(0) != '*') return null;
        if (msg.charAt(msg.length()-1) != '#') return null;

        int checkSum = symbolToInt(msg.charAt(msg.length()-2));
        if (checkSum > symbolToInt(msg.charAt(msg.length()-3))) checkSum--;
        int meanBitsNum = ((msg.length() - overhead) * 3 / 8) * 8;

        BitSet res = new BitSet(meanBitsNum);
        int i = 0;
        int prevSym = 8;
        for (int j = 1; j < msg.length()-2; j++) {
            int sym = symbolToInt(msg.charAt(j));
            if (sym == prevSym) {
                return null;
            } else if (sym > prevSym) {
                prevSym = sym;
                --sym;
            } else {
                prevSym = sym;
            }
            checkSum = checkSum ^ sym;
            res.set(i++, (sym & 0b100) > 0);
            if (i >= meanBitsNum) break;
            res.set(i++, (sym & 0b010) > 0);
            if (i >= meanBitsNum) break;
            res.set(i++, (sym & 0b001) > 0);
            if (i >= meanBitsNum) break;
        }

        if (checkSum != 0) return null;

        return res.toByteArray();
    }

    public static byte[] mergeBytesQueue(Queue<byte[]> q) {
        int sz = 0;
        for (byte[] elem : q) {
            sz += elem.length;
        }

        byte[] res = new byte[sz];

        int i = 0;
        for (byte[] elem : q) {
            System.arraycopy(elem, 0, res, i, elem.length);
            i += elem.length;
        }

        return res;
    }
}
