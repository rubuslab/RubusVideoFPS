package com.example.rubusvideofps;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.PixelFormat;
import android.graphics.ImageFormat;
import android.view.View;

import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private Camera mCamera = null;     // Camera对象，相机预览

    private String TAG = "main activity";
    private int mPreviewWidth = 260;
    private int mPreviewHeight = 462;

    private Boolean mInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void Init() {
        if (mInit) return;
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_video);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceView.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {}
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                stopPreview();
            }
        });
        mInit = true;
    }

    public void onStartPreview(View view) {
        Init();
        stopPreview();

        //打开摄像头，并且旋转90度
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        //Camera预览的数据回调：
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                // bytes即为相机采集出来的单帧Yuv格式数据，可转为Bitmap等格式使用
                Log.i(TAG, "onPreviewFrame");
            }
        });

        //相机的一些设置：
        Camera.Parameters parameters = mCamera.getParameters();
        //Camera Preview Callback的YUV420常用数据格式有两种：一个是NV21，一个是YV12。Android一般默认使用YUV_420_SP的格式（NV21）
        parameters.setPreviewFormat(ImageFormat.NV21);//设置回调数据的格式
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight); //对应手机的height和width
        mCamera.setParameters(parameters); //传入参数
        try {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_video);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();

            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onStopPreview(View view) {
        stopPreview();
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    // ----------- video preview -----------
    private void initCamera()//surfaceChanged中调用
    {
        Log.i(TAG, "going into initCamera");
        //if (mbIfPreview)
        //{
        //    mCamera.stopPreview();//stopCamera();
        //}
        if(null != mCamera)
        {
            try
            {
                /* Camera Service settings*/
                Camera.Parameters parameters = mCamera.getParameters();
                // parameters.setFlashMode("off"); // 无闪光灯
                parameters.setPictureFormat(PixelFormat.JPEG); //Sets the image format for picture 设定相片格式为JPEG，默认为NV21
                parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP); //Sets the image format for preview picture，默认为NV21
                /*【ImageFormat】JPEG/NV16(YCrCb format，used for Video)/NV21(YCrCb format，used for Image)/RGB_565/YUY2/YU12*/
                // 【调试】获取caera支持的PictrueSize，看看能否设置？？
                List<Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
                List<Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
                List<Integer> previewFrameRates = mCamera.getParameters().getSupportedPreviewFrameRates();
                Log.i(TAG+"initCamera", "cyy support parameters is ");
                Size psize = null;
                for (int i = 0; i < pictureSizes.size(); i++)
                {
                    psize = pictureSizes.get(i);
                    Log.i(TAG+"initCamera", "PictrueSize,width: " + psize.width + " height" + psize.height);
                }
                for (int i = 0; i < previewSizes.size(); i++)
                {
                    psize = previewSizes.get(i);
                    Log.i(TAG+"initCamera", "PreviewSize,width: " + psize.width + " height" + psize.height);
                }
                Integer pf = null;
                for (int i = 0; i < previewFormats.size(); i++)
                {
                    pf = previewFormats.get(i);
                    Log.i(TAG+"initCamera", "previewformates:" + pf);
                }
                // 设置拍照和预览图片大小
                parameters.setPictureSize(640, 480); //指定拍照图片的大小
                parameters.setPreviewSize(mPreviewWidth, mPreviewHeight); // 指定preview的大小
                //这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错
                // 横竖屏镜头自动调整
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
                {
                    parameters.set("orientation", "portrait"); //
                    parameters.set("rotation", 90); // 镜头角度转90度（默认摄像头是横拍）
                    mCamera.setDisplayOrientation(90); // 在2.2以上可以使用
                } else// 如果是横屏
                {
                    parameters.set("orientation", "landscape"); //
                    mCamera.setDisplayOrientation(0); // 在2.2以上可以使用
                }
                /* 视频流编码处理 */
                //添加对视频流处理函数
                // 设定配置参数并开启预览
                mCamera.setParameters(parameters); // 将Camera.Parameters设定予Camera
                mCamera.startPreview(); // 打开预览画面
                // mbIfPreview = true;
                // 【调试】设置后的图片大小和预览大小以及帧率
                Camera.Size csize = mCamera.getParameters().getPreviewSize();
                mPreviewHeight = csize.height; //
                mPreviewWidth = csize.width;
                Log.i(TAG+"initCamera", "after setting, previewSize:width: " + csize.width + " height: " + csize.height);
                csize = mCamera.getParameters().getPictureSize();
                Log.i(TAG+"initCamera", "after setting, pictruesize:width: " + csize.width + " height: " + csize.height);
                Log.i(TAG+"initCamera", "after setting, previewformate is " + mCamera.getParameters().getPreviewFormat());
                Log.i(TAG+"initCamera", "after setting, previewframetate is " + mCamera.getParameters().getPreviewFrameRate());
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public Camera open() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            return Camera.open(i);
        }
        return null;
    }
}