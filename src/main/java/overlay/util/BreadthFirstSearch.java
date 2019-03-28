package overlay.util;

import java.util.*;
import java.util.stream.Collectors;

public class BreadthFirstSearch {

    public static List<Integer> getShortestDistance(int[][] adj, int vertices, int src, int dest) {
        List<Integer> prediction = new ArrayList<>(vertices);
        Queue<Integer> queue = new LinkedList<>();
        List<Boolean> visited = new ArrayList<>(vertices);

        for (int i = 0; i < vertices; i++) {
            visited.add(false);
            prediction.add(-1);
        }

        visited.set(src, true);
        queue.add(src);

        while (!queue.isEmpty()) {
            int u = queue.remove();
            for (int i = 0; i < vertices; i++) {
                if (adj[u][i] == 1 && !visited.get(i)) {
                    visited.set(i, true);
                    prediction.set(i, u);
                    queue.add(i);
                    if (i == dest) {
                        // This may be incorrect
                        prediction.add(i);
                        queue.clear();
                        break;
                    }
                }
            }
        }
        return prediction.stream().filter(i -> i != -1).collect(Collectors.toList());
    }
}
