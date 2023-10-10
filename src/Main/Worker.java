package Main;

import Models.Message;
import serilize.Deserializer;
import serilize.Serializer;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Scanner;

public class Worker {

    Scanner scanner;
    int port;
    int storagePort;
    int argsLength;
    String[] commonArgs;
    WorkerServer workerServer;

    public Worker() {
        scanner = new Scanner(System.in);
        port = Integer.parseInt(scanner.nextLine());
        storagePort = Integer.parseInt(scanner.nextLine());
        argsLength = Integer.parseInt(scanner.nextLine());
        commonArgs = new String[argsLength];
        for (int i = 0; i < argsLength; i++) {
            commonArgs[i] = scanner.nextLine();
        }
        System.out.println("" + ProcessHandle.current().pid());
        System.out.println("" + port);


    }

    public void start() {
        workerServer = new WorkerServer(port, commonArgs, storagePort);
        workerServer.start();

        Thread closeChildThread = new Thread(() -> workerServer.interrupt());
        Runtime.getRuntime().addShutdownHook(closeChildThread);
    }

    public static void main(String[] args) throws InterruptedException {
        Worker worker = new Worker();
        worker.start();

    }
}

class WorkerServer extends Thread {

    int port;
    private ServerSocket serverSocket;
    private final String[] commonArgs;
    private final int storagePort;
    private final ArrayList<Socket> sockets = new ArrayList<>();

    public WorkerServer(int port, String[] commonArgs, int storagePort) {
        this.commonArgs = commonArgs;
        this.storagePort = storagePort;
        try {
            this.port = port;
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }

    }


    @Override
    public void run() {
        Logger.log("worker with port: " + port + " is started.");
        while (!isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();
                sockets.add(socket);
                new WorkerClientHandler(socket, this, commonArgs, storagePort).start();
            } catch (IOException e) {
                System.err.println(e.toString());
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
            System.err.println(e.toString());
            e.printStackTrace();
        }
        for (Socket socket : sockets) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println(e.toString());
                e.printStackTrace();
            }
        }
    }
}


class WorkerClientHandler extends Thread {

    private final Socket socket;
    private final WorkerServer server;
    private final String[] commonArgs;
    private final int storagePort;

    private BufferedReader scanner;
    private PrintStream printWriter;
    private WorkerState workerState;

    private final Object lock = new Object();
    private final Object lock2 = new Object();

    Integer data = null;
    Task task = null;
    boolean flag = true;
    int timeSlice;
    boolean isInterrupted = false;


    public WorkerClientHandler(Socket socket, WorkerServer server, String[] commonArgs, int storagePort) {
//        Logger.log("new worker clint handler with port: " + socket.getPort() + " is started.");
        this.socket = socket;
        this.server = server;
        this.commonArgs = commonArgs;
        this.storagePort = storagePort;
        try {
            this.scanner = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.printWriter = new PrintStream(socket.getOutputStream());
        } catch (IOException e) {
            Logger.log(e.getMessage());
        }
    }

    @Override
    public void run() {

        while (flag) {
            try {
                String s = scanner.readLine();
                if (s == null) {
                    return;
                }
                Message des = (Message) new Deserializer().deserialize(s);
                if (des.getType() == Message.Type.FIRST_MESSAGE){
                    printWriter.println("received");
                    printWriter.flush();
                    socket.close();
                    return;
                }
                try {
                    Task deserialize = des.getTask();
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    Logger.log("worker with port: " + server.port + " receive: " + deserialize.toString() + " at: " + timestamp);
                    timeSlice = deserialize.timeSlice;

                    Thread timer = new Thread(() -> {
                        try {
                            synchronized (lock2) {
                                lock2.wait(timeSlice + 1);
                            }
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            isInterrupted = true;
                            Logger.log("Worker interrupted with timeSlice= " + timeSlice);
                        } catch (InterruptedException e) {
//                            System.err.println(e.toString());
//                            e.printStackTrace();
                        }
                    });
                    timer.start();

                    Socket socket1 = new Socket(InetAddress.getLocalHost(), storagePort);
                    BufferedReader scanner1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));

                    PrintStream printStream1 = new PrintStream(socket1.getOutputStream());
                    printStream1.println("task " + deserialize.id);
                    printStream1.flush();


                    while (!deserialize.subtasks.isEmpty()){
                        if (isInterrupted) {
                            Logger.log("Interrupted normally . task is= " + deserialize.toString());
                            sendResultToMaster(printWriter, deserialize, false);
                            flag = false;
                            scanner.close();
                            scanner1.close();
                            socket1.close();
                            socket.close();
                            return;
                        }
                        workerState = WorkerState.STORAGE;
                        MyPair remove = deserialize.subtasks.get(0);
                        timestamp = new Timestamp(System.currentTimeMillis());

                        boolean b1 = storagePhase(printStream1, remove, scanner1, deserialize);
                        if (!b1) {
                            Logger.log("Interrupted in storagePhase. task is= " + deserialize.toString());
                            printStream1.println(new Serializer().serialize(new Message(Message.Type.INTERRUPT, new int[]{remove.index})));
                            sendResultToMaster(printWriter, deserialize, false);
                            flag = false;
                            isInterrupted = true;
                            scanner.close();
                            scanner1.close();
                            socket1.close();
                            socket.close();
                            return;
                        }


                        boolean b2 = waitingPhase(remove, deserialize);
                        if (!b2) {
                            Logger.log("Interrupted in waitingPhase. task is= " + deserialize.toString());
//                            printStream1.println(new Serializer().serialize(new Message(Message.Type.INTERRUPT, new int[]{remove.index})));
                            sendResultToMaster(printWriter, deserialize, false);
                            flag = false;
                            isInterrupted = true;
                            scanner.close();
                            scanner1.close();
                            socket1.close();
                            socket.close();
                            return;
                        }
                        deserialize.subtasks.remove(0);

                    }

                    timer.interrupt();
                    releaseLocks(deserialize, printStream1);

                    sendResultToMaster(printWriter, deserialize, true);
                    flag = false;
                    scanner.close();
                    scanner1.close();
                    socket1.close();
                    socket.close();
                } catch (IOException e) {
                    System.err.println(e.toString());
                    e.printStackTrace();
                }

            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }


