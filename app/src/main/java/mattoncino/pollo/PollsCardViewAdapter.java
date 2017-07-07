package mattoncino.pollo;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mattoncino.pollo.databinding.ActivePollsListItemBinding;


public class PollsCardViewAdapter extends RecyclerView.Adapter<PollsCardViewAdapter.CardViewHolder> {

    private List<Poll> activePolls;
    private final static String TAG = "CARDVIEW_ADAPTER";

    public PollsCardViewAdapter(List<Poll> polls){
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
        final Poll poll = activePolls.get(position);
        final LinearLayout rLayout = holder.getBinding().listItemLayout;

        for (int i = 0; i < poll.getOptions().size(); i++) {
            final View view = rLayout.getChildAt(i+2);
            if(view instanceof Button) {
                final Button button = (Button) view;
                final int opt = i+1;
                button.setVisibility(View.VISIBLE);
                button.setText(poll.getText(i+1));
                if(!poll.isDisabled()){
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            button.setText(button.getText().toString() + "    " + "\u2713");
                            sendVote(view, poll, opt);
                            setCardDetails(holder.getBinding().nameTextView.getContext(), rLayout, poll, opt);
                        }
                    });
                }
            }
        }

        if(poll.isDisabled())
            holder.getBinding().messageTextView.setVisibility(View.VISIBLE);

        holder.getBinding().setVariable(BR.poll, poll);
        holder.getBinding().executePendingBindings();
    }

    private void setCardDetails(final Context context, final LinearLayout rLayout, final Poll poll, final int opt){
        poll.setDisabled(true);
        disableOptionButtons(rLayout);

        Handler handler = new Handler();
        final Runnable r = new Runnable(){
            @Override
            public void run() {
                poll.addVote(opt);
                notifyDataSetChanged();
                System.out.println("HOW MANY TIMES AM I INVOKED?");
            }
        };
        handler.post(r);
    }

    private void sendVote(View view, Poll poll, int opt){
        ArrayList<String> pollData = new ArrayList<String>();
        pollData.add(poll.getName());
        pollData.add(new Integer(opt).toString());
        pollData.add(poll.getHostAddress());

        ClientThreadProcessor clientProcessor = new ClientThreadProcessor(poll.getHostAddress(),
                view.getContext(), Consts.POLL_VOTE, pollData);
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

    private TextView addMessageView(Context context){
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView textView = new TextView(context);
        textView.setLayoutParams(lparams);
        textView.setId(View.generateViewId());
        textView.setText("Thanks for voting! Wait for results...");
        textView.setTextSize(20);
        textView.setTypeface(null, Typeface.BOLD_ITALIC);

        return textView;
    }

    @Override
    public int getItemCount() {
        return activePolls.size();
    }
}


