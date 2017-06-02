package mattoncino.pollo;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import mattoncino.pollo.databinding.ActivityCreatePollBinding;

public class CreatePollActivity extends AppCompatActivity {

    private ActivityCreatePollBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_create_poll);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_poll);

        binding.MultiOptPollActivityButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                startActivity(new Intent(CreatePollActivity.this, MultiOptPollActivity.class));
            }
        });
    }
}
