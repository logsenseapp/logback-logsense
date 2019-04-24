package com.logsense;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Wombat {
    final Logger logger = LoggerFactory.getLogger(Wombat.class);
    Integer t;
    Integer oldT;

    private void throwIfBelowAbsoluteZero(int temp) {
        if (temp < -273)
            throw new RuntimeException("That is really low temperature: " + temp);
    }

    private void tempSanitizer(int temp) {
        
        
        logger.info("...");
        
        
        throwIfBelowAbsoluteZero(temp);
    }

    public void setTemperature(Integer temperature) {
        oldT = t;
        t = temperature;

        if(temperature.intValue() > 60) {
            logger.info("Current temperature: {} has risen above 60 degrees.", temperature.intValue());
        } else if (temperature.intValue() > 50) {
            logger.warn("Current temperature: {} is already above 50 degrees - that's concerning!", temperature.intValue());
        }
        try {
            tempSanitizer(temperature.intValue());
        } catch (RuntimeException re) {
            logger.error("Sanitization failed", re);
        }
    }
}
