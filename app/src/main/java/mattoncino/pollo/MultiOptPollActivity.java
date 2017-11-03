    package mattoncino.pollo;

    import android.Manifest;
    import android.content.Context;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.databinding.DataBindingUtil;
    import android.graphics.Bitmap;
    import android.media.MediaPlayer;
    import android.net.Uri;
    import android.os.Bundle;
    import android.os.Parcelable;
    import android.os.StrictMode;
    import android.os.SystemClock;
    import android.os.Vibrator;
    import android.support.annotation.NonNull;
    import android.support.design.widget.FloatingActionButton;
    import android.support.design.widget.Snackbar;
    import android.support.design.widget.TextInputLayout;
    import android.support.v4.app.ActivityCompat;
    import android.support.v7.app.AppCompatActivity;
    import android.util.Log;
    import android.view.MotionEvent;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.RelativeLayout;

    import java.io.File;
    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.List;

    import mattoncino.pollo.databinding.ActivityMultiOptPollBinding;

    /**
     * <p>Activity Class to create and launch a new Poll.</p>
     * <p>
     * A Poll must include at least:
     * <ul>
     * <li> A Title, it will be showed in the new poll notification
     * <li> A text based Question
     * <li> Two text based Options
     * </ul>
     * And can include:
     * <ul>
     * <li> Three more text based options
     * <li> An image as part of the question
     * <li> A sound record as part of the question
     * </ul>
     * </p>
     *
     */

    public class MultiOptPollActivity extends AppCompatActivity {
        private static final String TAG = "MultiOptPollActivity";
        private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
        private static final int PICK_IMAGE_ID = 87;
        private static int VIEW_MAX = 12;
        private static int FIRST_CHILD = 4;
        private ActivityMultiOptPollBinding binding;
        private Bitmap bitmap;
        private ImageInfo imageInfo;
        private boolean hasImage = false;
        private int count = FIRST_CHILD + 2;
        private SoundRecord record;
        private String recordPath;
        private boolean hasRecord = false;
        private int duration = -1;

        // Requesting permission to RECORD_AUDIO
        private boolean permissionToRecordAccepted = false;
        private String [] permissions = {Manifest.permission.RECORD_AUDIO};


        /**
         * If user added image and/or sound record to the poll,
         * save their paths to the bundle
         *
         * @param savedInstanceState
         */
        @Override
        public void onSaveInstanceState(Bundle savedInstanceState){
            super.onSaveInstanceState(savedInstanceState);
            if(imageInfo != null) {
                savedInstanceState.putString("imagePath", imageInfo.getPath());
                savedInstanceState.putBoolean("isCamera", imageInfo.isCamera());
            }
            if(hasRecord){
                savedInstanceState.putString("recordPath", recordPath);
                savedInstanceState.putInt("duration", duration);
            }
        }

        /**
         * If user added image and/or sound record to the poll,
         * restore their paths from bundle and sets the UI to
         * display/play them
         *
         * @param savedInstanceState
         * @see ImageInfo
         */
        @Override
        protected void onRestoreInstanceState(Bundle savedInstanceState){
            if(savedInstanceState != null) {
                super.onRestoreInstanceState(savedInstanceState);

                imageInfo = new ImageInfo(savedInstanceState.getString("imagePath"), savedInstanceState.getBoolean("isCamera"));
                if(imageInfo.getPath() != null) {
                    bitmap = ImagePicker.getBitmapImage(Uri.parse(imageInfo.getPath()), MultiOptPollActivity.this, imageInfo.isCamera());

                    ViewGroup.LayoutParams params = binding.imageCardView.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    binding.imageCardView.setLayoutParams(params);

                    binding.imageView.setVisibility(View.VISIBLE);
                    binding.imageView.setImageBitmap(bitmap);
                    binding.imageView.invalidate();

                    hasImage = true;
                }

                recordPath = savedInstanceState.getString("recordPath");
                if(recordPath != null){
                    duration = savedInstanceState.getInt("duration");
                    binding.chronometer.setBase(SystemClock.elapsedRealtime() - duration);
                    hasRecord = true;
                }
            }
        }


        /**
         * If grant permission for sound record then
         * creates a temp file for the sound record
         * and initiliazes a SoundRecord object to manage
         * sound record and play.
         *
         * @param requestCode
         * @param permissions
         * @param grantResults
         * @see SoundRecord
         */
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            //TODO fix it to ask for permission adeguately
            switch (requestCode){
                case REQUEST_RECORD_AUDIO_PERMISSION:
                    permissionToRecordAccepted  = grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    //TODO check for a better file extension
                    recordPath = SoundRecord.getTempFile(MultiOptPollActivity.this, "3gp");
                    record = new SoundRecord(recordPath);
                    break;
            }
            if (!permissionToRecordAccepted ) {
                //TODO display a dialog to ask and put controls in case no permission granted
                binding.recordCardView.setVisibility(View.GONE);
            }

        }

        /**
         * Sets binding of the layout and listeners for buttons.
         *
         * @param savedInstanceState
         */
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = DataBindingUtil.setContentView(this, R.layout.activity_multi_opt_poll);
            setTitle("Create Poll");

            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                        .permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }


            binding.addFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final RelativeLayout rLayout = (RelativeLayout) findViewById(R.id.activity_multi_opt_poll);
                    rLayout.getChildAt(count++).setVisibility(View.VISIBLE);
                    System.out.println("addFab: count: " + count);

                    if(count == VIEW_MAX) binding.addFAB.setVisibility(View.GONE);
                }
            });

            binding.addImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent chooseImageIntent = ImagePicker.getPickImageIntent(MultiOptPollActivity.this);
                    startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
                }
            });


            binding.recordFAB.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch(event.getAction() & MotionEvent.ACTION_MASK){
                        case MotionEvent.ACTION_DOWN:
                            binding.recordFAB.setSize(FloatingActionButton.SIZE_NORMAL);
                            Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(250);
                            record.startRecording();
                            binding.chronometer.setBase(SystemClock.elapsedRealtime());
                            binding.chronometer.start();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_POINTER_UP:
                        case MotionEvent.ACTION_CANCEL:
                            binding.recordFAB.setSize(FloatingActionButton.SIZE_MINI);
                            binding.chronometer.stop();
                            record.stopRecording();
                            duration = record.getDuration(MultiOptPollActivity.this);
                            Log.d(TAG, "record duration: " + duration); //milliSeconds
                            hasRecord = true;
                            break;
                    }
                    return true;
                }
            });

            binding.playFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(hasRecord) {
                        if (record.isPlaying()) {
                            record.startPlaying();
                            record.setCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    binding.playFAB.setImageResource(android.R.drawable.ic_media_play);
                                    binding.chronometer.stop();
                                    record.flipPlay();
                                }
                            });
                            binding.chronometer.setBase(SystemClock.elapsedRealtime());
                            binding.chronometer.start();
                            binding.playFAB.setImageResource(android.R.drawable.ic_media_pause);
                        } else {
                            record.stopPlaying();
                            binding.chronometer.stop();
                            binding.chronometer.setBase(SystemClock.elapsedRealtime() - duration);
                            binding.playFAB.setImageResource(android.R.drawable.ic_media_play);
                        }

                        record.flipPlay();
                    }
                }
            });

            binding.removeFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(hasRecord) {
                        binding.chronometer.setBase(SystemClock.elapsedRealtime());
                        duration = -1;
                        hasRecord = false;
                    }
                }
            });



            binding.launchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String name = binding.nameEditText.getText().toString();
                    String question = binding.questionEditText.getText().toString();

                    final RelativeLayout rLayout = (RelativeLayout) findViewById(R.id.activity_multi_opt_poll);
                    List<String> options = new ArrayList<>();

                    for (int i = 4; i < count; i++) {
                        TextInputLayout til = (TextInputLayout) rLayout.getChildAt(i);
                        String op = til.getEditText().getText().toString();
                        options.add(op);
                    }

                    if(name.isEmpty() || question.isEmpty() || isEmpty(options)){
                        Snackbar.make(binding.launchButton, "You should fill all fields!", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        return;
                    }


                    if(imageInfo != null && imageInfo.isCamera()){
                        String tempPath = imageInfo.getPath().substring(7);
                        File temp = new File(tempPath);
                        try {
                            File perm = ImagePicker.createFile(MultiOptPollActivity.this, "jpg");
                            boolean r = temp.renameTo(perm);
                            if(r) {
                                String realPath = perm.getPath();
                                imageInfo.setPath("file://" + realPath);
                                Log.v(TAG, "image file renamed successfully");
                            } else Log.wtf(TAG, "can't rename image file");
                        } catch(IOException e){
                            e.printStackTrace();
                        }
                    }

                    if(hasRecord){
                        File temp = new File(recordPath);
                        try {
                            File perm = ImagePicker.createFile(MultiOptPollActivity.this, "3gp");
                            boolean r = temp.renameTo(perm);
                            if(r) {
                                recordPath = perm.getPath();
                                Log.v(TAG, "record file renamed successfully");
                            } else Log.wtf(TAG, "can't rename record file");
                        } catch(IOException e){
                            e.printStackTrace();
                        }

                    } else recordPath = null;

                    Poll poll = new Poll(name, question, options, hasImage, imageInfo, recordPath, duration);

                    Intent intent = new Intent(MultiOptPollActivity.this, mattoncino.pollo.ActivePollsActivity.class)
                            .putExtra(Consts.OWNER, Consts.OWN)
                            .putExtra(Consts.POLL, (Parcelable) poll);

                    startActivity(intent);
                }
            });
        }


        /**
         * Displays returned image in the imageView and
         * sets flag hasImage to indicate that poll has
         * an image
         *
         * @param requestCode
         * @param resultCode
         * @param data
         */
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch(requestCode) {
                case PICK_IMAGE_ID:
                    if(resultCode == RESULT_OK){
                        Log.d(TAG, "RESULT IS OK");
                        imageInfo = ImagePicker.getImageFromResult(this, resultCode, data);
                        bitmap = ImagePicker.getBitmapImage(Uri.parse(imageInfo.getPath()), MultiOptPollActivity.this, imageInfo.isCamera());
                        binding.imageView.setImageBitmap(bitmap);
                        binding.imageView.setVisibility(View.VISIBLE);

                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) binding.imageCardView.getLayoutParams();
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        //binding.imageView.invalidate();
                        hasImage = true;
                    }
                    else
                        Log.d(TAG, "RESULT CANCELED");
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }

        /**
         * Checks if there is an empty option
         *
         * @param options Option list of the poll
         * @return <code>true</code> if there is empty option
         *         <code>false</code> otherwise
         */
        private boolean isEmpty(List<String> options){
            for(String op : options){
                if(op.isEmpty())
                    return true;
            }
            return false;
        }

        /** When Back button is pressed user will return to Home */
        @Override
        public void onBackPressed()
        {
            startActivity(new Intent(MultiOptPollActivity.this, mattoncino.pollo.MainActivity.class));
        }

    }
