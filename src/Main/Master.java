package Main;

import Models.Message;
import Schedule.FCFSScheduler;
import Schedule.RRScheduler;
import Schedule.SJFScheduler;
import Schedule.Scheduler;
import serilize.Serializer;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Master {


    final String[] commonArgs = {
            "C:/Users/Mojtaba/.jdks/corretto-15.0.2/bin/java", // replace with your java path with version 1.8
            "-classpath",
            "out/production/OS_HW2/" // replace with your classpath
    };

    int interval = Integer.MAX_VALUE - 1;
    ArrayList<Process> workers = new ArrayList<>();
    ArrayList<Integer> workerPorts = new ArrayList<>();
    String[] tasks;
    ArrayList<Task> tasks1 = new ArrayList<>();
    String[] commonArgs1;
    int masterPort, workersCount;
    String schedulingAlgorithm;
    String deadlockMode;
    int storagePort;
    String data;
    int numberOfTasks;
    Scheduler scheduler;

    Process storage;
    Server server;
    PrintStream storageWriter;
    Scanner storageReader;

    public Master() {
        Logger.clearFile();

//        Scanner scanner = null;
//        try {
//            scanner = new Scanner(new File("tests2/input20.txt"));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        Scanner scanner = new Scanner(System.in);
        int argsLength = Integer.parseInt(scanner.nextLine());
        commonArgs1 = new String[argsLength];
        for (int i = 0; i < argsLength; i++) {
            commonArgs1[i] = scanner.nextLine();
        }
        masterPort = Integer.parseInt(scanner.nextLine());
        workersCount = Integer.parseInt(scanner.nextLine());
        schedulingAlgorithm = scanner.nextLine();
        if (schedulingAlgorithm.equals("RR")) {
            interval = Integer.parseInt(scanner.nextLine());
        }

        deadlockMode = scanner.nextLine();
        storagePort = Integer.parseInt(scanner.nextLine());
        data = scanner.nextLine();
        numberOfTasks = Integer.parseInt(scanner.nextLine());

        tasks = new String[numberOfTasks];


        for (int i = 0; i < numberOfTasks; i++) {
            tasks[i] = scanner.nextLine();
            Task task = new Task(tasks[i], i, interval);
            System.out.println(task);
            tasks1.add(task);
        }


        System.out.println(schedulingAlgorithm);
        if (schedulingAlgorithm.contains("FCFS")) {
            scheduler = new FCFSScheduler(tasks1);
        } else if (schedulingAlgorithm.contains("SJF")) {
            scheduler = new SJFScheduler(tasks1);
        } else {
            scheduler = new RRScheduler(tasks1);
        }

        Logger.log("Master main successfully received all inputs. its pid is: " + ProcessHandle.current().pid());
        System.out.println("Success");

    }

    public void start() {
        runStorageProcess();

        runWorkerProcesses();

        server = new Server(masterPort, workers, workerPorts, storagePort, storage, tasks1, scheduler, storageWriter, storageReader, interval);
        server.start();

        manageShutDownHook();
        System.out.println("master start " + ProcessHandle.current().pid() + " " + masterPort);

    }

    private void runWorkerProcesses() {
        try {
            for (int i = 0; i < workersCount; i++) {
                Process process = new ProcessBuilder(
                        commonArgs[0], commonArgs[1], commonArgs[2], "Main.Worker"
                ).start();
                PrintStream printStream = new PrintStream(process.getOutputStream());
                printStream.println((masterPort + 2 + i));
                printStream.println(storagePort);
                printStream.println(commonArgs.length);
                Arrays.stream(commonArgs).forEach(printStream::println);
                printStream.flush();
                Scanner scanner2 = new Scanner(process.getInputStream());
                Logger.log("worker " + i + " start with pid: " + scanner2.nextLine() + " on port: " + scanner2.nextLine());
                workers.add(process);
                workerPorts.add((masterPort + 2 + i));
                int finalI = i;
                new Thread(() -> {
                    Scanner sc = new Scanner(process.getErrorStream());
                    Logger.log("error stream for worker " + finalI + " is now working");
                    while (true) {
                        try {
                            System.out.println(COLOR.ANSI_BLUE + sc.nextLine() + COLOR.ANSI_RESET);
                        } catch (Exception ignored) {

                        }
                    }
                }).start();
            }
            Logger.log("All workers are created.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runStorageProcess() {
        try {
            storage = new ProcessBuilder(
                    commonArgs[0], commonArgs[1], commonArgs[2], "Main.Storage"
            ).start();
            storageWriter = new PrintStream(storage.getOutputStream());
            new Thread(() -> {
                Scanner sc = new Scanner(storage.getErrorStream());
                Logger.log("error stream for storage is now working");
                while (true) {
                    try {
                        System.out.println(COLOR.ANSI_PURPLE + sc.nextLine() + COLOR.ANSI_RESET);
                    } catch (Exception ignored) {

                    }
                }
            }).start();


            storageWriter.println(storagePort);
            storageWriter.println(data);
            storageWriter.println(tasks1.size());
            for (Task task : tasks1) {
                storageWriter.println(new Serializer().serialize(task));
            }
            storageWriter.println(deadlockMode);
            storageWriter.flush();
            storageReader = new Scanner(storage.getInputStream());
            Logger.log("cache start with pid: " + storageReader.nextLine() + " on port: " + storageReader.nextLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manageShutDownHook() {
        Thread closeChildThread = new Thread(() -> {
            try {
                server.interrupt();
                Logger.log("MASTER MAIN IS GETTING OUT *******************************************************************");
                storage.destroy();
                Logger.log("Storage destroyed *******************************");
                storage.waitFor();
                Logger.log("Storage stopped *******************************");
                for (Process worker : workers) {
                    worker.destroy();
                    worker.waitFor();
                    Logger.log("worker stopped *******************************");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


        Runtime.getRuntime().addShutdownHook(closeChildThread);
    }

}


class Server extends Thread {

    int archivePort;
    ArrayList<Integer> workerPorts;
    ServerSocket serverSocket;
    private final ArrayList<Task> tasks;
    private final ArrayList<Task> doneTasks;
    private final ArrayList<Process> workers;
    private final ArrayList<Boolean> workersStatus;
    private final Process archiveServer;
    Scheduler scheduler;
    PrintStream storageWriter;
    Scanner storageReader;
    int interval;
    boolean sent = false;

    boolean notImportantLog = false;


    public Server(int port, ArrayList<Process> workers, ArrayList<Integer> workerPorts, int archivePort, Process archiveServer, ArrayList<Task> tasks, Scheduler scheduler, PrintStream storageWriter, Scanner storageReader, int interval) {
        this.workers = workers;
        this.workerPorts = workerPorts;
        this.archivePort = archivePort;
        this.archiveServer = archiveServer;
//        this.clientHandlers = new ArrayList<>();
        this.doneTasks = new ArrayList<>();
        this.tasks = tasks;
        this.scheduler = scheduler;
        workersStatus = new ArrayList<>();
        this.storageReader = storageReader;
        this.storageWriter = storageWriter;
        this.interval = interval;
        for (Process ignored : workers) {
            workersStatus.add(false);
        }
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Logger.log("Master now want to start processing requests.");
        Logger.log("*****************************************************************************");
        sendFirstMessage();
        processRequests();
    }

    private void sendFirstMessage() {
        try {
            Socket socket1 = new Socket(InetAddress.getLocalHost(), workerPorts.get(0));
            BufferedReader scanner1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));

            PrintStream printStream1 = new PrintStream(socket1.getOutputStream());
            String serialize = new Serializer().serialize(new Message(Message.Type.FIRST_MESSAGE));
            Logger.log("FIRST MESSAGE SENT AT TIME: " + new Timestamp(System.currentTimeMillis()));
            printStream1.println(serialize);
            printStream1.flush();
            String s = scanner1.readLine();
            Logger.log(s + " AT TIME: " + new Timestamp(System.currentTimeMillis()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRequest(Task task) {
        scheduler.addTask(task);
    }

    public void addRequest2(Task task) {
        scheduler.addTask2(task);
    }


    private void processRequests() {
        new Thread(() -> {
            while (!isInterrupted()) {
                Task task = scheduler.getTask();
                if (task == null) {
                    Logger.log("Master stopped working because it received null task.");
                    System.exit(0);
                }
                int p = -1;
                synchronized (workersStatus) {
                    for (int i = 0; i < workersStatus.size(); i++) {
                        if (!workersStatus.get(i)) {
                            workersStatus.set(i, true);
                            p = i;
                            break;
                        }
                    }

                    if (p != -1) {
                        boolean canBeAllocated = askForDeadlock(task);
                        if (canBeAllocated) {
                            new MasterWorkerConnector(task, p, this, workerPorts.get(p), interval).start();
                            scheduler.resortTasks();
//                            getFrom(p, task);
                        } else {
                            workersStatus.set(p, false);
//                            addRequest(task);
                            addRequest2(task);
                        }
                    } else {
                        addRequest(task);
                    }

                }
            }
        }).start();


    }


    private boolean askForDeadlock(Task task) {
        storageWriter.println(task.id);
        storageWriter.flush();
        boolean b = storageReader.nextBoolean();
        return b;
    }


    public void setWorkerOffline(int p) {
        synchronized (workersStatus) {
            workersStatus.set(p, false);
        }
    }
}

