package mattoncino.pollo;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import mattoncino.pollo.databinding.ActivePollsListItemBinding;


public class PollsCardViewAdapter extends RecyclerView.Adapter<PollsCardViewAdapter.CardViewHolder> {

    private List<PollData> activePolls;
    private final static String TAG = "CARDVIEW_ADAPTER";

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
        final PollData pollData = activePolls.get(position);
        final Poll poll = pollData.getPoll();
        final LinearLayout rLayout = holder.getBinding().listItemLayout;


        for (int i = 0; i < poll.getOptions().size(); i++) {
            final View view = rLayout.getChildAt(i+2);

            if(view instanceof Button) {
                final Button button = (Button) view;
                final int opt = i+1;
                button.setVisibility(View.VISIBLE);
                button.setText(pollData.getText(i+1));
                button.setEnabled(!pollData.isDisabled());

                if(!pollData.isDisabled()){
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            button.setText(button.getText().toString() + "    " + "\u2713");
                            if(pollData.getOwner() == Consts.OTHER)
                                sendVote(view, pollData, opt);
                            System.out.println("HOST IN ONCLICK" + pollData.getHostAddress());
                            setCardDetails(holder.getBinding().nameTextView.getContext(), rLayout, pollData, opt);
                        }
                    });
                }
            }
        }

        if(pollData.isDisabled())
            holder.getBinding().messageTextView.setVisibility(View.VISIBLE);

        holder.getBinding().setVariable(BR.poll, poll);
        holder.getBinding().executePendingBindings();
    }

    private void setCardDetails(final Context context, final LinearLayout rLayout, final PollData pd, final int opt){
        pd.setDisabled(true);
        disableOptionButtons(rLayout);

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

    private void sendVote(View view, PollData pd, int opt){
        ArrayList<String> pollInfo = new ArrayList<String>();
        pollInfo.add(pd.getID());
        pollInfo.add(new Integer(opt).toString());
        pollInfo.add(pd.getHostAddress());

        ClientThreadProcessor clientProcessor = new ClientThreadProcessor(pd.getHostAddress(),
                view.getContext(), Consts.POLL_VOTE, pollInfo);
        Thread t = new Thread(clientProcessor);
        t.start();

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


