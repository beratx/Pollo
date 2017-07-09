package mattoncino.pollo;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PollData extends BaseObservable implements Parcelable, Serializable {
    private Poll poll;
    private Set<String> voters;
    private List<Integer> votes;
    private boolean disabled;
    private String hostAddress;
    private int owner;

    public PollData(Poll poll, String hostAddress, int owner) {
        this.poll = poll;
        this.voters = new HashSet<>(); //need to be thread safe!
        this.votes = new ArrayList<>();  //need to be thread safe!
        this.hostAddress = hostAddress;
        this.disabled = false;
        this.owner = owner;
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public Set<String> getVoters() {
        return voters;
    }

    public String getID() {
        return poll.getId();
    }

    public void setVoters(Set<String> voters) {
        this.voters = voters;
    }

    public String getPollName(){
        return poll.getName();
    }


    @Bindable
    public List<Integer> getVotes() {
        return votes;
    }

    public void setVotes(List<Integer> votes) {
        this.votes = votes;
    }

    public void addVote(int vote){
        votes.add(vote);
        notifyPropertyChanged(BR.votes);
    }

    public void addVoter(String hostAddress){
        voters.add(hostAddress);
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    @Bindable
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        notifyPropertyChanged(BR.disabled);
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getText(int opt){
        return poll.getOptions().get(opt - 1) + " >> " + getResult(opt);
    }

    public double getResult(int opt) {
        int count = 0;

        for (Integer vote : votes) {
            if (vote == opt)
                count++;
        }

        System.out.println("pollname: " + poll.getName() + " votes: " + votes.toString());

        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PollData pollData = (PollData) o;

        if (!getPoll().equals(pollData.getPoll())) return false;
        return getHostAddress().equals(pollData.getHostAddress());

    }

    @Override
    public int hashCode() {
        int result = getPoll().hashCode();
        result = 31 * result + getHostAddress().hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(poll,i);
        parcel.writeString(hostAddress);
        parcel.writeByte((byte) (disabled ? 1 : 0));
        parcel.writeStringList(new ArrayList<String>(voters));
        //parcel.writeString(hostAddress);
        //votes = Collections.synchronizedList(new ArrayList());
        //parcel.writeList(votes);

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

    public PollData(Parcel parcel) {
        poll = parcel.readParcelable(Poll.class.getClassLoader());
        hostAddress = parcel.readString();
        disabled = parcel.readByte() != 0;
        ArrayList<String> list = new ArrayList<>();
        parcel.readStringList(list);
        voters = new HashSet<String>(list);
        //hostAddress = parcel.readString();
        //votes      = Collections.synchronizedList(new ArrayList());
        //parcel.readList(votes, null);
    }
}