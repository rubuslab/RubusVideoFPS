package com.example.rubusvideofps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ShowLineView extends View {
    String TAG = "ShowLineView";
    private Paint mPaint;    ///< 画笔

    private int mBarPosToBorder = 20;

    int mPreviewImageShortSideLength = 0;
    Interleaved25Bar m25Bar;
    ITF25ProgressBarsDecoder mItf25ProgressBar;

    // 在java代码里new的时候会用到
    // @param context
    public ShowLineView(Context context) { super(context); onShowLineViewInit(); }

    // 在xml布局文件中使用时自动调用
    // @param context
    public ShowLineView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); onShowLineViewInit(); }

    // 不会自动调用，如果有默认style时，在第二个构造函数中调用
    // @param context
    // @param attrs
    // @param defStyleAttr
    public ShowLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); onShowLineViewInit(); }

    // 只有在API版本>21时才会用到
    // 不会自动调用，如果有默认style时，在第二个构造函数中调用
    // @param context
    // @param attrs
    // @param defStyleAttr
    // @param defStyleRes
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ShowLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) { super(context, attrs, defStyleAttr, defStyleRes); onShowLineViewInit(); }

    private void onShowLineViewInit() {
        mPaint = new Paint();
        m25Bar = new Interleaved25Bar();
        mItf25ProgressBar = new ITF25ProgressBarsDecoder();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // super.onDraw(canvas);
        ///< 2.进行绘制

        // draw one gray line in YUV image
        if (m25Bar.mPreviewLineBuffer != null && m25Bar.mPreviewImageShortSideLength > 0) {
            // start draw line
            for (int i = 0; i < m25Bar.mPreviewImageShortSideLength; ++i) {
                int gray = m25Bar.mPreviewLineBuffer[i];
                mPaint.setARGB(255, gray, gray, gray);

                //canvas.drawPoint(i * 2, 10, mPaint);
                //canvas.drawPoint(i * 2 + 1, 10, mPaint);

                canvas.drawLine(i * 2, 10, i * 2, 110, mPaint);
                canvas.drawLine(i * 2 + 1, 10, i * 2 + 1, 110, mPaint);
            }
        }

        mPaint.setColor(Color.parseColor("#F50808"));
        canvas.drawCircle(5,5, 5, mPaint);
    }

    // set preview image short side length
    public void SetPreviewShortSideLength(int len) {
        mPreviewImageShortSideLength = len;
        mItf25ProgressBar.Initialize(len);
        m25Bar.Initialize(len);
    }

    private void UpdateYUV(byte[] yData, int yImageWidth, int yImageHeight, short[] previewLineBuff) {
        int count = 0;
        for (int h = 0; h < mPreviewImageShortSideLength; ++h) {
            int index = h * yImageWidth + mBarPosToBorder;
            short y = (short)(yData[index] & 0xFF);
            count += y;
            previewLineBuff[mPreviewImageShortSideLength - h - 1] = y;
        }

        // 平均值法二值化
        short avg = (short)(count / mPreviewImageShortSideLength);
        avg = avg > 128 ? 128 : avg;

        // String s ="";
        for (int i = 0; i < mPreviewImageShortSideLength; ++i) {
            short v = previewLineBuff[i];
            short gray = (short)(v >= avg ? 255 : 0);  // 0: black, 255: white
            // s += gray == 0 ? "|" : "-";
            previewLineBuff[i] = gray;
        }
        // Log.i(TAG, "line: " + s);
    }

    public void UpdateYUVImageData(byte[] yData, int yImageWidth, int yImageHeight) {
        UpdateYUV(yData, yImageWidth, yImageHeight, m25Bar.mPreviewLineBuffer);
        UpdateYUV(yData, yImageWidth, yImageHeight, mItf25ProgressBar.mPreviewLineBuffer);
    }

    public int GetProgressBarPos() { return mItf25ProgressBar.DecodeAndGetPos(); }

    public String GetBar25Code() {
        return m25Bar.Decode();
    }

    public void InvalidateView() {
        super.invalidate();
    }
}

