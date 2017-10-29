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
 * Poll class rappresents a Poll object that can be created through
 * MultiOptPollActivity class or can be received through the network
 *
 * A Poll consist of:
 * <ul>
 * <li> String id: identifier of poll
 * <li> String name : title of poll
 * <li> String question : poll question
 * <li> List<String> options : a list of text based options (from 2 up to 5 options)
 * <li> ImageInfo imageInfo : store image info in case it has an image
 * <li> boolean hasImage : flag to indicate if it has an image
 * <li> String recordPath :  file path of sound record in case it has a record
 * <li> boolean hasRecord : lag to indicate if it has a record
 * <li> int duration : duration of sound record in case it has a record
 * </ul>
 *
 */
public class Poll extends BaseObservable implements Parcelable, Serializable {

    /**
     * identifier of the poll
     */
    private String id;
    private String name;
    private String question;
    private List<String> options;
    private ImageInfo imageInfo;
    private boolean hasImage;
    private String recordPath;
    private boolean hasRecord;
    private int duration;

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

    public Poll(String name, String question, List<String> options, boolean hasImage, ImageInfo info, String recordPath, int duration) {
        this(UUID.randomUUID().toString(), name, question, options, hasImage, info, recordPath, duration);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Bindable
    public String getQuestion() {
        return question;
    }

    @Bindable
    public List<String> getOptions(){
        return options;
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public boolean hasImage() {
        return hasImage;
    }

    public boolean hasRecord() {
        return hasRecord;
    }

    @Bindable
    public String getRecordPath() {
        return recordPath;
    }

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public int getDuration() {
        return duration;
    }

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
