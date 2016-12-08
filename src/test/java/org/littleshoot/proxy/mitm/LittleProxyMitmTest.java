package org.littleshoot.proxy.mitm;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ganskef.test.Client;
import de.ganskef.test.IClient;
import de.ganskef.test.Server;
import de.ganskef.test.TrustedServer;

public class LittleProxyMitmTest {

    private static final String IMAGE_PATH = "src/test/resources/www/netty-in-action.gif";

    protected static LittleProxyMitmProxy proxy;
    private static Server server;
    private static Server secureServer;

    @AfterClass
    public static void afterClass() {
        secureServer.stop();
        server.stop();
        proxy.stop();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = new Server(9091).start();
        secureServer = new TrustedServer(9092).start();
    }

    @BeforeClass
    public static void initProxy() throws Exception {
        proxy = new LittleProxyMitmProxy(9093).start();
    }

    @Before
    public void before() {
        proxy.setConnectionUnlimited();
    }

    protected IClient newClient() {
        return new Client();
    }

    @Test
    public void testSimpleImage() throws Exception {
        String url = server.getBaseUrl() + "/" + IMAGE_PATH;
        File direct = newClient().get(url);

        File proxied = newClient().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

    @Test
    public void testSecuredImage() throws Exception {
        String url = secureServer.getBaseUrl() + "/" + IMAGE_PATH;
        File direct = newClient().get(url);

        File proxied = newClient().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

    @Test
    public void testOnlineTextSecured() throws Exception {
        String url = "https://www.google.com/humans.txt";
        File direct = null;
        try {
            direct = newClient().get(url);
        } catch (ConnectException ignored) {
            System.out.println("Ignored test while offline");
        } catch (UnknownHostException ignored) {
            System.out.println("Ignored test while offline");
        } catch (UnresolvedAddressException ignored) {
            System.out.println("Ignored test while offline");
        }
        assumeThat("has internet connection", direct, notNullValue());

        File proxied = newClient().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

    @Test
    public void testCachedResponse() throws Exception {
        proxy.setConnectionLimited();
        String url = "http://somehost/somepath";
        File proxied = newClient().get(url, proxy);
        assertEquals("Offline response", FileUtils.readFileToString(proxied));
    }

    // To use MITM without an Internet connection you have to use the branch
    // enable_offline_caching_with_mitm
    //
    @Test(expected = IOException.class)
    public void testCachedResponseSecured() throws Exception {
        proxy.setConnectionLimited();
        String url = "https://somehost/somepath";
        File proxied = newClient().get(url, proxy);
        assertEquals("Offline response", FileUtils.readFileToString(proxied));
    }

    @Test
    public void testOnlineServerNameIndicationIssue207() throws Exception {

        // https://netty.io site fails with 522 today 7/11/2015
        // String url = "https://netty.io/";

        String url = "https://kitematic.com/";
        File direct = null;
        try {
            direct = newClient().get(url);
        } catch (ConnectException ignored) {
            System.out.println("Ignored test while offline");
        } catch (UnknownHostException ignored) {
            System.out.println("Ignored test while offline");
        } catch (UnresolvedAddressException ignored) {
            System.out.println("Ignored test while offline");
        }
        assumeThat("has internet connection", direct, notNullValue());

        File proxied = newClient().get(url, proxy);
        assertEquals(direct.length(), proxied.length());
    }

}
