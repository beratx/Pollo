package mattoncino.pollo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Poll implements Parcelable {
    private String name;
    private String question;
    private String first_opt;
    private String second_opt;
    private List votes;

    public Poll(String name, String question, String first_opt, String second_opt) {
        this.name = name;
        this.question = question;
        this.first_opt = first_opt;
        this.second_opt = second_opt;
        this.votes = Collections.synchronizedList(new ArrayList());
    }

    public String getName() {
        return name;
    }

    public String getQuestion() {
        return question;
    }

    public String getFirstOpt() {
        return first_opt;
    }

    public String getSecondOpt() {
        return second_opt;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(question);
        parcel.writeString(first_opt);
        parcel.writeString(second_opt);
        votes = Collections.synchronizedList(new ArrayList());
        parcel.writeList(votes);

    }

    public static final Parcelable.Creator<Poll> CREATOR
            = new Parcelable.Creator<Poll>() {
        public Poll createFromParcel(Parcel in) {
            return new Poll(in);
        }

        public Poll[] newArray(int size) {
            return new Poll[size];
        }
    };

    public Poll(Parcel parcel) {
        name       = parcel.readString();
        question   = parcel.readString();
        first_opt  = parcel.readString();
        second_opt = parcel.readString();
        votes      = Collections.synchronizedList(new ArrayList());
        parcel.readList(votes, null);
    }

}
