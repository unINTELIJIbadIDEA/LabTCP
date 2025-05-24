package com.unINTELIJIbadIDEA;

import com.unINTELIJIbadIDEA.server.model.Question;
import com.unINTELIJIbadIDEA.server.services.QuestionService;
import com.unINTELIJIbadIDEA.server.services.ResultService;
import com.unINTELIJIbadIDEA.server.services.ScoreService;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    private final QuestionService questionService = new QuestionService();
    private final ResultService resultService = new ResultService();
    private final ScoreService scoreService = new ScoreService();

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

    private void startQuiz() {
        sendMessage("TIMEOUT:" + answerTimeout);

        List<Question> questions;
        try {
            questions = questionService.getAll();
        } catch (SQLException e) {
            sendMessage("Błąd serwera: nie można załadować pytań.");
            return;
        }

        int totalQuestions = questions.size();

        for (Question q : questions) {
            if (!connected) break;

            sendMessage(
                    q.getQuestionText() + "\n" +
                            "a) " + q.getAnswerA() + "\n" +
                            "b) " + q.getAnswerB() + "\n" +
                            "c) " + q.getAnswerC() + "\n" +
                            "d) " + q.getAnswerD()
            );

            try {
                String answer = in.readLine();
                lastActivity = System.currentTimeMillis();

                if (answer == null || answer.equals("__TIMEOUT__")) {
                    sendMessage("Czas na odpowiedź minął. Prawidłowa odpowiedź to: " + q.getCorrectAnswer());
                    userAnswers.add(" - ");
                } else if (answer.equalsIgnoreCase("rezygnuje")) {
                    disconnect("Użytkownik zrezygnował z quizu");
                    return;
                } else {
                    if (answer.trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) {
                        sendMessage("Prawidłowa odpowiedź!");
                        score++;
                    } else {
                        sendMessage("Nieprawidłowa odpowiedź. Prawidłowa odpowiedź to: " + q.getCorrectAnswer());
                    }
                    userAnswers.add(answer.trim());
                }

            } catch (IOException e) {
                disconnect("Błąd podczas odczytu odpowiedzi: " + e.getMessage());
                return;
            }
        }

        sendMessage("Koniec quizu! Twój wynik to: " + score + "/" + totalQuestions);

        try {
            scoreService.save(clientName, score, totalQuestions);
            resultService.save(clientName, userAnswers);
        } catch (SQLException e) {
            sendMessage("Błąd przy zapisie wyników: " + e.getMessage());
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
                Server.removeClient(this);
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