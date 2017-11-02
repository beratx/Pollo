package mattoncino.pollo;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/** Stores information about an image added to a poll. */
final class ImageInfo implements Parcelable, Serializable {
    private String path;
    private final boolean isCamera;

    /**
     * Constructor
     *
     * @param path path of the image
     * @param isCamera flag to indicate if image is captured from camera
     *
     */
    public ImageInfo(String path, boolean isCamera) {
        //this.uri = uri;
        this.path = path;
        this.isCamera = isCamera;
    }

    /** @return path of the image  */
    public String getPath() {
        return path;
    }

    /**
     * sets path of the image to the path
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /** @return <code>true</code> if image is captured from camera
     *          <code>false</code> otherwise
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
