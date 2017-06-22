package mattoncino.pollo;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import mattoncino.pollo.databinding.ActivePollsListItemBinding;

public class PollsCardViewAdapter extends RecyclerView.Adapter<PollsCardViewAdapter.CardViewHolder> {

    private List<Poll> activePolls;
    private final static String TAG = "CARDVIEW_ADAPTER";
    private ActivePollsListItemBinding listItemBinding;

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




    @Override
    public PollsCardViewAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.active_polls_list_item, parent, false);
        CardViewHolder holder = new CardViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final PollsCardViewAdapter.CardViewHolder holder, final int position) {
        final Poll poll = activePolls.get(position);
        holder.getBinding().setVariable(BR.poll, poll);
        holder.getBinding().executePendingBindings();

        holder.getBinding().opt1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //vote = Consts.FIRST_OPT;
                //listItemBinding.opt1Button.setEnabled(false);
                //listItemBinding.opt2Button.setEnabled(false);
                poll.addVote(Consts.FIRST_OPT);
                //Toast.makeText(view.getContext(), "POLL HOST ADDRESS: " + poll.getHostAddress(), Toast.LENGTH_LONG).show();

                String pollMessages[] = {poll.getName(), new Integer(Consts.FIRST_OPT).toString(), poll.getHostAddress()};

                ClientThreadProcessor clientProcessor = new ClientThreadProcessor(poll.getHostAddress(),
                                            view.getContext(), Consts.POLL_VOTE, pollMessages);
                Thread t = new Thread(clientProcessor);
                t.start();
                //mPeople.remove(holder.getAdapterPosition());
                //notifyItemRemoved(holder.getAdapterPosition());
            }
        });

        holder.getBinding().opt2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //vote = Consts.SECOND_OPT;
                //listItemBinding.opt1Button.setEnabled(false);
                //listItemBinding.opt2Button.setEnabled(false);
                poll.addVote(Consts.SECOND_OPT);
                //Toast.makeText(view.getContext(), "POLL HOST ADDRESS: " + poll.getHostAddress(), Toast.LENGTH_LONG).show();

                String pollMessages[] = {poll.getName(), new Integer(Consts.SECOND_OPT).toString(), poll.getHostAddress()};

                ClientThreadProcessor clientProcessor = new ClientThreadProcessor(poll.getHostAddress(),
                                            view.getContext(), Consts.POLL_VOTE, pollMessages);
                Thread t = new Thread(clientProcessor);
                t.start();
                //launch a thread
                //activePolls.remove(holder.getAdapterPosition());
                //notifyItemRemoved(holder.getAdapterPosition());
            }
        });


    }

    @Override
    public int getItemCount() {
        return activePolls.size();
    }
}
