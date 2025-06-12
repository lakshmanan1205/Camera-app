package com.example.camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class GlareDetection {
    Bitmap bitmap;

    public GlareDetection(Bitmap bitmap) {
//        this.bitmap = bitmap;
        if (bitmap == null || bitmap.isRecycled()) {
            throw new IllegalArgumentException("Bitmap is null or recycled");
        }

        // Ensure bitmap is in correct format and mutable
        this.bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

    }

    public Boolean isGlare(){
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed");
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully");
        }
        Boolean isDetected = false;
        Bitmap tempBitmap = bitmap;
        Mat mat = new Mat();
        //convert to gary
        Utils.bitmapToMat(tempBitmap,mat);
        Imgproc.cvtColor(mat,mat,Imgproc.COLOR_RGB2GRAY);
        //calculate thresh
        Mat threshMat = new Mat();
        double thresholdValue = 240;
        Imgproc.threshold(mat,threshMat,thresholdValue,255.0,Imgproc.THRESH_BINARY);
        //count bright pixels
        int whitePixels = Core.countNonZero(threshMat);
        int totalPixels = mat.rows() * mat.cols();
        float glareRatio = (float) whitePixels/totalPixels;
        boolean hasGlare = glareRatio > 0.01;
        Log.d("GLARE"," "+glareRatio+" | "+hasGlare);
        return  isDetected;
    }
}
