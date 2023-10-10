package Main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Logger {

    private static final String file = "log.txt";
    private static final String file1 = "log1.txt";
    private static boolean cleared = false;

    public static void log(String string) {
        synchronized (file) {
            Calendar calendar = Calendar.getInstance();
            try {
                FileWriter fileWriter = new FileWriter(file, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                Locale locale = new Locale("English", "England");
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale);
//                bufferedWriter.write(formatter.format(calendar.getTime()) + "  " + string + "\n");
                bufferedWriter.write( string + "\n");
                bufferedWriter.flush();
                bufferedWriter.close();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearFile() {
        try {
            PrintWriter printWriter=new PrintWriter(file);
            printWriter.println("File cleared \n");
            printWriter.flush();
            printWriter.close();
            cleared = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log1(String string) {
//        synchronized (file1) {
//            Calendar calendar = Calendar.getInstance();
//            try {
//                FileWriter fileWriter = new FileWriter(file1, true);
//                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
//                Locale locale = new Locale("English", "England");
//                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale);
//                bufferedWriter.write(formatter.format(calendar.getTime()) + "  " + string + "\n");
//                bufferedWriter.flush();
//                bufferedWriter.close();
//                fileWriter.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }


}
