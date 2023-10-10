package Models;

import Main.Task;

import java.io.Serializable;
import java.util.Arrays;

public class Message implements Serializable {

    public enum Type{
        INTERRUPT,
        INTERRUPT2,
        OBTAIN,
        RELEASE,
        ASSIGN,
        RESULT_FROM_STORAGE_TO_WORKER,
        RESULT_FROM_WORKER_TO_MASTER,
        FINISH,
        FIRST_MESSAGE,
    }


    private Type type;
    private Task task;
    private String data;
    private int[] values;

    public Message() {
    }

    public Message(Type type) {
        this.type = type;
    }

    public Message(Type type, int[] values) {
        this.type = type;
        this.values = values;
    }

    public Message(Type type, Task task) {
        this.type = type;
        this.task = task;
    }


    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int[] getValues() {
        return values;
    }

    public void setValues(int[] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", task=" + task +
                ", data='" + data + '\'' +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}

