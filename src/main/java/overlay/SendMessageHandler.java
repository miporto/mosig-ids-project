package overlay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import overlay.network.virtual.Message;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SendMessageHandler implements HttpHandler {

    private ConcurrentLinkedQueue<Message> messages;

    public SendMessageHandler(ConcurrentLinkedQueue<Message> messages) {
        this.messages = messages;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> jsonMap = mapper.readValue(is, new TypeReference<Map<String, String>>() {});

        for (Map.Entry<String, String> entry : jsonMap.entrySet()) {
            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            Message message = new Message(entry.getKey(), entry.getValue());
            messages.add(message);
        }
        is.close();
    }
}
