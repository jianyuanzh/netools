# netools
A tool based on Pcap4j to implement the network packet capturing. 

# Why create my own wheel?
Troubleshooting issues in customer side is one of my daily work and many issues need capturing the network packets, 
eg: troubleshoot for SSL handshake issues or SNMP packets loss issues.A well-known command line capturing tool in linux 
is `tcpdump` and most distributions have installed it by default. In windows,
we can use wireshark's from GUI or capture the packets by netsh tools. But, not all customer's linux OS have installed 
the tcpdump and netsh is very heavy for us. This is the motivation for inventing my own wheel for capturing packets.

# Additional libpcap
For linux and mac, use "org.pcap4j.core.pcapLibName" to specify the full path of the `.so` or `.dylib` files.

If your system is not installed with libpcap, you can have it installed or build from source code. Please notice compiled `libpcap.so` files cannot work cross systems and my compiled `libpcap.so.1.8.1` in ubuntu cannot work in centos. So make sure the so file is built and used in the same system.


