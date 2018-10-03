package com.wavefront.integrations;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.apache.commons.io.input.Tailer;

import com.wavefront.integrations.metrics.WavefrontReporter;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class collects and periodically reports metrics to Wavefront Proxy or can perform Direct Ingestion.
 */
public class FDBMetricsReporter {

    private final static Logger logger = Logger.getLogger(FDBMetricsReporter.class.getCanonicalName());

    private final static String service = "fdblog";

    private final static int FILE_PARSING_PERIOD = 30000;

    private final static int METRICS_REPORTING_PERIOD = 60;

    static {
        SharedMetricRegistries.setDefault("defaultFDBMetrics", new MetricRegistry());
    }

    private WavefrontReporter reporter;

    private String directory;

    private String matching;

    private boolean fail;

    public FDBMetricsReporter(FDBMetricsReporterArguments arguments) {
        this.directory = arguments.getDirectory();
        this.matching = arguments.getMatching();
        this.fail = arguments.isFail();

        if (arguments.isProxy()) {
            init(arguments.getProxyHost(), arguments.getProxyPort());
        } else {
            init(arguments.getServer(), arguments.getToken());
        }
    }

    private void init(String server, String token) {
        this.reporter = WavefrontReporter.forRegistry(SharedMetricRegistries.getDefault()).
                withSource(getHostName()).
                withPointTag("service", service).
                buildDirect(server, token);
    }

    private void init(String proxyHostname, int proxyPort) {
        this.reporter = WavefrontReporter.forRegistry(SharedMetricRegistries.getDefault()).
                withSource(getHostName()).
                withPointTag("service", service).
                build(proxyHostname, proxyPort);
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
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            final Pattern pattern = Pattern.compile(matching);

            final ConcurrentSkipListMap<File, Tailer> files = new ConcurrentSkipListMap<>();

            final ExecutorService es = Executors.newCachedThreadPool();

            @Override
            public void run() {
                disableInactiveTailers();

                File[] logFiles = new File(directory).listFiles(pathname -> pattern.matcher(pathname.getName()).matches());

                for (File logFile : logFiles) {
                    if (files.containsKey(logFile) &&
                            logFile.lastModified() < System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)) {
                        disableTailer(logFile, "Disabling listener for file due to inactivity: ");
                    } else if (logFile.lastModified() > System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1) &&
                            !files.containsKey(logFile)) {
                        createTailer(logFile);
                    }
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
                    // the put didn't succeed. stop the tailer.
                    tailer.stop();
                }
            }
        }, 0, FILE_PARSING_PERIOD);
    }
}
