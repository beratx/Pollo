package mattoncino.pollo;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

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
        final Poll poll = activePolls.get(position);
        final List<String> options = (ArrayList<String>) poll.getOptions();

        final LinearLayout rLayout = holder.getBinding().listItemLayout;

        holder.getBinding().opt1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //vote = Consts.FIRST_OPT;
                //listItemBinding.opt1Button.setEnabled(false);
                //listItemBinding.opt2Button.setEnabled(false);
                poll.addVote(Consts.FIRST_OPT);
                Toast.makeText(view.getContext(), "VOTED: " + Consts.FIRST_OPT, Toast.LENGTH_SHORT).show();

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
                Toast.makeText(view.getContext(), "VOTED: " + Consts.SECOND_OPT, Toast.LENGTH_SHORT).show();

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

        for (int i = 2; i < poll.getOptions().size(); i++) {
            Button button = createNewOptionButton(holder.getBinding().nameTextView.getContext(), options.get(i));
            final int opt = i + 1;

            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    poll.addVote(opt);
                    Toast.makeText(view.getContext(), "VOTED " + opt, Toast.LENGTH_LONG).show();

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


        holder.getBinding().setVariable(BR.poll, poll);
        holder.getBinding().executePendingBindings();
    }


    private Button createNewOptionButton(Context context, String option) {
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        final Button button = new Button(context);
        button.setLayoutParams(lparams);
        button.setId(View.generateViewId());
        button.setTextSize(18);
        button.setText(option);

        return button;
    }

    @Override
    public int getItemCount() {
        return activePolls.size();
    }
}
