package Main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Task implements Serializable , Comparable<Task>{

    ArrayList<MyPair> subtasks;
    ArrayList<MyPair> zapas = new ArrayList<>();
    int id;
    int delay;
    boolean done;
    int result;
    boolean isInterrupt;
    int timeSlice;

    public Task() {
    }

    public Task(String data , int id , int timeSlice) {
        subtasks = new ArrayList<>();
        this.id = id;
        done = false;
        this.timeSlice = timeSlice;
        String[] s = data.split(" ");
        if (s.length %2 == 1){
            s = Arrays.copyOfRange(s , 0 , s.length-1);
        }
        for (int i = 0; i < s.length; i+=2) {
            subtasks.add(new MyPair(Integer.parseInt(s[i]) , Integer.parseInt(s[i+1])));
            zapas.add(new MyPair(Integer.parseInt(s[i]) , Integer.parseInt(s[i+1])));
            delay +=Integer.parseInt(s[i]);
        }

    }


    @Override
    public String toString() {
        return "Task{" +
                "subtasks=" + subtasks +
                ", done=" + done +
                ", timeSlice=" + timeSlice +
                ", id=" + id +
                '}';
    }

    public ArrayList<MyPair> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(ArrayList<MyPair> subtasks) {
        this.subtasks = subtasks;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }


    @Override
    public int compareTo(Task o) {
        if (delay< o.delay){
            return -1;
        }else if (delay > o.delay){
            return +1;
        }
        return 0;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
