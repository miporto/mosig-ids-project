package overlay.network;

import java.util.List;
import java.util.Map;

public class NetworkInfo {
    private String host;
    private int port;
    private String exchangeName;
    private int virtualID;
    private Map<Integer, Integer> tagTranslations;
    private List<List<Integer>> pTopology;
    private List<List<Integer>> vTopology;

    public NetworkInfo(String host, int port, String exchangeName, int virtualID,
                       Map<Integer, Integer> tagTranslations, List<List<Integer>> pTopology,
                       List<List<Integer>> vTopology) {
        this.host = host;
        this.port = port;
        this.exchangeName = exchangeName;
        this.virtualID = virtualID;
        this.tagTranslations = tagTranslations;
        this.pTopology = pTopology;
        this.vTopology = vTopology;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public int getVirtualID() {
        return virtualID;
    }

    public int getPhysicalID() {
        return tagTranslations.get(virtualID);
    }

    public int translateToPhysical(int id) {
        return tagTranslations.get(id);
    }

    public Map<Integer, Integer> getTagTranslations() {
        return tagTranslations;
    }

    public List<List<Integer>> getpTopology() {
        return pTopology;
    }

    public List<List<Integer>> getvTopology() {
        return vTopology;
    }
}