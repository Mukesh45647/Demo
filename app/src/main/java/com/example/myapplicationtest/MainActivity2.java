package com.example.myapplicationtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {
    ScreenShotContentObserver screenShotContentObserver;
    ImageView imageView;
    private String[] imagenName;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        imageView = findViewById(R.id.image);

        if (isStoragePermissionGranted()) {
            HandlerThread handlerThread = new HandlerThread("content_observer");
            handlerThread.start();
            final android.os.Handler handler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                }
            };

            screenShotContentObserver = new ScreenShotContentObserver(handler, this) {
                @Override
                protected void onScreenShot(String path, String fileName) {
                    File file = new File(path); //this is the file of screenshot image
                    if (file.exists()) {
                        file.delete();
                        Log.e("TAG", path);
                        imagenName = path.split("/");

                    /*for (int i = 0; i<a.length; i++){
                        Log.e("TAG", a[i]);
                    }*/

                        //Toast.makeText(MainActivity2.this, "Screenshot captured!!", Toast.LENGTH_SHORT).show();
                        String filePath = file.getPath();

                        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                        saveToInternalStorage(bitmap);

                        //mark(bitmap, "moreplex", , "", 10, 10, false);
                        // BitmapaddWaterMark(bitmap);
                /*    Bitmap a = addWaterMark(bitmap);
                    Log.e("Tag", String.valueOf(a));
                    imageView.setImageBitmap(a);*/
                        //    7779
                    }
                    Log.e("TAG", String.valueOf(file));
                }
            };
        } else {
            Toast.makeText(this, "Please enable storage permission", Toast.LENGTH_SHORT).show();
        }
     /*   Point p=new Point();
        p.set(180, 1000);
        Bitmap b=waterMark(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background),"Welcome To Hamad's Blog",p,Color.WHITE,90,30,true);
        imageView.setImageBitmap(b);*/


    }


    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("tag", "Permission is granted");
                return true;
            } else {

                Log.v("tag", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG", "Permission is granted");
            return true;
        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File sdCardRoot1 = Environment.getExternalStorageDirectory();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());
        Date currentTime = Calendar.getInstance().getTime();
        File mypath = new File(sdCardRoot1, "/DCIM/Screenshots/Screenshots_" + currentDateandTime + "_" + getPackageName() + ".jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sdCardRoot1.getAbsolutePath();
    }

    public Bitmap waterMark(Bitmap src, String watermark, Point location, int color, int alpha, int size, boolean underline) {
        //get source image width and height
        /*int w = src.getWidth();
        int h = src.getHeight();*/
        int w = 10;
        int h = 10;

        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        //create canvas object
        Canvas canvas = new Canvas(result);
        //draw bitmap on canvas
        canvas.drawBitmap(src, 0, 0, null);
        //create paint object
        Paint paint = new Paint();
        //apply color
        paint.setColor(color);
        //set transparency
        paint.setAlpha(alpha);
        //set text size
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        //set should be underlined or not
        paint.setUnderlineText(underline);
        //draw text on given location
        canvas.drawText(watermark, location.x, location.y, paint);

        return result;
    }

    private Bitmap addWaterMark(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        Bitmap waterMark = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher_background);
        canvas.drawBitmap(waterMark, 0, 0, null);

        Log.e("tag", String.valueOf(waterMark));
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("TAG", "Permission: " + permissions[0] + "was " + grantResults[0]);
            //resume tasks needing this permission
        }
    }


    public static Bitmap mark(Bitmap src, String watermark, Point location, Color color, int alpha, int size, boolean underline) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        Paint paint = new Paint();
        // paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setTextSize(size);
        paint.setAntiAlias(true);
        paint.setUnderlineText(underline);
        canvas.drawText(watermark, location.x, location.y, paint);

        return result;
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e("TAG", "resume");
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenShotContentObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("TAG", "pause");
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenShotContentObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("TAG", "Destroy");
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenShotContentObserver);
    }
}