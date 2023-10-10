package Schedule;

import Main.Logger;
import Main.Task;

import java.util.ArrayList;

public class FCFSScheduler implements Scheduler {

    final ArrayList<Task> tasks;
    final ArrayList<Task> doneTasks;

    final int size;
    public FCFSScheduler(ArrayList<Task> tasks) {
        this.tasks = tasks;
        doneTasks = new ArrayList<>();
        this.size = tasks.size();
    }

    @Override
    public void resortTasks() {

    }

    @Override
    public void addTask(Task task) {
       synchronized (tasks) {
            if (task.isDone()){
                doneTasks.add(task);
            }else {
                tasks.add(0 , task);
//                Logger.log("adding new task to FCFS. request list is: " + tasks.toString());
            }
            tasks.notifyAll();
        }
    }

    @Override
    public void addTask2(Task task) {
        synchronized (tasks) {
            if (task.isDone()){
                doneTasks.add(task);
            }else {
                tasks.add(task);
//                Logger.log("adding new task to FCFS. request list is: " + tasks.toString());
            }
            tasks.notifyAll();
        }
    }

    @Override
    public Task getTask() {
        Task task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                try {
                    tasks.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (doneTasks.size() == size){
                    Logger.log("all tasks are done!");
                    return null;
                }
            }
            task = tasks.remove(0);
        }
        return task;
//        return null;
    }
}
