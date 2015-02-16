package io.advantageous.qbit.client;

import io.advantageous.qbit.QBit;
import io.advantageous.qbit.http.client.HttpClient;
import io.advantageous.qbit.http.request.HttpRequest;
import io.advantageous.qbit.http.websocket.WebSocket;
import io.advantageous.qbit.http.websocket.WebSocketBuilder;
import io.advantageous.qbit.http.websocket.WebSocketMessage;
import io.advantageous.qbit.http.websocket.WebSocketSender;
import io.advantageous.qbit.message.MethodCall;
import io.advantageous.qbit.service.Callback;
import io.advantageous.qbit.spi.FactorySPI;
import io.advantageous.qbit.spi.HttpClientFactory;
import org.boon.core.Sys;
import org.boon.core.reflection.BeanUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.function.Consumer;

import static io.advantageous.qbit.http.websocket.WebSocketBuilder.webSocketBuilder;
import static org.boon.Boon.puts;
import static org.boon.Exceptions.die;

public class BoonClientTest {

    Client client;
    boolean httpStopCalled;
    boolean httpStartCalled;
    boolean httpSendWebSocketCalled;
    boolean httpFlushCalled;
    boolean httpPeriodicFlushCallbackCalled;
    boolean ok;
    volatile int sum;

    public static interface ServiceMock {
        void add(int a, int b);

        void sum(Callback<Integer> callback);
    }

    @Before
    public void setUp() throws Exception {
        client = new BoonClientFactory().create("/uri", new HttpClientMock() , 10);
        FactorySPI.setHttpClientFactory(new HttpClientFactory() {

            @Override
            public HttpClient create(String host, int port, int requestBatchSize, int timeOutInMilliseconds, int poolSize, boolean autoFlush, int flushRate, boolean keepAlive, boolean pipeLine) {
                return new HttpClientMock();
            }
        });


    }

    @After
    public void tearDown() throws Exception {
        client.flush();
        client.stop();

    }

    @Test
    public void testStop() throws Exception {

        client.stop();
        ok = httpStopCalled || die();

    }

    @Test
    public void testFlush() throws Exception {

        client.flush();
    }

    @Test
    public void testCreateProxy() throws Exception {
        client.start();
        Sys.sleep(100);

        final ServiceMock mockService = client.createProxy(ServiceMock.class, "mockService");

        mockService.add(1, 2);

        ((ClientProxy)mockService).clientProxyFlush();

        Sys.sleep(100);

        ok = httpSendWebSocketCalled || die();



    }

    @Test
    public void testWithBuilder() throws Exception {

        final ClientBuilder clientBuilder = new ClientBuilder();
        ok = clientBuilder.setFlushInterval(100).getFlushInterval() == 100 || die();
        ok = clientBuilder.setPollTime(51).getPollTime() == 51 || die();
        ok = clientBuilder.setRequestBatchSize(13).getRequestBatchSize() == 13 || die();
        ok = clientBuilder.setAutoFlush(true).isAutoFlush() == true || die();
        ok = clientBuilder.setAutoFlush(false).isAutoFlush() == false || die();
        ok = clientBuilder.setAutoFlush(true).isAutoFlush() == true || die();
        ok = clientBuilder.setUri("/book").getUri().equals("/book")  || die();
        ok = clientBuilder.setUri("/services").getUri().equals("/services")  || die();
        ok = clientBuilder.setHost("host").getHost().equals("host")  || die();
        ok = clientBuilder.setHost("localhost").getHost().equals("localhost")  || die();
        ok = clientBuilder.setPort(9090).getPort() == 9090  || die();
        ok = clientBuilder.setPort(8080).getPort() == 8080  || die();
        ok = clientBuilder.setTimeoutSeconds(67).getTimeoutSeconds() == 67  || die();
        ok = clientBuilder.setTimeoutSeconds(5).getTimeoutSeconds() == 5  || die();
        ok = clientBuilder.setProtocolBatchSize(5).getProtocolBatchSize() == 5  || die();
        ok = clientBuilder.setProtocolBatchSize(50).getProtocolBatchSize() == 50  || die();

        client = clientBuilder.build();

        ok = clientBuilder.setProtocolBatchSize(-1).getProtocolBatchSize() == -1  || die();

        client = clientBuilder.build();

        client.start();

        Sys.sleep(100);

        final ServiceMock mockService = client.createProxy(ServiceMock.class, "mockService");


        mockService.sum(integer -> puts("SUM", integer));

        ((ClientProxy)mockService).clientProxyFlush();


        Sys.sleep(100);

        ok = httpSendWebSocketCalled || die();


    }


    @Test
    public void testCallBack() throws Exception {
        client.start();
        Sys.sleep(100);

        final ServiceMock mockService = client.createProxy(ServiceMock.class, "mockService");

        mockService.sum(integer -> puts("SUM", integer));

        ((ClientProxy)mockService).clientProxyFlush();

        Sys.sleep(100);

        ok = httpSendWebSocketCalled || die();



    }



    @Test
    public void testStart() throws Exception {

        client.start();
        ok = httpPeriodicFlushCallbackCalled || die();
        ok = httpStartCalled || die();
    }


    private class HttpClientMock implements HttpClient {

        Consumer<Void> periodicFlushCallback;


        @Override
        public void sendHttpRequest(HttpRequest request) {

        }


        @Override
        public WebSocket createWebSocket(final String uri) {

            final WebSocketBuilder webSocketBuilder = webSocketBuilder().setRemoteAddress("test").setUri(uri).setBinary(false).setOpen(true);

            final WebSocket webSocket = webSocketBuilder.build();

            final WebSocketSender webSocketSender = new WebSocketSender() {
                @Override
                public void sendText(final String body) {


                    httpSendWebSocketCalled = true;
                    periodicFlushCallback.accept(null);
                    Sys.sleep(100);

                }
            };

            BeanUtils.idx(webSocket, "networkSender", webSocketSender);

            return webSocket;
        }

        @Override
        public void periodicFlushCallback(Consumer<Void> periodicFlushCallback) {
            httpPeriodicFlushCallbackCalled = true;
            this.periodicFlushCallback = periodicFlushCallback;

        }

        @Override
        public HttpClient start() {
            httpStartCalled = true;
            return this;

        }

        @Override
        public void flush() {
            httpFlushCalled = true;

        }

        @Override
        public void stop() {

            httpStopCalled = true;
        }
    }
}