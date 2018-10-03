# FoundationDB Metrics

The fdb-metrics is a Java application that parses FoundationDB logs into metrics and sends them to Wavefront via [Wavefront Proxy](https://docs.wavefront.com/proxies.html) or direct ingestion.

## Usage

### Sending metrics via proxy

You need to provide the proxy address, the proxy port (if not provided by default it will send to 2878), the directory where the FoundationDB logs are stored and a pattern to match the files in the directory (if not provided by default it'll parse all files) to the application.
Example of the options to provide for sending metrics to a Wavefront Proxy running on the local machine.

```
    --proxyHost 127.0.0.1
    --dir "/usr/local/foundationdb/logs"
    --matching ".*\.xml$"
```

### Sending metrics via direct ingestion

You need to provide the server address and the [Wavefront API token](https://docs.wavefront.com/wavefront_api.html#generating-an-api-token)
Example of the options to provide for direct ingestion, when the server is running on the local machine.

```
    --server http://localhost:8080
    --token <wavefront_api_token>
    --dir "/usr/local/foundationdb/logs"
    --matching ".*\.xml$"
```

### Configuring application using config file

Options also can be provided to the application through a configuration file.
You can take a set of options from one of the examples above, put it into a file and give the file path to the application

```
    --file <file_path>
```

