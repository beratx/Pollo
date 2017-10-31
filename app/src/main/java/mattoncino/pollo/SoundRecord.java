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
    public interface OnStopListener {
        void onStop(boolean stopped);
    }

    private static final String TAG = "SoundRecord";
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private String recordPath = null;
    private static final String TEMP_RECORD = "tempRecord";
    private boolean startPlaying = true;
    private OnStopListener onStopListener;



    public SoundRecord(String recordPath){
        this.recordPath = recordPath;
        this.onStopListener = null;
    }

    public void setOnStopListener(OnStopListener listener){
        this.onStopListener = listener;
    }

    public boolean isPlaying(){
        return startPlaying;
    }

    public void flipPlay(){
        startPlaying = !startPlaying;
        Log.d(TAG, String.valueOf(startPlaying));
    }

    public String getDataSource(){
        return recordPath;
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
            mPlayer.setDataSource(recordPath);
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mPlayer.start();
                    Log.d(TAG, "Record is started.");
                }
            });
            mPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }


    public void setCompletionListener( MediaPlayer.OnCompletionListener listener) {
        mPlayer.setOnCompletionListener(listener);
    }

    public void stopPlaying() {
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;

        Log.d(TAG, "Record is stopped.");

        if (onStopListener != null)
            onStopListener.onStop(startPlaying);
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

        Log.d(TAG, "Start recording...");
    }

    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        Log.d(TAG, "Stop recording...");
    }

    public static String createTempFile(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File record =  ImagePicker.isExternalStorageWritable()
                ? new File(context.getExternalCacheDir(), "pollo_" + timestamp + "." + ext)
                : new File(context.getCacheDir() , "pollo_" + timestamp + "." + ext);

        return record.getPath();
    }


    public static String getTempFile(Context context, String ext) {
        boolean external = ImagePicker.isExternalStorageWritable();

        File recordFile =  external ? new File(context.getExternalCacheDir(), TEMP_RECORD + "." + ext)
                                    : new File(context.getCacheDir() + TEMP_RECORD + "." + ext);

        Log.d(TAG, "record file path: " + recordFile.getPath());

        return recordFile.getPath();
    }

    public static String createFile2(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean external = ImagePicker.isExternalStorageWritable();

        File dir = external ? context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                            : new File(context.getFilesDir() + File.separator + "pollo_sound");

        if (!external && !dir.exists()) {
            File imagesDir = new File(context.getFilesDir(), "pollo_sound");
            imagesDir.mkdirs();
        }

        return new File(dir, "pollo_" + timestamp + "." + ext).getPath();
    }
}
