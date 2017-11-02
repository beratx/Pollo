package mattoncino.pollo;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Poll object that can be created through
 * MultiOptPollActivity class or can be received through the network
 */
public class Poll extends BaseObservable implements Parcelable, Serializable {
    private String id;
    private String name;
    private String question;
    private List<String> options;
    private ImageInfo imageInfo;
    private boolean hasImage;
    private String recordPath;
    private boolean hasRecord;
    private int duration;

    /**
     * Constructor.
     *
     * @param id identifier of the poll
     * @param name title of poll
     * @param question question of poll
     * @param options a list of text based options (from 2 up to 5 options)
     * @param hasImage flag to indicate if it has an image
     * @param info store image info in case it has an image
     * @param recordPath  file path of sound record in case it has a record
     * @param duration  duration of sound record in case it has a record
     */
    public Poll(String id, String name, String question, List<String> options, boolean hasImage, ImageInfo info, String recordPath, int duration) {
        this.id = id;
        this.name = name;
        this.question = question;
        this.options = options;
        this.hasImage = hasImage;
        this.imageInfo = (hasImage) ? info : null;
        this.recordPath = recordPath;
        this.hasRecord = recordPath != null;
        this.duration = hasRecord ? duration : -1;
    }

    /**
     * Constructor overload. Generates an id automatically.
     * @param name title of poll
     * @param question question of poll
     * @param options a list of text based options (from 2 up to 5 options)
     * @param hasImage flag to indicate if it has an image
     * @param info store image info in case it has an image
     * @param recordPath  file path of sound record in case it has a record
     * @param duration  duration of sound record in case it has a record
     */
    public Poll(String name, String question, List<String> options, boolean hasImage, ImageInfo info, String recordPath, int duration) {
        this(UUID.randomUUID().toString(), name, question, options, hasImage, info, recordPath, duration);
    }

    /** Returns poll's identifier */
    public String getId() {
        return id;
    }

    /**
     * Sets poll id
     * @param id identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /** Returns poll's title */
    @Bindable
    public String getName() {
        return name;
    }

    /**
     * Sets poll title
     * @param name title
     */
    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
    }

    /**
     * Sets question of poll
     * @param question question
     */
    public void setQuestion(String question) {
        this.question = question;
        notifyPropertyChanged(BR.question);
    }

    /** Returns poll's question */
    @Bindable
    public String getQuestion() {
        return question;
    }

    /** Returns poll's option list */
    @Bindable
    public List<String> getOptions(){
        return options;
    }

    /** Returns poll's image info */
    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    /** Returns true if Poll has an image, false otherwise */
    public boolean hasImage() {
        return hasImage;
    }

    /** Returns true if Poll has a record, false otherwise */
    public boolean hasRecord() {
        return hasRecord;
    }

    /** Returns poll's record file path */
    @Bindable
    public String getRecordPath() {
        return recordPath;
    }

    /**
     * Sets poll's record file path
     * @param recordPath
     */
    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    /** Returns poll's record's duration */
    public int getDuration() {
        return duration;
    }

    /**
     * Sets poll's record's duration
     * @param duration time in milliseconds
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(question);
        parcel.writeStringList(options);

        parcel.writeByte((byte) (hasImage ? 1 : 0));
        if(hasImage)
            parcel.writeParcelable(imageInfo,i);
        else parcel.writeValue(imageInfo);

        parcel.writeByte((byte) (hasRecord ? 1 : 0));
        parcel.writeString(recordPath);
        parcel.writeInt(duration);

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

    public Poll(Parcel parcel) {
        id         = parcel.readString();
        name       = parcel.readString();
        question   = parcel.readString();
        options = new ArrayList<>();
        parcel.readStringList(options);
        hasImage = parcel.readByte() != 0;
        if(hasImage)
            imageInfo = parcel.readParcelable(ImageInfo.class.getClassLoader());
        else
            imageInfo = (ImageInfo) parcel.readValue(ImageInfo.class.getClassLoader());

        hasRecord  = parcel.readByte() != 0;
        recordPath = parcel.readString();
        duration = parcel.readInt();


        //imageUri = Uri.CREATOR.createFromParcel(parcel);
        //hostAddress = parcel.readString();
        //votes      = Collections.synchronizedList(new ArrayList());
        //parcel.readList(votes, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Poll poll = (Poll) o;

        if (!id.equals(poll.id)) return false;
        if (!getName().equals(poll.getName())) return false;
        if (!getQuestion().equals(poll.getQuestion())) return false;
        return getOptions().equals(poll.getOptions());

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + getName().hashCode();
        result = 31 * result + getQuestion().hashCode();
        result = 31 * result + getOptions().hashCode();
        return result;
    }
}
