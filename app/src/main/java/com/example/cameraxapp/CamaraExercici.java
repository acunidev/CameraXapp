package com.example.cameraxapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CamaraExercici extends AppCompatActivity implements View.OnClickListener {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara_exercici);
        initBtns();

        previewView = findViewById(R.id.previewView);
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());

        //        initCamara();
    }

    private void initCamara() {
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    //    @SuppressLint("RestrictedApi")
    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        // Camera Selector use case
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                                                    .build();

        Preview preview = new Preview.Builder().build();

        // Preview use case
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                 .build();

        //Video capture use case
        videoCapture = new VideoCapture.Builder().setVideoFrameRate(30)
                                                 .build();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);
    }

    private void initBtns() {
        ImageButton btnFerFoto = findViewById(R.id.imgBtnFerFoto);
        ImageButton btnGrabar = findViewById(R.id.imgBtnFerVideo);

        btnFerFoto.setOnClickListener(this);
        btnGrabar.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgBtnFerFoto:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ferFoto();
                }
                break;
            case R.id.imgBtnFerVideo:
                if (v.getTag()
                     .equals("startVideo")) {
                    Log.d("grabas?", "si estic graban " + v.getTag());
                    v.setTag("stopVideo");
                    Toast.makeText(this, "Estic graban y ahora mi tag es " + v.getTag(), Toast.LENGTH_SHORT)
                         .show();
                    grabarVideo();
                } else {
                    videoCapture.stopRecording();
                    v.setTag("startVideo");
                    Toast.makeText(this, "He parat de grabar", Toast.LENGTH_SHORT)
                         .show();
                }
                break;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint("RestrictedApi")
    private void grabarVideo() {
        //El video es guardara en el directori segons les variables d'entorn
        if (videoCapture != null) {
            File videoPath = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                videoPath = getExternalFilesDir(Environment.DIRECTORY_RECORDINGS);
            }

            if (!videoPath.exists()) {
                videoPath.mkdir();
            }
            Date date = new Date();
            String videoTime = String.valueOf(date.getTime());
            String videoFilePath = videoPath.getAbsolutePath() + "/" + videoTime + ".mp4";


            Uri uriPath = Uri.parse(videoFilePath);

            // Agrega la foto a la galería de medios
            ContentResolver contentResolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            contentValues.put(MediaStore.Video.Media.DATA, String.valueOf(uriPath));
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);

            //checkPerms
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            videoCapture.startRecording(new VideoCapture.OutputFileOptions.Builder(contentResolver,
                                                                                   MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                                                   contentValues).build(),
                                        getExecutor(), new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            Log.i("onVideoSaved", "deberia guardarlo");
                            Toast.makeText(CamaraExercici.this, "Video has been saved succesfuly", Toast.LENGTH_SHORT)
                                 .show();
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            Toast.makeText(CamaraExercici.this, "Error saving the video: " + message,
                                           Toast.LENGTH_SHORT)
                                 .show();
                        }
                    });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void ferFoto() {
        // La foto es guardará en el directorio segons les variables d'entorn
        File photoExternalFilesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!photoExternalFilesDir.exists()) {
            photoExternalFilesDir.mkdir();
        }
        Date date = new Date();
        String timestamp = String.valueOf(date.getTime());
        String photoFilePath = photoExternalFilesDir.getAbsolutePath() + "/" + timestamp + ".jpg";
        Uri test = Uri.parse(photoFilePath);

        // Agrega la foto a la galería de medios
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, String.valueOf(test));
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        // Hace la foto
        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                           values).build(), getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    // Si la guarda:
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {

                        Toast.makeText(CamaraExercici.this, "Photo saved", Toast.LENGTH_SHORT)
                             .show();
                    }

                    // Si da error:
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.d("funciona??", exception.getMessage());
                        Toast.makeText(CamaraExercici.this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT)
                             .show();
                    }
                });
    }
}