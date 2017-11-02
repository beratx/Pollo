package mattoncino.pollo;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
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

/**
 * <p> Adapter class that represents each Poll as a Card.
 * Through the adapter class User interacts with its Polls
 * created or received. </p>
 * <p> User can:
 * <ul>
 * <li> vote for a created/accepted Poll
 * <li> terminate a Poll if its the owner
 * <li> remove a Poll from list
 * <li> play sound record of a Poll
 * <li> receive Poll results for an accepted Poll
 * <li> see stats about the Poll(#devices received/accepted/voted)
 * </ul>
 * </p>
 */
public class PollsCardViewAdapter extends RecyclerView.Adapter<PollsCardViewAdapter.CardViewHolder> {
    private List<PollData> activePolls;
    private final static String TAG = "PollsCardViewAdapter";
    private final static int VIEW_COUNT = 4;
    private static final int VIEW_OWN = 1;
    private static final int VIEW_OTHER = 2;
    public static SoundRecord record = null;
    private static boolean sameAudio = true;

    /**
     * Constructor
     *
     * @param polls list of PollData for the polls user has
     */
    public PollsCardViewAdapter(List<PollData> polls){
        this.activePolls = polls;
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


    /**
     * Creates a new CardViewHolder
     * @param parent
     * @param viewType
     * @return a new CardViewHolder
     */
    @Override
    public PollsCardViewAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.active_polls_list_item, parent, false);

