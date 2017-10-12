package mattoncino.pollo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;


public class ToastHelper {

    public static void useLongToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    public static void useShortToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void doInUIThread(final String message, final Context context) {
        Activity act = (Activity) context;
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastHelper.useLongToast(context, message);
            }
        });
    }

    public static void doInUIThreadShort(final String message, final Context context) {
        Activity act = (Activity) context;
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastHelper.useShortToast(context, message);
            }
        });
    }

    public static void  useToastInService(final Handler handler, final Context context, final String message){
        handler.post(new Runnable() {
            @Override
            public void run() {
                useLongToast(context, message);
            }
        });
    }

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
