package org.jboss.test.ejb3.async;

import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.*;
import java.util.concurrent.Future;

/**
 * Implementation of an EJB with @Asynchronous methods
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
@Stateless
@Local(AsyncLocalBusiness.class)
@Remote(AsyncRemoteBusiness.class)
@LocalBean
public class AsyncBean implements AsyncCommonBusiness
{

    private static final Logger log = Logger.getLogger(AsyncBean.class);

    @Resource
    private SessionContext context;

    @EJB
    private StatusCommonBusiness status;

    @Asynchronous
    public Future<String> getExecutingThreadNameAsync() {
        return this.getExecutingThreadName();
    }

    public Future<String> getExecutingThreadNameBlocking() {
        return this.getExecutingThreadName();
    }

    @Asynchronous
    public Future<Void> waitTenSeconds(){

        log.info("Entered request to wait one minute");
        // Reset the singleton status flag
        status.reset();

        final long start = System.currentTimeMillis();
        // End in 10 seconds
        final long end = start+(10*1000);

        // Loop
        log.info("START LOOP");
        while(System.currentTimeMillis()<end){
            
            // If we've been cancelled, update the singleton status bit
            if(context.wasCancelCalled())
            {
                status.set(true);
                log.info("Set status to true");
                // Break out of the loop
                break;
            }

            // Wait a bit and check again
            try {
                log.info("Waiting...");
                Thread.sleep(50);
            } catch (final InterruptedException e) {
                Thread.interrupted(); // Clear the flag
                throw new RuntimeException("Should not have been interrupted");
            }

        }
        // Return a dummy value
        return new AsyncResult<Void>(null);    
    }

    private Future<String> getExecutingThreadName(){
      return new AsyncResult<String>(Thread.currentThread().getName());
    }
    
}
