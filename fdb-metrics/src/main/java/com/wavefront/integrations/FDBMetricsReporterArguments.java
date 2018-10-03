package com.wavefront.integrations;

import com.beust.jcommander.*;
import com.google.common.base.Joiner;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * This class reads and validates arguments passed to fdb-metrics jar.
 *
 * @author Hovhannes Tonakanyan (htonakanyan@vmware.com).
 */
public class FDBMetricsReporterArguments {
    private static final Logger logger = Logger.getLogger(FDBMetricsReporterArguments.class.getCanonicalName());

    public static class DirectoryValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) throws ParameterException {
            if (!new File(value).isDirectory()) {
                logger.warning(value + " is not a directory");
                throw new ParameterException("Parameter " + name + " should be the path to the FDB logs directory");
            }
        }
    }

    public static class PortValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) throws ParameterException {
            try {
                int port = Integer.parseInt(value);
                if (port <= 0) {
                    logger.warning("Not a valid value for the port: " + value);
                    throw new ParameterException("Parameter " + name + " should be a valid port number");
                }
            } catch (NumberFormatException e) {
                logger.warning("Not a numeric value for the port: " + value);
                throw new ParameterException("Parameter " + name + " should be a numeric value");
            }
        }
    }

    public static class FileConverter implements IStringConverter<File> {

        @Override
        public File convert(String value) {
            return new File(value);
        }
    }

    static final String ALL_FILES = ".*";

    static final int DEFAULT_PORT = 2878;

    @Parameter(names = {"--help", "-h"}, description = "Prints available options", help = true)
    private boolean help;

    @Parameter(names = {"--fail"}, description = "Stops the execution in case of an error")
    private boolean fail = false;

    @Parameter(description = "")
    private List<String> unparsedParams;

    @Parameter(names = {"--file", "-f"}, converter = FileConverter.class, description = "Configuration file path")
    private File configFile;

    @Parameter(names = {"--dir", "-d"}, validateWith = DirectoryValidator.class, description = "Path to the FDB logs directory")
    private String directory;

    @Parameter(names = {"--matching", "-m"}, description = "Pattern for log file names to match")
    private String matching = ALL_FILES;

    @Parameter(names = {"--proxyHost"}, description = "Proxy Host")
    private String proxyHost;

    @Parameter(names = {"--proxyPort"}, validateWith = PortValidator.class, description = "Proxy Port")
    private int proxyPort = DEFAULT_PORT;

    @Parameter(names = {"--server"}, description = "Server name for the direct ingestion")
    private String server;

    @Parameter(names = {"--token"}, description = "Wavefront API token for the direct ingestion")
    private String token;

    FDBMetricsReporterArguments() {
    }

    boolean isFail() {
        return fail;
    }

    String getDirectory() {
        return directory;
    }

    String getMatching() {
        return matching;
    }

    String getProxyHost() {
        return proxyHost;
    }

    int getProxyPort() {
        return proxyPort;
    }

    String getServer() {
        return server;
    }

    String getToken() {
        return token;
    }

    boolean isValid() {
        return isProxy() || isDirect();
    }

    void load() {
        if (configFile == null) {
            logger.info("No config file is provided");
            return;
        }

        String[] args;
        try {
            Scanner sc = new Scanner(configFile);
            List<String> lines = new ArrayList<>();
            while (sc.hasNext()) {
                lines.add(sc.next());
            }
            args = lines.toArray(new String[0]);
            JCommander jCommander = new JCommander(this, args);
        } catch (FileNotFoundException e) {
            logger.info("Config file doesn't exist");
        }
    }

    boolean isProxy() {
        return directory != null && proxyHost != null;
    }

    boolean isDirect() {
        return directory != null && server != null && token != null;
    }

    boolean getHelp() {
        return help;
    }

    List<String> getUnparsedParams() {
        return unparsedParams;
    }

    void printUnparsedParams() {
        if (unparsedParams != null) {
            logger.info("Unparsed arguments: " + Joiner.on(", ").join(unparsedParams));
        }
    }

    public static void main(String[] args) {
        logger.info("Arguments: " + Joiner.on(", ").join(args));
        FDBMetricsReporterArguments arguments = new FDBMetricsReporterArguments();
        JCommander jCommander = new JCommander(arguments, args);
        arguments.load();
        if (arguments.getHelp() || !arguments.isValid()) {
            jCommander.setProgramName(FDBMetricsReporter.class.getCanonicalName());
            jCommander.usage();
            System.exit(0);
        }
        arguments.printUnparsedParams();

        FDBMetricsReporter reporter = new FDBMetricsReporter(arguments);
        reporter.start();
    }
}
