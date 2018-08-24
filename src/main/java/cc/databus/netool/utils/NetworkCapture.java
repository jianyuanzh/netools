package cc.databus.netool.utils;

import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static cc.databus.netool.utils.NetworkUtils.getFirstInterface;
import static cc.databus.netool.utils.NetworkUtils.getNetworkInterfance;

public class NetworkCapture {

    private final CaptureOptions options;

    public NetworkCapture(CaptureOptions options) {
        this.options = options;
    }


    public static void startCapturing(CaptureOptions options) throws PcapNativeException, NotOpenException {
        new NetworkCapture(options).run();
    }

    private void run() throws PcapNativeException, NotOpenException {
        // 1. get interface
        List<PcapNetworkInterface> interfaces = new ArrayList<>();
        if (options.getInterfaceNames().isEmpty()) {
            interfaces = Collections.singletonList(getFirstInterface());
        }
        else {
            for (String name : options.getInterfaceNames()) {
                interfaces.add(getNetworkInterfance(name));
            }
        }

        if (interfaces.isEmpty()) {
            throw new IllegalStateException("No interface selected.");
        }

        // 2. get Pcap handles
        Map<String, PcapHandle> handles = new HashMap<>();
        for (PcapNetworkInterface networkInterface : interfaces) {
            handles.put(networkInterface.getName(),
                    networkInterface.openLive(options.getSnapLen(), PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, options.getTimeout()));
        }

        //3. open dumpers
        Map<String, PcapDumper> dumpers = new HashMap<>();
        if (!StringUtils.isNullOrEmpty(options.getFilePath())) {
            File pathFile = new File(options.getFilePath());
            String path = pathFile.getParent();
            String filename = pathFile.getName();
            String suffix = ".pcap";
            if (filename.toLowerCase().endsWith(suffix)) {
                filename = filename.substring(0, (filename.length() - suffix.length()));
            }

            for (Map.Entry<String, PcapHandle> entry : handles.entrySet()) {
                dumpers.put(entry.getKey(),
                        entry.getValue().dumpOpen(new File(path, String.format("%s_%s%s", filename, entry.getKey(), suffix)).getPath()));
            }
        }

        // 4. set filter if needed
        if (!StringUtils.isNullOrEmpty(options.getFilter())) {
            for (PcapHandle handle : handles.values()) {
                handle.setFilter(options.getFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);
            }
        }

        // 5. prepare listener
        Map<String, NetworkPacketListener> listeners = new HashMap<>();
        ExecutorService dumperExecutor = Executors.newSingleThreadExecutor();

        CountDownLatch sharedCount = new CountDownLatch(0);
        if (options.getCount() > 0) {
           sharedCount = new CountDownLatch(options.getCount());
        }
        for (String interfaceName : handles.keySet()) {
            listeners.put(interfaceName, new NetworkPacketListener(handles.get(interfaceName), dumpers.get(interfaceName), dumperExecutor, sharedCount));
        }

        // 6. start tasks.
        ExecutorService taskExecutor = Executors.newFixedThreadPool(handles.size());
        Map<String, CapturingTask> tasks = new HashMap<>();
        for (String interfaceName : handles.keySet()) {
            CapturingTask task = new CapturingTask(interfaceName, handles.get(interfaceName), listeners.get(interfaceName), options.getCount(), options.getDuration());
            tasks.put(interfaceName, task);
        }
        try {
            execute(taskExecutor, tasks, sharedCount);
        }
        finally {
            try {
                taskExecutor.shutdownNow();
            }
            catch (Exception e) {}


            for (String interName : handles.keySet()) {
                PcapHandle handle = handles.get(interName);
                if (handle != null) {
                    try {
                        handle.breakLoop();
                    }
                    catch (Exception ignore) {
                    }
                }
            }

            try {
                List<Runnable> runnables = dumperExecutor.shutdownNow();
                SystemOutHelper.println("Still " +  runnables.size() + " packets not dumped!");
                for (Runnable runnable : runnables) {
                    runnable.run();
                }
            }
            catch (Exception e){
            }

        }
    }

