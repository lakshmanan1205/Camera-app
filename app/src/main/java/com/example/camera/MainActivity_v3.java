package com.example.camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity_v3 extends AppCompatActivity{
    ImageButton capture, toggleFlash, flipCamera, autofocus,grayBtn;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    boolean isAutoFocus = true;
    boolean isGrayImage = false;
    ImageAnalysis imageAnalysis;
    String TAG = "analyzis";
    ImageView grayView;
    View frameOverlay;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                startCamera(cameraFacing,isAutoFocus);
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        Log.d("laksh","message");
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipcamera);
        autofocus = findViewById(R.id.autofocus);

        grayBtn = findViewById(R.id.toggleGray);

        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        // Convert to dp if needed
        float density = getResources().getDisplayMetrics().density;

        // Responsive size: 80% of width and 60% of height, adjust as needed
        int frameWidth = (int) (screenWidth * 0.8);
        int frameHeight = (int) (screenHeight * 0.6);
        ViewGroup.LayoutParams params = frameOverlay.getLayoutParams();
        params.width = frameWidth;
        params.height = frameHeight;
        frameOverlay.setLayoutParams(params);

        if (ContextCompat.checkSelfPermission(MainActivity_v3.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);

        } else {
            startCamera(cameraFacing,isAutoFocus);
        }
        flipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera(cameraFacing,isAutoFocus);
            }
        });

    }

    public void startCamera(int CameraFacing,boolean isAutofocus) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
        ListenableFuture listenableFuture = ProcessCameraProvider.getInstance(this);
        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = (ProcessCameraProvider) listenableFuture.get();
                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();
                ImageCapture imageCapture;
                ImageCapture.Builder builder = new ImageCapture.Builder();

                if (isAutofocus) {
                    builder.setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);
                } else {
                    builder.setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);
                }

                builder.setTargetRotation(getWindowManager().getDefaultDisplay().getRotation());

                imageCapture=builder.build();
                //ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
                processCameraProvider.unbindAll();
                // IMAGE ANALYSIS
//                imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                //.setTargetResolution(new Size(1280, 720))
                                .setTargetRotation(Surface.ROTATION_90)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        int rotationDegrees = image.getImageInfo().getRotationDegrees();
                        // insert your code here.
                        Log.d(TAG, "analyze: " + image.getImageInfo().getTimestamp());

                        final Bitmap bitmap = previewView.getBitmap();

                        image.close();

//                        if (bitmap == null){
////                            return;
////                        }
//                        final Bitmap bitmapGray = toGrayScale(bitmap);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                grayView.setImageBitmap(bitmapGray);
//                            }
//                        });

                    }
                });

                Camera camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture,imageAnalysis);
//
                capture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePicture(imageCapture);
//                        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                            activityResultLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                        } else {
//                            takePicture(imageCapture);
//                        }
                    }

                });
                toggleFlash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setFlashIcon(camera);
                    }
                });
                autofocus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setAutofocus(camera);
                    }
                });
                grayBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleGrayImage();
                    }
                });
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap toGrayScale(Bitmap bitmap) {
        Bitmap grayScaleBmp = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas =new Canvas(grayScaleBmp);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixColorFilter);
        canvas.drawBitmap(grayScaleBmp,0,0,paint);
        return grayScaleBmp;

    }


    public void takePicture(ImageCapture imageCapture) {
        //v1
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + timestamp + ".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraX-Images");
        }

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();
        // CAPTURE
//        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"IMG" + System.currentTimeMillis() + ".jpg");
//        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity_v3.this, "Image saved Succesfully ! " , Toast.LENGTH_LONG).show();
                    }
                });
                startCamera(cameraFacing,isAutoFocus);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity_v3.this, "Failed to save : " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                startCamera(cameraFacing,isAutoFocus);
            }
        });
    }

    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.ic_round_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.ic_round_flash_on_24);
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity_v3.this, "Flash is not available currently!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private  void setAutofocus(Camera camera){
        if (isAutoFocus) {
            isAutoFocus = false;
            autofocus.setImageResource(R.drawable.ic_round_focus_off_24);
        } else {
            isAutoFocus = true;
            autofocus.setImageResource(R.drawable.ic_round_focus_on_24);
        }
    }
    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }


    private void handleGrayImage() {
        if (isGrayImage) {
            isGrayImage = false;
            grayBtn.setImageResource(R.drawable.ic_round_gray_off_24);
//            grayView.setVisibility(View.VISIBLE);
        } else {
            isGrayImage = true;
            grayBtn.setImageResource(R.drawable.ic_round_gray_on_24);
//            grayView.setVisibility(View.INVISIBLE);
        }
    }
}