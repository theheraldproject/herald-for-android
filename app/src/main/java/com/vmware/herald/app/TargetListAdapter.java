//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.vmware.herald.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/// Target list adapter for presenting list of targets on UI
public class TargetListAdapter extends ArrayAdapter<Target> {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final static SimpleDateFormat dateFormatterTime = new SimpleDateFormat("HH:mm:ss");
    private final static DecimalFormat decimalFormat = new DecimalFormat("0.0");

    public TargetListAdapter(@NonNull Context context, List<Target> targets) {
        super(context, R.layout.listview_targets_row, targets);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final Target target = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_targets_row, parent, false);
        }
        final TextView textLabel = (TextView) convertView.findViewById(R.id.targetTextLabel);
        final TextView detailedTextLabel = (TextView) convertView.findViewById(R.id.targetDetailedTextLabel);
        final StringBuilder attributes = new StringBuilder();
        attributes.append("Read");
        if (target.didReadTimeInterval().count() != 0) {
            attributes.append("=");
            attributes.append(decimalFormat.format(target.didReadTimeInterval().mean()) + "s");
        }
        if (target.didMeasureTimeInterval().count() != 0) {
            attributes.append(",Measure=");
            attributes.append(decimalFormat.format(target.didMeasureTimeInterval().mean()) + "s");
        }
        if (target.didShare() != null) {
            attributes.append(",Share");
        }
        final String didReceive = (target.didReceive() == null ? "" : " (receive " + dateFormatterTime.format(target.didReceive()) + ")");
        textLabel.setText(target.payloadData().shortName() + " [" + attributes.toString() + "]");
        detailedTextLabel.setText(dateFormatter.format(target.lastUpdatedAt()) + didReceive);
        return convertView;
    }
}
