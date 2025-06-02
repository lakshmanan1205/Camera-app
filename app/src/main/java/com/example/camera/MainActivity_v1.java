package com.example.camera;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MainActivity_v1 extends AppCompatActivity {
    ImageButton capture, toggleFlash, flipCamera;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;

    private String TAG = "CameraXApp";
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                startCamera(cameraFacing);
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

        if (ContextCompat.checkSelfPermission(MainActivity_v1.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);

        } else {
            startCamera(cameraFacing);
        }
        flipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera(cameraFacing);
            }
        });

    }

    public void startCamera(int CameraFacing) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
        ListenableFuture listenableFuture = ProcessCameraProvider.getInstance(this);
        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = (ProcessCameraProvider) listenableFuture.get();
                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();
                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();
                processCameraProvider.unbindAll();
                Camera camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
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
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

//    public void takePicture(ImageCapture imageCapture) {
//        // CAPTURE
//        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_DCIM),"IMG" + System.currentTimeMillis() + ".jpg");
//        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
//        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
//            @Override
//            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(MainActivity.this, "Image saved at : " + file.getPath(), Toast.LENGTH_LONG).show();
//                    }
//                });
//                startCamera(cameraFacing);
//            }
//
//            @Override
//            public void onError(@NonNull ImageCaptureException exception) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(MainActivity.this, "Failed to save : " + exception.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//                startCamera(cameraFacing);
//            }
//        });
//    }
public void takePicture(ImageCapture imageCapture) {
    ContentValues contentValues = new ContentValues();
    String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
    contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
    contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
    contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp"); // Custom folder in Gallery

    Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    Log.d("CameraX", "Trying to save image to: " + imageUri.toString());
    if (imageUri == null) {
        Toast.makeText(this, "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show();
        return;
    }

    ImageCapture.OutputFileOptions outputFileOptions =
            new ImageCapture.OutputFileOptions.Builder(getContentResolver(), imageUri, contentValues).build();

    //imageCapture.takePicture(outputFileOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
    imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {

        @Override
        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
            Uri savedUri = outputFileResults.getSavedUri();
            String msg = "Photo capture succeeded: " + savedUri;
            String path = savedUri != null ? savedUri.getPath() : "N/A";

            if (savedUri == null) {
                Toast.makeText(MainActivity_v1.this, "Error: savedUri is null", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register content observer
            ContentResolver contentResolver = getContentResolver();
            contentResolver.registerContentObserver(savedUri, true, new android.database.ContentObserver(new android.os.Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    // Unregister immediately after change detected
                    contentResolver.unregisterContentObserver(this);

                    // Now safe to decode bitmap
                    try {
                        Bitmap bitmap;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.Source source = ImageDecoder.createSource(contentResolver, savedUri);
                            bitmap = ImageDecoder.decodeBitmap(source);
                        } else {
                            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, savedUri);
                        }

                        Log.e("Bitmap", BitMapToString(bitmap));
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity_v1.this, "Image saved and ready!", Toast.LENGTH_SHORT).show();
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("BitmapError", "Bitmap decoding failed: " + e.getMessage());
                    }
                }
            });

            Bundle bundle = new Bundle();
            bundle.putString("image", path);
            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            Log.d(TAG, msg);
            runOnUiThread(() -> {

                Toast.makeText(MainActivity_v1.this, "Image saved to Gallery!", Toast.LENGTH_SHORT).show();
            });
            startCamera(cameraFacing);
        }

        @Override
        public void onError(@NonNull ImageCaptureException exception) {
            runOnUiThread(() -> {
                Log.e("ImageCapture", "Error saving image", exception);
                Toast.makeText(MainActivity_v1.this, "Save failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            });
            startCamera(cameraFacing);
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
                    Toast.makeText(MainActivity_v1.this, "Flash is not available currently!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }
    private String BitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }
}