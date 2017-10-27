package mattoncino.pollo;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import mattoncino.pollo.databinding.WaitingPollsListItemBinding;


public class WaitingPollsAdapter extends RecyclerView.Adapter<WaitingPollsAdapter.CardViewHolder> {
    private final static String TAG = "WaitingPollsAdapter";
    private List<WaitingData> waitingPolls;


    public WaitingPollsAdapter(List<WaitingData> polls){
        waitingPolls = polls;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        private WaitingPollsListItemBinding waitingItemBinding;

        public CardViewHolder(View v) {
            super(v);
            waitingItemBinding = DataBindingUtil.bind(v);
        }

        public  WaitingPollsListItemBinding  getBinding(){
            return waitingItemBinding;
        }
    }


    @Override
    public WaitingPollsAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.waiting_polls_list_item, parent, false);

        WaitingPollsAdapter.CardViewHolder holder = new WaitingPollsAdapter.CardViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final WaitingPollsAdapter.CardViewHolder holder, int position) {
        final WaitingPollsListItemBinding binding = holder.getBinding();
        final WaitingData wd = waitingPolls.get(position);
        binding.setVariable(BR.poll, wd.getPoll());
        binding.executePendingBindings();

        binding.acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), mattoncino.pollo.ActivePollsActivity.class)
                        .putExtra(Consts.OWNER, Consts.WAITED)
                        .putExtra(Consts.POLL, (Parcelable) wd.getPoll())
                        .putExtra(Consts.NOTIFICATION_ID, wd.getNotificationID())
                        .putExtra(Consts.ADDRESS, wd.getHostAddress());

                WaitingPolls.sendRemoveBroadcast(view.getContext(), wd.getNotificationID());

                view.getContext().startActivity(intent);
            }
        });

        binding.rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NotificationManager notificationManager =
                        (NotificationManager) view.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(wd.getNotificationID());

                WaitingPolls.sendRemoveBroadcast(view.getContext(), wd.getNotificationID());
                //to update main activity
                WaitingPolls.sendUpdateBroadcast(view.getContext(), waitingPolls.size() - 1);
            }
        });
    }

    @Override
    public int getItemCount() {
        return waitingPolls.size();
    }


    /*private void sendRemoveBroadcast(Context context, Integer id){
        Intent intent = new Intent(Receivers.W_REMOVE)
                .putExtra(Consts.NOTIFICATION_ID, id);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendUpdateBroadcast(Context context, int count) {
        Intent intent = new Intent(Receivers.W_COUNT)
                .putExtra(Consts.COUNT, count);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }*/

}
