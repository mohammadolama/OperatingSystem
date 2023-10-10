package Schedule;

import Main.Logger;
import Main.Task;

import java.util.ArrayList;
import java.util.Collections;

public class SJFScheduler implements Scheduler {

    final ArrayList<Task> tasks;
    final ArrayList<Task> doneTasks;
    final int size;

    public SJFScheduler(ArrayList<Task> tasks) {
        this.tasks = tasks;
        size = tasks.size();
        doneTasks = new ArrayList<>();
        Collections.sort(this.tasks);
        Logger.log("sorted : " + this.tasks.toString());
        System.out.println(this.tasks);
    }

    @Override
    public void resortTasks() {
        synchronized (tasks){
            Collections.sort(tasks);
        }
    }

    @Override
    public void addTask(Task task) {
        synchronized (tasks) {
            if (task.isDone()) {
                doneTasks.add(task);
            } else {
                tasks.add(task);
                Collections.sort(tasks);
//                Logger.log("adding new task to SJF. request list is: " + tasks.toString());
            }
            tasks.notifyAll();
        }
    }

    @Override
    public void addTask2(Task task) {
        synchronized (tasks) {
            if (task.isDone()) {
                doneTasks.add(task);
            } else {
                tasks.add(task);
//                Collections.sort(tasks);
//                Logger.log("adding new task to SJF. request list is: " + tasks.toString());
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
                if (doneTasks.size() == size) {
                    Logger.log("all tasks are done!");
                    return null;
                }
            }
            task = tasks.remove(0);
        }
        return task;
    }

}
