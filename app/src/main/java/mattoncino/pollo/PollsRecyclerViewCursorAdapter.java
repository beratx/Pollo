package mattoncino.pollo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import  mattoncino.pollo.databinding.OldPollsListItemBinding;


public class PollsRecyclerViewCursorAdapter extends RecyclerView.Adapter<PollsRecyclerViewCursorAdapter.ViewHolder> {
    Context mContext;
    Cursor mCursor;

    public PollsRecyclerViewCursorAdapter(Context mContext, Cursor mCursor) {
        this.mContext = mContext;
        this.mCursor = mCursor;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        OldPollsListItemBinding itemBinding;

        public ViewHolder(View itemView) {
            super(itemView);
            itemBinding = DataBindingUtil.bind(itemView);

        }

        public void bindCursor(Cursor cursor) {
            itemBinding.nameTextView.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(PollDBContract.Poll.COLUMN_NAME)
            ));

            /*Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(cursor.getLong(
                    cursor.getColumnIndexOrThrow(PollDBContract.Poll.COLUMN_DATE)));
            itemBinding.dateLabel.setText(new SimpleDateFormat("dd/MM/yyyy").format(calendar.getTime()));*/
        }

        /*@Override
        public void onClick(View v){
            startActivity(new Intent(mContext, PollDetailActivity.class));

        }*/
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.bindCursor(mCursor);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.old_polls_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }
}
