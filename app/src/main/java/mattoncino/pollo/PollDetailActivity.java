package mattoncino.pollo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import mattoncino.pollo.databinding.ActivityPollDetailBinding;

public class PollDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_detail);
    }
}
