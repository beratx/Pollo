package mattoncino.pollo;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import mattoncino.pollo.databinding.ActivityMultiOptPollBinding;

public class MultiOptPollActivity extends AppCompatActivity {
    private static final String TAG = "MultiOptPollActivity";
    private ActivityMultiOptPollBinding binding;

    //SharedPreferences pref;

    /*Recycler View for options?*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_multi_opt_poll);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_multi_opt_poll);

        //pref = PreferenceManager.getDefaultSharedPreferences(this);
        //SharedPreferences.Editor editor = pref.edit();


        binding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Poll poll = createPoll();

                if(poll == null)  return;



                Intent intent = new Intent(MultiOptPollActivity.this, mattoncino.pollo.ActivePollsActivity.class)
                     .putExtra("poll", (Parcelable) poll);
                startActivity(intent);

            }
        });

    }

    private Poll createPoll(){
        String name = binding.nameEditText.getText().toString();
        String question = binding.questionEditText.getText().toString();
        String first_opt = binding.opt1EditText.getText().toString();
        String second_opt = binding.opt2EditText.getText().toString();

        if(name.isEmpty() || question.isEmpty() || first_opt.isEmpty() || second_opt.isEmpty()){
            Snackbar.make(binding.saveButton, "You should fill all fields!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return null;
        }

        Poll poll = new Poll(name,question,first_opt,second_opt);

        return poll;

        //Log.e(TAG, "!!!DB INSERT Error!!!");

    }

    private void saveToDB(){

        SQLiteDatabase database = new PollDBSQLiteHelper(this).getWritableDatabase();
        ContentValues values = new ContentValues();

        if(binding.nameEditText.getText().toString().isEmpty()
           || binding.questionEditText.getText().toString().isEmpty()
           || binding.opt1EditText.getText().toString().isEmpty()
           || binding.opt2EditText.getText().toString().isEmpty()){

            Snackbar.make(binding.saveButton, "You should fill all fields!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        /*We will save it when poll will be finished!)
        values.put(PollDBContract.Poll.COLUMN_NAME, binding.nameEditText.getText().toString());
        values.put(PollDBContract.Poll.COLUMN_QUESTION, binding.questionEditText.getText().toString());
        values.put(PollDBContract.Poll.COLUMN_FIRST_OPT, binding.opt1EditText.getText().toString());
        values.put(PollDBContract.Poll.COLUMN_SECOND_OPT, binding.opt2EditText.getText().toString());
        //values.put(PollDBContract.Poll.COLUMN_DATE, "2017-05-30");


        /*try {
            // Create an instance of SimpleDateFormat used for formatting
            // the string representation of date (month/day/year)
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            // Get the date today using Calendar object.
            Date today = Calendar.getInstance().getTime();
            // Using DateFormat format method we can create a string
            // representation of a date with the defined format.
            String reportDate = df.format(today);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2017);
            cal.set(Calendar.MONTH, Calendar.APRIL);
            cal.set(Calendar.DAY_OF_MONTH, 28);
            long date = cal.getTimeInMillis();
            values.put(PollDBContract.Poll.COLUMN_DATE, date);
        }
        catch (Exception e) {
            Log.e(TAG, "Error", e);
            Toast.makeText(this, "Date is in the wrong format", Toast.LENGTH_LONG).show();
            return;
        }*/

        try {
            long newRowId = database.insertOrThrow(PollDBContract.Poll.TABLE_NAME, null, values);
            Toast.makeText(this, "The new Row Id is " + newRowId, Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Log.e(TAG, "!!!DB INSERT Error!!!", e);
            Toast.makeText(this, "Cant insert to db, there is an error ", Toast.LENGTH_LONG).show();

        }

    }


}
