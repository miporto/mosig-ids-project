package overlay.network.physical;

import com.rabbitmq.client.DeliverCallback;
import overlay.network.NetworkInfo;
import overlay.network.virtual.Message;
import overlay.util.BreadthFirstSearch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class Router implements Runnable {
    private NetworkInfo networkInfo;
    private final int myID;
    private Map<Integer, Integer> nextHopsForDestinations;
    private BlockingQueue<Message> incomingMessages;
    private BlockingQueue<Message> outgoingMessages;
    private Driver driver;
    private DeliverCallback deliverCallback;

    public Router(NetworkInfo networkInfo, BlockingQueue<Message> incomingMessages,
                  BlockingQueue<Message> outgoingMessages) throws IOException, TimeoutException {
        this.networkInfo = networkInfo;
        myID = networkInfo.getPhysicalID();
        nextHopsForDestinations = BreadthFirstSearch.calculateNextHops(myID, networkInfo.getpTopology());

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
                    this.incomingMessages.put(pkg.getMessage());
                }
            } catch (ClassNotFoundException | InterruptedException e) {
                // TODO log error
                e.printStackTrace();
            }
        };

        setConnections();
    }

    private void setConnections() throws IOException {
        List<Integer> adjList = networkInfo.getpTopology().get(myID);
        for (int i = 0; i < adjList.size(); i++) {
            if (adjList.get(i) == 1) connect(i);
        }
    }

    private void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            Message msg;
            try {
                msg = outgoingMessages.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                break;
            }
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

    private void connect(int dest) throws IOException {
        driver.addIncomingConnection(dest, deliverCallback);
        driver.addOutgoingConnection(dest);
    }

    @Override
    public void run() {
        processMessages();
        driver.stop();
        System.out.println("Finishing");
    }
}