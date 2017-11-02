package mattoncino.pollo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ImagePicker class manages to Pick an image from gallery
 * or capture an image from the camera and returns information
 * to save and display the image.
 *
 * Author: Mario Velasco Casquero
 * Date: 08/09/2015
 * Email: m3ario@gmail.com
 */
public class ImagePicker {

    private static final int DEFAULT_MIN_WIDTH_QUALITY = 400;        // min pixels
    private static final String TAG = "ImagePicker";
    private static final String TEMP_IMAGE_NAME = "tempImage";

    private static int minWidthQuality = DEFAULT_MIN_WIDTH_QUALITY;

    /*
     * Creates intents to choose an image from gallery or to capture
     * from camera
     *
     * @param context
     * @return a chooser to chose the action
     */
    public static Intent getPickImageIntent(Context context) {
        Intent chooserIntent = null;

        List<Intent> intentList = new ArrayList<>();

        Intent pickIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra("return-data", true);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTempFile(context)));

        intentList = addIntentsToList(context, intentList, pickIntent);
        intentList = addIntentsToList(context, intentList, takePhotoIntent);

        if (intentList.size() > 0) {
            //Convenience function for creating a ACTION_CHOOSER Intent.
            chooserIntent = Intent.createChooser(intentList.remove(intentList.size() - 1),
                    "image picker intent text");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[]{}));
        }

        return chooserIntent;
    }

    private static List<Intent> addIntentsToList(Context context, List<Intent> list, Intent intent) {
        //Retrieve all activities that can be performed for the given intent.
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);

        for (ResolveInfo resolveInfo : resInfo) {
            String packageName = resolveInfo.activityInfo.packageName;
            Intent targetedIntent = new Intent(intent);
            targetedIntent.setPackage(packageName);
            list.add(targetedIntent);
            Log.d(TAG, "Intent: " + intent.getAction() + " package: " + packageName);
        }

        return list;
    }


    /**
     * Gets image from the result of the Users action and returns information
     * of the image.
     *
     * @param context Activity's context
     * @param resultCode indicates if action is failed or not
     * @param imageReturnedIntent Intent with the result data of the image
     * @return ImageInfo object
     *
     * @see ImageInfo
     */
    public static ImageInfo getImageFromResult(Context context, int resultCode,
                                               Intent imageReturnedIntent) {
        Log.d(TAG, "getImageFromResult, resultCode: " + resultCode);
        File imageFile = getTempFile(context);

        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage;
            boolean isCamera = (imageReturnedIntent == null ||
                    imageReturnedIntent.getData() == null  ||
                    imageReturnedIntent.getData().toString().contains(imageFile.toString()));

            if (isCamera) {     /** CAMERA **/
                selectedImage = Uri.fromFile(imageFile);
            } else {            /** ALBUM **/
                selectedImage = imageReturnedIntent.getData();
            }
            Log.d(TAG, "selectedImage: " + selectedImage);

            return new ImageInfo(selectedImage.toString(), isCamera);
        }

        return null;
    }

    /**
     * Returns bitmap object for the image in the given Uri
     * @param selectedImage Uri of the selected image
     * @param context Activity's context
     * @param isCamera flag to indicate if is captured from camera
     * @return  bitmap object for the image in the given Uri
     */
    public static Bitmap getBitmapImage(Uri selectedImage, Context context, boolean isCamera){
        Bitmap bm = getImageResized(context, selectedImage);
        int rotation = getRotation(context, selectedImage, isCamera);
        bm = rotate(bm, rotation);
        return bm;
    }

    /**
     * Creates a temp file (in the external cache if possible, otherwise in internal cache)
     *
     * @param context Activity's context
     * @return File object that represents the created temp file
     */

    public static File getTempFile(Context context) {
        File imageFile;

        if(isExternalStorageWritable())
            imageFile = new File(context.getExternalCacheDir(), TEMP_IMAGE_NAME);
        else
            imageFile = new File(context.getCacheDir(), TEMP_IMAGE_NAME);

        imageFile.getParentFile().mkdirs();

        return imageFile;
    }


    /**
     * Decodes that image in the given Uri respect to sampleSize
     * @param context
     * @param theUri
     * @param sampleSize
     * @return
     */
    private static Bitmap decodeBitmap(Context context, Uri theUri, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;

        AssetFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = context.getContentResolver().openAssetFileDescriptor(theUri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap actuallyUsableBitmap = BitmapFactory.decodeFileDescriptor(
                fileDescriptor.getFileDescriptor(), null, options);

        if(actuallyUsableBitmap == null) {
            InputStream imgFile;
            try {
                imgFile = context.getAssets().open(theUri.toString());
                actuallyUsableBitmap = BitmapFactory.decodeStream(imgFile, null, options);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        if(actuallyUsableBitmap == null)
            Log.wtf(TAG, "actuallyUsableBitmap is NULL");
        else
            Log.d(TAG, options.inSampleSize + " sample method bitmap ... " +
                    actuallyUsableBitmap.getWidth() + " " + actuallyUsableBitmap.getHeight());

        return actuallyUsableBitmap;
    }

    /** Resize to avoid using too much memory loading big images (e.g.: 2560*1920) */
    private static Bitmap getImageResized(Context context, Uri selectedImage) {
        Bitmap bm;
        int[] sampleSizes = new int[]{5, 3, 2, 1};
        int i = 0;
        do {
            bm = decodeBitmap(context, selectedImage, sampleSizes[i]);
            Log.d(TAG, "resizer: new bitmap width = " + bm.getWidth());
            i++;
        } while (bm.getWidth() < minWidthQuality && i < sampleSizes.length);
        return bm;
    }


    /**
     * get rotation info for the image in the given Uri
     * @param context
     * @param imageUri
     * @param isCamera
     * @return
     */
    private static int getRotation(Context context, Uri imageUri, boolean isCamera) {
        int rotation;
        if (isCamera) {
            rotation = getRotationFromCamera(context, imageUri);
        } else {
            rotation = getRotationFromGallery(context, imageUri);
        }
        Log.d(TAG, "Image rotation: " + rotation);
        return rotation;
    }

    /**
     * Get rotation info for an image captured from camera
     * @param context
     * @param imageFile
     * @return
     */
    private static int getRotationFromCamera(Context context, Uri imageFile) {
        int rotate = 0;
        try {

            context.getContentResolver().notifyChange(imageFile, null);
            ExifInterface exif = new ExifInterface(imageFile.getPath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }

    /**
     * Get rotation info for an image from the gallery
     * @param context
     * @param imageUri
     * @return
     */
    public static int getRotationFromGallery(Context context, Uri imageUri) {
        int result = 0;
        String[] columns = {MediaStore.Images.Media.ORIENTATION};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(imageUri, columns, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int orientationColumnIndex = cursor.getColumnIndex(columns[0]);
                result = cursor.getInt(orientationColumnIndex);
            }
        } catch (Exception e) {
            //Do nothing
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }//End of try-catch block
        return result;
    }


    /**
     * Applies rotation to the bitmap
     * @param bm
     * @param rotation
     * @return
     */
    private static Bitmap rotate(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            return bmOut;
        }
        return bm;
    }

    /**
     * Returns real path of an image obtained from gallery(that has an URI
     * in the form of : content://media/external/images/42 )
     * @param context
     * @param contentUri
     * @return real path of an image obtained from gallery
     */
    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            //Can launch NUllPointerException
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }



    /**
     * Creates a temporary file in the external cache of the app, if external storage is avaiable,
     * otherwise creates im the internal cache
     * @param context Activity's context
     * @param ext extension of the file
     * @return File object that represents new created file
     */
    public static File createTempFile(Context context, String ext) {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        //file path: /storage/emulated/0/Android/data/mattoncino.pollo/cache/pollo_20171030_102419.jpeg
        return isExternalStorageWritable()
                    ? new File(context.getExternalCacheDir(), "pollo_" + timestamp + "." + ext)
                    : new File(context.getCacheDir() , "pollo_" + timestamp + "." + ext);
    }

    public static File createTempFile2(Context context, String ext) throws IOException {
        return isExternalStorageWritable()
                    ? File.createTempFile("pollo", ext, context.getExternalCacheDir())
                    : File.createTempFile("pollo", ext, context.getCacheDir());
    }


    /**
     * Creates a file in the external storage of the app, if external storage is avaiable,
     * otherwise creates in the internal storage
     * @param context Activity's context
     * @param ext extension of the file
     * @return File object that represents new created file
     * @throws IOException
     */
    public static File createFile(Context context, String ext) throws IOException {
        String timestamp =  new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean external = isExternalStorageWritable();

        if(ext.isEmpty())  ext = "bmp";

        File dir = external ? context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                            : new File(context.getFilesDir() + File.separator + "pollo_images");

        if (!external && !dir.exists()) {
            File imagesDir = new File(context.getFilesDir(), "pollo_images");
            imagesDir.mkdirs();
        }

        return new File(dir, "pollo_" + timestamp + "." + ext);
    }

    /** @return <code>true</code> if external storage is writable
     *          <code>false</code> otherwise
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }


    /**
     * Returns MIME info for the file in the given URI
     * @param context Activity's context
     * @param uri Uri of the file
     * @return MIME type of the file in the given URI
     */
    public static String getMimeType(Context context, Uri uri) {
        String mimeType;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    /**
     * Returns extension of the file in the given URI
     * @param context Activity's context
     * @param uri Uri of the file
     * @return file extension
     */
    public static String getImageType(Context context, Uri uri){
        String mimeType = ImagePicker.getMimeType(context, uri);
        return mimeType.substring(mimeType.lastIndexOf("/") + 1);
    }

}