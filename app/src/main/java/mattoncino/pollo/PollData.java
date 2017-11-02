package mattoncino.pollo;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
  * Represents a Poll object with its meta-data, data that depends
  * and changes in the lifetime of a Poll object.
  *
  */
public class PollData extends BaseObservable implements Parcelable, Serializable {
    private Poll poll;
    private Set<String> contactedDevices;
    private Set<String> acceptedDevices;
    private Set<String> votedDevices;
    private int responseCount;
    private int[] votes;
    private int myVote;
    private boolean disabled;
    private String hostAddress;
    private int owner;
    private boolean terminated;


    /**
     * Constructor
     *
     * @param poll Poll object @link mattoncino.pollo.Poll
     * @param hostAddress local ip address of the device which has created the Poll
     * @param owner owner of the poll : [OWN, OTHER]
     */
    public PollData(Poll poll, String hostAddress, int owner) {
        this.poll = poll;
        this.contactedDevices = new HashSet<>();
        this.acceptedDevices = new HashSet<>();
        this.votedDevices = new HashSet<>();
        this.votes = new int[5];
        this.myVote = 0;
        this.hostAddress = hostAddress;
        this.disabled = false;
        this.owner = owner;
        this.terminated = false;
        this.responseCount = 0;
    }

    /** Returns Poll object */
    public Poll getPoll() {
        return poll;
    }

    /**
     * Sets Poll object
     * @param poll
     */
    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    /** Returns list of host addresses of users which have voted for the Poll */
    public Set<String> getVotedDevices() {
        return votedDevices;
    }

    /** Returns poll's identifier */
    public String getID() {
        return poll.getId();
    }

    /** Returns poll's title */
    public String getPollName(){
        return poll.getName();
    }

    /** Returns list of host addresses to whom sent request for the Poll */
    @Bindable
    public Set<String> getContactedDevices() {
        return contactedDevices;
    }

    /**
     * Set contacted devices list
     * @param contactedDevices
     */
    public void setContactedDevices(Set<String> contactedDevices) {
        this.contactedDevices = contactedDevices;
        notifyPropertyChanged(BR.contactedDevices);
    }

    /** Returns poll's votes */
    @Bindable
    public int[] getVotes() {
        return votes;
    }

    /**
     * Sets poll's votes
     * @param votes
     */
    public void setVotes(int[] votes){
        this.votes = votes;
        notifyPropertyChanged(BR.votes);
    }

    /**
     * Adds vote to the vote list
     * @param vote
     */
    public void addVote(int vote){
        votes[vote-1]++;
        notifyPropertyChanged(BR.votes);
    }

    /**
     * Adds voted user's host address to the list of votedDevices
     * @param hostAddress of the user which is voted for the poll
     */
    public void addVotedDevice(String hostAddress){
        votedDevices.add(hostAddress);
    }

    /** Returns vote of the user*/
    public int getMyVote() {
        return myVote;
    }

    /**
     * Sets vote of the user
     * @param myVote
     */
    public void setMyVote(int myVote) {
        this.myVote = myVote;
    }

    /**
     * @return list of accepted devices, devices which sent an accept message
     * for the poll request
     */
    @Bindable
    public Set<String> getAcceptedDevices(){
        return acceptedDevices;
    }

    /**
     * Adds host address of the device to the accepted devices list
     * @param hostAddress
     */
    public void addAcceptedDevice(String hostAddress){
        acceptedDevices.add(hostAddress);
        notifyPropertyChanged(BR.acceptedDevices);
    }

    /** Returns owner of the poll */
    public int getOwner() {
        return owner;
    }

    /** Returns true is poll is disabled, false otherwise */
    @Bindable
    public boolean isDisabled() {
        return disabled;
    }

    /** Sets poll as disabled */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        notifyPropertyChanged(BR.disabled);
    }

    /** Returns host address of the device */
    public String getHostAddress() {
        return hostAddress;
    }

    /** Returns true is poll is terminated, false otherwise */
    public boolean isTerminated() {
        return terminated;
    }

    /** Sets poll as terminated */
    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    /** Returns i-th option from the option list */
    public String getOption(int i){
        return poll.getOptions().get(i- 1);
    }

    /** Returns response count (accept or reject responses) for the poll */
    public int getResponseCount() {
        return responseCount;
    }

    /** increments response count */
    public void incrementResponseCount(){
        responseCount++;
    }

    /** Returns true is poll has an Image, false otherwise */
    public boolean hasImage(){
        return poll.hasImage();
    }

    /** Returns poll's image info */
    public ImageInfo getImageInfo(){
        return poll.getImageInfo();
    }

    /** Returns true if Poll has a record, false otherwise */
    public boolean hasRecord(){
        return poll.hasRecord();
    }

    /** Returns poll's record file path */
    public String getRecordPath(){
        return poll.getRecordPath();
    }

    /** Returns poll's record's duration */
    public int getDuration() {
        return poll.getDuration();
    }

    /** Return votes for the i-th option */
    public int getVotesFor(int opt) {
        return votes[opt - 1];
    }

    public int getSumVotes(){
        int sum = 0;
        for (int i = 0; i < votes.length; i++)
            sum += votes[i];

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
    }
}