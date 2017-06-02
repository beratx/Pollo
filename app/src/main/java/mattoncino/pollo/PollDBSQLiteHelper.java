package mattoncino.pollo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by berat on 28.04.2017.
 */

public class PollDBSQLiteHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 4;
    public static final String DATABASE_NAME = "poll_database";

    public PollDBSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(PollDBContract.Poll.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + PollDBContract.Poll.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
