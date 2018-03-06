package io.github.j54n1n.webradio;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.github.j54n1n.webradio.service.contentcatalogs.radiotime.model.BrowseIndexItem;

// TODO: list items
public class BrowseIndexFragment extends Fragment {

    private static final String ARG_ITEM_TEXT = "item_text";
    private static final String ARG_ITEM_URL = "item_url";
    private static final String ARG_ITEM_KEY = "item_key";

    public BrowseIndexFragment() { }

    public static BrowseIndexFragment newInstance(@NonNull BrowseIndexItem item) {
        BrowseIndexFragment fragment = new BrowseIndexFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ITEM_TEXT, item.text);
        args.putString(ARG_ITEM_URL, item.url);
        args.putString(ARG_ITEM_KEY, item.key);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        TextView textView = rootView.findViewById(R.id.section_label);
        final Bundle args = getArguments();
        if(args != null) {
            textView.setText(args.getString(ARG_ITEM_KEY));
        }
        return rootView;
    }
}
