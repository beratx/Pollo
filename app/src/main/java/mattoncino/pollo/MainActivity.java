package mattoncino.pollo;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import mattoncino.pollo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Pollo Main Activity";
    private ActivityMainBinding binding;
    public static boolean exist_active_pool = true;
    public static boolean exist_saved_pool = true;
    public static PollManager pollManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        //pollManager = new PollManager();

        //if(wifi connected)
        //mNsdHelper = new NsdHelper(this);
        //mNsdHelper.initializeNsd();

        /*
        if(mConnection.getLocalPort() > -1) {
            mNsdHelper.registerService(mConnection.getLocalPort());
        } else {
            Log.d(TAG, "ServerSocket isn't bound.");
        }

        mNsdHelper.discoverServices();
        */



        binding.createPollActivityButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(wifiConnected())
                    startActivity(new Intent(MainActivity.this, mattoncino.pollo.MultiOptPollActivity.class));
            }
        });


        if(exist_active_pool) {
            binding.activePollsActivityButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, mattoncino.pollo.ActivePollsActivity.class));
                }
            });
            binding.activePollsActivityButton.setVisibility(View.VISIBLE);
        }

        if(exist_saved_pool) {
            binding.oldPollsActivityButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, mattoncino.pollo.OldPollsActivity.class));
                }
            });
            binding.oldPollsActivityButton.setVisibility(View.VISIBLE);
        }
    }


    private boolean wifiConnected() {
        // BEGIN_INCLUDE(connect)
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.i(TAG, getString(R.string.wifi_connection));
            return true;
        } else {
            Log.i(TAG, getString(R.string.mobile_connection) + " or " + R.string.no_wifi_or_mobile);
            Toast.makeText(this, "Pollo works only under LAN. Please activate your wifi and connect to an Access Point", Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
