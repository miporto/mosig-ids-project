package overlay.network.physical;

import com.rabbitmq.client.DeliverCallback;
import overlay.network.NetworkInfo;
import overlay.network.virtual.Message;
import overlay.util.BreadthFirstSearch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

public class Router {
    private NetworkInfo networkInfo;
    private final int myID;
    private Map<Integer, Integer> nextHopsForDestinations;
    private ConcurrentLinkedQueue<Message> incomingMessages;
    private ConcurrentLinkedQueue<Message> outgoingMessages;
    private Driver driver;
    private DeliverCallback deliverCallback;

    public Router(NetworkInfo networkInfo, ConcurrentLinkedQueue<Message> incomingMessages,
                  ConcurrentLinkedQueue<Message> outgoingMessages) throws IOException, TimeoutException {
        this.networkInfo = networkInfo;
        myID = networkInfo.getPhysicalID();
        // TODO use real info
        int[][] adj = {{}};
        int vertices = 0;
        nextHopsForDestinations = BreadthFirstSearch.calculateNextHops(myID, adj, vertices);

        this.incomingMessages = incomingMessages;
        this.outgoingMessages = outgoingMessages;

        driver = new Driver(networkInfo.getHost(), networkInfo.getPort(), networkInfo.getExchangeName());
        deliverCallback = (consumerTag, delivery) -> {
            ByteArrayInputStream is = new ByteArrayInputStream(delivery.getBody());
            try (ObjectInputStream ois = new ObjectInputStream(is)) {
                Package pkg = (Package) ois.readObject();
                System.out.println(" [x] Received '" +
                        delivery.getEnvelope().getRoutingKey() + "':'" + pkg.getSrc() + "'");
                if (pkg.getDest() != myID) {
                    pkg.setNextHop(nextHopsForDestinations.get(pkg.getDest()));
                    driver.send(pkg);
                } else {
                    this.incomingMessages.add(pkg.getMessage());
                }
            } catch (ClassNotFoundException e) {
                // TODO log error
                e.printStackTrace();
            }
        };
    }

    private void processMessages() {
        while (!Thread.interrupted()) {
            Message msg = outgoingMessages.remove();
            int pSrc = networkInfo.translateToPhysical(msg.getSrc());
            int pDest = networkInfo.translateToPhysical(msg.getNextHop());
            int nextHop = nextHopsForDestinations.get(pDest);
            Package pkg = new Package(pSrc, pDest, nextHop, msg);
            try {
                driver.send(pkg);
            } catch (IOException e) {
                // TODO log error
                e.printStackTrace();
            }
        }
    }

    public void connect(int dest) throws IOException {
        driver.addIncomingConnection(dest, deliverCallback);
        driver.addOutgoingConnection(dest);
    }
}