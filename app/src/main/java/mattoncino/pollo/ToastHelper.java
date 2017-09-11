package mattoncino.pollo;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.util.Iterator;
import java.util.Set;


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

    public static void useLongToastForIntegerSet(Set<Integer> ourSet, Context context) {

        String pringStr = "Set : ";

        Iterator itr = ourSet.iterator();
        while (itr.hasNext()) {
            Object element = itr.next();
            pringStr += element.toString() + ", ";
        }

        useLongToast(context, pringStr);
    }


    public static void  useToastInService(final Handler handler, final Context context, final String message){

        handler.post(new Runnable() {
            @Override
            public void run() {
                useLongToast(context, message);
            }
        });
    }
}
