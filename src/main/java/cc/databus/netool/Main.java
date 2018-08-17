package cc.databus.netool;

import cc.databus.netool.utils.NetworkUtils;
import cc.databus.netool.utils.SystemOutHelper;
import org.apache.commons.cli.*;
import org.pcap4j.core.PcapNativeException;

import java.io.IOException;

import static cc.databus.netool.utils.SystemOutHelper.println;

public class Main {
    public static void main(String[] args) throws ParseException, PcapNativeException, IOException {

        Options options = new Options()
                .addOption("l", false, "list all interfaces");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("l")) {
            SystemOutHelper.init();
            println(NetworkUtils.listInterfaces());;
        }
        else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "netool", options );
        }
    }
}
