package Schedule;

import Main.Task;

public interface Scheduler {


    public void resortTasks();

    public void addTask(Task task);
    public void addTask2(Task task);

    public Task getTask();

}
