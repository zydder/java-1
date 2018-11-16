package com.wavefront.integrations;

import com.beust.jcommander.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Joiner;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class FDBMetricsReporterInit {

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

    public static class FileConverter implements IStringConverter<File> {

        @Override
        public File convert(String value) {
            return new File(value);
        }
    }

    @Parameter(names = {"--file", "-f"}, converter = FileConverter.class, description = "Configuration file path")
    private File configFile;

    @Parameter(description = "")
    private List<String> unparsedParams;

    @Parameter(names = {"--help", "-h"}, description = "Prints available options", help = true)
    private boolean help;

    public FDBMetricsReporterArguments arguments;


    public void load() {

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

    public static void main(String[] args) throws UnknownHostException {
        logger.info("Arguments: " + Joiner.on(", ").join(args));
        FDBMetricsReporterInit init = new FDBMetricsReporterInit();
        init.arguments = new FDBMetricsReporterArguments();
        JCommander jCommander = new JCommander(init, args);
        init.load();
        if (init.getHelp()) {
            jCommander.setProgramName(FDBMetricsReporter.class.getCanonicalName());
            jCommander.usage();
            System.exit(0);
        }
        init.printUnparsedParams();

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            init.arguments = mapper.readValue(init.configFile, FDBMetricsReporterArguments.class);
        } catch (Exception e) {
            logger.info("Failed to parse yaml config file: " + e.getMessage());
            System.exit(-1);
        }

        FDBMetricsReporter reporter = new FDBMetricsReporter(init.arguments);
        reporter.start();

        Semaphore semaphore = new Semaphore(0);
        semaphore.acquireUninterruptibly();

        logger.info("CRASH THIS SHOULD NEVER BE HIT");
    }
}
