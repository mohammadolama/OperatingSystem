package Deadlock;

import Main.Logger;
import Main.MyPair;
import Main.Task;
import Models.Graph;

public class DeadlockDetection implements DeadlockManager {

    Graph graph;

    public DeadlockDetection(Graph graph) {
        this.graph = graph;
    }

    @Override
    public boolean canBeAllocated(int taskID, Task a) {

        synchronized (graph) {
            boolean b = graph.canBeAllocated(taskID , a);
            Logger.log("Task " + taskID + " deadlock status= " + b);
            if (b){
                for (MyPair subtask : a.getSubtasks()) {
                    graph.flipEdge(taskID , subtask.getIndex());
                }
                Logger.log(graph.toString());
            }
            return b;
        }
    }
}
