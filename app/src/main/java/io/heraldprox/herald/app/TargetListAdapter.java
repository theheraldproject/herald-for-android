//  Copyright 2020-2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.heraldprox.herald.sensor.datatype.LegacyPayloadData;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Target list adapter for presenting list of targets on UI.
 */
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
        // Event time interval statistics
        final StringBuilder statistics = new StringBuilder();
        statistics.append("R");
        if (target.didReadTimeInterval().mean() != null) {
            statistics.append("=");
            statistics.append(decimalFormat.format(target.didReadTimeInterval().mean())).append("s");
        }
        if (target.didMeasureTimeInterval().mean() != null) {
            statistics.append(",M=");
            statistics.append(decimalFormat.format(target.didMeasureTimeInterval().mean())).append("s");
        }
        if (target.didShareTimeInterval().mean() != null) {
            statistics.append(",S=");
            statistics.append(decimalFormat.format(target.didShareTimeInterval().mean())).append("s");
        }
        // Distance
        final StringBuilder distance = new StringBuilder();
        if (target.distance() != null) {
            distance.append(String.format("%.2fm", target.distance().value));
        }
        final StringBuilder labelText = new StringBuilder(target.payloadData().shortName());
        if (target.payloadData() instanceof LegacyPayloadData) {
            labelText.append(':');
            labelText.append(((LegacyPayloadData) target.payloadData()).protocolName().name().charAt(0));
        }
        if (!distance.toString().isEmpty()) {
            labelText.append(" ~ ");
            labelText.append(distance.toString());
        }
        final String didReceive = (target.didReceive() == null ? "" : " (receive " + dateFormatterTime.format(target.didReceive()) + ")");
        textLabel.setText(labelText.toString() + didReceive);
        detailedTextLabel.setText(dateFormatter.format(target.lastUpdatedAt()) + " [" + statistics.toString() + "]");
        return convertView;
    }
}
