package com.wavefront.integrations;

/**
 * This class holds the configuration parameters for the fdb-metrics jar.
 */
public class FDBMetricsReporterArguments {


    enum ReporterType
    {
        DIRECT, PROXY, GRAPHITE;
    }

    static final String ALL_FILES = ".*";

    static final String DEFAULT_HOST = "localhost";

    static final int DEFAULT_PORT = 2878;

    private boolean fail = false;

    private String directory;

    private String matching = ALL_FILES;

    private String proxyHost = DEFAULT_HOST;

    private int proxyPort = DEFAULT_PORT;

    private String server;

    private String token;

    private String graphiteServer;

    private int graphitePort;

    private ReporterType type;

    public void setFail(boolean fail) {
        this.fail = fail;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setMatching(String matching) {
        this.matching = matching;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setGraphiteServer(String graphiteServer) {
        this.graphiteServer = graphiteServer;
    }

    public void setGraphitePort(int graphitePort) {
        this.graphitePort = graphitePort;
    }

    public void setType(ReporterType type) {
        this.type = type;
    }


    public boolean isFail() {
        return fail;
    }

    public String getDirectory() {
        return directory;
    }

    public String getMatching() {
        return matching;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() { return proxyPort; }

    public String getServer() {
        return server;
    }

    public String getToken() {
        return token;
    }

    public String getGraphiteServer() {
        return graphiteServer;
    }

    public int getGraphitePort() {
        return graphitePort;
    }

    public ReporterType getType() {
        return type;
    }
}
