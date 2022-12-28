package com.rubus.videofps;

import android.util.Log;
import java.util.Vector;

public class Interleaved25Bar {
    class Bar {
        public int width = 0;
    };

    private String TAG = "Interleaved25Bar";

    private Bar mBlackBars[];
    private Bar mWhiteBars[];

    public int mPreviewImageShortSideLength = 0;
    public short[] mPreviewLineBuffer = null;  // java不支持无符号类型

    public void Initialize(int previewShortSideLength) {
        mPreviewImageShortSideLength = previewShortSideLength;
        mPreviewLineBuffer = new short[mPreviewImageShortSideLength];

        mBlackBars = new Bar[mPreviewImageShortSideLength / 2];
        mWhiteBars = new Bar[mPreviewImageShortSideLength / 2];
        for (int i = 0; i < mBlackBars.length; ++ i) {
            mBlackBars[i] = new Bar();
            mWhiteBars[i] = new Bar();
        }
    }

    public String Decode() {
        String code = "";
        short lastColor = 255;
        int lastBlackBarIndex = -1;
        int lastWhiteBarIndex = -1;
        boolean findBarStart = false;
        Bar bar = new Bar();

        for (int i = 0; i < mPreviewImageShortSideLength; ++i) {
            short y = mPreviewLineBuffer[i];

            // 到最后一个像素，强制结束
            if (i == mPreviewImageShortSideLength - 1) y = (short)(255 - lastColor);

            if (y == lastColor) {
                bar.width++;
            } else {  // y != lastColor
                // color not the same with last color
                if (!findBarStart) {
                    findBarStart = true;
                    bar.width = 1;
                    lastColor = y;
                    continue;
                }



                if (lastColor == 0) { // black bar
                    lastBlackBarIndex++;
                    mBlackBars[lastBlackBarIndex].width = bar.width;
                } else { // 255, white bar
                    lastWhiteBarIndex++;
                    mWhiteBars[lastWhiteBarIndex].width = bar.width;
                }

                bar.width = 1;
                lastColor = y;
            }
        }

        if (lastBlackBarIndex == -1 || lastWhiteBarIndex == -1 || lastWhiteBarIndex != lastBlackBarIndex) {
            // Log.e(TAG, "Decode error.");
            return code;
        }

        // drop last empty white bar
        lastWhiteBarIndex--;

        // 偶数个字符，4起始条 + .. + 结束条，黑色条至少 2 + 5 + 2 = 9, 白色条至少 2 + 5 + 1 = 8
        if (!(lastBlackBarIndex >= 8 && lastWhiteBarIndex >= 7)) {
            // Log.e(TAG, "Decode failed");
            return code;
        }

        // decode
        // https://www.laivz.com/barcode-generate/interleaved-25-code-generator/#:~:text=%E5%8F%82%E8%80%83%E8%AF%91%E7%A0%81%E7%AE%97%E6%B3%95
        int blackCharsBars = lastBlackBarIndex + 1 - 2 - 2; // -2 -2 去掉左右结束符号
        int whiteCharsBars = lastWhiteBarIndex + 1 - 2 - 1; // -2 -1 去掉左右结束符
        if (blackCharsBars != whiteCharsBars || blackCharsBars % 5 != 0) {
            // Log.i(TAG, "back chars bars: " + blackCharsBars + ", white chars bars: " + whiteCharsBars);
            // Log.e(TAG, "black chars bars not equal to white chars bars or bars not equal to 5x.");
            return code;
        }

        int charPairs = blackCharsBars / 5;
        int blackBarStartIndex = 2;
        int whiteBarStartIndex = 2;
        String blackBinCodes = "";
        String whiteBinCodes = "";
        for (int pair = 0; pair < charPairs; ++pair) {
            // decode pair chars
            int blackBarIndex = blackBarStartIndex + pair * 5;
            int whiteBarIndex = whiteBarStartIndex + pair * 5;
            int s = 0;
            for (int i = 0; i < 5; ++i) {
                s += mBlackBars[blackBarIndex + i].width;
                s += mWhiteBars[whiteBarIndex + i].width;
            }
            int t = (int)((float)s * 7.0 / 64.0);

            blackBinCodes = "";
            whiteBinCodes = "";
            for (int i = 0; i < 5; ++i) {
                blackBinCodes += mBlackBars[blackBarIndex + i].width > t ? "1" : "0";
                whiteBinCodes += mWhiteBars[whiteBarIndex + i].width > t ? "1" : "0";
            }
            code += FindCodeFromBinCodes(blackBinCodes);
            code += FindCodeFromBinCodes(whiteBinCodes);
        }

        return code;
    }

    private String FindCodeFromBinCodes(String binCodes) {
        if (binCodes.equals("00110")) return "0";
        if (binCodes.equals("10001")) return "1";
        if (binCodes.equals("01001")) return "2";
        if (binCodes.equals("11000")) return "3";
        if (binCodes.equals("00101")) return "4";

        if (binCodes.equals("10100")) return "5";
        if (binCodes.equals("01100")) return "6";
        if (binCodes.equals("00011")) return "7";
        if (binCodes.equals("10010")) return "8";
        if (binCodes.equals("01010")) return "9";

        // Log.e(TAG, "Decode from bin codes error, can not find digital, bin codes: " + binCodes);
        return "x";
    }
}