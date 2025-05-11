import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private static final long connectionTimeout = 300000;
    private static final long answerTimeout = 15000;
    private final List<String> userAnswers = new ArrayList<>();
    private String clientName;
    private long lastActivity;
    private boolean connected = true;
    private int score = 0;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lastActivity = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            sendMessage("TIMEOUT:" + connectionTimeout);
            // Wysłanie wiadomości powitalnej
            sendMessage("""
                Witaj na quizie!
                W każdym pytaniu wybierz odpowiedź a, b, c lub d.
                Wpisz 'rezygnuje' aby zakończyć quiz.
                Podaj swoje imię i nazwisko:
                """);


            // Czekanie na podanie imienia i nazwiska
            clientName = in.readLine();
            lastActivity = System.currentTimeMillis();

            if (clientName == null || clientName.equals("__TIMEOUT__")) {
                disconnect("Czas na podanie imienia i nazwiska minął.");
                return;
            }
            startQuiz();
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } finally {
            disconnect("Połączenie zakończone.");
        }
    }

    public void checkConnection() {
        if (System.currentTimeMillis() - lastActivity > connectionTimeout) {
            disconnect(String.format("Uzytkownik nieaktywny przez %d sekund", connectionTimeout/1000));
        }
        if (connected) {
            try {
                if (out != null) {
                    out.println("");
                }
            } catch (Exception e) {
                disconnect("Blad podczas pingowania uzytkownika");
            }
        }
    }

    private void startQuiz() throws IOException {
        sendMessage("TIMEOUT:" + answerTimeout);
        try (Scanner fileScanner = new Scanner(new File("utils/bazaPytan.txt"))) {
            int totalQuestions = Integer.parseInt(fileScanner.nextLine());

            while (fileScanner.hasNextLine() && connected) {
                String line = fileScanner.nextLine();
                String[] parts = line.split(";");
                String correctAnswer = parts[5];



                sendMessage(String.join("\n", Arrays.copyOfRange(parts, 0, 5)));

                String answer = in.readLine();
                lastActivity = System.currentTimeMillis();

                if (answer == null || answer.equals("__TIMEOUT__")) {
                    sendMessage("Czas na odpowiedź minął. Prawidłowa odpowiedź to: " + correctAnswer);
                    userAnswers.add(" - ");
                } else if (answer.equalsIgnoreCase("rezygnuje")) {
                    disconnect("Użytkownik zrezygnował z quizu");

                    return;
                } else {
                    if (answer.trim().equalsIgnoreCase(correctAnswer.trim())) {
                        sendMessage("Prawidłowa odpowiedź!");
                        score++;
                    } else {
                        sendMessage("Nieprawidłowa odpowiedź. Prawidłowa odpowiedź to: " + correctAnswer);
                    }
                    userAnswers.add(answer.trim());
                }

            }

            sendMessage("Koniec quizu! Twój wynik to: " + score + "/" + totalQuestions);
            ResultSaver.saveResultScore(clientName, score, totalQuestions);
            ResultSaver.saveResult(clientName, userAnswers);
        }
    }

    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void disconnect(String reason) {
        if (connected) {
            connected = false;
            try {
                sendMessage("Disconnected: " + reason);
                Serverr.removeClient(this);
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }
}