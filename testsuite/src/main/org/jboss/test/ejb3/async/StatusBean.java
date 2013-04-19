package org.jboss.test.ejb3.async;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Singleton;

/**
 * Implementation of an EJB to track the status
 * of @Asynchronous cancel methods
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
@Singleton
@Local(StatusCommonBusiness.class)
public class StatusBean implements StatusCommonBusiness {

    private static final Logger log = Logger.getLogger(StatusBean.class);
    private boolean cancelled;

    @PostConstruct
    public void reset() {
        this.set(false);
    }

    public void set(final boolean cancelled) {
        log.info("Setting Cancelled Status: " + cancelled);
        this.cancelled = cancelled;
    }

    public boolean wasCancelled() {
        return cancelled;
    }
}