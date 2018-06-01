package cc.databus.netool;

import cc.databus.netool.utils.NetworkUtils;
import org.apache.commons.cli.*;
import org.pcap4j.core.PcapNativeException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws ParseException, PcapNativeException, IOException {

        Options ops = new Options()
                .addOption("si", "list network interfaces")
                .addOption("ci",true, "choose the interface by given host");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmdline = parser.parse(ops, args);

        if (cmdline.hasOption("si")) {
            System.out.println(NetworkUtils.listInterfaces());
        }
        else if (cmdline.hasOption("ci")) {
            String hostname = cmdline.getOptionValue("ci");
            System.out.println(NetworkUtils.getInterfaceByRemoteHost(hostname));
        }
        else {
            System.err.println("unknown branches");
        }
    }
}
