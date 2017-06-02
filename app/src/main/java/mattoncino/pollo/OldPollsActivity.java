package mattoncino.pollo;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;

import mattoncino.pollo.databinding.ActivityOldPollsBinding;

public class OldPollsActivity extends AppCompatActivity {
    private static final String TAG = "OldPollsActivity";
    private ActivityOldPollsBinding binding;
    //private RecyclerView.Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Old Polls");
        //setContentView(R.layout.activity_old_polls);
        binding = DataBindingUtil.setContentView(
                this, R.layout.activity_old_polls);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));


        getPollListFromDB();
    }

    private void getPollListFromDB() {
        /*String name = binding.nameEditText.getText().toString();
        String question = binding.questionEditText.getText().toString();
        String first_option = binding.opt1EditText.getText().toString();
        String second_option = binding.opt2EditText.getText().toString();*/
        //long date = 0;

        /*try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime((new SimpleDateFormat("dd/MM/yyyy")).parse(
                    binding.foundedEditText.getText().toString()));
            date = calendar.getTimeInMillis();
        }
        catch (Exception e) {}
        */
        String[] projection = new String[]{"_id", PollDBContract.Poll.COLUMN_NAME};

        SQLiteDatabase database = new PollDBSQLiteHelper(this).getReadableDatabase();

        /*String[] projection = {
                PollDBContract.Poll._ID,
                PollDBContract.Poll.COLUMN_NAME
        };*/

        /*String selection =
                PollDBContract.Poll.COLUMN_NAME + " like ?";*/

        //String[] selectionArgs = {"%" + name + "%", question + "%" + first_option + "%" + second_option + "%"};
        //String[] selectionArgs = {"%" + name};

        Cursor cursor = database.query(
                PollDBContract.Poll.TABLE_NAME,     // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // don't sort
        );

        binding.recyclerView.setAdapter(new PollsRecyclerViewCursorAdapter(this, cursor));
    }

}
