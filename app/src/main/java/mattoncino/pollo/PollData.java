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
    private Set<String> contactedDevices; /* devices which sent accept response */
    private Set<String> votedDevices; /* devices which sent accept and voted */
    private List<Integer> votes;
    private List<Double> result;
    private boolean disabled;
    private String hostAddress;
    private int owner;
    private boolean terminated;
    private int deviceCount; /* #devices that i sent request */
    private int responseCount; /* #devices that sent accept/reject response */

    public PollData(Poll poll, String hostAddress, int owner) {
        this.poll = poll;
        this.contactedDevices = new HashSet<>(); //need to be thread safe!
        this.votedDevices = new HashSet<>(); //need to be thread safe!
        this.votes = new ArrayList<>();  //need to be thread safe!
        this.hostAddress = hostAddress;
        this.disabled = false;
        this.owner = owner;
        this.terminated = false;
        this.deviceCount = 0; /* number of devices that i sent request */
        this.responseCount = 0; /* number of devices that sent accept/reject response */
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public Set<String> getVotedDevices() {
        return votedDevices;
    }

    public String getID() {
        return poll.getId();
    }

    public String getPollName(){
        return poll.getName();
    }

    @Bindable
    public List<Integer> getVotes() {
        return votes;
    }

    public void addVote(int vote){
        votes.add(vote);
        notifyPropertyChanged(BR.votes);
    }

    public void addVotedDevice(String hostAddress){
        votedDevices.add(hostAddress);
    }

    public Set<String> getContactedDevices(){
        return contactedDevices;
    }

    public void addContactedDevice(String hostAddress){
        contactedDevices.add(hostAddress);
    }

    public void removeDevice(String hostAddress){
        contactedDevices.remove(hostAddress);
    }


    public List<Double> getResult() {
        return result;
    }

    public void setResult(List<Double> result) {
        this.result = result;
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

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public String getText(int opt){
        return poll.getOptions().get(opt - 1) + " >> " + getResult(opt);
    }

    public int getResponseCount() {
        return responseCount;
    }

    public void incrementResponseCount(){
        responseCount++;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(int count) {
        this.deviceCount = count;
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
        parcel.writeStringList(new ArrayList<String>(contactedDevices));
        parcel.writeStringList(new ArrayList<String>(votedDevices));
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

        ArrayList<String> list2 = new ArrayList<>();
        parcel.readStringList(list2);
        contactedDevices = new HashSet<String>(list2);

        ArrayList<String> list = new ArrayList<>();
        parcel.readStringList(list);
        votedDevices = new HashSet<String>(list);

        //hostAddress = parcel.readString();
        //votes      = Collections.synchronizedList(new ArrayList());
        //parcel.readList(votes, null);
    }
}