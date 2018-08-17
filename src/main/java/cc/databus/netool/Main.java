package cc.databus.netool;

import cc.databus.netool.utils.CaptureOptions;
import cc.databus.netool.utils.NetworkUtils;
import cc.databus.netool.utils.SystemOutHelper;
import org.apache.commons.cli.*;
import org.pcap4j.core.PcapNativeException;

import java.io.IOException;

import static cc.databus.netool.utils.SystemOutHelper.println;

public class Main {
    public static void main(String[] args) throws ParseException, PcapNativeException, IOException {

        Options options = new Options()
                .addOption("h", false, "show usage")
                .addOption("l", false, "list all interfaces")
                .addOption("w", true, "file path to dump packets")
                .addOption("i", true, "interface name")
                .addOption("c", true, "packet counts")
                .addOption("s", true, "snap length")
                .addOption("G", true,"seconds to keep running");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.getArgs().length == 0 || cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "netool", options );
        }
        else if (cmd.hasOption("l")) {
            SystemOutHelper.init();
            println(NetworkUtils.listInterfaces());
        }
        else {
            CaptureOptions captureOptions = parseCaptureOptions(cmd);
            // TODO, handle parsed capture options.
        }
    }

    private static CaptureOptions parseCaptureOptions(final CommandLine cmd) {
        CaptureOptions.Builder builder = CaptureOptions.newBuilder();
        if (cmd.hasOption("w")) {
            builder.filePath(cmd.getOptionValue("w"));
        }
        if (cmd.hasOption("i")) {
            builder.interfaceName(cmd.getOptionValue("i"));
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

        return builder.build();
    }
}
