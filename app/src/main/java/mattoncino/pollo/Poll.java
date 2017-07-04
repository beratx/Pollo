package mattoncino.pollo;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Poll extends BaseObservable implements Parcelable, Serializable {

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

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
    }

    public void setQuestion(String question) {
        this.question = question;
        notifyPropertyChanged(BR.question);
    }

    public void setOptions(List<String> options) {
        this.options = options;
        notifyPropertyChanged(BR.options);
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
        notifyPropertyChanged(BR.hostAddress);
    }

    public void setParticipants(Set participants) {
        this.participants = participants;
        notifyPropertyChanged(BR.participants);
    }

    @Bindable
    public String getQuestion() {
        return question;
    }

    @Bindable
    public List<String> getOptions(){
        return options;
    }

    @Bindable
    public String getHostAddress() {
        return hostAddress;
    }

    @Bindable
    public List<String> getParticipants(){
        return new ArrayList<String>(participants);
    }

    public void addParticipant(String device){
        participants.add(device);
        notifyPropertyChanged(BR.participants);
    }

    public int participantCount(){
        return participants.size();
    }

    @Bindable
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        notifyPropertyChanged(BR.disabled);
    }

    public String getText(int vote){
        return getName() + " >> " + getResult(vote);
    }

    public void setOption(int i, String text){
        options.set(i,text);
    }


    /*public void setHostAddress(String hostAddress){
        this.hostAddress = hostAddress;
    }*/

    @Bindable
    public List<Integer> getVotes() {
        return votes;
    }

    public void setVotes(List<Integer> votes) {
        this.votes = votes;
        notifyPropertyChanged(BR.votes);
    }

    public void addVote(int vote){
        votes.add(vote);
        notifyPropertyChanged(BR.votes);
    }

    public double getResult(int opt){
        int count = 0;
        //if(votes.size() == 0) return 0.0;

        for (Integer vote:votes) {
            if(vote == opt)
                count++;
        }

        System.out.println("pollname: " + name + " votes: " + votes.toString());

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
