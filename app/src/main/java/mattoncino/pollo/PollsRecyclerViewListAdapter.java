package mattoncino.pollo;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import mattoncino.pollo.databinding.ActivePollsListItemBinding;


public class PollsRecyclerViewListAdapter extends RecyclerView.Adapter<PollsRecyclerViewListAdapter.SimpleViewHolder> {

    private List<Poll> activePolls;

    public PollsRecyclerViewListAdapter(List<Poll> polls){
        activePolls = polls;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.active_polls_list_item, parent, false);
        SimpleViewHolder holder = new SimpleViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        final Poll poll = activePolls.get(position);
        holder.getBinding().setVariable(BR.poll, poll);
        holder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return activePolls.size();
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {

        private ActivePollsListItemBinding listItemBinding;

        public SimpleViewHolder(View v) {
            super(v);
            listItemBinding = DataBindingUtil.bind(v);
        }

        public ActivePollsListItemBinding getBinding(){
            return listItemBinding;
        }
    }
}
