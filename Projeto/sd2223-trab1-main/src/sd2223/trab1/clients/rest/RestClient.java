package sd2223.trab1.clients.rest;

import java.net.URI;
import java.util.logging.Logger;
import java.util.function.Supplier;

import javax.net.ssl.HttpsURLConnection;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.client.Client;
import sd2223.trab1.api.java.Result;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response.Status;

import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.tls.InsecureHostnameVerifier;

import static sd2223.trab1.api.java.Result.ok;
import static sd2223.trab1.api.java.Result.error;

public class RestClient {
    private static Logger Log = Logger.getLogger(RestClient.class.getName());

    protected static final int MAX_RETRIES = 20;
    protected static final int RETRY_SLEEP = 1000;
    protected static final int READ_TIMEOUT = 5000;
    protected static final int CONNECT_TIMEOUT = 5000;

    final URI serverURI;
    final ClientConfig config;
    protected final Client client;

    public RestClient(URI serverURI) {
        HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
        this.serverURI = serverURI;
        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = ClientBuilder.newClient(config);
    }

    protected <T> Result<T> reTry(Supplier<Result<T>> func) {
        for (int i = 0; i < MAX_RETRIES; i++)
            try {
                return func.get();
            } catch (ProcessingException x) {
                Log.fine("Timeout: " + x.getMessage());
                sleep(RETRY_SLEEP);
            } catch (Exception x) {
                x.printStackTrace();
                return Result.error(ErrorCode.INTERNAL_ERROR);
            }
        return Result.error(ErrorCode.TIMEOUT);
    }

    protected <T> Response reTryResponse(Supplier<Response> func) {
        for (int i = 0; i < MAX_RETRIES; i++)
            try {
                return func.get();
            } catch (ProcessingException x) {
                Log.fine("Timeout: " + x.getMessage());
                sleep(RETRY_SLEEP);
            } catch (Exception x) {
                x.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        return Response.status(Status.BAD_REQUEST).build();
    }

    protected <T> Result<T> toJavaResult(Response r, Class<T> entityType) {
        try {
            var status = r.getStatusInfo().toEnum();
            if (status == Status.OK && r.hasEntity())
                return ok(r.readEntity(entityType));
            else if (status == Status.NO_CONTENT)
                return ok();

            return error(getErrorCodeFrom(status.getStatusCode()));
        } finally {
            r.close();
        }
    }

    protected <T> Result<T> toJavaResult(Response r, GenericType<T> entityType) {
        try {
            var status = r.getStatusInfo().toEnum();
            if (status == Status.OK && r.hasEntity())
                return ok(r.readEntity(entityType));
            else if (status == Status.NO_CONTENT)
                return ok();

            return error(getErrorCodeFrom(status.getStatusCode()));
        } finally {
            r.close();
        }
    }

    public static ErrorCode getErrorCodeFrom(int status) {
        return switch (status) {
            case 200, 209 -> ErrorCode.OK;
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 500 -> ErrorCode.INTERNAL_ERROR;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException x) {
        }
    }

    @Override
    public String toString() {
        return serverURI.toString();
    }
}
