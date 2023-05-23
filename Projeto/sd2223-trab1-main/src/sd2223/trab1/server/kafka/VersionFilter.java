package sd2223.trab1.server.kafka;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import sd2223.trab1.server.kafka.sync.SyncPoint;

@Provider
public class VersionFilter implements ContainerResponseFilter {
    SyncPoint<Object> repManager;

    public VersionFilter(SyncPoint<Object> repManager) {
        this.repManager = repManager;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response)
            throws IOException {
        response.getHeaders().add(RepFeedsService.HEADER_VERSION, repManager.getVersion());
    }
}