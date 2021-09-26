package com.example.thumbnail_generator;

import static android.content.ContentValues.TAG;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    VideoView vv;
    SeekBar sb;
    TextView time;
    RelativeLayout layout;
    ImageView poster,pb,di;
    Uri selectedImageUri;
    FileOutputStream ss=null;
    String paths = "";
    FloatingActionButton fab;
    int cpos=0;
    boolean isdone = false,isplayed=false;

    private final static int REQUEST_TAKE_GALLERY_VIDEO = 100, PERMISSION_REQUEST_CODE = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vv = findViewById(R.id.vv);
        sb = findViewById(R.id.sb);
        time = findViewById(R.id.time);
        di = findViewById(R.id.di);

        fab = findViewById(R.id.fab);

        pb = findViewById(R.id.pb);

        layout = findViewById(R.id.layout);
        poster = findViewById(R.id.poster);

        sb.setProgress(1);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
            }
        });

        di.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                get_and_download_img();
            }
        });


        pb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(isplayed)
                {
                    vv.start();
                    pb.setImageResource(R.drawable.pause);
                    isplayed=false;
                }
                else
                {
                    vv.pause();
                    pb.setImageResource(R.drawable.play);
                    isplayed=true;
                }
            }
        });

        Runnable onEverySecond = new Runnable() {

            @Override
            public void run() {

                if (sb != null) {
                    sb.setProgress(vv.getCurrentPosition());
                }

                if (vv.getCurrentPosition() < vv.getDuration()) {
                    sb.postDelayed(this, 1000);

                 //  if(isdone)
                 //  {
                        time.setText(getTimeString((long) vv.getCurrentPosition()));
                 //  }
                 //  else
                    // {
                 //     time.setText(getTimeString((long) vv.getCurrentPosition()));
                 //   }
                }
            }
        };


        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp) {
                sb.setMax(vv.getDuration());
                gettotaltime(vv.getDuration());
                sb.postDelayed(onEverySecond, 1000);
            }
        });

        vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                pb.setImageResource(R.drawable.play);
                isplayed=true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sb.setProgress(0,true);
                }
                else
                {
                    sb.setProgress(0);
                }
            }
        });

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

                if (fromUser) {
                    vv.seekTo(progress);
                    cpos=progress;
                }
            }
        });
    }

    private void get_and_download_img() {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, selectedImageUri);
            retriever.setDataSource(getBaseContext(), selectedImageUri);
            if(cpos!=0) {
                //TimeUnit.NANOSECONDS.convert((long)vv.getCurrentPosition(), TimeUnit.MICROSECONDS)

                //bitmap = retriever.getFrameAtTime(MILLISECONDS.toMicros(vv.getCurrentPosition()));
                bitmap = retriever.getFrameAtTime(MILLISECONDS.toMicros(vv.getCurrentPosition()), MediaMetadataRetriever.OPTION_CLOSEST);
            }
            storeImage(bitmap);

        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file.
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
            }
        }
    }


    private void storeImage(Bitmap bitmap) {

        if (checkPermission()) {
            SaveImage(bitmap);
        } else {
            requestPermission(); // Code for permission
        }

    }

    private void gettotaltime(int duration)
    {
        String tt=getTimeString((long)duration);
        String ss=tt.substring(0,2);
        int w=Integer.valueOf(ss);

        if(w==0)
        {
            isdone=true;
        }
        else
        {
            isdone=false;
        }
    }


    static String getTimeString(Long millis) {
        return String.format("%02d:%02d:%02d",
                MILLISECONDS.toHours(millis),
                MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(MILLISECONDS.toHours(millis)),
                MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(MILLISECONDS.toMinutes(millis)));
    }


    static String getTimeString1(Long millis) {
        return String.format("%02d:%02d",
                MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(MILLISECONDS.toHours(millis)),
                MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(MILLISECONDS.toMinutes(millis)));
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                selectedImageUri = data.getData();

                if (selectedImageUri != null && vv != null)
                {
                  layout.setVisibility(View.VISIBLE);
                  poster.setVisibility(View.INVISIBLE);
                  vv.setVideoURI(selectedImageUri);
                  isplayed=true;

                  pb.setImageResource(R.drawable.play);
                  sb.setProgress(1);
               }
                else
                {
                    isplayed=false;
                    layout.setVisibility(View.INVISIBLE);
                    pb.setImageResource(R.drawable.play);
                    poster.setVisibility(View.VISIBLE);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to save files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.e("value", "Permission Granted, Now you can use local drive .");
            } else{
                Log.e("value", "Permission Denied, You cannot use local drive .");
            }
            break;
        }
    }

    private void SaveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();

        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
            // sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
            //     Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
       MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener()
                {
                    public void onScanCompleted(String path, Uri uri)
                    {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    public static class ScalableVideoView extends VideoView {

        private int mVideoWidth;
        private int mVideoHeight;
        private DisplayMode displayMode = DisplayMode.ORIGINAL;

        public enum DisplayMode {
            ORIGINAL,       // original aspect ratio
            FULL_SCREEN,    // fit to screen
            ZOOM            // zoom in
        };

        public ScalableVideoView(Context context) {
            super(context);
        }

        public ScalableVideoView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public ScalableVideoView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            mVideoWidth = 0;
            mVideoHeight = 0;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = getDefaultSize(0, widthMeasureSpec);
            int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

            if (displayMode == DisplayMode.ORIGINAL) {
                if (mVideoWidth > 0 && mVideoHeight > 0) {
                    if ( mVideoWidth * height  > width * mVideoHeight ) {
                        // video height exceeds screen, shrink it
                        height = width * mVideoHeight / mVideoWidth;
                    } else if ( mVideoWidth * height  < width * mVideoHeight ) {
                        // video width exceeds screen, shrink it
                        width = height * mVideoWidth / mVideoHeight;
                    } else {
                        // aspect ratio is correct
                    }
                }
            }
            else if (displayMode == DisplayMode.FULL_SCREEN) {
                // just use the default screen width and screen height
            }
            else if (displayMode == DisplayMode.ZOOM) {
                // zoom video
                if (mVideoWidth > 0 && mVideoHeight > 0 && mVideoWidth < width) {
                    height = mVideoHeight * width / mVideoWidth;
                }
            }

            setMeasuredDimension(width, height);
        }

        public void changeVideoSize(int width, int height)
        {
            mVideoWidth = width;
            mVideoHeight = height;

            // not sure whether it is useful or not but safe to do so
            getHolder().setFixedSize(width, height);

            requestLayout();
            invalidate();     // very important, so that onMeasure will be triggered
        }

        public void setDisplayMode(DisplayMode mode) {
            displayMode = mode;
        }
    }
}