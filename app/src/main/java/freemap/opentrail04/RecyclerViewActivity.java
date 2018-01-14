package freemap.opentrail04;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

/**
 * Created by nick on 12/01/18.
 */

abstract public class RecyclerViewActivity extends AppCompatActivity implements ListAdapter.ListClickListener {

    protected RecyclerView view;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_recycler_view);
        view = findViewById(R.id.recyclerView);
        view.setAdapter(getAdapter());
    }

    abstract public RecyclerView.Adapter getAdapter();
    abstract public void onListItemClick(int pos);
}
