## Build

```bash
lein uberjar
```

After rebuild, in android-studio it might be necessary to „Reload Grade Project“ to see new symbols.

## Develop and Test

In `./visualize` is a test project, that reads test datasets from files, applies the filter and stores the classified data into elasticsearch.
This allows examination of the results in kibana.

```bash
cd visualize
lein run sensor_data*_locations.json
```

## Configuration

Configuration is managed by [yogthos](https://github.com/yogthos/config). You find the available options in `./src/config.edn`./ 
