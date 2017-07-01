package mattoncino.pollo;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import mattoncino.pollo.databinding.ActivePollsListItemBinding;

public class PollsCardViewAdapter extends RecyclerView.Adapter<PollsCardViewAdapter.CardViewHolder> {

    private List<Poll> activePolls;
    private final static String TAG = "CARDVIEW_ADAPTER";
    private static final Type LIST_TYPE = new TypeToken<List<Poll>>() {}.getType();

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
        final Poll poll = activePolls.get(position);
        final List<String> options = poll.getOptions();
        final LinearLayout rLayout = holder.getBinding().listItemLayout;

        holder.getBinding().opt1Button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                poll.addVote(Consts.FIRST_OPT);
                poll.setDisabled(true);
                disableOptionButtons(rLayout);

                holder.getBinding().opt1Button.setText(
                        holder.getBinding().opt1Button.getText().toString() + "    "+ "\u2713");

                rLayout.addView(addMessageView(holder.getBinding().nameTextView.getContext()));

                ArrayList<String> pollData = new ArrayList<String>();
                pollData.add(poll.getName());
                pollData.add(new Integer(Consts.FIRST_OPT).toString());
                pollData.add(poll.getHostAddress());

                ClientThreadProcessor clientProcessor = new ClientThreadProcessor(poll.getHostAddress(),
                                            view.getContext(), Consts.POLL_VOTE, pollData);
                Thread t = new Thread(clientProcessor);
                t.start();

            }
        });

        holder.getBinding().opt2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                poll.addVote(Consts.SECOND_OPT);
                poll.setDisabled(true);
                disableOptionButtons(rLayout);

                holder.getBinding().opt2Button.setText(
                        holder.getBinding().opt2Button.getText().toString() + "    "+ "\u2713");

                rLayout.addView(addMessageView(holder.getBinding().nameTextView.getContext()));

                ArrayList<String> pollData = new ArrayList<String>();
                pollData.add(poll.getName());
                pollData.add(new Integer(Consts.SECOND_OPT).toString());
                pollData.add(poll.getHostAddress());

                ClientThreadProcessor clientProcessor = new ClientThreadProcessor(poll.getHostAddress(),
                                            view.getContext(), Consts.POLL_VOTE, pollData);
                Thread t = new Thread(clientProcessor);
                t.start();
            }
        });

        for (int i = 2; i < poll.getOptions().size(); i++) {
            final Button button = createNewOptionButton(holder.getBinding().nameTextView.getContext(), options.get(i));
            if(poll.isDisabled())
                button.setEnabled(false);
            final int opt = i + 1;

            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    poll.addVote(opt);
                    poll.setDisabled(true);
                    disableOptionButtons(rLayout);

                    button.setText(button.getText().toString() + "    "+ "\u2713");

                    rLayout.addView(addMessageView(holder.getBinding().nameTextView.getContext()));

                    ArrayList<String> pollData = new ArrayList<String>();
                    pollData.add(poll.getName());
                    pollData.add(Integer.toString(opt));
                    pollData.add(poll.getHostAddress());

                    ClientThreadProcessor clientProcessor = new ClientThreadProcessor(poll.getHostAddress(),
                            view.getContext(), Consts.POLL_VOTE, pollData);
                    Thread t = new Thread(clientProcessor);
                    t.start();
                }
            });

            rLayout.addView(button);

        }

        if(poll.isDisabled()){
            rLayout.addView(addMessageView(holder.getBinding().nameTextView.getContext()));
        }

        holder.getBinding().setVariable(BR.poll, poll);
        holder.getBinding().executePendingBindings();
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



    private Button createNewOptionButton(Context context, String option) {
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        final Button button = new Button(context);
        button.setLayoutParams(lparams);
        button.setId(View.generateViewId());
        button.setTextSize(18);
        button.setText(option);
        //button.setBackgroundColor(Color.TRANSPARENT);

        return button;
    }

    @Override
    public int getItemCount() {
        return activePolls.size();
    }
}
