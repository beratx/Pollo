package mattoncino.pollo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Poll implements Parcelable, Serializable {
    private String name;
    private String question;
    private String first_opt;
    private String second_opt;
    private String hostAddress;
    //private int owner;
    private List votes;



    public Poll(String name, String question, String first_opt, String second_opt, String hostAddress) {
        this.name = name;
        this.question = question;
        this.first_opt = first_opt;
        this.second_opt = second_opt;
        //this.owner = owner;
        this.hostAddress = hostAddress;
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

    public String getHostAddress() {
        return hostAddress;
    }

/*public int getOwner() {
        return owner;
    }

    public void setOwner(int owner){
        this.owner = owner;
    }*/

    /*public void setHostAddress(String hostAddress){
        this.hostAddress = hostAddress;
    }*/

    public void addVote(int vote){
        votes.add(vote);
    }

    public double getResult(int opt){
        //int first = 0, second = 0, total = 0;
        if(votes.size() == 0) return 0.0;
        int count = 0;
        for(int vote=0; vote<votes.size(); vote++){
            if(vote == opt)
                count++;
        }

        return count/votes.size();

        //double firstPercent = (double)first / total;
        //double secondPercent = (double)second / total;

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
        parcel.writeString(hostAddress);
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
        hostAddress = parcel.readString();
        votes      = Collections.synchronizedList(new ArrayList());
        parcel.readList(votes, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Poll poll = (Poll) o;

        if (!getName().equals(poll.getName())) return false;
        if (!getQuestion().equals(poll.getQuestion())) return false;
        if (!first_opt.equals(poll.first_opt)) return false;
        if (!second_opt.equals(poll.second_opt)) return false;
        return getHostAddress().equals(poll.getHostAddress());

    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getQuestion().hashCode();
        result = 31 * result + first_opt.hashCode();
        result = 31 * result + second_opt.hashCode();
        result = 31 * result + getHostAddress().hashCode();
        return result;
    }
}
