package io.github.j54n1n.webradio.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

import io.github.j54n1n.webradio.BrowseIndexFragment;
import io.github.j54n1n.webradio.service.contentcatalogs.radiotime.model.BrowseIndexItem;

public class BrowseIndexPagerAdapter extends FragmentStatePagerAdapter {

    private final List<BrowseIndexItem> items;

    public BrowseIndexPagerAdapter(FragmentManager fragmentManager,
                                   @NonNull List<BrowseIndexItem> items) {
        super(fragmentManager);
        this.items = items;
    }

    @Override
    public Fragment getItem(int position) {
        return BrowseIndexFragment.newInstance(items.get(position));
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return items.get(position).text;
    }
}
