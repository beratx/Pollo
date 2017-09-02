package mattoncino.pollo;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mattoncino.pollo.databinding.ActivePollsListItemBinding;


public class PollsCardViewAdapter extends RecyclerView.Adapter<PollsCardViewAdapter.CardViewHolder> {

    private List<PollData> activePolls;
    private final static String TAG = "CARDVIEW_ADAPTER";
    private static final int VIEW_OWN = 1;
    private static final int VIEW_OTHER = 2;

    public PollsCardViewAdapter(List<PollData> polls){
        activePolls = polls;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        private ActivePollsListItemBinding listItemBinding;

        public CardViewHolder(View v) {
            super(v);
            listItemBinding = DataBindingUtil.bind(v);
        }

        public ActivePollsListItemBinding getBinding(){
            return listItemBinding;
        }

    }


    @Override
    public int getItemViewType(int position) {
        final PollData pd = activePolls.get(position);
        if( pd.getOwner() == Consts.OWN) {
            return VIEW_OWN;
        } else {
            return VIEW_OTHER;
        }
    }


    // Create new views (invoked by the layout manager)
    @Override
    public PollsCardViewAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.active_polls_list_item, parent, false);

        CardViewHolder holder = new CardViewHolder(v);
        return holder;
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final PollsCardViewAdapter.CardViewHolder holder, final int position) {
        holder.getBinding().messageTextView.setVisibility(View.GONE);
        //holder.getBinding().terminateButton.setVisibility(View.GONE);
        final PollData pollData = activePolls.get(position);
        final Poll poll = pollData.getPoll();
        final LinearLayout rLayout = holder.getBinding().listItemLayout;

        if(pollData.hasImage()){
            ImageInfo imageInfo = pollData.getImageInfo();
            Bitmap bitmap = ImagePicker.getBitmapImage(imageInfo.getUri(), rLayout.getContext(), imageInfo.isCamera());
            holder.getBinding().imageView.setVisibility(View.VISIBLE);
            holder.getBinding().imageView.setImageBitmap(bitmap);
            holder.getBinding().imageView.invalidate();
        }


        for (int i = 0; i < poll.getOptions().size(); i++) {
            final Button button = (Button) rLayout.getChildAt(i+3);
            final int opt = i+1;
            button.setVisibility(View.VISIBLE);

            //button.setEnabled(!pollData.isDisabled() && !pollData.isTerminated());

            if(pollData.isTerminated() || pollData.getOwner() == Consts.OWN) {
                int sum = pollData.getSumVotes();
                if(sum == 0)
                    button.setText(pollData.getOption(i+1) + " >> " + 0 + "%");
                else
                    button.setText(pollData.getOption(i+1) + " >> " +
                                            pollData.getVotesFor(i+1)*(100.0/sum) + "%");
                Log.d(TAG, "OPT"+opt + " : " + pollData.getOption(i+1));
            }
            else
                button.setText(pollData.getOption(opt));

            if (pollData.getMyVote() == opt)
                button.setText(button.getText().toString() + "    " + "\u2713");


            if(!pollData.isDisabled() && !pollData.isTerminated()){
                button.setEnabled(true);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        button.setText(button.getText().toString() + "    " + "\u2713");

                        if(pollData.getOwner() == Consts.OTHER)
                            sendVote(view, pollData.getID(), opt, pollData.getHostAddress());
                        else {
                            pollData.setMyVote(opt);
                            sendUpdateBroadcast(button.getContext(), pollData.getID());
                        }
                        setCardDetails(button.getContext(), rLayout, pollData, opt);
                        holder.getBinding().ownerLayout.getChildAt(0).setEnabled(false);
                    }
                });
            }

        }

        if(pollData.getOwner() == Consts.OTHER && pollData.isDisabled() && !pollData.isTerminated())
            holder.getBinding().messageTextView.setVisibility(View.VISIBLE);

        if(pollData.getOwner() == Consts.OWN) {
            //holder.getBinding().ownerLayout.setVisibility(View.VISIBLE);

                //if(pollData.getVotedDevices().size() == pollData.getAcceptedDevices().size()) {
                final Button button = (Button) holder.getBinding().ownerLayout.getChildAt(0);
                button.setVisibility(View.VISIBLE);

                if(!pollData.isTerminated()){
                    button.setEnabled(true);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //TODO: check if no body is accepted / voted yet, ask it with popup
                            button.setEnabled(false);
                            pollData.setTerminated(true);

                            for (int i = 0; i < poll.getOptions().size(); i++) {
                                Button option = (Button) rLayout.getChildAt(i + 3);
                                option.setEnabled(false);
                            }

                            Log.d(TAG, "poll " + pollData.getID() + " is terminated! Will send results...");

                            sendResultToAllDevices(button.getContext(), pollData.getID(),
                                        pollData.getVotes(), pollData.getAcceptedDevices());
                        }
                    });
                //}
                }

            TextView view = (TextView) holder.getBinding().ownerLayout.getChildAt(1);
            view.setText(pollData.getVotedDevices().size() + " voted / " + pollData.getAcceptedDevices().size()
                    + " accepted / "  + pollData.getContactedDevices().size() + " received");
            view.setVisibility(View.VISIBLE);
        }

        holder.getBinding().setVariable(BR.poll, poll);
        holder.getBinding().executePendingBindings();
    }

    private void setCardDetails(final Context context, final LinearLayout lLayout, final PollData pd, final int opt){
        pd.setDisabled(true);
        disableOptionButtons(lLayout);

        Handler handler = new Handler();
        final Runnable r = new Runnable(){
            @Override
            public void run() {
                pd.addVote(opt);
                notifyDataSetChanged();
            }
        };
        handler.post(r);
    }

    private void sendUpdateBroadcast(Context context, String id){
        Intent intent = new Intent("mattoncino.pollo.receive.poll.vote");
        intent.putExtra("pollID", id);
        intent.putExtra("myVote", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendVote(View view, String id, int opt, String hostAddress){
        ArrayList<String> pollInfo = new ArrayList<String>();
        pollInfo.add(id);
        pollInfo.add(new Integer(opt).toString());
        //pollInfo.add(hostAddress);

        ClientThreadProcessor clientProcessor = new ClientThreadProcessor(hostAddress,
                view.getContext(), Consts.POLL_VOTE, pollInfo);
        Thread t = new Thread(clientProcessor);
        t.start();

    }
    //(getContext(), pollData.getID(), pollData.getVotes(), pollData.getAcceptedDevices());
    private void sendResultToAllDevices(Context context, String id, int[] result, Set<String> hostAddresses){
        for (java.util.Iterator iterator = hostAddresses.iterator(); iterator.hasNext(); ) {
            String hostAddress = (String) iterator.next();

            ClientThreadProcessor clientProcessor = new ClientThreadProcessor(hostAddress,
                    context, Consts.RESULT, id, result);
            Thread t = new Thread(clientProcessor);
            t.start();
        }
    }

    private void disableOptionButtons(ViewGroup layout){
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if(child.hasOnClickListeners())
                child.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return activePolls.size();
    }
}
