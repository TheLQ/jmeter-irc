/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.jmeter.protocol.irc;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 *
 * @author lordquackstar
 */
public class IrcBotSampler extends AbstractSampler {
	
	private static final Logger log = LoggingManager.getLoggerForClass();

    // The name of the property used to hold our data
    public final static String DATA = "ExampleSampler.data"; //$NON-NLS-1$

    private static int classCount = 0; // keep track of classes created

    // (for instructional purposes only!)

    public IrcBotSampler() {
        classCount++;
        trace("ExampleSampler()");
    }

    /**
     * {@inheritDoc}
     */
    public SampleResult sample(Entry e) {
        trace("sample()");
        SampleResult res = new SampleResult();
        boolean isOK = false; // Did sample succeed?
        String data = getData(); // Sampler data
        String response = null;

        res.setSampleLabel(getTitle());
        /*
         * Perform the sampling
         */
        res.sampleStart(); // Start timing
        try {

            // Do something here ...

            response = Thread.currentThread().getName();

            /*
             * Set up the sample result details
             */
            res.setSamplerData(data);
            res.setResponseData(response, null);
            res.setDataType(SampleResult.TEXT);

            res.setResponseCodeOK();
            res.setResponseMessage("OK");
            isOK = true;
        } catch (Exception ex) {
            log.debug("", ex);
            res.setResponseCode("500");
            res.setResponseMessage(ex.toString());
        }
        res.sampleEnd(); // End timimg

        res.setSuccessful(isOK);

        return res;
    }

    /**
     * @return a string for the sampleResult Title
     */
    private String getTitle() {
        return this.getName();
    }

    /**
     * @return the data for the sample
     */
    public String getData() {
        return getPropertyAsString(DATA);
    }

    /*
     * Helper method
     */
    private void trace(String s) {
        String tl = getTitle();
        String tn = Thread.currentThread().getName();
        String th = this.toString();
        log.debug(tn + " (" + classCount + ") " + tl + " " + s + " " + th);
    }
	
}
