package cc.databus.netool.utils;

import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.util.LinkLayerAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;

public class NetworkUtils {

    public static String getInterfaceByRemoteHost(String hostname) throws IOException, PcapNativeException {
        Objects.requireNonNull(hostname, "Should not provide null hostname");
        PcapNetworkInterface pnIf = Pcaps.getDevByAddress(InetAddress.getByName(hostname));
        if (pnIf == null) {
            throw new IOException("Fail to get interface with provided hostname - " + hostname);
        }
        return networkInterfaceToString(pnIf);
    }

    public static String listInterfaces() throws PcapNativeException {

        List<PcapNetworkInterface> inters = Pcaps.findAllDevs();
        StringBuilder stringBuilder = new StringBuilder();

        for (PcapNetworkInterface inf : inters) {
            stringBuilder.append(networkInterfaceToString(inf));
        }

        return stringBuilder.toString();
    }

    static String networkInterfaceToString(PcapNetworkInterface pcapNetworkInterface) {
        StringBuilder sb = new StringBuilder();
        sb.append(pcapNetworkInterface.getName()).append(": ").append(pcapNetworkInterface.getDescription()).append("\n");

        List<PcapAddress> addresses = pcapNetworkInterface.getAddresses();
        sb.append("\t").append(addresses.size()).append(" address(es):").append("\n");
        for (int i = 0; i < addresses.size();  i++) {
            PcapAddress address = addresses.get(i);
            sb.append("\t -address    :").append(address.getAddress()).append("\n")
                    .append("\t    broadcast  :").append(address.getBroadcastAddress()).append(" ")
                    .append("\t destination:").append(address.getDestinationAddress()).append(" ")
                    .append("\t netmask    :").append(address.getNetmask()).append("\n");
        }
        List<LinkLayerAddress> linkLayerAddresses = pcapNetworkInterface.getLinkLayerAddresses();
        sb.append("\t").append(linkLayerAddresses.size()).append(" link layer address(es):").append("\n");
        for (int i = 0; i < linkLayerAddresses.size(); i++) {
            sb.append("\t ").append(linkLayerAddresses.get(i)).append(" ");
        }

        return sb.toString();
    }


    public static void main(String[] args) {
        try {
//            System.out.println(NetworkUtils.listInterfaces());
            System.out.println(NetworkUtils.getInterfaceByRemoteHost("www.baidu.com"));
        }
        catch (PcapNativeException e) {
            e.printStackTrace();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
