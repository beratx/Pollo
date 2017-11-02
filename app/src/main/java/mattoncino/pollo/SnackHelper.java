package mattoncino.pollo;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * Util class to create a SnackBar with a specified message
 * and pre-determinated action to launch Wifi Settings Activity
 */
public class SnackHelper {

    /**
     * Displays SnackBar with the given message
     * @param context
     * @param view
     * @param message
     */
    public static void showSnackBar(final Context context, View view, final String message){
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction("Wi-Fi", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                }).show();

    }
}
