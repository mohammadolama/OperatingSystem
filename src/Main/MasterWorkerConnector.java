package Main;

import Models.Message;
import serilize.Deserializer;
import serilize.Serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.sql.Timestamp;
import java.util.Scanner;

public class MasterWorkerConnector  extends Thread{


    Task task;
    int p;
    Server server;
    int port;
    Message data;
    private final Object lock = new Object();
    Socket socket1;
    BufferedReader scanner1;
    PrintStream printStream1;
    int timeSlice;

    boolean notImportantLog = false;

    public MasterWorkerConnector(Task task, int p, Server server, int port , int timeSlice) {
        this.task = task;
        this.p = p;
        this.server = server;
        this.port = port;
        this.timeSlice = timeSlice;
    }

    @Override
    public void run() {
        try {
            socket1 = new Socket(InetAddress.getLocalHost(), port);
            scanner1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));

            printStream1 = new PrintStream(socket1.getOutputStream());


            new Thread(() -> {
                try {
                    String s = scanner1.readLine();
                    data = (Message) new Deserializer().deserialize(s);
                    synchronized (lock){
                        lock.notifyAll();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            if (notImportantLog)
                Logger.log1("Master main is getting " + task.toString() + " from worker " + p);
            Task des = getFromWorker(task, p);
            if (des == null) {

            } else if (des.done) {
                System.out.println("task " + task.id + " executed successfully with result " + des.result);
                server.addRequest(des);
                server.setWorkerOffline(p);
            }else {
                server.addRequest(des);
                server.setWorkerOffline(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private Task getFromWorker(Task task, int p) {
        try {

            String serialize = new Serializer().serialize(new Message(Message.Type.ASSIGN, task));

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Logger.log("Task: " + task + " is sent from Master main to worker " + p + " at: " + timestamp);
            printStream1.println(serialize);
            printStream1.flush();
            synchronized (lock){
                lock.wait();
            }
            if (data == null){
                // send interrupt

                return null;
            }else {
                scanner1.close();
                printStream1.close();
                socket1.close();
                return data.getTask();
            }

        } catch (IOException | InterruptedException e) {
            System.err.println(e.toString());
            e.printStackTrace();
            return null;
        }
    }
}
