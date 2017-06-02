package mattoncino.pollo;

import android.databinding.DataBindingUtil;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import mattoncino.pollo.databinding.ActivityCurrentPollBinding;

public class CurrentPollActivity extends AppCompatActivity {
    private ActivityCurrentPollBinding binding;
    private Poll poll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_current_poll);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_current_poll);

        //to retrieve data
        Bundle data = getIntent().getExtras();
        poll = (Poll) data.getParcelable("poll");
        binding.setPoll(poll);

        /*if(poll == null)
            Snackbar.make(binding.nameTextView, "Poll object is null!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        else*/


        /*else {

            binding.nameTextView.setText(poll.getName());
            binding.questionTextView.setText(poll.getQuestion());
            binding.opt1TextView.setText(poll.getFirstOpt());
            binding.opt2TextView.setText(poll.getSecondOpt());
        }*/
    }
}
