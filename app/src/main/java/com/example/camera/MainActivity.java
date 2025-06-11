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

        // Responsive size: 80% of width and 60% of height, adjust as needed
        int frameWidth = (int) (screenWidth * 0.8);
        int frameHeight = (int) (screenHeight * 0.8);

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



}