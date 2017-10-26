package mattoncino.pollo;

import android.databinding.BaseObservable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class WaitingData extends BaseObservable implements Parcelable, Serializable {
    private Poll poll;
    private int notificationID;
    private String hostAddress;

    public WaitingData(Poll poll, int notificationID, String hostAddress) {
        this.poll = poll;
        this.notificationID = notificationID;
        this.hostAddress = hostAddress;
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public int getNotificationID() {
        return this.notificationID;
    }


    public void setNotificationID(int notificationID) {
        this.notificationID = notificationID;
    }


    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(poll,i);
        parcel.writeInt(notificationID);
        parcel.writeString(hostAddress);
    }

    public static final Parcelable.Creator<WaitingData> CREATOR
            = new Parcelable.Creator<WaitingData>() {
        public WaitingData createFromParcel(Parcel in) {
            return new WaitingData(in);
        }

        public WaitingData[] newArray(int size) {
            return new WaitingData[size];
        }
    };

    public WaitingData(Parcel parcel) {
        poll = parcel.readParcelable(Poll.class.getClassLoader());
        notificationID = parcel.readInt();
        hostAddress = parcel.readString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WaitingData that = (WaitingData) o;

        if (getNotificationID() != that.getNotificationID()) return false;
        if (!getPoll().equals(that.getPoll())) return false;
        return getHostAddress().equals(that.getHostAddress());

    }

    @Override
    public int hashCode() {
        int result = getPoll().hashCode();
        result = 31 * result + getNotificationID();
        result = 31 * result + getHostAddress().hashCode();
        return result;
    }
}
