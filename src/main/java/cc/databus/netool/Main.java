package cc.databus.netool;

import cc.databus.netool.utils.CaptureOptions;
import cc.databus.netool.utils.NetworkUtils;
import cc.databus.netool.utils.SystemOutHelper;
import org.apache.commons.cli.*;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;

import java.io.IOException;

import static cc.databus.netool.utils.SystemOutHelper.println;

public class Main {

    public static void main(String[] args) throws ParseException, PcapNativeException, IOException, NotOpenException {

        Options options = new Options()
                .addOption("h", false, "show usage")
                .addOption("S", "silently", true, "only show stdout of netool")
                .addOption("l", false, "list all interfaces")
                .addOption("w", true, "file path to dump packets")
                .addOption("f", true, "filter")
                .addOption("i", true, "interface name")
                .addOption("c", true, "packet counts")
                .addOption("s", true, "snap length")
                .addOption("t", true, "packet reading timeout in milliseconds")
                .addOption("G", true,"seconds to keep running")
                .addOption("v", false, "show version information");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (args.length == 0 || cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "netools [optinos]", options );
            return;
        }
        boolean silient = Boolean.parseBoolean(cmd.getOptionValue("S", "true"));
        if (silient) {
            SystemOutHelper.redirectSystemOut();
        }
        if (cmd.hasOption("v")) {
            String libpcapVersion = Pcaps.libVersion();
            println(libpcapVersion);
        }
        else if (cmd.hasOption("l")) {
            println(NetworkUtils.listInterfaces());
        }
        else {
            CaptureOptions captureOptions = parseCaptureOptions(cmd);
            NetworkUtils.capturePackets(captureOptions);
        }
    }

    private static CaptureOptions parseCaptureOptions(final CommandLine cmd) {
        CaptureOptions.Builder builder = CaptureOptions.newBuilder();
        if (cmd.hasOption("w")) {
            builder.filePath(cmd.getOptionValue("w"));
        }
        if (cmd.hasOption("i")) {
            builder.interfaceNames(cmd.getOptionValues("i"));
        }
        if (cmd.hasOption("c")) {
            builder.count(Integer.parseInt(cmd.getOptionValue("c", "0")));
        }

        if (cmd.hasOption("s")) {
            builder.snapLength(Integer.parseInt(cmd.getOptionValue("s", "65536")));
        }

        if (cmd.hasOption("G")) {
            builder.duration(Integer.parseInt(cmd.getOptionValue("G", "0")));
        }

        if (cmd.hasOption("t")) {
            builder.timeout(Integer.parseInt(cmd.getOptionValue("t", "10")));
        }

        if (cmd.hasOption("f")) {
            builder.filter(cmd.getOptionValue("f", ""));
        }

        return builder.build();
    }
}
