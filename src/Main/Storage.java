package Main;

import Deadlock.DeadlockDetection;
import Deadlock.DeadlockManager;
import Deadlock.DeadlockPreventation;
import Deadlock.NoneDetection;
import Models.Graph;
import Models.Message;
import serilize.Deserializer;
import serilize.Serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class Storage {


    ArrayList<Integer> datas = new ArrayList<>();
    HashMap<Integer, Integer> whoHasTheLock = new HashMap<>();
    ArrayList<Queue<Integer>> queues = new ArrayList<>();
    ArrayList<Task> tasks = new ArrayList<>();
    String deadlockMode;
    DeadlockManager deadlockManager;

    Graph graph;
    int port;
    int numberOfTasks;
    ArchiveServerThread archiveServerThread;
    Scanner scanner;
    ArrayList<Integer> locks = new ArrayList<>();
    public Storage() {
        scanner = new Scanner(System.in);
        port = Integer.parseInt(scanner.nextLine());
        String data = scanner.nextLine();

        String[] s = data.split(" ");
        for (String s1 : s) {
            datas.add(Integer.parseInt(s1));
        }

        numberOfTasks = Integer.parseInt(scanner.nextLine());
        for (int i = 0; i < numberOfTasks; i++) {
            String s1 = scanner.nextLine();
            tasks.add((Task) new Deserializer().deserialize(s1));
            System.err.println(tasks);
        }
        deadlockMode = scanner.nextLine().trim();
        Logger.log("Deadlock mode is= " + deadlockMode);


        for (int i = 0; i < datas.size(); i++) {
            locks.add(null);
            whoHasTheLock.put(i, -1);
            queues.add(new LinkedList<>());
        }


        graph = new Graph(datas.size(), tasks.size());
        Logger.log("Graph with " + datas.size() + " resources and " + tasks.size() + " tasks created.");
        for (Task task : tasks) {
            for (MyPair subtask : task.subtasks) {
                graph.addRequestEdge(task.id, subtask.index);
                Logger.log("Add edge from task " + task.id + " to resource " + subtask.index);
            }
        }
        Logger.log(graph.toString());

        if (deadlockMode.equalsIgnoreCase("NONE")) {
            deadlockManager = new NoneDetection();
        } else if (deadlockMode.equalsIgnoreCase("DETECT")) {
            deadlockManager = new DeadlockDetection(graph);
        } else {
            deadlockManager = new DeadlockPreventation(locks);
        }


        System.out.println("" + ProcessHandle.current().pid());
        System.out.println("" + port);

    }

    private void manageShutDownHook() {
        Thread closeChildThread = new Thread(() -> archiveServerThread.interrupt());
        Runtime.getRuntime().addShutdownHook(closeChildThread);


    }

    public void start() {

        archiveServerThread = new ArchiveServerThread(datas, port, whoHasTheLock, graph , locks , queues);
        archiveServerThread.start();


        manageShutDownHook();

        while (true) {
            String s1 = scanner.nextLine();
            int taskID = Integer.parseInt(s1);

            boolean b1 = deadlockManager.canBeAllocated(taskID, findTask(taskID));
            System.out.println(b1);
        }
    }

    public Task findTask(int taskId){
        for (Task task : tasks) {
            if (task.id == taskId){
                return task;
            }
        }
        return null;
    }

    public static void main(String[] args) throws InterruptedException {
        Storage storage = new Storage();
        storage.start();
    }
}


class ArchiveServerThread extends Thread {

    ArrayList<Integer> datas;
//    ArrayList<Semaphore> locks;
    ArrayList<Integer> locks = new ArrayList<>();
    ArrayList<Queue<Integer>> queues;
    HashMap<Integer , ArchiveServerClientHandler> taskWorkerMap = new HashMap<>();

    int port;
    private ServerSocket serverSocket;
    HashMap<Integer, Integer> whoHasTheLock;
    ArrayList<Socket> sockets = new ArrayList<>();
    final Graph graph;