    private void execute(final ExecutorService taskExecutor, final Map<String, CapturingTask> capturingTasks, final CountDownLatch sharedCount) {
        Map<String, Future<Void>> futures = new HashMap<>();

        long startedAt = System.currentTimeMillis();
        for (String interfacename : capturingTasks.keySet()) {
            futures.put(interfacename, taskExecutor.submit(capturingTasks.get(interfacename)));
        }

        while (true) {
            boolean hasAnyDone = false;
            for (Map.Entry<String, Future<Void>> entry : futures.entrySet()) {
                if (entry.getValue().isDone()) {
                    hasAnyDone = true;
                }
            }

            if (hasAnyDone) {
                SystemOutHelper.println("Some tasks already finished, stop all.");
                stopRun(futures);
                break;
            }

            if (options.getCount() > 0 && sharedCount.getCount() <= 0) {
                SystemOutHelper.println(String.format("Already captured %d packets, stop all tasks", options.getCount()));
                stopRun(futures);
                break;
            }

            long durationInMs = options.getDuration() * 1000;
            if (durationInMs > 0 && (System.currentTimeMillis() - startedAt) > durationInMs) {
                SystemOutHelper.println("Capturing exceeds given timeout, stop all tasks.");
                stopRun(futures);
                break;
            }

            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                SystemOutHelper.println("Interrupted.");
                stopRun(futures);
                break;
            }
        }
    }

    private void stopRun(Map<String, Future<Void>> taskFutures) {
        for (Map.Entry<String, Future<Void>> entry : taskFutures.entrySet()) {
            String interName = entry.getKey();
            Future<Void> future = entry.getValue();
            if (future.isDone()) {
                try {
                    future.get();
                    SystemOutHelper.println(String.format("- Capturing on [%s] has finished!", interName));
                }
                catch (Exception e) {
                    SystemOutHelper.println(String.format("- Capturing on [%s] failed with exception - %s:%s", interName, e.getClass().getCanonicalName(), e.getMessage()));
                }
            }
            else {
                SystemOutHelper.println(String.format("- Capturing on [%s] has not finished, stop it.", interName));
                entry.getValue().cancel(true);
            }
        }
    }

    private static class NetworkPacketListener implements PacketListener {

        private final PcapHandle handle;
        private final PcapDumper dumper;
        private final ExecutorService executor;

        private final CountDownLatch sharedCount;

        private final AtomicLong count = new AtomicLong(0);

        private NetworkPacketListener(PcapHandle handle, PcapDumper dumper, ExecutorService executor, CountDownLatch sharedCount) {
            this.handle = handle;
            this.dumper = dumper;
            this.executor = executor;
            this.sharedCount = sharedCount;
        }

        @Override
        public void gotPacket(Packet packet) {
            count.incrementAndGet();
            sharedCount.countDown();

            DumpPacketTask dumpPacketTask = new DumpPacketTask(dumper, packet, handle.getTimestamp());
            if (executor == null) {
                dumpPacketTask.run();
            }
            else {
                executor.submit(dumpPacketTask);
            }
        }

        public long getCount() {
            return count.get();
        }
    }



    private static class DumpPacketTask implements Runnable {

        private final PcapDumper dumper;
        private final Packet packet;
        private final Timestamp timestamp;

        private DumpPacketTask(PcapDumper dumper, Packet packet, Timestamp timestamp) {
            this.dumper = dumper;
            this.packet = packet;
            this.timestamp = timestamp;
        }


        @Override
        public void run() {
            try {
                if (dumper != null) {
                    dumper.dump(packet, timestamp);
                }
                else {
                    // print to std io
                    SystemOutHelper.println(packet);
                }
            }
            catch (NotOpenException e) {
                //ignored
            }
        }
    }


    private static class CapturingTask implements Callable<Void> {

        private final String interfaceName;
        private final PcapHandle handle;
        private final NetworkPacketListener listener;
        private final int count;
        private final long durationInMs;


        private CapturingTask(String interfaceName, PcapHandle handle, NetworkPacketListener listener, int count, long durationInS) {
            this.interfaceName = interfaceName;
            this.handle = handle;
            this.listener = listener;
            this.count = count == 0? Integer.MAX_VALUE : count;
            this.durationInMs = durationInS * 1000;
        }


        @Override
        public Void call() throws Exception {
            while (true) {
                try {
                    SystemOutHelper.println(String.format("Start loop for [%s], count=%d, duration=%dms", interfaceName, count, durationInMs));
                    handle.loop(count, listener);
                }
                catch (InterruptedException e) {
                    SystemOutHelper.println(String.format("Capturing [%s] interrupted, captured %d packets.", interfaceName, listener.getCount()));
                    break;
                }

                if (count != Integer.MAX_VALUE) {
                    SystemOutHelper.println(String.format("Capturing [%s] reach count limit, captured %d packets.", interfaceName, listener.getCount()));
                    break;
                }

                if (durationInMs <= 0) {
                    break;
                }
                else if (System.currentTimeMillis() >= System.currentTimeMillis() + durationInMs) {
                    SystemOutHelper.println(String.format("Capturing [%s] reach running time limit, captured %d packets.", interfaceName, listener.getCount()));
                    break;
                }
            }

            return null;
        }
    }

}
