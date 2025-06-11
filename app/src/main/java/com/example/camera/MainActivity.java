package com.example.camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity{
    ImageButton capture, toggleFlash, flipCamera, autofocus;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    boolean isAutoFocus = true;
    String TAG = "cropAccurately";

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
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipcamera);
        autofocus = findViewById(R.id.autofocus);
        frameOverlay = findViewById(R.id.frameOverlay);

        int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        // Convert to dp if needed
        float density = getResources().getDisplayMetrics().density;

        // Responsive size: 80% of width and 60% of height, adjust as needed
        int frameWidth = (int) (screenWidth * 0.8);
        int frameHeight = (int) (screenHeight * 0.8);
//        ViewGroup.LayoutParams params = frameOverlay.getLayoutParams();
//        params.width = frameWidth;
//        params.height = frameHeight;
//        frameOverlay.setLayoutParams(params);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(frameWidth, frameHeight);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        frameOverlay.setLayoutParams(layoutParams);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

                imageCapture=builder.setTargetAspectRatio(aspectRatio).build();
                //ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
                processCameraProvider.unbindAll();


                Camera camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                capture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        takePicture(imageCapture);
                        takeHighQualityPhotoAndCrop(imageCapture);
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
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
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
//        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"IMG" + System.currentTimeMillis() + ".jpg");
//        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Image saved Succesfully ! " , Toast.LENGTH_LONG).show();
                    }
                });
                startCamera(cameraFacing,isAutoFocus);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed to save : " + exception.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MainActivity.this, "Flash is not available currently!", Toast.LENGTH_SHORT).show();
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

