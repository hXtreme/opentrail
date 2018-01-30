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

public class AnnotatedListAdapter extends ListAdapter {

    String[] annotations;

    class CustomViewHolder extends RecyclerView.ViewHolder {
        TextView titlesView, detailsView;

        public CustomViewHolder(View v) {
            super(v);
            titlesView =  v.findViewById(R.id.annotatedListName);
            detailsView =  v.findViewById(R.id.annotatedListDetails);
        }

    }


    public AnnotatedListAdapter(Context ctx, String[] t,String[] a, ListClickListener listener) {

        super(ctx, t, listener);
        this.annotations=a;
    }

    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType)  {
        LayoutInflater inflater = (LayoutInflater)
                ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.annotatedlistitem, parent, false);
        Log.d("opentrail", "Created a view holder");
        return new CustomViewHolder(rowView);
    }

    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        CustomViewHolder holder = (CustomViewHolder)h;
        holder.itemView.setOnClickListener (new ItemViewClickListener(position));
        holder.titlesView.setText(titles[position]);
        holder.detailsView.setText(annotations[position]);
        Log.d("opentrail","Index = "+ position + " Name="+titles[position]+" Type="+annotations[position]);
    }
}