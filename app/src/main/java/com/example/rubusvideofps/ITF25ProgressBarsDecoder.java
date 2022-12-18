package com.example.rubusvideofps;

import java.util.Vector;

public class ITF25ProgressBarsDecoder {
    class Bar {
        public int width = 0;
        public int pos = 0;
        public boolean isBlack = false;
        public byte code = 0;  // 0: small bar, 1: big bar
        public void Reset() { width = 0; isBlack = false; pos = 0;}
    };

    private String TAG = "Interleaved25Bar";

    private Bar mBlackBars[];
    private Bar mWhiteBars[];

    private Vector<Bar> mBars;
    private int mValidBars = 0;

    public int mPreviewImageShortSideLength = 0;
    public short[] mPreviewLineBuffer = null;  // java不支持无符号类型

    public void Initialize(int previewShortSideLength) {
        mPreviewImageShortSideLength = previewShortSideLength;
        mPreviewLineBuffer = new short[mPreviewImageShortSideLength];

        int maxBars = mPreviewImageShortSideLength / 2;
        mBlackBars = new Bar[maxBars];
        mWhiteBars = new Bar[maxBars];
        mBars = new Vector<Bar>();
        for (int i = 0; i < mBlackBars.length; ++ i) {
            mBlackBars[i] = new Bar();
            mWhiteBars[i] = new Bar();
            mBars.add(new Bar());
        }
    }

    private void InitBars() {
        mValidBars = 0;
        short lastColor = 0;
        int barCount = 0;
        Bar bar = new Bar();
        for (int i = 0; i < mPreviewImageShortSideLength; ++i) {
            short y = mPreviewLineBuffer[i];
            // 到最后一个像素，强制结束
            if (i == mPreviewImageShortSideLength - 1) y = (short)(255 - lastColor);
            if (i == 0) lastColor = y;

            if (y == lastColor) {
                bar.width++;
            } else {  // y != lastColor
                // color not the same with last color, end find a new bar
                Bar el = mBars.elementAt(barCount);
                el.width = bar.width;
                el.pos = i - 1;
                el.isBlack = lastColor == 0;
                bar.width = 1;
                lastColor = y;
                barCount++;
            }
        }
        mValidBars = barCount;
    }

    // isStartBarsGroup: true, try check is start indicator bars group.
    // isStartBarsGroup: false, try check is end indicator bars group.
    private boolean FindIndicatorBarsGroup(Bar b0, Bar b1, Bar b2, Bar b3, Bar b4, Bar b5, Bar b6, boolean isStartBarsGroup) {
        if (b0.isBlack == false) return false;
        int whiteWidth = b1.width + b3.width + b5.width;
        float avg = whiteWidth / 3.0f;
        // is 3 white bar's width is equal ?
        float w1 = Math.abs(b1.width / avg - 1.0f); if (w1 > 0.1) return false;  // width / avg_width belong[0.9, 1.1]
        float w2 = Math.abs(b3.width / avg - 1.0f); if (w2 > 0.1) return false;
        float w3 = Math.abs(b5.width / avg - 1.0f); if (w3 > 0.1) return false;

        // 设窄条宽为x, 宽条为2x, M = sum_width(7 bars)
        // 5x + 2x + 2x = M, x = M / 9 = 11M / 99
        // 设窄条宽x, 宽条为3x
        // 5x + 3x + 3x = M, x = M / 11 = 9M / 99
        // small       - big
        // (x) 11M/99, - 22M/99 (2x)
        // (x)  9M/99, - 27M/99 (3x)
        // medianWidth = 16M/99, (9,11 - 22,27)/99, greater than medianWidth is big bar, else is small bar
        float M = b0.width + b1.width + b2.width + b3.width + b4.width + b5.width + b6.width;
        float medianWidth = M * 16.0f / 99.0f;
        b0.code = (byte)(b0.width > medianWidth ? 1 : 0);  // 0: small bar, 1: big bar
        b2.code = (byte)(b2.width > medianWidth ? 1 : 0);
        b4.code = (byte)(b4.width > medianWidth ? 1 : 0);
        b6.code = (byte)(b6.width > medianWidth ? 1 : 0);
        boolean ok;
        if (isStartBarsGroup) {  // start indicator: 0-1-1-0
            ok = (b0.code == 0 && b2.code == 1 && b4.code == 1 && b6.code == 0);
        } else {  // end indicator: 0-1-0-1
            ok = (b0.code == 0 && b2.code == 1 && b4.code == 0 && b6.code == 1);
        }
        return ok;
    }

    public String Decode() {
        InitBars();
        boolean foundStarter = false;
        for (int i = 0; i < mValidBars; ++i) {
            if (!foundStarter) {}
        }
        // ----------------------
        String code = "";
        short lastColor = 255;
        int lastBlackBarIndex = -1;
        int lastWhiteBarIndex = -1;
        boolean findBarStart = false;
        Interleaved25Bar.Bar bar = new Interleaved25Bar.Bar();

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
