package cc.databus.netool.utils;

import java.io.*;

public class SystemOutHelper {

    private static final PrintStream originSystemOut = System.out;
    private static boolean initialized = false;

    public static synchronized void redirectSystemOut() {
        if (!initialized) {
            System.setOut(new DummyPrintStream());
        }
    }

    public static PrintStream getOriginSystemOut() {
        return originSystemOut;
    }

    private SystemOutHelper() {
    }

    public static void println(Object l) {
        originSystemOut.println(l);
    }

    private static class DummyOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }

    private static class DummyPrintStream extends PrintStream {

        public DummyPrintStream() {
            super(new DummyOutputStream());
        }
    }
}
