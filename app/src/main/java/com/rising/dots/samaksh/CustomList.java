package com.rising.dots.samaksh;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by anish on 10-06-2018.
 */

public class CustomList extends ArrayAdapter<HashMap<String, String>> {
    private final Activity context;
    private final ArrayList<HashMap<String, String>> web;

    public CustomList(Activity context, ArrayList<HashMap<String, String>> web) {
        super(context, R.layout.list_item, web);
        this.context = context;
        this.web = web;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.list_item, null, true);

        TextView txtDesc = (TextView) rowView.findViewById(R.id.tvDesc);
        txtDesc.setText(web.get(position).get("desc").toString());

        TextView txtAddress = (TextView) rowView.findViewById(R.id.tvAddress);
        txtAddress.setText("Address: "+ web.get(position).get("loc_url").toString());

        TextView txtremarks = (TextView) rowView.findViewById(R.id.tvRemarks);
        txtremarks.setText("Tags: "+web.get(position).get("remarks").toString());

        return rowView;
    }
}