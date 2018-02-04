/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BasicListAdapter extends ListAdapter {


    class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView titlesView;

        public CustomViewHolder(View v) {
            super(v);
            titlesView =  v.findViewById(android.R.id.text1);
        }
    }



    public BasicListAdapter(Context ctx, String[] t,  ListClickListener listener) {
        super(ctx, t, listener);
    }

    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType)  {
        LayoutInflater inflater = (LayoutInflater)
                ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        return new CustomViewHolder(rowView);
    }

    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        CustomViewHolder holder = (CustomViewHolder)h;
        holder.itemView.setOnClickListener (new ItemViewClickListener(position));
        holder.titlesView.setText(titles[position]);
    }
}