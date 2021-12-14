package io.timmi.de4lroadtracker.helper;

import android.os.SystemClock;


public class EventTime {

    /**
     * Calculates the static offset (ms) which needs to
     * be added to the `event.time` (ns) to get the Unix
     * timestamp of the event.
     *
     * Credits: https://stackoverflow.com/questions/5500765/accelerometer-sensorevent-timestamp
     *
     * @param eventTimeNanos the {@code SensorEvent.time} to be used to determine the time offset
     * @return the offset in milliseconds
     */
    public static long eventTimeOffset(final long eventTimeNanos) {
        // Capture timestamps of event reporting time
        final long elapsedRealTimeMillis = SystemClock.elapsedRealtime();
        final long upTimeMillis = SystemClock.uptimeMillis();
        final long currentTimeMillis = System.currentTimeMillis();

        // Check which timestamp the event.time is closest to the event.time
        final long eventTimeMillis = eventTimeNanos / 1_000_000L;
        final long elapsedTimeDiff = elapsedRealTimeMillis - eventTimeMillis;
        final long upTimeDiff = upTimeMillis - eventTimeMillis;
        final long currentTimeDiff = currentTimeMillis - eventTimeMillis;

        // Default case (elapsedRealTime, following the documentation)
        if (Math.abs(elapsedTimeDiff) <= Math.min(Math.abs(upTimeDiff), Math.abs(currentTimeDiff))) {
            final long bootTimeMillis = currentTimeMillis - elapsedRealTimeMillis;
            return bootTimeMillis;
        }

        // Other seen case (currentTime, e.g. Nexus 4)
        if (Math.abs(currentTimeDiff) <= Math.abs(upTimeDiff)) {
            return 0;
        }

        // Possible case, but unknown if actually used by manufacturers (upTime)
        throw new IllegalStateException("The event.time seems to be upTime. In this case we cannot use a static offset to calculate the Unix timestamp of the event");
    }

    public static long eventTimeNanoToUnixTimeMS(final long eventTimeNanos,final long offsetInMS) {
        return eventTimeNanos / 1_000_000L + offsetInMS;
    }

}
