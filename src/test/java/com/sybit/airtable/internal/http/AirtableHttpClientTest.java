package com.sybit.airtable.internal.http;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sybit.airtable.exception.AirtableServerException;
import io.reactivex.Single;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AirtableHttpClientTest {

    private AsyncHttpClient asyncHttpClient = mock(AsyncHttpClient.class);
    private ObjectMapper objectMapper = mock(ObjectMapper.class);
    private HttpResponseExceptionHandler exceptionHandler = mock(HttpResponseExceptionHandler.class);
    private AirtableHttpClient client = new AirtableHttpClient(asyncHttpClient, objectMapper, exceptionHandler, 1, 2);

    /**
     * Should execute the request through AsyncHttpClient, check the response status with exception handler,
     * and return the response
     */
    @Test
    public void executeTest() throws ExecutionException, InterruptedException {
        ListenableFuture<Response> future = buildFuture();
        when(asyncHttpClient.executeRequest(any(Request.class))).thenReturn(future);
        Response response = mock(Response.class);
        when(future.get()).thenReturn(response);
        when(exceptionHandler.checkResponse(argThat(arg -> arg != null && arg.getStatusCode() != 429)))
                .then(invocation -> Single.just(invocation.getArgument(0)));
        when(response.getStatusCode()).thenReturn(200);

        client.execute(new RequestBuilder().build()).test().await().assertResult(response);
    }

    /**
     * Should use the exception handler to handle any exception returned from executing the request
     */
    @Test
    public void executeExceptionTest() throws ExecutionException, InterruptedException {
        ListenableFuture<Response> future = buildFuture();
        when(asyncHttpClient.executeRequest(any(Request.class))).thenReturn(future);
        when(future.get()).thenThrow(new ExecutionException(new RuntimeException("test")));
        when(exceptionHandler.handleError(any())).then(invocation ->
                Single.error(new AirtableServerException(500, "", null, invocation.getArgument(0))));

        client.execute(new RequestBuilder().build()).test().await().assertError(AirtableServerException.class);
    }

    /**
     * Should return the exception generated by the exception handler when the handler indicates an error
     */
    @Test
    public void executeErrorStatusCodeTest() throws ExecutionException, InterruptedException {
        ListenableFuture<Response> future = buildFuture();
        when(asyncHttpClient.executeRequest(any(Request.class))).thenReturn(future);
        Response response = mock(Response.class);
        when(future.get()).thenReturn(response);
        when(exceptionHandler.checkResponse(argThat(arg -> arg != null && arg.getStatusCode() == 500))).thenReturn(
                Single.error(new AirtableServerException(500)));
        when(response.getStatusCode()).thenReturn(500);

        client.execute(new RequestBuilder().build()).test().await().assertError(AirtableServerException.class);
    }

    /**
     * Should automatically retry 429 status codes
     */
    @Test
    public void executeRetryTest() throws ExecutionException, InterruptedException {
        ListenableFuture<Response> future = buildFuture();
        ListenableFuture<Response> future2 = buildFuture();
        when(asyncHttpClient.executeRequest(any(Request.class))).thenReturn(future).thenReturn(future2);
        Response response = mock(Response.class);
        Response response2 = mock(Response.class);
        when(future.get()).thenReturn(response);
        when(future2.get()).thenReturn(response2);
        when(exceptionHandler.checkResponse(argThat(arg -> arg != null && arg.getStatusCode() == 429))).thenReturn(
                Single.error(new AirtableServerException(429)));
        when(exceptionHandler.checkResponse(argThat(arg -> arg != null && arg.getStatusCode() != 429)))
                .then(invocation -> Single.just(invocation.getArgument(0)));
        when(response.getStatusCode()).thenReturn(429);
        when(response2.getStatusCode()).thenReturn(200);

        client.execute(new RequestBuilder().build()).test().await().assertResult(response2);
    }

    @SuppressWarnings("unchecked")
    private <T> ListenableFuture<T> buildFuture() {
        ListenableFuture<T> future = mock(ListenableFuture.class);
        when(future.addListener(any(), any())).then(invocation -> {
            Executor executor = invocation.getArgument(1);
            Runnable r = invocation.getArgument(0);
            if (executor == null)
                r.run();
            else
                executor.execute(r);
            return invocation.getMock();
        });
        return future;
    }
}
