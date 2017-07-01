package mattoncino.pollo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Poll implements Parcelable, Serializable {
    private String name;
    private String question;
    private List<String> options;
    private String hostAddress;
    //private int owner;
    private List<Integer> votes;
    private Set participants;
    private boolean disabled;

    public Poll(String name, String question, List<String> options, String hostAddress) {
        this.name = name;
        this.question = question;
        this.options = options;
        //this.owner = owner;
        this.hostAddress = hostAddress;
        this.votes = Collections.synchronizedList(new ArrayList<Integer>());
        this.participants = Collections.synchronizedSet(new HashSet<String>());
        this.disabled = false;
    }

    public String getName() {
        return name;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions(){
        return options;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public List<String> getParticipants(){
        return new ArrayList<String>(participants);
    }

    public void addParticipant(String device){
        participants.add(device);
    }

    public int participantCount(){
        return participants.size();
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setOption(int i, String text){
        options.set(i,text);
    }


    /*public void setHostAddress(String hostAddress){
        this.hostAddress = hostAddress;
    }*/

    //!!!ATTENTION WE SKIP THE "0" !!!
    public void addVote(int vote){
        votes.add(vote);
    }

    public double getResult(int opt){
        int count = 0;
        if(votes.size() == 0) return 0.0;

        for (Integer vote:votes) {
            if(vote == opt)
                count++;
        }

        return count;
        //return count/votes.size();

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
        parcel.writeStringList(options);
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
        options = new ArrayList<String>();
        parcel.readStringList(options);
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
        if (!getOptions().equals(poll.getOptions())) return false;
        return getHostAddress().equals(poll.getHostAddress());

    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getQuestion().hashCode();
        result = 31 * result + getOptions().hashCode();
        result = 31 * result + getHostAddress().hashCode();
        return result;
    }
}
