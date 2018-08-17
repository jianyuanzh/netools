package cc.databus.netool.utils;

import java.io.File;

public class CaptureOptions {
    private String filePath = "." + File.separator + "netools-dump.dump";
    private int snapLen = 65536;
    private String interfaceName = "";
    private int duration = 0;
    private long count = 0;
    private String filter = "";

    public String getFilePath() {
        return filePath;
    }

    public int getSnapLen() {
        return snapLen;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public int getDuration() {
        return duration;
    }

    public long getCount() {
        return count;
    }

    public String getFilter() {
        return filter;
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

    private void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    private void setDuration(int duration) {
        this.duration = duration;
    }

    private void setCount(long count) {
        this.count = count;
    }

    private void setFilter(String filter) {
        this.filter = filter;
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

        public Builder interfaceName(String interfaceName) {
            inner.setInterfaceName(interfaceName);
            return this;
        }

        public Builder duration(int duration) {
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

        public CaptureOptions build() {
            return inner;
        }
    }


}
