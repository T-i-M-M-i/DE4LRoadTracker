package io.timmi.de4lroadtracker.model;

public class ProcessedResult {
    public String resultJSON;
    public double distance;

    public ProcessedResult(String _res, double _dist) {
        resultJSON = _res;
        distance = _dist;
    }
}
