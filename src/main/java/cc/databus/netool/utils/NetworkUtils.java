package cc.databus.netool.utils;

import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.LinkLayerAddress;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkUtils {

    public static PcapNetworkInterface getNetworkInterfance(String name) throws PcapNativeException {
        Objects.requireNonNull(name, "Interface name cannot be null.");
        return Pcaps.getDevByName(name);
    }

    public static void capturePackets(final CaptureOptions options) throws PcapNativeException, NotOpenException {
        // 1. get interface
        PcapNetworkInterface networkInterface = null;
        if (StringUtils.isNllOrEmpty(options.getInterfaceName())) {
            networkInterface = getFirstInterface();
        }
        else {
            networkInterface = getNetworkInterfance(options.getInterfaceName());
        }

        // 2. get snapLenth
        PcapHandle handle = networkInterface.openLive(options.getSnapLen(), PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, options.getTimeout());
        // 3. open dumper if needed
        PcapDumper dumper = null;
        if (!StringUtils.isNllOrEmpty(options.getFilePath())) {
            dumper = handle.dumpOpen(options.getFilePath());
        }

        // 4. set filter if needed
        if (!StringUtils.isNllOrEmpty(options.getFilter())) {
            handle.setFilter(options.getFilter(), BpfProgram.BpfCompileMode.OPTIMIZE);
        }

        // 5. prepare listener
        NetworkPacketListener listener = new NetworkPacketListener(dumper);

        ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
        int count = options.getCount() > 0 ? options.getCount() : Integer.MAX_VALUE;
        Future<Void> future = singleExecutor.submit(new CapturingTask(handle, listener, count, options.getDuration()));

        try {
            if (options.getDuration() > 0) {
                try {
                    future.get(options.getDuration(), TimeUnit.SECONDS);
                }
                catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    SystemOutHelper.println(String.format("Capturing failed with exception - %s:%s", cause.getClass().getCanonicalName(), cause.getMessage()));
                }
                catch (InterruptedException | TimeoutException e) {
                    SystemOutHelper.println("Capturing timeout or interrupted.");
                    future.cancel(true);
                }
                finally {
                    handle.breakLoop();
                }
            }
            else {
                try {
                    future.get();
                }
                catch (InterruptedException e) {
                    SystemOutHelper.println("Capturing interrupted.");
                    future.cancel(true);
                }
                catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    SystemOutHelper.println(String.format("Capturing failed with exception - %s:%s", cause.getClass().getCanonicalName(), cause.getMessage()));
                }
                finally {
                    handle.breakLoop();
                }
            }
        }
        finally {

            try {
                singleExecutor.shutdownNow();
            }
            catch (Exception ignore) {
            }

            if (dumper != null) {
                try {
                    dumper.close();
                    SystemOutHelper.println("Dumper closed!");
                }
                catch (Exception ignore) {
                }
            }

            try {
                handle.close();
                SystemOutHelper.println("PcapHandle closed!");
            }
            catch (Exception ignore) {
            }


        }
    }

    public static String listInterfaces() throws PcapNativeException {

        List<PcapNetworkInterface> inters = Pcaps.findAllDevs();
        StringBuilder stringBuilder = new StringBuilder();

        for (PcapNetworkInterface inf : inters) {
            stringBuilder.append(networkInterfaceToString(inf));
        }

        return stringBuilder.toString();
    }

    private static PcapNetworkInterface getFirstInterface() throws PcapNativeException {
        List<PcapNetworkInterface> all = Pcaps.findAllDevs();
        if (!all.isEmpty()) {
            return all.get(0);
        }

        throw new IllegalStateException("Cannot get any interface!");
    }

    private static String networkInterfaceToString(PcapNetworkInterface pcapNetworkInterface) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10sLink encap: %s", pcapNetworkInterface.getName(), pcapNetworkInterface.isLoopBack() ? "Local loopback" : "Ethernet"));
        if (!pcapNetworkInterface.getLinkLayerAddresses().isEmpty()) {
            // only get the first one
            LinkLayerAddress hwAddr = pcapNetworkInterface.getLinkLayerAddresses().get(0);
            if (hwAddr != null) {
                sb.append(String.format("  HWaddr %s", hwAddr.toString()));
            }
        }
        sb.append("\n");

        List<PcapAddress> addresses = pcapNetworkInterface.getAddresses();
        for (PcapAddress address : addresses) {
            if (address instanceof PcapIpV4Address) {
                sb.append(
                        String.format("%10sinet addr:%s%s%s\n",
                                "",
                                address.getAddress().getHostAddress(),
                                address.getBroadcastAddress() != null ? String.format(" Bcast:%s", address.getBroadcastAddress().getHostAddress()) : "",
                                address.getNetmask() != null ? String.format(" Mask:%s", address.getNetmask().getHostAddress()) : ""));
            }
            else {
                sb.append(String.format("%10sinet6 addr: %s\n", "", address.getAddress().getHostAddress()));
            }
        }

        return sb.toString();
    }

    private static class NetworkPacketListener implements PacketListener {

        private final PcapDumper dumper;
        private final AtomicLong count = new AtomicLong(0);

        private NetworkPacketListener(PcapDumper dumper) {
            this.dumper = dumper;
        }

        public long getCount() {
            return count.get();
        }

        @Override
        public void gotPacket(Packet packet) {

            count.incrementAndGet();
            boolean needPrint = true;
            if (dumper != null) {
                try {
                    dumper.dump(packet);
                    needPrint = false;
                }
                catch (NotOpenException e) {
                    // ignore
                }
            }

            if (needPrint) {
                SystemOutHelper.println(packet);
            }
        }
    }

    private static class CapturingTask implements Callable<Void> {

        private final PcapHandle handle;
        private final NetworkPacketListener listener;
        private final int count;
        private final long durationInMs;


        private CapturingTask(PcapHandle handle, NetworkPacketListener listener, int count, long durationInS) {
            this.handle = handle;
            this.listener = listener;
            this.count = count;
            this.durationInMs = durationInS * 1000;
        }


        @Override
        public Void call() throws Exception {
            long start = System.currentTimeMillis();
            while (true) {
                try {
                    SystemOutHelper.println(String.format("Start loop, count=%d, duration=%dms", count, durationInMs));
                    handle.loop(count, listener);
                }
                catch (InterruptedException e) {
                    SystemOutHelper.println("Interrupted, captured " + listener.getCount() + " packets.");
                    break;
                }

                if (count != Integer.MAX_VALUE) {
                    SystemOutHelper.println("Reach count limit, captured " + listener.getCount() + " packets.");
                    break;
                }

                if (durationInMs <= 0) {
                    break;
                }
                else if (System.currentTimeMillis() >= System.currentTimeMillis() + durationInMs) {
                    SystemOutHelper.println("Reach running time limit, captured " + listener.getCount() + " packets.");
                    break;
                }
            }

            return null;
        }
    }
}
