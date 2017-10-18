package mattoncino.pollo;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
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

import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mattoncino.pollo.databinding.ActivePollsListItemBinding;


public class PollsCardViewAdapter extends RecyclerView.Adapter<PollsCardViewAdapter.CardViewHolder> {

    private List<PollData> activePolls;
    private final static String TAG = "PollsCardViewAdapter";
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
        final ActivePollsListItemBinding  binding = holder.getBinding();
        final PollData pollData = activePolls.get(position);
        final Poll poll = pollData.getPoll();
        final LinearLayout rLayout = binding.listItemLayout;
        //holder.rLayout.removeAllViews();

        //TODO remove extra options
        resetHolder(binding);

        if(pollData.hasImage()){
            ImageInfo imageInfo = pollData.getImageInfo();
            Log.d(TAG, "imagePath: " + Uri.parse(imageInfo.getPath()));
            binding.imageView.setVisibility(View.VISIBLE);
            Picasso.with(rLayout.getContext()).
                    load(imageInfo.getPath()).
                    fit().centerInside().
                    //.memoryPolicy(MemoryPolicy.NO_CACHE )
                    //.networkPolicy(NetworkPolicy.NO_CACHE)
                    into(binding.imageView);
        }
        else binding.imageView.setImageDrawable(null);


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
                            new DecimalFormat("#.##").format(pollData.getVotesFor(i+1)*(100.0/sum)) + "%");
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
                        pollData.setMyVote(opt);

                        if(pollData.getOwner() == Consts.OTHER)
                            sendVote(view, pollData.getID(), opt, pollData.getHostAddress());
                        else {
                            sendUpdateBroadcast(button.getContext(), pollData.getID());
                        }
                        setCardDetails(rLayout, pollData, opt);
                        binding.ownerLayout.getChildAt(0).setEnabled(false);
                    }
                });
            }

        }

        if(pollData.getOwner() == Consts.OTHER && pollData.isDisabled() && !pollData.isTerminated())
            binding.messageTextView.setVisibility(View.VISIBLE);

        if(pollData.getOwner() == Consts.OWN) {
                final Button button = (Button) binding.ownerLayout.getChildAt(0);
                button.setVisibility(View.VISIBLE);

                if(!pollData.isTerminated()){
                    button.setEnabled(true);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            pollData.setTerminated(true);
                            button.setEnabled(false);
                            button.invalidate();

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

            TextView view = binding.statsTextView;
            view.setText(pollData.getVotedDevices().size() + " voted / " + pollData.getAcceptedDevices().size()
                    + " accepted / "  + pollData.getContactedDevices().size() + " received");
            view.setVisibility(View.VISIBLE);
        }

        binding.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRemoveBroadcast(rLayout.getContext(), pollData.getID());
            }
        });

        binding.setVariable(BR.poll, poll);
        binding.executePendingBindings();
    }


    private void resetHolder(ActivePollsListItemBinding binding){
        binding.opt3Button.setVisibility(View.GONE);
        binding.opt4Button.setVisibility(View.GONE);
        binding.opt5Button.setVisibility(View.GONE);
        binding.messageTextView.setVisibility(View.GONE);
        binding.imageView.setImageDrawable(null);
        binding.imageView.setVisibility(View.GONE);
        binding.statsTextView.setVisibility(View.GONE);
        binding.terminateButton.setEnabled(false);
    }

    private void setCardDetails(final LinearLayout lLayout, final PollData pd, final int opt){
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

    private void sendRemoveBroadcast(Context context, String id){
        Intent intent = new Intent("mattoncino.pollo.receive.poll.remove");
        intent.putExtra("pollID", id);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendUpdateBroadcast(Context context, String id){
        Intent intent = new Intent("mattoncino.pollo.receive.poll.vote");
        intent.putExtra("pollID", id);
        intent.putExtra("myVote", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendVote(View view, String id, int opt, String hostAddress){
        ArrayList<String> pollInfo = new ArrayList<>();
        pollInfo.add(id);
        pollInfo.add(Integer.toString(opt));

        ClientThreadProcessor clientProcessor = new ClientThreadProcessor(hostAddress,
                view.getContext(), Consts.VOTE, pollInfo);
        Thread t = new Thread(clientProcessor);
        t.start();

    }

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
        for (int i = 0; i < layout.getChildCount() - 1; i++) {
            View child = layout.getChildAt(i);
            //if(child.hasOnClickListeners())
            if(child.isClickable())
                child.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return activePolls.size();
    }



}
