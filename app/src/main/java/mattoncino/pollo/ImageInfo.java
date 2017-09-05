package mattoncino.pollo;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

final class ImageInfo implements Parcelable, Serializable {
    //private final Uri uri;
    private String path;
    private final boolean isCamera;

    public ImageInfo(String path, boolean isCamera) {
        //this.uri = uri;
        this.path = path;
        this.isCamera = isCamera;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /*
    public Uri getUri() {
        return uri;
    }
    */

    public boolean isCamera() {
        return isCamera;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        //Uri.writeToParcel(parcel, uri);
        parcel.writeString(path);
        parcel.writeByte((byte) (isCamera ? 1 : 0));
    }

    public static final Parcelable.Creator<ImageInfo> CREATOR
            = new Parcelable.Creator<ImageInfo>() {
        public ImageInfo createFromParcel(Parcel in) { return new ImageInfo(in); }

        public ImageInfo[] newArray(int size) { return new ImageInfo[size]; }
    };

    public ImageInfo(Parcel parcel) {
        //uri = Uri.CREATOR.createFromParcel(parcel);
        path = parcel.readString();
        isCamera = parcel.readByte() != 0;
    }
}