//    private void takeHighQualityPhotoAndCrop(ImageCapture imageCapture) {
//        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
//
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + timestamp + ".jpg");
//        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CroppedCameraX");
//        }
//
//        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
//                getContentResolver(),
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                contentValues
//        ).build();
//
//        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
//                new ImageCapture.OnImageSavedCallback() {
//                    @Override
//                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                        Uri savedUri = outputFileResults.getSavedUri();
//                        if (savedUri != null) {
//                            cropAndSaveBitmap(savedUri);
//                        } else {
//                            Log.e("CameraX", "Saved URI is null.");
//                        }
//                    }
//
//                    @Override
//                    public void onError(@NonNull ImageCaptureException exception) {
//                        exception.printStackTrace();
//                    }
//                });
//    }
//    private void cropAndSaveBitmap(Uri imageUri) {
//        try {
//            Bitmap fullBitmap;
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
//                fullBitmap = ImageDecoder.decodeBitmap(source);
//            } else {
//                fullBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//            }
//
//            // Get frame position and size
//            View frameView = findViewById(R.id.frameOverlay);
//            int[] location = new int[2];
//            frameView.getLocationOnScreen(location);
//            int frameX = location[0];
//            int frameY = location[1];
//            int frameWidth = frameView.getWidth();
//            int frameHeight = frameView.getHeight();
//
//            // Get preview size
//            View previewView = findViewById(R.id.cameraPreview);
//            int previewWidth = previewView.getWidth();
//            int previewHeight = previewView.getHeight();
//
//            // Scale frame coords to match bitmap
//            float scaleX = (float) fullBitmap.getWidth() / previewWidth;
//            float scaleY = (float) fullBitmap.getHeight() / previewHeight;
//
//            int cropX = (int) (frameX * scaleX);
//            int cropY = (int) (frameY * scaleY);
//            int cropWidth = (int) (frameWidth * scaleX);
//            int cropHeight = (int) (frameHeight * scaleY);
//
//            // Clamp bounds
//            cropX = Math.max(0, cropX);
//            cropY = Math.max(0, cropY);
//            cropWidth = Math.min(fullBitmap.getWidth() - cropX, cropWidth);
//            cropHeight = Math.min(fullBitmap.getHeight() - cropY, cropHeight);
//
//            // ✅ Crop
//            Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, cropX, cropY, cropWidth, cropHeight);
//
//            // ✅ Save cropped image to gallery
//            saveBitmapToGallery(croppedBitmap);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void takeHighQualityPhotoAndCrop(ImageCapture imageCapture) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + timestamp + ".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CroppedCameraX");
        }

        Uri imageUri = null;

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        imageCapture.takePicture(outputOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = outputFileResults.getSavedUri();
                if (savedUri != null) {
                    try {
                        Bitmap fullBitmap;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), savedUri);
                            fullBitmap = ImageDecoder.decodeBitmap(source);
                        } else {
                            fullBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), savedUri);
                        }

                        // Get frame overlay coordinates and dimensions
                        int[] location = new int[2];
                        frameOverlay.getLocationOnScreen(location);
                        int frameX = location[0];
                        int frameY = location[1];
                        int frameWidth = frameOverlay.getWidth();
                        int frameHeight = frameOverlay.getHeight();

                        // Get PreviewView location
                        int[] previewLocation = new int[2];
                        previewView.getLocationOnScreen(previewLocation);

                        // Calculate scale ratio
                        float scaleX = (float) fullBitmap.getWidth() / previewView.getWidth();
                        float scaleY = (float) fullBitmap.getHeight() / previewView.getHeight();

                        // Calculate cropped area in bitmap coordinates
                        int cropX = (int) ((frameX - previewLocation[0]) * scaleX);
                        int cropY = (int) ((frameY - previewLocation[1]) * scaleY);
                        int cropW = (int) (frameWidth * scaleX);
                        int cropH = (int) (frameHeight * scaleY);

                        // Fix cropping bounds if necessary
                        cropX = Math.max(0, cropX);
                        cropY = Math.max(0, cropY);
                        cropW = Math.min(cropW, fullBitmap.getWidth() - cropX);
                        cropH = Math.min(cropH, fullBitmap.getHeight() - cropY);

                        // Perform cropping
                        Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, cropX, cropY, cropW, cropH);

                        // Save cropped image
                        ContentValues croppedValues = new ContentValues();
                        croppedValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "CROPPED_" + timestamp + ".jpg");
                        croppedValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            croppedValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CroppedCameraX");
                        }

                        Uri croppedUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, croppedValues);
                        if (croppedUri != null) {
                            OutputStream out = getContentResolver().openOutputStream(croppedUri);
                            if (out != null) {
                                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.close();
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Cropped image saved!", Toast.LENGTH_SHORT).show());
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Cropping failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Image capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void cropAccurately(Uri savedUri) {
        try {
            // Decode full image bitmap
            Bitmap fullBitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), savedUri);
                fullBitmap = ImageDecoder.decodeBitmap(source);
            } else {
                fullBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), savedUri);
            }

            // 1️⃣ Get preview screenshot (to match overlay frame)
            PreviewView previewView = findViewById(R.id.cameraPreview);
            Bitmap previewBitmap = previewView.getBitmap();

            // 2️⃣ Get frame's location & size in preview
            View frameView = findViewById(R.id.frameOverlay);
            int[] frameLocation = new int[2];
            frameView.getLocationInWindow(frameLocation);
            Log.d(TAG, "cropAccurately: frameLocation "+frameLocation[0]+" " + frameLocation[1]);
            int[] previewLocation = new int[2];
            previewView.getLocationInWindow(previewLocation);
            Log.d(TAG, "cropAccurately: previewLocation "+previewLocation[0]+" "+previewLocation[1]);
            int relativeX = frameLocation[0] - previewLocation[0];
            int relativeY = frameLocation[1] - previewLocation[1];
            Log.d(TAG, "cropAccurately: relative X,Y "+relativeX+" "+relativeY );
            int frameWidth = frameView.getWidth();
            int frameHeight = frameView.getHeight();
            Log.d(TAG, "cropAccurately: frame Width,Height "+frameWidth+" "+frameHeight );
            // 3️⃣ Map coordinates to full resolution
            float scaleX = (float) fullBitmap.getWidth() / previewBitmap.getWidth();
            float scaleY = (float) fullBitmap.getHeight() / previewBitmap.getHeight();
            Log.d(TAG, "cropAccurately: previewBitmap Width,Height "+previewBitmap.getWidth()+" "+previewBitmap.getHeight() );

            int cropX = (int) (relativeX * scaleX);
            int cropY = (int) (relativeY * scaleY);
            int cropW = (int) (frameWidth * scaleX);
            int cropH = (int) (frameHeight * scaleY);
            Log.d(TAG, "cropAccurately: crop X,Y,W,H "+cropX+" "+cropY+" "+cropW+" "+cropH );
            // Clamp
            cropX = Math.max(0, cropX);
            cropY = Math.max(0, cropY);
            cropW = Math.min(fullBitmap.getWidth() - cropX, cropW);
            cropH = Math.min(fullBitmap.getHeight() - cropY, cropH);

            // ✂️ Crop
            Bitmap cropped = Bitmap.createBitmap(fullBitmap, cropX, cropY, cropW, cropH);

            // 💾 Save cropped
            saveBitmapToGallery(cropped);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void saveBitmapToGallery(Bitmap bitmap) {
        String fileName = "CROPPED_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CroppedCameraX");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            Toast.makeText(this, "Cropped image saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save cropped image", Toast.LENGTH_SHORT).show();
        }
    }


}