package com.example.rubusvideofps;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ShowLineView extends View {
    private Paint mPaint;    ///< 画笔

    private byte[] mYUV_yBuffer = null; // NV21-Y buffer
    private int mYStartIndex = 0;
    private int mYImageWidth = 0;

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
        mPaint.setColor(Color.parseColor("#F50808"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // super.onDraw(canvas);
        ///< 2.进行绘制

        // draw one gray line in YUV image
        if (mYUV_yBuffer != null && mYImageWidth > 0) {
            // start draw line
            Paint paint = new Paint();
            // paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < mYImageWidth; ++i) {
                int gray = mYUV_yBuffer[mYStartIndex + i] + 16;
                paint.setARGB(255, gray, gray, gray);

                canvas.drawPoint(i * 2, 10, paint);
                canvas.drawPoint(i * 2 + 1, 10, paint);
            }
            // end draw line
            mYUV_yBuffer = null;
            mYImageWidth = 0;
        }

        canvas.drawCircle(10,10, 5, mPaint);
    }

    public void DrawYUVImageLineData(byte[] yData, int yStartIndex, int yImageWidth) {
        mYUV_yBuffer = yData;
        mYStartIndex = yStartIndex;
        mYImageWidth = yImageWidth;
        super.invalidate();
    }
}
