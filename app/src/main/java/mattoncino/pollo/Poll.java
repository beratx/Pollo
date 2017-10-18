package mattoncino.pollo;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class Poll extends BaseObservable implements Parcelable, Serializable {

    private String id;
    private String name;
    private String question;
    private List<String> options;
    private ImageInfo imageInfo;
    private boolean hasImage;

    public Poll(String id, String name, String question, List<String> options, boolean hasImage, ImageInfo info) {
        this.id = id;
        this.name = name;
        this.question = question;
        this.options = options;
        this.hasImage = hasImage;
        this.imageInfo = (hasImage) ? info : null;
    }

    public Poll(String name, String question, List<String> options, boolean hasImage, ImageInfo info) {
        this(UUID.randomUUID().toString(), name, question, options, hasImage, info);
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

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    @Bindable
    public List<String> getOptions(){
        return options;
    }


    public boolean hasImage() {
        return hasImage;
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
