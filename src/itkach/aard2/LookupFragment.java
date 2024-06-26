package itkach.aard2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;

import java.util.Timer;
import java.util.TimerTask;

public class LookupFragment extends BaseListFragment implements LookupListener {

    private Timer       timer;
    private SearchView searchView;
    private Application app;
    private SearchView.OnQueryTextListener queryTextListener;
    private SearchView.OnCloseListener closeListener;

    private final static String TAG = LookupFragment.class.getSimpleName();

    @Override
    char getEmptyIcon() {
        return IconMaker.IC_SEARCH;
    }

    @Override
    CharSequence getEmptyText() {
        return "";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (Application) getActivity().getApplication();
        app.addLookupListener(this);
    }

    @Override
    protected boolean supportsSelection() {
        return false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setBusy(false);
        ListView listView = getListView();
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.i("--", "Item clicked: " + position);
                Intent intent = new Intent(getActivity(),
                        ArticleCollectionActivity.class);
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });
        final Application app = (Application) getActivity().getApplication();
        getListView().setAdapter(app.lastResult);

        closeListener = new SearchView.OnCloseListener() {

            @Override
            public boolean onClose() {
                return true;
            }
        };

        queryTextListener = new SearchView.OnQueryTextListener() {

            TimerTask scheduledLookup = null;

            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "query text submit: " + query);
                onQueryTextChange(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "new query text: " + newText);
                TimerTask doLookup = new TimerTask() {
                    @Override
                    public void run() {
                        final String query = searchView.getQuery().toString();
                        if (app.getLookupQuery().equals(query)) {
                            return;
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                app.lookup(query);
                            }
                        });
                        scheduledLookup = null;
                    }
                };
                final String query = searchView.getQuery().toString();
                if (!app.getLookupQuery().equals(query)) {
                    if (scheduledLookup != null) {
                        scheduledLookup.cancel();
                    }
                    scheduledLookup = doLookup;
                    timer.schedule(doLookup, 600);
                }
                return true;
            }
        };

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        timer = new Timer();
        inflater.inflate(R.menu.lookup, menu);
        MenuItem miFilter = menu.findItem(R.id.fldLookup);
        searchView = (SearchView) miFilter.getActionView();
        searchView.setQueryHint(miFilter.getTitle());
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(queryTextListener);
        searchView.setOnCloseListener(closeListener);
        searchView.setSubmitButtonEnabled(false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (app.autoPaste()) {
            CharSequence clipboard  = Clipboard.take(this.getActivity());
            if (clipboard != null) {
                app.lookup(clipboard.toString(), false);
            }
        }
        CharSequence query = app.getLookupQuery();
        searchView.setQuery(query, true);
        if (app.lastResult.getCount() > 0) {
            searchView.clearFocus();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (searchView != null) {
            String query = searchView.getQuery().toString();
            outState.putString("lookupQuery", query);
        }
    }

    private void setBusy(boolean busy) {
        setListShown(!busy);
        if (!busy) {
            TextView emptyText = ((TextView)emptyView.findViewById(R.id.empty_text));
            String msg = "";
            String query = app.getLookupQuery();
            if (query != null && !query.toString().equals("")) {
                msg = getString(R.string.lookup_nothing_found);
            }
            emptyText.setText(msg);
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        Application app = (Application) getActivity().getApplication();
        app.removeLookupListener(this);
        super.onDestroy();
    }

    @Override
    public void onLookupStarted(String query) {
        setBusy(true);
    }

    @Override
    public void onLookupFinished(String query) {
        setBusy(false);
    }

    @Override
    public void onLookupCanceled(String query) {
        setBusy(false);
    }

}
