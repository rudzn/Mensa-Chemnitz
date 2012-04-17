package de.tzwebdesign.tucmensaapp;

import android.app.Fragment;
import android.os.Bundle;
import android.view.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stephan
 * Date: 17.04.12
 * Time: 10:43
 * To change this template use File | Settings | File Templates.
 */
public class ListView_Fragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.listview_fragment, container, false);
    }
}
