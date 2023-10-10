package Models;

import Main.Logger;
import Main.MyPair;
import Main.Task;

import java.util.ArrayList;
import java.util.Arrays;

public class Graph {

    int resourcesCount, taskCount;
    ArrayList<Integer>[] adjacent;

    public Graph(int resourcesCount, int taskCount) {
        this.resourcesCount = resourcesCount;
        this.taskCount = taskCount;
        this.adjacent = new ArrayList[resourcesCount + taskCount];

        for (int i = 0; i < adjacent.length; i++) {
            adjacent[i] = new ArrayList<>();
        }
    }

    public void addClaimEdge(int resource, int task) {
//        adjacent[task].add(resource + taskCount);

        adjacent[taskCount + resource].add(task);
    }

    public void addRequestEdge(int task, int resource) {
        if (! adjacent[task].contains(resource + taskCount)){
            adjacent[task].add(resource + taskCount);
        }
//        adjacent[taskCount + resource].add(task);
    }

    public boolean canBeAllocated(int task , Task a) {
        boolean b = hasCycle(task);
        boolean b1 = SBHasResource(a);
        if (b){
            Logger.log("Deadlock found with Graph. it has cycle.");
        }
        if (b1){
            Logger.log("Deadlock found with Graph. Other tasks owns lock for resources.");
        }
        return !(b || b1);
//        return hasCycle(task);
    }

    private boolean SBHasResource(Task a) {
        boolean flag = false;
        for (MyPair subtask : a.getSubtasks()) {
            flag = flag || (adjacent[subtask.getIndex()+taskCount].size() != 0 && adjacent[subtask.getIndex() + taskCount].get(0) != a.getId());

        }
        return flag;
    }


    public void flipEdge(int task, int resource) {
        boolean remove = adjacent[task].remove(Integer.valueOf(resource + taskCount));
//        adjacent[task].remove((Integer) resource+taskCount);
        if (remove) {
            adjacent[resource + taskCount].add(task);
        }
    }

    public void removeEdges(int task){
        for (int i = taskCount ; i < taskCount + resourcesCount ; i++) {
            if (adjacent[i].contains(task)){
                adjacent[i].remove(Integer.valueOf(task));
            }
        }
    }


    public boolean dfs(int i, boolean[] visited, int target) {
        boolean flag = false;
        visited[i] = true;
        for (Integer integer : adjacent[i]) {
            if (!visited[integer]) {
                flag = flag || dfs(integer , visited,  target);
            } else if (integer == target) {
                return true;
            }
        }
        return flag;
    }


    public boolean hasCycle(int index) {
        boolean[] visited = new boolean[taskCount + resourcesCount];
        return dfs(index, visited, index);
    }

    @Override
    public String toString() {
        return "Graph{" +
                "resourcesCount=" + resourcesCount +
                ", taskCount=" + taskCount +
                ", tasks= " + Arrays.toString(Arrays.copyOfRange(adjacent , 0  , taskCount)) +
                ", resources= " + Arrays.toString(Arrays.copyOfRange(adjacent , taskCount , adjacent.length))+
//                ", adjacent=" + Arrays.toString(adjacent) +
                '}';
    }
}

