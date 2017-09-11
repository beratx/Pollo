    package mattoncino.pollo;

    import android.content.ContentValues;
    import android.content.Intent;
    import android.database.sqlite.SQLiteDatabase;
    import android.databinding.DataBindingUtil;
    import android.graphics.Bitmap;
    import android.net.Uri;
    import android.os.Bundle;
    import android.os.Parcelable;
    import android.os.StrictMode;
    import android.support.design.widget.Snackbar;
    import android.support.design.widget.TextInputLayout;
    import android.support.v7.app.AppCompatActivity;
    import android.util.Log;
    import android.view.View;
    import android.widget.RelativeLayout;
    import android.widget.Toast;

    import java.util.ArrayList;
    import java.util.HashSet;
    import java.util.List;

    import mattoncino.pollo.databinding.ActivityMultiOptPollBinding;

    public class MultiOptPollActivity extends AppCompatActivity {
        private static final String TAG = "MultiOptPollActivity";
        private static final int PICK_IMAGE_ID = 87;
        private ActivityMultiOptPollBinding binding;
        private int count = 6;
        //private List<TextInputLayout> optionsViews = new ArrayList<TextInputLayout>();
        private List<String> options = new ArrayList<String>();
        private boolean hasImage = false;
        private Bitmap bitmap;
        private ImageInfo imageInfo;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //setContentView(R.layout.activity_multi_opt_poll);
            binding = DataBindingUtil.setContentView(this, R.layout.activity_multi_opt_poll);

            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                        .permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }


            //optionsViews.add(binding.opt1InputLayout);
            //optionsViews.add(binding.opt2InputLayout);

            binding.addFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final RelativeLayout rLayout = (RelativeLayout) findViewById(R.id.activity_multi_opt_poll);
                    rLayout.getChildAt(count++).setVisibility(View.VISIBLE);
                    System.out.println("addFab: count: " + count);
                    //rLayout.addView(createNewOptionEntry());

                    if(count == 9) binding.addFAB.setVisibility(View.GONE);
                }
            });

            binding.addImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent chooseImageIntent = ImagePicker.getPickImageIntent(MultiOptPollActivity.this);
                    startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
                }
            });

            binding.saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Poll poll = createPoll();

                    if (poll == null) return;

                    /*if(!presentOnlineDevices()) {
                        Toast.makeText(MultiOptPollActivity.this, "No device is present", Toast.LENGTH_LONG).show();
                        return;
                    }*/

                    Intent intent = new Intent(MultiOptPollActivity.this, mattoncino.pollo.ActivePollsActivity.class)
                            .putExtra(Consts.OWNER, Consts.OWN)
                            .putExtra(Consts.POLL, (Parcelable) poll);

                    startActivity(intent);
                }
            });
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch(requestCode) {
                case PICK_IMAGE_ID:
                    if(resultCode == RESULT_OK){
                        Log.d(TAG, "RESULT IS OK");
                        imageInfo = ImagePicker.getImageFromResult(this, resultCode, data);
                        bitmap = ImagePicker.getBitmapImage(Uri.parse(imageInfo.getPath()), MultiOptPollActivity.this, imageInfo.isCamera());
                        binding.imageView.setVisibility(View.VISIBLE);
                        binding.imageView.setImageBitmap(bitmap);
                        binding.imageView.invalidate();
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


        private boolean presentOnlineDevices(){
            String deviceId = ((MyApplication)getApplication()).getDeviceId();
            mattoncino.pollo.JmDnsManager connManager = ((MyApplication)getApplication()).getConnectionManager();
            final HashSet<String> onlineDevices = (HashSet<String>) connManager.getOnlineDevices(MultiOptPollActivity.this);
            return onlineDevices.size() != 0;
        }


        private Poll createPoll(){
            String name = binding.nameEditText.getText().toString();
            String question = binding.questionEditText.getText().toString();

            final RelativeLayout rLayout = (RelativeLayout) findViewById(R.id.activity_multi_opt_poll);

            for (int i = 4; i < count; i++) {
                TextInputLayout til = (TextInputLayout) rLayout.getChildAt(i);
                String op = til.getEditText().getText().toString();
                options.add(op);
            }

            if(name.isEmpty() || question.isEmpty() || isEmpty(options)){
                Snackbar.make(binding.saveButton, "You should fill all fields!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
                return null;
            }


            mattoncino.pollo.JmDnsManager connectionManager = ((MyApplication) getApplication()).getConnectionManager();
            if (connectionManager == null) {
                Log.d(TAG, "connectionManager is null!!!");
                return null;
            }

            Poll poll = new Poll(name, question, options, hasImage, imageInfo);

            return poll;
        }

        private boolean isEmpty(List<String> options){
            for(String op : options){
                if(op.isEmpty())
                    return true;
            }
            return false;
        }

        @Override
        public void onBackPressed()
        {
            startActivity(new Intent(MultiOptPollActivity.this, mattoncino.pollo.MainActivity.class));
        }



    }
