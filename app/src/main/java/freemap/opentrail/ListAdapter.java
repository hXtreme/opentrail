/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

abstract public class ListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    String[] titles;


    public interface ListClickListener {
        void onListItemClick(int index);
    }

    class ItemViewClickListener implements View.OnClickListener {
        int index;
        public ItemViewClickListener(int index) {
            this.index = index;
        }
        public void onClick(View v) {
            listener.onListItemClick(index);
        }
    }

    ListClickListener listener;
    Context ctx;

    public ListAdapter(Context ctx, String[] n, ListClickListener listener) {

        this.ctx=ctx;
        this.titles=n;
        this.listener = listener;
    }



    public int getItemCount() {
        return titles==null? 0: titles.length;
    }
}