    public void releaseLocks(Task deserialize, PrintStream printStream1) {
        for (int i = 0; i < deserialize.zapas.size(); i++) {
            MyPair remove = deserialize.zapas.get(i);
            printStream1.println(new Serializer().serialize(new Message(Message.Type.RELEASE, new int[]{remove.index})));
        }
        printStream1.println(new Serializer().serialize(new Message(Message.Type.FINISH, new int[]{0})));
        printStream1.flush();
    }

    public void sendResultToMaster(PrintStream printWriter, Task deserialize, boolean done) {
        deserialize.done = done;
        printWriter.println(new Serializer().serialize(new Message(Message.Type.RESULT_FROM_WORKER_TO_MASTER, deserialize)));
        printWriter.flush();

    }

    public void readFromStorage(BufferedReader scanner1) {
        new Thread(() -> {
            try {
                if (isInterrupted) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                    return;
                }
                String s1 = scanner1.readLine();
                Message deserialize1 = (Message) new Deserializer().deserialize(s1);
//                Logger.log("Messeage from storage: " + deserialize1.toString());
                if (deserialize1.getType() == Message.Type.RESULT_FROM_STORAGE_TO_WORKER) {
                    data = deserialize1.getValues()[0];
                } else {
                    data = null;
                }
                synchronized (lock) {
                    lock.notifyAll();
                }
//                Logger.log("worker notified that receive message from storage.");
            } catch (IOException e) {
                System.err.println(e.toString());
                e.printStackTrace();
            }
        }).start();
    }

    public boolean storagePhase(PrintStream printStream1, MyPair remove, BufferedReader scanner1, Task deserialize) {
        try {
            if (isInterrupted){
                return false;
            }
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//            Logger.log("worker with port: " + server.port + " now start to ask storage about: " + remove.toString() + " at: " + timestamp);
            readFromStorage(scanner1);
            printStream1.println(new Serializer().serialize(new Message(Message.Type.OBTAIN, new int[]{remove.index})));
            printStream1.flush();
            data = null;
            synchronized (lock) {
                lock.wait();
            }
//            Logger.log("WORKER NOTIFY WITH DATA: " + data);
            if (data == null) {
//                Logger.log("data is null for task " + deserialize.id);
                return false;
            } else {
                remove.value = data;
//                deserialize.result += data;
                return true;
            }
        } catch (Exception e) {
            System.err.println(e.toString());
            return false;
        }
    }

    public boolean waitingPhase(MyPair remove, Task task) {
        if (isInterrupted){
            return false;
        }
        workerState = WorkerState.WAITING;
//        Logger.log("Start to wait. " + task);
        synchronized (lock) {
            try {
                long l = System.currentTimeMillis();
                lock.wait(remove.time + 1);
                long l1 = System.currentTimeMillis();
                int dif = (int) (l1 - l);
//                Logger.log("End to wait. " + task + " time is: " + dif);
                if (dif < remove.time) {          // we receive interrupt
                    remove.time = (remove.time - dif);
                    return false;
                }
            } catch (InterruptedException e) {
                System.err.println(e.toString());
                e.printStackTrace();
            }
            task.result += remove.value;
            return true;

        }
    }

}


enum WorkerState {
    STORAGE,
    WAITING,
}
