package com.wavefront.integrations;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.wavefront.dropwizard.metrics.DropwizardMetricsReporter;
import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.direct_ingestion.WavefrontDirectIngestionClient;
import com.wavefront.sdk.proxy.WavefrontProxyClient;
import org.apache.commons.io.input.Tailer;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class collects and periodically reports metrics via Wavefront Proxy, Wavefront Direct Ingestion, or GraphiteReporter.
 */
public class FDBMetricsReporter {

    private final static Logger logger = Logger.getLogger(FDBMetricsReporter.class.getCanonicalName());

    private final static String service = "fdblog";

    private final static int FILE_PARSING_PERIOD = 30;

    private final static int METRICS_REPORTING_PERIOD = 60;


    static {
        SharedMetricRegistries.setDefault("defaultFDBMetrics", new MetricRegistry());
    }

    private ScheduledReporter reporter;

    private WavefrontSender sender;

    private String directory;

    private String matching;

    private boolean fail;

    public FDBMetricsReporter(FDBMetricsReporterArguments arguments) throws UnknownHostException {
        this.directory = arguments.getDirectory();
        this.matching = arguments.getMatching();
        this.fail = arguments.isFail();

        if (arguments.getType() == FDBMetricsReporterArguments.ReporterType.PROXY) {
            initProxy(arguments.getProxyHost(), arguments.getProxyPort());
        } else if (arguments.getType() == FDBMetricsReporterArguments.ReporterType.DIRECT){
            initDirect(arguments.getServer(), arguments.getToken());
        } else if (arguments.getType() == FDBMetricsReporterArguments.ReporterType.GRAPHITE) {
            initGraphite(arguments.getGraphiteServer(), arguments.getGraphitePort());
        }
    }

    private void initDirect(String server, String token) {
          this.sender = new WavefrontDirectIngestionClient.Builder(server, token).build();

          this.reporter = DropwizardMetricsReporter.forRegistry(SharedMetricRegistries.getDefault()).
                  withSource(getHostName()).
                  withReporterPointTag("service", service).
                  withJvmMetrics().
                  build(this.sender);
    }

    private void initProxy(String proxyHostname, int proxyPort) throws UnknownHostException {
          this.sender = new WavefrontProxyClient.Builder(proxyHostname).metricsPort(proxyPort).build();

          this.reporter = DropwizardMetricsReporter.forRegistry(SharedMetricRegistries.getDefault()).
                  withSource(getHostName()).
                  withReporterPointTag("service", service).
                  withJvmMetrics().
                  build(this.sender);

    }

    private void initGraphite(String graphiteServer, int graphitePort) {
        final Graphite graphite = new Graphite(new InetSocketAddress(graphiteServer, graphitePort));

        this.reporter = GraphiteReporter.forRegistry(SharedMetricRegistries.getDefault()).
                convertRatesTo(TimeUnit.SECONDS).
                convertDurationsTo(TimeUnit.MILLISECONDS).
                filter(MetricFilter.ALL).
                build(graphite);

    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warning("Cannot get host name");
            return "UnknownHost";
        }
    }

    public void start() {
        this.reporter.start(METRICS_REPORTING_PERIOD, TimeUnit.SECONDS);


        collectMetrics();
    }

    private void collectMetrics() {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        logger.info("Preparing to schedule");

        final ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(new Runnable() {

            final Pattern pattern = Pattern.compile(matching);
            final ConcurrentSkipListMap<File, Tailer> files = new ConcurrentSkipListMap<>();
            final ExecutorService es = Executors.newCachedThreadPool();

            @Override
            public void run() {
                try {
                    logger.info("Top of run");
                    disableInactiveTailers();

                    logger.info("After the disable");

                    File[] logFiles = new File(directory).listFiles(pathname -> pattern.matcher(pathname.getName()).matches());

                    logger.info("Before the for loop");

                    for (File logFile : logFiles) {
                        if (files.containsKey(logFile) &&
                                logFile.lastModified() < System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)) {
                            disableTailer(logFile, "Disabling listener for file due to inactivity: ");
                        } else if (logFile.lastModified() > (System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)) &&
                                !files.containsKey(logFile)) {
                            createTailer(logFile);
                        }
                    }

                    logger.info("After the for loop");
                } catch (Throwable e) {
                    logger.info("CRASH FIND ME");//TODO
                    logger.info(e.getMessage());
                    logger.info(e.getCause().toString());
                    logger.info(e.getStackTrace().toString());
                }
            }

            private void disableInactiveTailers() {
                for (File logFile : files.keySet()) {
                    if (logFile.exists()) {
                        continue;
                    }
                    disableTailer(logFile, "Disabling listener for the file since it no longer exists: ");
                }
            }

            private void disableTailer(File logFile, String msg) {
                logger.info(msg + logFile);
                Tailer tailer = files.remove(logFile);
                if (tailer != null) {
                    tailer.stop();
                }
            }

            private void createTailer(File logFile) {
                logger.info("Creating new listener for file: " + logFile);
                if (!logFile.exists()) {
                    logger.warning(logFile + " not found");
                    if (fail) System.exit(1);
                    return;
                }

                if (!logFile.canRead()) {
                    logger.warning(logFile + " is not readable");
                    if (fail) System.exit(1);
                    return;
                }

                Tailer tailer = new Tailer(logFile, new FDBLogListener(), 1000, true);
                es.submit(tailer);
                if (files.putIfAbsent(logFile, tailer) != null) {
                    // The put didn't succeed, stop the tailer.
                    tailer.stop();
                }
            }
        },0, FILE_PARSING_PERIOD, TimeUnit.SECONDS);
    }
}
