package com.wavefront.integrations;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This class tests FDBMetricsReporterArguments.
 *
 * @author Hovhannes Tonakanyan (htonakanyan@vmware.com).
 */
public class FDBMetricsReporterArgumentsTest {

    private FDBMetricsReporterArguments arguments;
    private static final String FDB_LOG_DIR = "/usr/local/foundationdb/logs";

    @Before
    public void setUp() {
        arguments = new FDBMetricsReporterArguments();
    }

    @Test(expected = ParameterException.class)
    public void testDirectoryValidator() {
        String dir = "invalidDir";
        String[] args = {"--dir", dir};
        JCommander jCommander = new JCommander(arguments, args);
    }

    @Test(expected = ParameterException.class)
    public void testPortInvalidValue1() {
        String[] args = {"-d", FDB_LOG_DIR, "--proxyPort", "-8"};
        JCommander jCommander = new JCommander(arguments, args);
    }

    @Test(expected = ParameterException.class)
    public void testPortInvalidValue2() {
        String[] args = {"--proxyPort", "0"};
        JCommander jCommander = new JCommander(arguments, args);
    }

    @Test(expected = ParameterException.class)
    public void testPortInvalidFormat() {
        String[] args = {"-d", FDB_LOG_DIR, "--proxyPort", "abc"};
        JCommander jCommander = new JCommander(arguments, args);
    }

    @Test
    public void testFailOption() {
        String[] args = {"--fail"};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertFalse(arguments.isValid());
        assertNull(arguments.getDirectory());
        assertTrue(arguments.isFail());
    }

    @Test
    public void testUnparsedParams() {
        String[] args = {"--dir", FDB_LOG_DIR, "unparsed1", "unparsed2"};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertFalse(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        List<String> unparsed = new ArrayList<>();
        unparsed.add("unparsed1");
        unparsed.add("unparsed2");
        assertEquals(arguments.getUnparsedParams(), unparsed);
    }

    @Test
    public void testDirOption() {
        String[] args = {"--dir", FDB_LOG_DIR};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertFalse(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getMatching(), arguments.ALL_FILES);
        assertNull(arguments.getProxyHost());
        assertEquals(arguments.getProxyPort(), arguments.DEFAULT_PORT);
        assertFalse(arguments.isFail());
        assertNull(arguments.getServer());
        assertNull(arguments.getToken());
        assertFalse(arguments.getHelp());
        assertNull(arguments.getUnparsedParams());
        assertFalse(arguments.isDirect());
        assertFalse(arguments.isProxy());
    }

    @Test
    public void testDOption() {
        String[] args = {"-d", FDB_LOG_DIR};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertFalse(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getMatching(), arguments.ALL_FILES);
        assertNull(arguments.getProxyHost());
        assertEquals(arguments.getProxyPort(), arguments.DEFAULT_PORT);
        assertFalse(arguments.isFail());
        assertNull(arguments.getServer());
        assertNull(arguments.getToken());
        assertFalse(arguments.getHelp());
        assertNull(arguments.getUnparsedParams());
        assertFalse(arguments.isDirect());
        assertFalse(arguments.isProxy());
    }

    @Test
    public void testProxyHost() {
        String host = "testHost";
        String[] args = {"-d", FDB_LOG_DIR, "--proxyHost", host};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertTrue(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getProxyHost(), host);
        assertFalse(arguments.isDirect());
        assertTrue(arguments.isProxy());
    }

    @Test
    public void testProxyPort() {
        Integer port = 123;
        String[] args = {"-d", FDB_LOG_DIR, "--proxyPort", port.toString()};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertFalse(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getProxyPort(), port.intValue());
        assertFalse(arguments.isDirect());
        assertFalse(arguments.isProxy());
    }

    @Test
    public void testServerOption() {
        String server = "testServer";
        String[] args = {"-d", FDB_LOG_DIR, "--server", server};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertFalse(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getServer(), server);
        assertFalse(arguments.isDirect());
        assertFalse(arguments.isProxy());
    }

    @Test
    public void testTokenOption() {
        String token = "testToken";
        String[] args = {"-d", FDB_LOG_DIR, "--token", token};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertFalse(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getToken(), token);
        assertFalse(arguments.isDirect());
        assertFalse(arguments.isProxy());
    }

    @Test
    public void testIsDirect() {
        String server = "testServer";
        String token = "testToken";
        String[] args = {"-d", FDB_LOG_DIR, "--server", server, "--token", token};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertTrue(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getServer(), server);
        assertEquals(arguments.getToken(), token);
        assertTrue(arguments.isDirect());
        assertFalse(arguments.isProxy());
    }

    @Test
    public void testIsProxy() {
        String host = "testHost";
        Integer port = 2222;
        String[] args = {"-d", FDB_LOG_DIR, "--proxyHost", host, "--proxyPort", port.toString()};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertTrue(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getProxyHost(), host);
        assertEquals(arguments.getProxyPort(), port.intValue());
        assertFalse(arguments.isDirect());
        assertTrue(arguments.isProxy());
    }

    @Test
    public void testIsProxyAndIsDirect() {
        String host = "testHost";
        Integer port = 2222;
        String server = "testServer";
        String token = "testToken";
        String[] args = {"-d", FDB_LOG_DIR, "--proxyHost", host, "--proxyPort", port.toString(), "--server", server, "--token", token};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertTrue(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getProxyHost(), host);
        assertEquals(arguments.getProxyPort(), port.intValue());
        assertTrue(arguments.isDirect());
        assertTrue(arguments.isProxy());
    }

    @Test
    public void testConfigFile() {
        String host = "someHost";
        Integer port = 2878;
        String server = "someServer";
        String token = "<<TOKEN>>";
        String[] args = {"-f", "config"};
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        assertTrue(arguments.isValid());
        assertEquals(arguments.getDirectory(), FDB_LOG_DIR);
        assertEquals(arguments.getProxyHost(), host);
        assertEquals(arguments.getProxyPort(), port.intValue());
        assertTrue(arguments.isDirect());
        assertTrue(arguments.isProxy());
    }
}