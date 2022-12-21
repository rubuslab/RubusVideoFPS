package com.example.rubusvideofps;

import android.util.Log;

import java.util.Vector;

public class ITF25ProgressBarsDecoder {
    class Bar {
        public int width = 0;
        public int pos = 0;
        public boolean isBlack = false;
        public byte code = 0;  // 0: small bar, 1: big bar
        public void Reset() { width = 0; isBlack = false; pos = 0;}
    };

    private String TAG = "Interleaved25BarProgress";

    private Vector<Bar> mBars;
    private int mValidBars = 0;
    private float mStarterSmallWhiteSpaceBarWidth = 0.1f;
    private float mEnderSmallWhiteSpaceBarWidth = 0.1f;

    public int mPreviewImageShortSideLength = 0;
    public short[] mPreviewLineBuffer = null;  // java不支持无符号类型

    public void Initialize(int previewShortSideLength) {
        mPreviewImageShortSideLength = previewShortSideLength;
        mPreviewLineBuffer = new short[mPreviewImageShortSideLength];

        int maxBars = mPreviewImageShortSideLength / 2;
        mBars = new Vector<Bar>();
        for (int i = 0; i < maxBars; ++ i) {
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
        // 0-1
        if (!b0.isBlack) return false;
        int whiteWidth = b1.width + b3.width + b5.width;
        float avgSmallSpaceBarWidth = whiteWidth / 3.0f;
        // is 3 white bar's width is equal ?
        float w1 = Math.abs(b1.width / avgSmallSpaceBarWidth - 1.0f); if (w1 > 0.3) return false;  // width / avg_width belong[0.8, 1.2]
        float w2 = Math.abs(b3.width / avgSmallSpaceBarWidth - 1.0f); if (w2 > 0.3) return false;
        float w3 = Math.abs(b5.width / avgSmallSpaceBarWidth - 1.0f); if (w3 > 0.3) return false;

        // 设窄条宽为x, 宽条为2x, M = sum_width(7 bars)
        // 5x + 2x + 2x = M, x = M / 9 = 11M / 99
        // 设窄条宽x, 宽条为3x
        // 5x + 3x + 3x = M, x = M / 11 = 9M / 99
        // small       - big
        // (x) 11M/99, - 22M/99 (2x)
        // (x)  9M/99, - 27M/99 (3x)
        // medianWidth = 18M/99, (9,11 - 22,27)/99, greater than medianWidth is big bar, else is small bar
        float M = b0.width + b1.width + b2.width + b3.width + b4.width + b5.width + b6.width;
        float medianWidth = M * 18.0f / 99.0f;
        Log.e(TAG, String.format("median width: %5.3f", medianWidth));
        b0.code = (byte)(b0.width > medianWidth ? 1 : 0); if (b0.code != 0) return false;  // 0: small bar, 1: big bar
        b2.code = (byte)(b2.width > medianWidth ? 1 : 0); if (b2.code != 1) return false;
        b4.code = (byte)(b4.width > medianWidth ? 1 : 0);
        b6.code = (byte)(b6.width > medianWidth ? 1 : 0);
        boolean ok;
        if (isStartBarsGroup) {  // start indicator: 0-1-1-0
            ok = (b0.code == 0 && b2.code == 1 && b4.code == 1 && b6.code == 0);
        } else {  // end indicator: 0-1-0-1
            ok = (b0.code == 0 && b2.code == 1 && b4.code == 0 && b6.code == 1);
        }

        if (ok && isStartBarsGroup) mStarterSmallWhiteSpaceBarWidth = avgSmallSpaceBarWidth;
        if (ok && !isStartBarsGroup) mEnderSmallWhiteSpaceBarWidth = avgSmallSpaceBarWidth;
        return ok;
    }

    public int DecodeAndGetPos() {
        InitBars();

        int progressPos = -1;
        // at least 19 bars.
        if (mValidBars < 19) return progressPos;

        // try find ITFProgressBar star and end indicators' index
        boolean foundStarter = false;
        boolean foundEnder = false;
        int starterRightIndex = 0;
        int enderLeftIndex = 0;
        int maxEndPos = mValidBars - 7 - 1;  // must have 1 white space bar on right
        for (int i = 0; i <= maxEndPos && maxEndPos > 7; ++i) {
            if (!foundStarter) {
                if (FindIndicatorBarsGroup(mBars.get(i), mBars.get(i + 1), mBars.get(i + 2), mBars.get(i + 3),
                        mBars.get(i + 4), mBars.get(i + 5), mBars.get(i + 6), true)) {
                    starterRightIndex = i + 6;
                    // skip left starter, white space bar, pos black bar
                    i += 9;
                    foundStarter = true;
                }
            }
            if (foundStarter && !foundEnder) {
                if (FindIndicatorBarsGroup(mBars.get(i), mBars.get(i + 1), mBars.get(i + 2), mBars.get(i + 3),
                        mBars.get(i + 4), mBars.get(i + 5), mBars.get(i + 6), false)) {
                    enderLeftIndex = i;
                    foundEnder =true;
                    break;
                }
            }
        }

        if (!foundStarter || !foundEnder) return progressPos;

        // try find pos
        int posRightIndex = enderLeftIndex - 2;
        int posLeftIndex = starterRightIndex + 2;
        for (int posIndex = posRightIndex; posIndex >= posLeftIndex; --posIndex) {
            Bar bar = mBars.get(posIndex);
            if (bar.isBlack) {
                // found pos bar
                // 统计进度块左边所有Bars的宽度，换算成白色小条Bar，计算进度条Pos值。
                int posLeftBarsWidth = 0;
                for (int i = posIndex - 1; i > starterRightIndex; i--) {
                    posLeftBarsWidth += mBars.elementAt(i).width;
                }

                int posRightBarsWidth = 0;
                for (int i = posIndex + 1; i < enderLeftIndex; i++) {
                    posRightBarsWidth += mBars.elementAt(i).width;
                }

                float avgSmallBarWidth = (mStarterSmallWhiteSpaceBarWidth + mEnderSmallWhiteSpaceBarWidth) / 2.0f;
                float leftEquivalentSmallBars = posLeftBarsWidth / avgSmallBarWidth;
                float rightEquivalentSmallBars = posRightBarsWidth / avgSmallBarWidth;

                int roundEquivalentLeftSmallBars = Math.round(leftEquivalentSmallBars);
                int roundEquivalentRightSmallBars = Math.round(rightEquivalentSmallBars);
                int posValue = roundEquivalentLeftSmallBars - 1;  // skip first white small bar
                // progressPos = posValue >= 0 ? posValue : 0;
                progressPos = posValue;

                float wholeProgressBarWidth = (float)(posLeftBarsWidth + posRightBarsWidth - 2.0f * avgSmallBarWidth);
                float posPercent = (posLeftBarsWidth - avgSmallBarWidth) / wholeProgressBarWidth;
                float equivalentAllBars = wholeProgressBarWidth / avgSmallBarWidth;
                float equivalentLeftBars = posPercent * equivalentAllBars;
                Log.e(TAG, String.format("posPercent: %5.3f, wholeProgressBarWidth: %5.3f, equivalentAllBars: %5.3f, equivalentLeftBars: %5.3f",
                        posPercent, wholeProgressBarWidth, equivalentAllBars, equivalentLeftBars));
                progressPos = Math.round(equivalentLeftBars);
                // ----------
                Log.e(TAG, String.format("avgSmallBarWidth: %5.3f, posLeftBarsWidth: %d, posRightBarsWidth: %d, " +
                                "roundEquivalentLeftSmallBars: %d, roundEquivalentRightSmallBars: %d",
                        avgSmallBarWidth, posLeftBarsWidth, posRightBarsWidth, roundEquivalentLeftSmallBars, roundEquivalentRightSmallBars));
                Log.e(TAG, String.format("posValue: %d, posPercent: %5.3f, round pos percent: %d",
                        posValue, posPercent, Math.round(posPercent)));
                break;
            }
        }
        return progressPos;
    }
}
