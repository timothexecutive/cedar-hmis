package ke.cedar.hmis.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Handle preflight OPTIONS in the REQUEST filter — this is allowed
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            requestContext.abortWith(
                Response.ok()
                    .header("Access-Control-Allow-Origin",      "http://localhost:3000")
                    .header("Access-Control-Allow-Methods",     "GET,POST,PUT,DELETE,PATCH,OPTIONS")
                    .header("Access-Control-Allow-Headers",     "Content-Type,Authorization,Accept,X-Requested-With")
                    .header("Access-Control-Allow-Credentials", "false")
                    .build()
            );
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        // Add CORS headers to every response
        responseContext.getHeaders().add("Access-Control-Allow-Origin",      "http://localhost:3000");
        responseContext.getHeaders().add("Access-Control-Allow-Methods",     "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        responseContext.getHeaders().add("Access-Control-Allow-Headers",     "Content-Type,Authorization,Accept,X-Requested-With");
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "false");
    }
}