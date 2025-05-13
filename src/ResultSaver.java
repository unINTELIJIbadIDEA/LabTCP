import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ResultSaver {
    private static final String path = "utils/bazaOdpowiedzi.txt";
    private static final String pathscore = "utils/wynik.txt";
    private static final ReentrantLock lock = new ReentrantLock();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void saveResultScore(String username, int score, int totalQuestions){
        lock.lock();
        try{
            LocalDateTime now = LocalDateTime.now();
            String formattedDate = now.format(formatter);
            String result = String.format("%s %s %d/%d\n", formattedDate, username, score, totalQuestions);
            try(PrintWriter writer = new PrintWriter(new FileWriter(pathscore, true))){
                writer.print(result);
            } catch (IOException e){
                System.out.println("Error saving result: " + e.getMessage());
            }

        } catch (Exception e){
            System.out.println("Error: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public static void saveResult(String username, List<String> answers){
        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            String formattedDate = now.format(formatter);
            String answerLine = String.join(" ", answers); // np. "a b c d a b"
            String result = String.format("%s %s | Odpowiedzi: %s\n",
                    formattedDate,
                    username,
                    answerLine);

            try (PrintWriter writer = new PrintWriter(new FileWriter(path, true))) {
                writer.print(result);
            } catch (IOException e) {
                System.out.println("Error saving result: " + e.getMessage());
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
