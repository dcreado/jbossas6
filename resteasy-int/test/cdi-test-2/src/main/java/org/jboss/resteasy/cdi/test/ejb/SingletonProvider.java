package org.jboss.resteasy.cdi.test.ejb;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.jboss.resteasy.cdi.test.Cat;

@Singleton
@LocalBean
@Local(ExceptionMapper.class)
@Produces("text/plain")
@Provider
public class SingletonProvider implements ExceptionMapper<InstantiationException> {
    @Inject
    private Cat cat;
    @EJB
    private InjectedStatelessEjbLocal statelessEjb;
    private Cat constructorCat;
    private Cat initializerCat;
    @Context
    private Providers providers;

    public SingletonProvider() {
    }

    @Inject
    public SingletonProvider(Cat cat) {
        constructorCat = cat;
    }

    @Inject
    public void init(Cat cat) {
        initializerCat = cat;
    }

    public Response toResponse(InstantiationException exception) {
        StringBuilder builder = new StringBuilder();
        builder.append("CDI field injection: ");
        builder.append(cat != null);
        builder.append("\nCDI constructor injection: ");
        builder.append(constructorCat != null);
        builder.append("\nCDI initializer injection: ");
        builder.append(initializerCat != null);
        builder.append("\nEJB injection: ");
        builder.append(statelessEjb != null);
        builder.append("\nJAX-RS field injection: ");
        builder.append(providers != null);
        builder.append("\nProvider toString(): ");
        builder.append(toString());

        return Response.status(Status.OK).entity(builder.toString()).build();
    }
}
