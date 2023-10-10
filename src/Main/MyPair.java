package Main;

import java.io.Serializable;

public class MyPair implements Serializable {
    int time;
    int index;
    int value;

    public MyPair(int a, int b) {
        this.time = a;
        this.index = b;
    }

    public MyPair() {
    }

    @Override
    public String toString() {
        return "{" +
                 time +
                ", " + index +
                '}';
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
