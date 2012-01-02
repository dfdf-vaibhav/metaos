/*
 * Copyright 2011 - 2012
 * All rights reserved. License and terms according to LICENSE.txt file.
 * The LICENSE.txt file and this header must be included or referenced 
 * in each piece of code derived from this project.
 */
package com.metaos.signalgrt.predictors.specific.volume;

import java.util.*;
import java.util.logging.*;
import com.metaos.signalgrt.predictors.*;
import com.metaos.signalgrt.predictors.specific.*;
import com.metaos.util.*;
import com.metaos.datamgt.*;

/**
 * Calculates daily volume profile for the "typical stock" in the given
 * market.
 *
 * Thus, after receiving all daily traded volumes for each bin for every stock
 * in the market, this predictor calculates the common profile for the
 * "typical" stock in the market the next day.
 * <br/>
 * Prepared to be used for markets with opening and closing times, but not
 * with pauses during the session.
 * <br/>
 * Volume profile are scaled to 100: sum of all daily volume profile values
 * is always 100.
 * <br/>
 * Outliers are removed.
 * <br/>
 * When predicting, unknown data is represented as NaN.
 */
public final class PCAVolumeProfileConsideringMarketPredictor 
        extends PCADayByDayPredictor {
    private static final Logger log = Logger.getLogger(
            PCAVolumeProfileConsideringMarketPredictor.class.getPackage()
                    .getName());

    private final int ignoreElementsHead, ignoreElementsTail;

    //
    // Public methods ---------------------------------------
    //

    /**
     * Creates a day-by-day predictor which uses data from each day to
     * forecast data for next day.
     *
     * @param minimumVariance minimum explained variance by model.
     */
    public PCAVolumeProfileConsideringMarketPredictor(
            final CalUtils.InstantGenerator instantGenerator,
            final double minimumVariance, final String[] symbols) {
        super(instantGenerator, new Field.VOLUME(), minimumVariance, 100.0d,
                symbols);
        this.ignoreElementsHead = 0;
        this.ignoreElementsTail = 0;
    }


    /**
     * Creates a day-by-day predictor which uses data from each day to
     * forecast data for next day ignoring elements at the begining and at
     * the end of the trading day.
     *
     * @param minimumVariance minimum explained variance by model.
     * @param ignoreElementsHead number of elements to ignore from the first
     * element with value (maybe opening auction).
     * @param ignoreElementsHead number of elements to ignore from the last 
     * element with value (maybe closing auction).
     */
    public PCAVolumeProfileConsideringMarketPredictor(
            final CalUtils.InstantGenerator instantGenerator,
            final double minimumVariance, final String[] symbols,
            final int ignoreElementsHead, final int ignoreElementsTail) {
        super(instantGenerator, new Field.VOLUME(), minimumVariance, 100.0d,
                symbols);
        this.ignoreElementsHead = ignoreElementsHead;
        this.ignoreElementsTail = ignoreElementsTail;
    }


    
    /**
     * Performs a cleaning of outliers and modifies data to get an acceptable
     * volume profile (all values greater or equals to zero and with a sum 
     * of 100).
     */
    public double[] predictVector(final Calendar when) {
        final double result[] = super.predictVector(when);

        // Remove negative values
        double sum = 0;
        for(int i=0; i<result.length; i++) {
            if(Double.isNaN(result[i])) continue;
            if(result[i]<0) {
                log.severe("Setting negative value for prediction to 0 at "
                        + "index " + i);
                result[i] = 0;
            }
            sum += result[i];
        }

        // Rescale
        for(int i=0; i<result.length; i++) {
            result[i] = 100 * result[i] / sum;
        }

        return result;
    }


    /**
     * Returns the human name of the predictor.
     */
    public String toString() {
        return this.scale<=0 ? "Not Normalized PCA Volume Profile Considering "
                + "all market values Predictor" 
                : "Normalized to " + this.scale 
                + " PCA Volume Profile Considering all market values Predictor";
    }


    //
    // Hook methods ---------------------------------------------
    //


    /**
     * Remove outliers: values greater than 50% of the last value 
     * (as supposed to be the closing auction value) are removed.
     * Then, elements from tail and head are removed too, according to
     * initialization parameters.
     */
    @Override protected void cleanData(final double vals[]) {
        RemoveVolumeData.cleanOutliers(vals);
        RemoveVolumeData.cutHeadAndTail(vals, this.ignoreElementsHead,
                this.ignoreElementsTail);
    }
}