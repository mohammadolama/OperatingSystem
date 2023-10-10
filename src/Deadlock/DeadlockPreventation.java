package Deadlock;

import Main.Logger;
import Main.MyPair;
import Main.Task;

import java.util.ArrayList;
import java.util.HashMap;

public class DeadlockPreventation  implements DeadlockManager{

//    final HashMap<Integer , Integer> whoHasTheLock;
    final ArrayList<Integer> locks;

    public DeadlockPreventation(ArrayList<Integer> locks) {
        this.locks = locks;
    }

//    public DeadlockPreventation(HashMap<Integer, Integer> whoHasTheLock) {
//        this.whoHasTheLock = whoHasTheLock;
//    }




    @Override
    public boolean canBeAllocated(int task, Task a) {
        synchronized (locks){
            boolean flag = true;
            for (MyPair subtask : a.getSubtasks()) {
                if (locks.get(subtask.getIndex()) != null && locks.get(subtask.getIndex()) != task){
                    flag = false;
                }
            }
            if (flag){
                Logger.log("Deadlock NOT FOUND with Deadlock prevention method for task= " + a + " . Now locks list is: " + locks);

                for (MyPair subtask : a.getSubtasks()) {
                    locks.set(subtask.getIndex(), task);
                }
                Logger.log("locks are given to task= " + task +" . Now locks list is: " + locks);

            }else {
                Logger.log("Deadlock found with Deadlock prevention method for task= " + task);
            }
            return flag;
        }
    }


//    @Override
//    public boolean canBeAllocated(int task, Task a) {
//        synchronized (whoHasTheLock){
//            boolean flag = true;
//            for (MyPair subtask : a.getSubtasks()) {
//                if (whoHasTheLock.get(subtask.getIndex()) != -1 && whoHasTheLock.get(subtask.getIndex()) != task){
//                    flag = false;
//                }
//            }
//            if (flag){
//                for (MyPair subtask : a.getSubtasks()) {
//                    whoHasTheLock.replace(subtask.getIndex(), task);
//                }
//            }else {
//                Logger.log("Deadlock found with Deadlock prevention method.");
//            }
//            return flag;
//        }
//    }
}
