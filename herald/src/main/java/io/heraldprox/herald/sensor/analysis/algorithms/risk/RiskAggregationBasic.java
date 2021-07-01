//  Copyright 2021 Herald Project Contributors
//  SPDX-License-Identifier: Apache-2.0
//

package io.heraldprox.herald.sensor.analysis.algorithms.risk;

import androidx.annotation.NonNull;

import io.heraldprox.herald.sensor.analysis.sampling.Aggregate;
import io.heraldprox.herald.sensor.analysis.sampling.Sample;
import io.heraldprox.herald.sensor.datatype.DoubleValue;

/**
 * A Basic sample but non scientific risk aggregation model.
 * Similar in function to the Oxford Risk Model, but without its calibration values and scaling.
 * <br>
 * NOT FOR PRODUCTION EPIDEMIOLOGICAL USE - SAMPLE ONLY!!!
 * @param <T>
 */
public class RiskAggregationBasic<T extends DoubleValue> implements Aggregate<T> {
    private int run = 1;
    private final double timeScale;
    private final double distanceScale;
    private final double minimumDistanceClamp;
    private final double minimumRiskScoreAtClamp;
    private final double logScale;
    private double nMinusOne; // distance of n-1
    private double n; // distance of n
    private long timeMinusOne; // time of n-1
    private long time; // time of n
    private double riskScore;

    public RiskAggregationBasic(final double timeScale, final double distanceScale, final double minimumDistanceClamp, final double minimumRiskScoreAtClamp) {
        this(timeScale, distanceScale, minimumDistanceClamp, minimumRiskScoreAtClamp, 3.3598856662);
    }

    public RiskAggregationBasic(final double timeScale, final double distanceScale, final double minimumDistanceClamp, final double minimumRiskScoreAtClamp, final double logScale) {
        this.timeScale = timeScale;
        this.distanceScale = distanceScale;
        this.minimumDistanceClamp = minimumDistanceClamp;
        this.minimumRiskScoreAtClamp = minimumRiskScoreAtClamp;
        this.logScale = logScale;
        this.nMinusOne = -1;
        this.n = -1;
        this.timeMinusOne = 0;
        this.time = 0;
        this.riskScore = 0;
    }

    @Override
    public int runs() {
        return 1;
    }

    @Override
    public void beginRun(final int thisRun) {
        run = thisRun;
        if (1 == run) {
            // clear run temporaries
            nMinusOne = -1.0;
            n = -1.0;
            timeMinusOne = 0;
            time = 0;
        }
    }

    @Override
    public void map(@NonNull Sample<T> value) {
        nMinusOne = n;
        timeMinusOne = time;
        n = value.value().doubleValue();
        time = value.taken().secondsSinceUnixEpoch();
   }

    @Override
    public Double reduce() {
        if (-1.0 != nMinusOne) {
            // we have two values with which to calculate
            // using nMinusOne and n, and calculate interim risk score addition
            final double dist = distanceScale * n;
            final double t = timeScale * (time - timeMinusOne); // seconds

            double riskSlice = minimumRiskScoreAtClamp; // assume < clamp distance
            if (dist > minimumDistanceClamp) {
                // otherwise, do the inverse log of distance to get the risk score

                // don't forget to clamp at risk score
                riskSlice = minimumRiskScoreAtClamp - (logScale * Math.log10(dist));
                if (riskSlice > minimumRiskScoreAtClamp) {
                    // possible as the passed in logScale could be a negative
                    riskSlice = minimumRiskScoreAtClamp;
                }
                if (riskSlice < 0.0) {
                    riskSlice = 0.0; // cannot have a negative slice
                }
            }
            riskSlice *= t;

            // add it to the risk score
            riskScore += riskSlice;
        }

        // return current full risk score
        return riskScore;
    }

    @Override
    public void reset() {
        run = 1;
        riskScore = 0.0;
        nMinusOne = -1.0;
        n = -1.0;
    }
}
