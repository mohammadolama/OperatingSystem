package Deadlock;

import Main.Task;

public class NoneDetection implements DeadlockManager{

    public NoneDetection() {
    }

    @Override
    public boolean canBeAllocated(int task, Task a) {
        return true;
    }
}
