package mattoncino.pollo;

import android.provider.BaseColumns;

/**
 * Created by berat on 28.04.2017.
 */

public final class PollDBContract {

    private PollDBContract() {
    }

    public static class Poll implements BaseColumns {
        public static final String TABLE_NAME = "poll";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_QUESTION = "question";
        public static final String COLUMN_FIRST_OPT = "first_option";
        public static final String COLUMN_SECOND_OPT = "second_option";
        public static final String COLUMN_DATE = "date";

        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_QUESTION + " TEXT, " +
                COLUMN_FIRST_OPT + " TEXT, " +
                COLUMN_SECOND_OPT + " TEXT" + ")";
                //COLUMN_DATE + " INTEGER" + ")";
    }
}
