package com.example.myapplicationtest;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    ProgressBar progressBar;
    TextView textView;
    RelativeLayout layout;

    private static final String ARG_PROCESS_TYPE = "argProcessType";

    private static final String[] IGNORED_SYSTEM_PACKAGES = new String[]{
            "android",
            "com.android.systemui"
    };

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ProcessType.ALL,
            ProcessType.LAST_DAY,
            ProcessType.LAST_HOUR,
            ProcessType.RECENT})
    public @interface ProcessType {
        int ALL = 1;
        int LAST_DAY = 2;
        int LAST_HOUR = 3;
        int RECENT = 4;
    }

    @ProcessType
    private int mProcessType;
    private ActivityManager mActivityManager;
    private List<ProcessDetail> mProcessDetails = new ArrayList<>();
    //private ProcessDetailAdapter mAdapter;
    @Nullable
    private ProcessDetailTask mProcessDetailTask;
    private PackageManager mPackageManager;
    private List<String> mIgnoredPackages = new ArrayList<>();
    private UsageStatsManager mUsageStatsManager;
    //private Views mViews;

    private static Comparator<ProcessDetail> ALPHABETIC_COMPARATOR = new Comparator<ProcessDetail>() {
        @Override
        public int compare(ProcessDetail lhs, ProcessDetail rhs) {
            return lhs.getApplicationName().compareTo(rhs.getApplicationName());
        }
    };

    private static Comparator<ProcessDetail> TIMESTAMP_COMPARATOR = new Comparator<ProcessDetail>() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public int compare(ProcessDetail lhs, ProcessDetail rhs) {
            return Long.compare(
                    rhs.getLastUsedTimestamp(),
                    lhs.getLastUsedTimestamp());
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        refreshData();


        // setMicMuted(true);
    }


    private void refreshData() {
        long start;
        long now = Calendar.getInstance().getTimeInMillis();
        mProcessType = 4;
        switch (mProcessType) {
            case ProcessType.ALL:
                start = 0;
                break;
            case ProcessType.LAST_DAY:
                start = now - TimeUnit.DAYS.toMillis(1);
                break;
            case ProcessType.LAST_HOUR:
                start = now - TimeUnit.HOURS.toMillis(1);
                break;
            case ProcessType.RECENT:
            default:
                // Within last 10 seconds
                start = now - TimeUnit.SECONDS.toMillis(10);
                break;
        }

        // We'll only show a spinner if we are not currently showing any item; otherwise we'll just
        // let the list update when the data comes in
        // showProgress(mProcessDetails.isEmpty());

        if (mProcessDetailTask != null) {
            mProcessDetailTask.cancel(true);
        }
        mProcessDetailTask = new ProcessDetailTask(
                mPackageManager,
                mUsageStatsManager,
                start,
                now,
                new ProcessDetailTask.OnTaskCompleteListener() {
                    @Override
                    public void onTaskComplete(@NonNull List<ProcessDetail> processDetails) {
                       /* if (getView() == null) {
                            return;
                        }*/
                        mProcessDetailTask = null;
                        //  showProgress(false);

                        // Filter out ignored packages

                        mProcessDetails.clear();
                        List<String> appList = new ArrayList();
                        List<String> forgroundappList = new ArrayList();
                        appList.add("record");
                        appList.add("video");
                        appList.add("screen");
                        appList.add("recorder");
                        appList.add("com.kimcy929.screenrecorder");
                        forgroundappList.clear();
                        for (ProcessDetail processDetail : processDetails) {
                            /*if (mIgnoredPackages.contains(processDetail.getPackageName())) {
                                continue;
                            }*/
                            Log.e("TAGA", String.valueOf(processDetail.getPackageName()));
                            forgroundappList.add(processDetail.getPackageName());
                            //mProcessDetails.add(processDetail);
                        }

                        boolean outer = false;
                        for (int j = 0; j < appList.size(); j++) {
                            if (outer) break;
                            for (int i = 0; i < forgroundappList.size(); i++) {
                                if (forgroundappList.get(i).contains(appList.get(j))) {
                                    Log.e("TAG3", "app is running");
                                    outer = true;
                                    break;
                                }
                            }

                        }

                        // Sort the data depending on the process type and update the adapter
                        Collections.sort(
                                mProcessDetails,
                                mProcessType == ProcessType.RECENT ?
                                        TIMESTAMP_COMPARATOR :
                                        ALPHABETIC_COMPARATOR);
                        // mAdapter.notifyDataSetChanged();
                    }
                }
        );
        mProcessDetailTask.execute();
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void checkPermissions() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("enable_usage_stats_title")
                .setMessage("enable_usage_stats_message")
                .setPositiveButton("enable_usage_stats_ok_button",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                            }
                        })
                .setCancelable(false)
                .create()
                .show();
    }

    public void onFilterTouchEventForSecurity(MotionEvent event) {
        if ((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) == MotionEvent.FLAG_WINDOW_IS_OBSCURED) {
            // show error message
            //return false;
        }
        //return super.onFilterTouchEventForSecurity(event);
    }

    boolean isNamedProcessRunning(String processName) {
        if (processName == null)
            return false;

        ActivityManager manager =
                (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            Log.e("Test3", process.processName);
            if (processName.equals(process.processName)) {
                return true;
            }
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setFlags();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.layout);

        String strSDCardPath = System.getenv("SECONDARY_STORAGE");

        if ((strSDCardPath == null) || (strSDCardPath.length() == 0)) {
            strSDCardPath = System.getenv("EXTERNAL_SDCARD_STORAGE");
        }

        //If may get a full path that is not the right one, even if we don't have the SD Card there.
        //We just need the "/mnt/extSdCard/" i.e and check if it's writable

        if(strSDCardPath != null) {
            if (strSDCardPath.contains(":")) {
                strSDCardPath = strSDCardPath.substring(0, strSDCardPath.indexOf(":"));
            }
            File externalFilePath = new File(strSDCardPath);
            Log.e("path", String.valueOf(externalFilePath));


            if (externalFilePath.exists() && externalFilePath.canWrite()){
                //do what you need here
                Log.e("path1", String.valueOf(externalFilePath));
            }
        }






        //mProcessType = getArguments().getInt(ARG_PROCESS_TYPE, ProcessType.RECENT);
        mActivityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        mPackageManager = this.getPackageManager();
        mUsageStatsManager = (UsageStatsManager) getSystemService(Activity.USAGE_STATS_SERVICE);

        // Add this package and some system packages to list of packages to ignore
        mIgnoredPackages.add(MainActivity.this.getPackageName());
        mIgnoredPackages.addAll(Arrays.asList(IGNORED_SYSTEM_PACKAGES));
        for (int i = 0; i < mIgnoredPackages.size(); i++) {
            Log.e("FINALTAG", mIgnoredPackages.get(i));
        }


//layout.onFilterTouchEventForSecurity()


// App is not running

        // detectScreenShotService(MainActivity.this);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            setupActivityListener();
        }*/

        backupProccessApp();
        progressBar = findViewById(R.id.progressBarMovie);
        Button btn = findViewById(R.id.btn);
        textView = findViewById(R.id.txt);
        File sdCardRoot1 = Environment.getExternalStorageDirectory();
        final File src = new File(sdCardRoot1, "/Download/mirzapur.mkv");
        final File dir = new File(sdCardRoot1, "/Mirzapur2/VID.mp4");


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                startActivity(intent);
               /* Log.e("copy", String.valueOf(src));
                if (src.exists()) {
                    new MyCopyTask(MainActivity.this).execute(src);
                  *//*  try {
                        Log.e("copy", "file found");
                        InputStream in = new FileInputStream(src);
                        OutputStream out = new FileOutputStream(dir);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            FileUtils.copy(in, out);
                        }
                        //   copyFile(src, dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*//*
                } else {
                    Log.e("copy", "file not found");
                }*/
            }
        });


    }

    private void backupProccessApp() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = am.getRunningAppProcesses();
        List<String> appList = new ArrayList();
        appList.add("record");
        appList.add("video");
        appList.add("screen");
        appList.add("recorder");
        appList.add("com.kimcy929.screenrecorder");
        for (int i = 0; i < runningAppProcessInfo.size(); i++) {
            Log.e("Test", String.valueOf(runningAppProcessInfo.get(i).processName));

            for (int j = 0; j < appList.size(); j++) {
                if (runningAppProcessInfo.get(i).processName.contains(appList.get(j))) {
                    Log.e("Test", "called");
                    // Do you stuff
                }
            }
        }
    }


    public void detectScreenShotService(final Activity activity) {
        final Handler h = new Handler();
        final int delay = 3000; //milliseconds
        final ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);

        h.postDelayed(new Runnable() {
            public void run() {

                List<ActivityManager.RunningServiceInfo> rs = am.getRunningServices(200);

                for (ActivityManager.RunningServiceInfo ar : rs) {
                    Log.e("Test1", ar.process);

                    if (ar.process.equals("com.android.systemui:screenshot")) {
                        Toast.makeText(activity, "Screenshot captured!!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(activity, "else!", Toast.LENGTH_LONG).show();
                    }
                }
                h.postDelayed(this, delay);
            }
        }, delay);

    }

    private void setMicMuted(boolean state) {
        AudioManager myAudioManager = (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);

        // get the working mode and keep it
        int workingAudioMode = myAudioManager.getMode();

        myAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        // change mic state only if needed
        if (myAudioManager.isMicrophoneMute() != state) {
            Log.e("TAG", String.valueOf(state));
            myAudioManager.setMicrophoneMute(state);
        } else {
            myAudioManager.setMicrophoneMute(state);
            Log.e("TAG", String.valueOf(state));
        }

        // set back the original working mode
        myAudioManager.setMode(workingAudioMode);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setMicMuted(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setMicMuted(false);
    }

    private class MyCopyTask extends AsyncTask<File, Integer, File> {
        Context context;

        public MyCopyTask(Context context) {
            this.context = context;
        }

        Dialog dialog;
        ProgressBar progressBar1;
        ProgressDialog progressDialog;

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        protected File doInBackground(File... params) {
            File source = params[0];
            Log.e("copy", String.valueOf(source));
            File destination = new File(Environment.getExternalStorageDirectory(), "/Mirzapur2/mirzapur.mkv");

            // call onProgressUpdate method to update progress on UI
            publishProgress(50);    // update progress to 50%

            try {
                int progress;

                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(destination);
                // Transfer bytes from in to out
                final long expectedBytes = source.length(); // This is the number of bytes we expected to copy..
                long totalBytesCopied = 0; // This will track the total number of bytes we've copied
                byte[] buf = new byte[1024];
                int len = 0;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    totalBytesCopied += len;
                    progress = (int) Math.round(((double) totalBytesCopied / (double) expectedBytes) * 100);
                    publishProgress(progress);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return destination;
        }

        @Override
        protected void onPostExecute(File result) {
            Log.e("copy1", String.valueOf(result));
            if (result.exists()) {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(result)));

                Toast.makeText(getApplicationContext(), "Stored at:  " + "---" + result.getParent() + "----" + "with name:   " + result.getName(), Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(getApplicationContext(), "File could not be copied", Toast.LENGTH_LONG).show();
            }

            // Hide ProgressDialog here
            dialog.dismiss();
            //progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            // Show ProgressDialog here
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog_layout);
            progressBar1 = dialog.findViewById(R.id.progressBarMovie);
            dialog.setCancelable(false);
            progressBar1.setIndeterminate(false);
            progressBar1.setMax(100);
            dialog.show();
           /* progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.show();*/
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Log.e("copy2", String.valueOf(values[0]));
            //progressDialog.setProgress(values[0]);
            progressBar1.setProgress(values[0]);
        }

    }
}


