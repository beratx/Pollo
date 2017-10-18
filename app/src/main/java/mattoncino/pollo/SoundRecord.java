package mattoncino.pollo;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SoundRecord {
    private static final String TAG = "SoundRecord";
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private String recordPath = null;
    private boolean isPlaying = false;

    public SoundRecord(String recordPath){
        this.recordPath = recordPath;
    }

    public boolean isPlay(){
        return isPlaying;
    }

    public void setPlay(){
        isPlaying = !isPlaying;
    }

    public void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(recordPath);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    public void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    public void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(recordPath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

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

        String recordPath =  external ? context.getExternalCacheDir().getAbsolutePath() + "/" + timestamp + "." + ext
                                      : context.getCacheDir().getAbsolutePath() + "/" + timestamp + "." + ext;

        Log.d(TAG, "record file path: " + recordPath);

        return recordPath;
    }

    public static String createTempFile(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean external = ImagePicker.isExternalStorageWritable();

        String recordPath =  external ? context.getExternalCacheDir().getAbsolutePath() + "/" + timestamp + "." + ext
                                      : context.getCacheDir().getAbsolutePath() + "/" + timestamp + "." + ext;

        Log.d(TAG, "record file path: " + recordPath);

        return recordPath;
    }

    public static String createFile2(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean external = ImagePicker.isExternalStorageWritable();

        /*File dir = external ? new File(Environment.getExternalStorageDirectory() + "/pollo_records")
                            : new File(context.getFilesDir() + "/pollo_records");*/

        /*if (!dir.exists()) {
            //TODO : replace /sdcard/ with Environment.getExternalStorageDirectory().getPath();
            File recordDir = external ? new File("/sdcard/pollo_records/") : new File("/pollo_records");
            recordDir.mkdirs();
        }*/

        String recordPath =  external ? Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + timestamp + "." + ext
                                      : context.getFilesDir().getAbsolutePath() + "/" + timestamp + "." + ext;

        Log.d(TAG, "record file path: " + recordPath);

        return recordPath;
        //return File.createTempFile(timestamp, "." + ext, dir).getAbsolutePath();

    }


    /*public static File createFile(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean external = ImagePicker.isExternalStorageWritable();

        return external ? new File(context.getExternalCacheDir(), "pollo_" + timestamp + "." + ext)
                        : new File(context.getCacheDir() , "pollo_" + timestamp + "." + ext);
    }*/
}
