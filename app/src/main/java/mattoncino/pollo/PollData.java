package mattoncino.pollo;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class PollData extends BaseObservable implements Parcelable, Serializable {
    private Poll poll;
    private Set<String> contactedDevices; /* devices that i sent request */
    private Set<String> acceptedDevices; /* devices which sent accept response, we hold this set to send them result */
    private Set<String> votedDevices; /* devices which sent accept and voted */
    private int responseCount; /* #devices that sent accept/reject response */
    private int[] votes;
    private int myVote;
    private boolean disabled;
    private String hostAddress;
    private int owner;
    private boolean terminated;


    public PollData(Poll poll, String hostAddress, int owner) {
        this.poll = poll;
        this.contactedDevices = new HashSet<>(); //need to be thread safe!
        this.acceptedDevices = new HashSet<>(); //need to be thread safe!
        this.votedDevices = new HashSet<>(); //need to be thread safe!
        this.votes = new int[5];  //need to be thread safe!
        this.myVote = 0;
        this.hostAddress = hostAddress;
        this.disabled = false;
        this.owner = owner;
        this.terminated = false;
        //this.deviceCount = 0; /* number of devices that i sent request */
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
    public Set<String> getContactedDevices() {
        return contactedDevices;
    }

    public void setContactedDevices(Set<String> contactedDevices) {
        this.contactedDevices = contactedDevices;
        notifyPropertyChanged(BR.contactedDevices);
    }

    @Bindable
    public int[] getVotes() {
        return votes;
    }

    public void setVotes(int[] votes){
        this.votes = votes;
        notifyPropertyChanged(BR.votes);
    }

    public void addVote(int vote){
        votes[vote - 1]++;
        notifyPropertyChanged(BR.votes);
    }

    public void addVotedDevice(String hostAddress){
        votedDevices.add(hostAddress);
    }

    public int getMyVote() {
        return myVote;
    }

    public void setMyVote(int myVote) {
        this.myVote = myVote;
    }

    @Bindable
    public Set<String> getAcceptedDevices(){
        return acceptedDevices;
    }

    public void addAcceptedDevice(String hostAddress){
        acceptedDevices.add(hostAddress);
        notifyPropertyChanged(BR.acceptedDevices);
    }

    public int getOwner() {
        return owner;
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

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public String getOption(int i){
        return poll.getOptions().get(i- 1);
    }

    public int getResponseCount() {
        return responseCount;
    }

    public void incrementResponseCount(){
        responseCount++;
    }

    public boolean hasImage(){
        return poll.hasImage();
    }

    public ImageInfo getImageInfo(){
        return poll.getImageInfo();
    }

    public int getVotesFor(int opt) {
        //System.out.println("pollname: " + poll.getName() + " votes: " + Arrays.toString(votes));
        return votes[opt - 1];
    }

    public int getSumVotes(){
        int sum = 0;
        for (int i = 0; i < votes.length; i++) {
            sum += votes[i];
        }

        return sum;
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
        parcel.writeStringList(new ArrayList<>(contactedDevices));
        parcel.writeStringList(new ArrayList<>(acceptedDevices));
        parcel.writeStringList(new ArrayList<>(votedDevices));
        parcel.writeIntArray(votes);
        //parcel.writeString(hostAddress);
        //votes = Collections.synchronizedList(new ArrayList());
        //parcel.writeList(votes);

    }

    public static final Parcelable.Creator<PollData> CREATOR
            = new Parcelable.Creator<PollData>() {
        public PollData createFromParcel(Parcel in) {
            return new PollData(in);
        }

        public PollData[] newArray(int size) {
            return new PollData[size];
        }
    };

    public PollData(Parcel parcel) {
        poll = parcel.readParcelable(Poll.class.getClassLoader());
        hostAddress = parcel.readString();
        disabled = parcel.readByte() != 0;

        ArrayList<String> list3 = new ArrayList<>();
        parcel.readStringList(list3);
        contactedDevices = new HashSet<>(list3);

        ArrayList<String> list2 = new ArrayList<>();
        parcel.readStringList(list2);
        acceptedDevices = new HashSet<>(list2);

        ArrayList<String> list = new ArrayList<>();
        parcel.readStringList(list);
        votedDevices = new HashSet<>(list);

        votes = parcel.createIntArray();

        //parcel.readIntArray(votes);

        //hostAddress = parcel.readString();
        //votes      = Collections.synchronizedList(new ArrayList());
        //parcel.readList(votes, null);
    }
}