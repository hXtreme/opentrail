/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AnnotatedListAdapter extends ArrayAdapter<String>
{
    Context ctx;
    String[] names, annotations;

    public AnnotatedListAdapter(Context ctx,int res,String[] n,String[] a)
    {
        super(ctx,res,n);
        this.ctx=ctx;
        this.names=n;
        this.annotations=a;
    }

    public View getView (int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater)
                ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.poilistitem, parent, false);
        TextView nameView = (TextView) rowView.findViewById(R.id.poiListPOIName),
                detailsView = (TextView) rowView.findViewById(R.id.poiListPOIDetails);
        nameView.setText(names[position]);
        detailsView.setText(annotations[position]);
        Log.d("OpenTrail","Name="+names[position]+" Type="+annotations[position]);
        return rowView;
    }

}