package cc.databus.netool.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * options for capturing network packets.
 */
public class CaptureOptions {
    /**
     * where to dump the captured packets
     */
    private String filePath = "";

    private int snapLen = 65536;

    /**
     * packet reading timeout in milliseconds
     */
    private int timeout = 10;

    /**
     * Interface names. If this is not empty, will choose the first one
     */
    private Set<String> interfaceNames = new HashSet<>();


    /**
     * max capturing duration in seconds
     */
    private long duration = 0;

    /**
     * max captured packets count
     */
    private int count = 0;
    /**
     * filter
     */
    private String filter = "";

    public String getFilePath() {
        return filePath;
    }

    public int getSnapLen() {
        return snapLen;
    }

    public Set<String> getInterfaceNames() {
        return Collections.unmodifiableSet(interfaceNames);
    }

    public long getDuration() {
        return duration;
    }

    public int getCount() {
        return count;
    }

    public String getFilter() {
        return filter;
    }

    public int getTimeout() {
        return timeout;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    private void setSnapLen(int snapLen) {
        this.snapLen = snapLen;
    }

    private void addInterfaceName(String interfaceName) {
        this.interfaceNames.add(interfaceName);
    }

    private void setDuration(long duration) {
        this.duration = duration;
    }

    private void setCount(int count) {
        this.count = count;
    }

    private void setFilter(String filter) {
        this.filter = filter;
    }

    private void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    public static class Builder {

        private CaptureOptions inner = new CaptureOptions();
        private Builder() {}

        public Builder filePath(String filePath) {
            inner.setFilePath(filePath);
            return this;
        }

        public Builder snapLength(int snapLen) {
            inner.setSnapLen(snapLen);
            return this;
        }

        public Builder interfaceNames(final String[] interfaceNames) {
            Arrays.stream(interfaceNames).forEach(inner::addInterfaceName);
            return this;
        }

        public Builder duration(long duration) {
            inner.setDuration(duration);
            return this;
        }

        public Builder count(int count) {
            inner.setCount(count);
            return this;
        }

        public Builder filter(String filter) {
            inner.setFilter(filter);
            return this;
        }

        public Builder timeout(int timeout) {
            inner.setTimeout(timeout);
            return this;
        }


        public CaptureOptions build() {
            return inner;
        }
    }


}