    public ArchiveServerThread(ArrayList<Integer> data, int port, HashMap<Integer, Integer> whoHasTheLock, Graph graph , ArrayList<Integer> locks , ArrayList<Queue<Integer>> queues) {
        this.datas = data;
        this.whoHasTheLock = whoHasTheLock;
//        locks = new ArrayList<>();
        this.locks = locks;
        this.queues = queues;
        this.graph = graph;

        try {
            this.port = port;
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Logger.log("Storage server has been built. now it's going to start");
        while (!isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();
                sockets.add(socket);
                new ArchiveServerClientHandler(socket, this).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Socket socket : sockets) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void getValue(int index, String name, ArchiveServerClientHandler clh) throws InterruptedException {
        new Thread(() -> {
            int taskID = Integer.parseInt(name.split(" ")[1]);
            taskWorkerMap.put(taskID , clh);
            if (locks.get(index) == null){
                locks.set(index , taskID);
                Logger.log(name + " has acquired the lock for index " + index);
                clh.setResult(datas.get(index) , name);
            }else if (locks.get(index) == taskID){
                Logger.log(name + " has acquired the lock for index " + index);
                clh.setResult(datas.get(index) , name);
            }else {
                queues.get(index).add(taskID);
            }
            Logger.log("LOCKS= " + locks+"");
        }).start();

    }

    public void sendResponse(){

    }

    public void releaseLock(int index, String name) {
        int taskID = Integer.parseInt(name.split(" ")[1]);
//        Logger.log("index= " + index + "LOCKS= " + locks+"");
        if (locks.get(index) != null && locks.get(index) == taskID){
            locks.set(index , queues.get(index).poll());
//            Logger.log(name + " has released the lock for index " + index);
            Integer currentTask = locks.get(index);
            if (currentTask != null){
//                Logger.log("task " + currentTask + " has acquired the lock for index " + index);
                taskWorkerMap.get(currentTask).setResult(datas.get(index) , "task " + currentTask);
            }
        }
    }

    public void clearQueue(int index , String name ){
        int taskID = Integer.parseInt(name.split(" ")[1]);
        queues.get(index).remove(taskID);
        releaseLock(index , name);
    }




//    public void getValue(int index, String name, ArchiveServerClientHandler clh) throws InterruptedException {
//        new Thread(() -> {
//            int na = Integer.parseInt(name.split(" ")[1]);
//            Logger.log(whoHasTheLock.get(index) + "");
//            if (whoHasTheLock.get(index) != na) {
//                Logger.log(name + " tried to acquire index " + index);
////            locks.get(index).lock();
//                try {
//                    locks.get(index).acquire();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                whoHasTheLock.replace(index, na);
//            }
//            Logger.log(name + " has acquired the lock for index " + index);
//            clh.setResult(datas.get(index), name);
////        return datas.get(index);
//        }).start();
//
//    }
//
//    public void releaseLock(int index, String name) {
//        whoHasTheLock.replace(index, -1);
////        locks.get(index).unlock();
//        locks.get(index).release();
//        Logger.log(name + " has released the lock for index " + index);
//    }

    public void releaseGraph(int taskID) {
        synchronized (graph) {
            graph.removeEdges(taskID);
        }
    }
}

class ArchiveServerClientHandler extends Thread {

    ArchiveServerThread archiveServerThread;
    Socket socket;
    private BufferedReader scanner;
    private PrintStream printWriter;
    int result;
    boolean isSet;
    private final Object lock = new Object();
    private boolean isInterrupted = false;

    public ArchiveServerClientHandler(Socket socket, ArchiveServerThread archiveServerThread) {
        this.archiveServerThread = archiveServerThread;
        this.socket = socket;
        try {
            this.scanner = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.printWriter = new PrintStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        try {
//            Logger.log("Storage handler for " + socket.getPort() + "has been built. now it's going to start");
            String name = scanner.readLine();
            while (! isInterrupted) {
                String s = scanner.readLine();
                if (s == null){
                    return;
                }
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                Message message = (Message) new Deserializer().deserialize(s);
//                Logger.log("Storage handler:" + " recive: " + message + " from: " + name + " at: " + timestamp);
                int index = message.getValues()[0];
                switch (message.getType()) {
                    case OBTAIN -> {
                        isSet = false;
                        archiveServerThread.getValue(index, name, this);
//                        Logger.log("start to lock index " + index + " for " + name);
                        synchronized (lock) {
//                            Logger.log("in sync for locking index " + index + " for " + name);
                            lock.wait();
                        }
//                        Logger.log("Lock of index " + index + " released for " + name);
                        printWriter.println(new Serializer().serialize(new Message(Message.Type.RESULT_FROM_STORAGE_TO_WORKER, new int[]{result})));
                        printWriter.flush();
                    }
                    case RELEASE -> {archiveServerThread.releaseLock(index, name);}
                    case FINISH -> {
                        archiveServerThread.releaseGraph(Integer.parseInt(name.split(" ")[1]));
                        return;
                    }
                    case INTERRUPT -> {
                        archiveServerThread.clearQueue(message.getValues()[0] , name);
                        isInterrupted = true;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setResult(int result, String name) {
        this.result = result;
        this.isSet = true;
//        Logger.log("Value set for " + name + " .  now start to notify client handler");
        synchronized (lock) {
            lock.notifyAll();
//            Logger.log("notify client handler done.");
        }
    }

}