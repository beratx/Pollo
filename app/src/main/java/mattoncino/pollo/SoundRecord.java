package mattoncino.pollo;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SoundRecord {
    private static final String TAG = "SoundRecord";
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private String recordPath = null;
    private static final String TEMP_RECORD = "tempRecord";
    private boolean startPlaying = true;

    public SoundRecord(String recordPath){
        this.recordPath = recordPath;
    }

    public boolean isPlay(){
        return startPlaying;
    }

    public void setPlay(){
        startPlaying = !startPlaying;
    }

    public void onPlay(){
        if (startPlaying) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    public int getDuration(Context context) {
        Uri uri = Uri.parse(recordPath);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(durationStr);
    }


    public void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            //mPlayer.reset()?
            mPlayer.setDataSource(recordPath);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    public void setCompletionListener( MediaPlayer.OnCompletionListener listener) {
        mPlayer.setOnCompletionListener(listener);
    }

    public void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    public void startRecording() {
        this.mRecorder = new MediaRecorder();
        this.mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        this.mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        this.mRecorder.setOutputFile(recordPath);
        this.mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    public static String createFile(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean external = ImagePicker.isExternalStorageWritable();

        //cache directory!
        String recordPath =  external ? context.getExternalCacheDir().getAbsolutePath() + "/" + timestamp + "." + ext
                                      : context.getCacheDir().getAbsolutePath() + "/" + timestamp + "." + ext;

        Log.d(TAG, "record file path: " + recordPath);

        return recordPath;
    }

    public static String createTempFile(Context context, String ext) {
        File recordFile;

        boolean external = ImagePicker.isExternalStorageWritable();

        recordFile =  external ? new File(context.getExternalCacheDir().getAbsolutePath()
                                                        + File.separator + TEMP_RECORD + "." + ext)
                               : new File(context.getCacheDir().getAbsolutePath()
                                                        + File.separator + TEMP_RECORD + "." + ext);

        recordFile.getParentFile().mkdirs();

        Log.d(TAG, "record file path: " + recordFile.getPath());

        return recordFile.getPath();
    }

    public static String createFile2(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean external = ImagePicker.isExternalStorageWritable();

        String recordPath =  external ? Environment.getExternalStorageDirectory().getAbsolutePath()
                                        + File.separator + timestamp + "." + ext
                                      : context.getFilesDir().getAbsolutePath() + File.separator +
                                        timestamp + "." + ext;

        Log.d(TAG, "record file path: " + recordPath);

        //return File.createTempFile(timestamp, "." + ext, dir).getAbsolutePath();
        return recordPath;
    }


    /*public static File createFile(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean external = ImagePicker.isExternalStorageWritable();

        return external ? new File(context.getExternalCacheDir(), "pollo_" + timestamp + "." + ext)
                        : new File(context.getCacheDir() , "pollo_" + timestamp + "." + ext);
    }*/
}
