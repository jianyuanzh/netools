package cc.databus.netool.utils;

import org.pcap4j.core.*;
import org.pcap4j.util.LinkLayerAddress;

import java.util.List;
import java.util.Objects;

public class NetworkUtils {

    public static PcapNetworkInterface getNetworkInterfance(String name) throws PcapNativeException {
        Objects.requireNonNull(name, "Interface name cannot be null.");
        return Pcaps.getDevByName(name);
    }

    public static String listInterfaces() throws PcapNativeException {

        List<PcapNetworkInterface> inters = Pcaps.findAllDevs();
        StringBuilder stringBuilder = new StringBuilder();

        for (PcapNetworkInterface inf : inters) {
            stringBuilder.append(networkInterfaceToString(inf));
        }

        return stringBuilder.toString();
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
}