        CardViewHolder holder = new CardViewHolder(v);
        return holder;
    }


    /**
     * Based on the Poll's characteristics (created by the user or
     * received from another, how many options, has image
     * or sound, active, voted or terminated) visualizes different
     * views and sets action listeners for these views.
     *
     * If user clicks on one of the option buttons:
     * if Poll is user's own poll then just updates PollData and UI
     * otherwise send vote to the owner of the Poll and updates UI
     *
     * For user's own Polls, if user click on Terminate button, then
     * poll terminates, updates UI in order to disable all the buttons
     * except Remove button and to display results. Then sends
     * results to the devices which have accepted the poll.
     *
     * If user clicks on Remove button, the  PollData will be deleted
     * from Poll list permanently.
     *
     *
     * @param holder
     * @param position position of the PollData element in PollData list
     */
    @Override
    public void onBindViewHolder(final PollsCardViewAdapter.CardViewHolder holder, final int position) {
        final ActivePollsListItemBinding  binding = holder.getBinding();
        final PollData pollData = activePolls.get(position);
        final Poll poll = pollData.getPoll();
        final LinearLayout rLayout = binding.listItemLayout;


        resetHolder(binding);

        /**
         * if Poll has an image then display it in the imageView.
         * Picasso library handles image recycling and memory caching
         */
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

        } else binding.imageView.setImageDrawable(null);


        /**
         * If Poll has a sound record prepare UI and SoundRecord object
         * to manage playback of the record.
         *
         * @see SoundRecord
         */
        if(pollData.hasRecord()){
            final String recordPath = pollData.getRecordPath();
            Log.d(TAG, "recordPath: " + recordPath);

            binding.chronometer.setBase(SystemClock.elapsedRealtime() - pollData.getDuration());
            binding.recordCardView.setVisibility(View.VISIBLE);

            binding.playFAB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(record != null) {
                        if(record.isPlaying())
                            record.stopPlaying();

                        sameAudio = record.getDataSource().equals(recordPath);

                        if(sameAudio)
                            record.flipPlay();
                        else record = null;
                    }
                    if(record == null) {
                        record = new SoundRecord(recordPath);
                        record.setOnStopListener(new SoundRecord.OnStopListener() {
                            @Override
                            public void onStop(boolean stopped) {
                                binding.chronometer.stop();
                                binding.chronometer.setBase(SystemClock.elapsedRealtime() - pollData.getDuration());
                                binding.playFAB.setImageResource(android.R.drawable.ic_media_play);
                                }
                        });
                    }
                    if (record.isPlaying()) {
                        record.startPlaying();
                        record.setCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                binding.playFAB.setImageResource(android.R.drawable.ic_media_play);
                                binding.chronometer.stop();
                                record.flipPlay();
                                record = null;
                            }
                        });
                        binding.chronometer.setBase(SystemClock.elapsedRealtime());
                        binding.chronometer.start();
                        binding.playFAB.setImageResource(android.R.drawable.ic_media_pause);
                    }
                }
            });
        }


        /** Set visible extra option buttons if there are more than 2 options
         *  Set also action listeners and update their text based on the Poll's
         *  state(if it's still active, voted or terminated)
         */
        for (int i = 0; i < poll.getOptions().size(); i++) {
            final Button button = (Button) rLayout.getChildAt(i + VIEW_COUNT);
            button.setVisibility(View.VISIBLE);
            final int opt = i+1;


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
                button.setText(button.getText().toString() + "\t" + "\u2713");


            if(!pollData.isDisabled() && !pollData.isTerminated()){
                button.setEnabled(true);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        button.setText(button.getText().toString() + "\t" + "\u2713");
                        pollData.setMyVote(opt);

                        if(pollData.getOwner() == Consts.OTHER)
                            sendVote(view, pollData.getID(), opt, pollData.getHostAddress());
                        else
                            sendUpdateBroadcast(button.getContext(), pollData.getID());

                        setCardDetails(rLayout, pollData, opt);
                        binding.ownerLayout.getChildAt(0).setEnabled(false);
                    }
                });
            } else {
                binding.opt1Button.setEnabled(false);
                binding.opt2Button.setEnabled(false);
            }
        }

        /** if the poll is a received poll and user has voted for the poll
         *  then display a message to the User
         */
        if(pollData.getOwner() == Consts.OTHER && pollData.isDisabled() && !pollData.isTerminated())
            binding.messageTextView.setVisibility(View.VISIBLE);


        /** if the poll is created by User and is not terminated yet,
         *  set action listener for the Terminate button which will
         *  send results to others when its clicked
         */
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
                                Button option = (Button) rLayout.getChildAt(i + VIEW_COUNT);
                                option.setEnabled(false);
                            }

                            Log.d(TAG, "poll " + pollData.getID() + " is terminated! Will send results...");

                            sendResultToAllDevices(button.getContext(), pollData.getID(),
                                        pollData.getVotes(), pollData.getAcceptedDevices());
                        }
                    });
                }

            /* Set stats about the Poll : #devices votes/accepted/received the Poll */
            TextView view = binding.statsTextView;
            view.setText(pollData.getVotedDevices().size() + " voted / " + pollData.getAcceptedDevices().size()
                    + " accepted / "  + pollData.getContactedDevices().size() + " received");
            view.setVisibility(View.VISIBLE);
        }

        binding.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(poll.hasRecord()){
                    if(record != null){
                        if(record.isPlaying() && record.getDataSource().equals(poll.getRecordPath())) {
                            record.stopPlaying();
                            record.flipPlay();
                            record = null;
                        }
                    }
                }
                sendRemoveBroadcast(rLayout.getContext(), pollData.getID());
            }
        });

        binding.setVariable(BR.poll, poll);
        binding.executePendingBindings();
    }


    /**
     * Reset all the views before replacing contents of the CardView
     *
     * @param binding binding of the layout
     */

    private void resetHolder(ActivePollsListItemBinding binding){
        binding.opt3Button.setVisibility(View.GONE);
        binding.opt4Button.setVisibility(View.GONE);
        binding.opt5Button.setVisibility(View.GONE);
        binding.messageTextView.setVisibility(View.GONE);
        binding.imageView.setImageDrawable(null);
        binding.imageView.setVisibility(View.GONE);
        binding.chronometer.setBase(SystemClock.elapsedRealtime());
        binding.statsTextView.setVisibility(View.GONE);
        binding.terminateButton.setEnabled(false);
        binding.recordCardView.setVisibility(View.GONE);
    }

    /**
     * Sets views in the CardView, when the Poll is terminated.
     * Sets disabled flag for the Poll
     * Disables all its option buttons
     * Updates PollData with the vote
     *
     * @param lLayout Root layout of the CardView
     * @param pd PollData object
     * @param opt voted option
     */
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

    /**
     * Sends a LocalBroadcast to propagate removal of a Poll from the Poll list
     *
     * @param context Activity's context
     * @param id identifier of a poll
     */
    private void sendRemoveBroadcast(Context context, String id){
        Intent intent = new Intent("mattoncino.pollo.receive.poll.remove");
        intent.putExtra("pollID", id);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Sends a LocalBroadcast to propagate update of a Poll with the users
     * own vote for its own Poll
     *
     * @param context Activity's context
     * @param id Identifier of a poll
     */
    private void sendUpdateBroadcast(Context context, String id){
        Intent intent = new Intent("mattoncino.pollo.receive.poll.vote");
        intent.putExtra("pollID", id);
        intent.putExtra("myVote", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Sends the vote for the Poll with the given id, sent from the device
     * with the host address. Connection and data transfering is realized
     * by the ClientThreadProcessor thread.
     *
     * @param view  view object
     * @param id    poll identifier
     * @param opt   voted option
     * @param hostAddress host address of the device which sent the Poll
     * @see ClientThreadProcessor
     */
    private void sendVote(View view, String id, int opt, String hostAddress){
        ArrayList<String> pollInfo = new ArrayList<>();
        pollInfo.add(id);
        pollInfo.add(Integer.toString(opt));

        ClientThreadProcessor clientProcessor = new ClientThreadProcessor(hostAddress,
                view.getContext(), Consts.VOTE, pollInfo);
        Thread t = new Thread(clientProcessor);
        t.start();

    }

    /**
     * Send results of the terminated Poll with the given id to the all devices
     * int the hostAddresses list
     *
     * @param context Activity's context
     * @param id poll identifier
     * @param result votes as an array
     * @param hostAddresses list of host addresses
     */
    private void sendResultToAllDevices(Context context, String id, int[] result, Set<String> hostAddresses){
        for (java.util.Iterator iterator = hostAddresses.iterator(); iterator.hasNext(); ) {
            String hostAddress = (String) iterator.next();

            ClientThreadProcessor clientProcessor = new ClientThreadProcessor(hostAddress,
                    context, Consts.RESULT, id, result);
            Thread t = new Thread(clientProcessor);
            t.start();
        }
    }

    /**
     * Disables option buttons
     *
     * @param layout
     */
    private void disableOptionButtons(ViewGroup layout){
        for (int i = 0; i < layout.getChildCount() - 1; i++) {
            View child = layout.getChildAt(i);
            if(child.isClickable())
                child.setEnabled(false);
        }
    }

    /** returns number of items in the PollData list */
    @Override
    public int getItemCount() {
        return activePolls.size();
    }

}
