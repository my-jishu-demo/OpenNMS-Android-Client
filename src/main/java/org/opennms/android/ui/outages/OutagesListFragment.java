package org.opennms.android.ui.outages;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.opennms.android.Loaders;
import org.opennms.android.R;
import org.opennms.android.provider.Contract;
import org.opennms.android.service.OutagesSyncService;

public class OutagesListFragment extends SherlockListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_OUTAGE_ID = "outage";
    private static final String STATE_ACTIVE_OUTAGE_ID = "active_outage_id";
    private SimpleCursorAdapter adapter;
    private boolean isDualPane = false;
    private MenuItem refreshItem;
    private SharedPreferences sharedPref;
    private FrameLayout detailsContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        detailsContainer = (FrameLayout) getActivity().findViewById(R.id.details_fragment_container);
        isDualPane = detailsContainer != null && detailsContainer.getVisibility() == View.VISIBLE;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (isDualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        adapter = new SimpleCursorAdapter(
                getActivity(),
                android.R.layout.simple_list_item_2,
                null,
                new String[]{Contract.Outages._ID, Contract.Outages.SERVICE_TYPE_NAME},
                new int[]{android.R.id.text1, android.R.id.text2},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        getListView().setAdapter(adapter);

        TextView emptyText = (TextView) getActivity().findViewById(R.id.empty_list_text);
        emptyText.setText(getString(R.string.outages_list_empty));

        getActivity().getSupportLoaderManager().initLoader(Loaders.OUTAGES, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isDualPane) {
            long activeOutageId = sharedPref.getLong(STATE_ACTIVE_OUTAGE_ID, -1);
            if (activeOutageId != -1) {
                showDetails(activeOutageId);
            } else {
                detailsContainer.removeAllViews();
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                RelativeLayout emptyView = (RelativeLayout) inflater.inflate(R.layout.empty_details, null);
                detailsContainer.addView(emptyView);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopRefreshAnimation();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails(position);
    }

    private void showDetails(int position) {
        getListView().setItemChecked(position, true);
        long id = getListView().getItemIdAtPosition(position);
        sharedPref.edit().putLong(STATE_ACTIVE_OUTAGE_ID, id).commit();
        showDetails(id);
    }

    private void showDetails(long id) {
        if (isDualPane) {
            detailsContainer.removeAllViews();
            OutageDetailsFragment detailsFragment = new OutageDetailsFragment(id);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.details_fragment_container, detailsFragment);
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            fragmentTransaction.commit();
        } else {
            Intent detailsIntent = new Intent(getActivity(), OutageDetailsActivity.class);
            detailsIntent.putExtra(EXTRA_OUTAGE_ID, id);
            startActivity(detailsIntent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshItem = item;
                refreshList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshList() {
        startRefreshAnimation();
        Intent intent = new Intent(getActivity(), OutagesSyncService.class);
        getActivity().startService(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                Contract.Outages._ID,
                Contract.Outages.SERVICE_TYPE_NAME
        };
        return new CursorLoader(
                getActivity(),
                Contract.Outages.CONTENT_URI,
                projection,
                null,
                null,
                Contract.Outages._ID + " DESC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        stopRefreshAnimation();
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private void startRefreshAnimation() {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);
        Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        refreshItem.setActionView(iv);
    }

    private void stopRefreshAnimation() {
        if (refreshItem != null && refreshItem.getActionView() != null) {
            refreshItem.getActionView().clearAnimation();
            refreshItem.setActionView(null);
        }
    }

}