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

/**
 * Wrapper class for MediaPlayer and MediaRecorder functionalities;
 * manages audio record and playing.
 */
public class SoundRecord {
    /**
     * Listener interface for audio stops
     */
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


    /**
     * Constructor
     * @param recordPath path of the file to be recorded/played
     */
    public SoundRecord(String recordPath){
        this.recordPath = recordPath;
        this.onStopListener = null;
    }

    /**
     * Sets a listener for the OnStop Action
     * @param listener
     */
    public void setOnStopListener(OnStopListener listener){
        this.onStopListener = listener;
    }

    /** Returns true if audio is playing, false otherwise */
    public boolean isPlaying(){
        return startPlaying;
    }

    /** Flips the IsPlaying state */
    public void flipPlay(){
        startPlaying = !startPlaying;
        Log.d(TAG, String.valueOf(startPlaying));
    }

    /** Returns path of the file */
    public String getDataSource(){
        return recordPath;
    }

    /*public void onPlay(){
        if (startPlaying) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }*/

    /**
     * Returns duration of the audio
     * @param context Activity's context
     * @return duration in milliseconds
     */
    public int getDuration(Context context) {
        Uri uri = Uri.parse(recordPath);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(durationStr);
    }


    /** Initiliazes MediaPlayer and starts playing audio */
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


    /** Sets a listener for the audio completion situation */
    public void setCompletionListener( MediaPlayer.OnCompletionListener listener) {
        mPlayer.setOnCompletionListener(listener);
    }

    /** Stops the playing audio and releases resources */
    public void stopPlaying() {
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;

        Log.d(TAG, "Record is stopped.");

        if (onStopListener != null)
            onStopListener.onStop(startPlaying);
    }

    /** Initiliazes MediaRecord and start audio recording */
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

    /** Stops audio recording */
    public void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        Log.d(TAG, "Stop recording...");
    }


    /**
     * Creates a temporary file with a unique name in the external cache
     * of the app if the external storage is available,
     * otherwise in the internal cache
     *
     * @param context Activity's context
     * @param ext extension of file
     * @return path of the created File
     */
    public static String createTempFile(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File record =  ImagePicker.isExternalStorageWritable()
                ? new File(context.getExternalCacheDir(), "pollo_" + timestamp + "." + ext)
                : new File(context.getCacheDir() , "pollo_" + timestamp + "." + ext);

        return record.getPath();
    }


    /**
     * Creates a temporary file in the external cache
     * of the app if the external storage is available,
     * otherwise in the internal cache
     *
     * @param context Activity's context
     * @param ext extension of file
     * @return path of the created File
     */
    public static String getTempFile(Context context, String ext) {
        boolean external = ImagePicker.isExternalStorageWritable();

        File recordFile =  external ? new File(context.getExternalCacheDir(), TEMP_RECORD + "." + ext)
                                    : new File(context.getCacheDir() + TEMP_RECORD + "." + ext);

        Log.d(TAG, "record file path: " + recordFile.getPath());

        return recordFile.getPath();
    }

    /**
     *
     * Creates a file with a unique name in the external storage
     * of the app if the external storage is available,
     * otherwise in the internal storage
     *
     * @param context Activity's context
     * @param ext extension of file
     * @return path of the created File
     */
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
