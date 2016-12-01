package com.algorismng.placeme;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * This fragment does absolutely nothing other than
 * to provide some info about the app
 * info which might not be useful
 */
public class AboutFragment extends Fragment {

    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_about, container, false);

        return rootView;
    }

    //on backpress is handled inside the parent activity

}
