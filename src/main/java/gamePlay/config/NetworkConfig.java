package gamePlay.config;

public class NetworkConfig {
    public ServerConfig server;

    public static class ServerConfig {
        public String host;
        public int tcp_port;
        public int udp_port;
    }
}