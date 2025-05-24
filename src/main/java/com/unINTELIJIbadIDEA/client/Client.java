package com.unINTELIJIbadIDEA.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8000;
    private static final int DEFAULT_TIMEOUT = 30000;

    private Socket socket;
    private BufferedReader serverIn;
    private PrintWriter serverOut;
    private Scanner userInput;
    private ExecutorService executor;

    private boolean connected = false;
    private int currentTimeout = DEFAULT_TIMEOUT;
    private AtomicBoolean waitingForAnswer = new AtomicBoolean(false);
    private AtomicBoolean timeoutOccurred = new AtomicBoolean(false);
    private Future<?> currentTimerTask;

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }

    public void start() {
        userInput = new Scanner(System.in);
        executor = Executors.newFixedThreadPool(2);
        if (!connectToServer()) {
            System.out.println("Nie udało się połączyć z serwerem. Kończenie programu.");
            return;
        }

        startServerListener();
    }

    private void startServerListener() {
        executor.execute(() -> {
            try {
                String serverMessage;
                while (connected && (serverMessage = serverIn.readLine()) != null) {
                    if (serverMessage.isEmpty()) {
                        continue;
                    }

                    waitingForAnswer.set(false);

                    if (serverMessage.startsWith("TIMEOUT:")) {
                        try {
                            currentTimeout = Integer.parseInt(serverMessage.substring(8).trim());
                            if (currentTimeout <= 60000) {
                                System.out.println("Masz " + (currentTimeout/1000) + " sekund na odpowiedź.");
                            } else {
                                System.out.println("Masz " + (currentTimeout/60000) + " minut na odpowiedź.");
                            }
                            continue;
                        } catch (NumberFormatException e) {
                            System.out.println("Otrzymano nieprawidłowy format czasu oczekiwania");
                        }
                    }

                    System.out.println(serverMessage);
                    if (serverMessage.startsWith("Disconnected:")) {
                        connected = false;
                        break;
                    }
                    if (shouldRespond(serverMessage)) {
                        waitingForAnswer.set(true);
                        timeoutOccurred.set(false);
                        startAnswerTimer();
                        System.out.print("> ");

                        String userResponse = null;
                        try {
                            while (waitingForAnswer.get() && connected) {
                                if (System.in.available() > 0) {
                                    userResponse = userInput.nextLine().trim();
                                    waitingForAnswer.set(false);
                                    break;
                                }
                                if (timeoutOccurred.get()) {
                                    waitingForAnswer.set(false);
                                    break;
                                }

                                Thread.sleep(100);
                            }
                        } catch (Exception e) {
                            System.out.println("Błąd podczas odczytu odpowiedzi: " + e.getMessage());
                            waitingForAnswer.set(false);
                        }

                        if (timeoutOccurred.get() || userResponse == null) {
                            serverOut.println("__TIMEOUT__");
                        } else {
                            serverOut.println(userResponse);

                            if (userResponse.equalsIgnoreCase("rezygnuje")) {
                                System.out.println("Rezygnujesz z quizu.");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    if (e.getMessage() != null && e.getMessage().contains("Socket closed")) {
                        System.out.println("Połączenie z serwerem zostało zamknięte.");
                    } else {
                        System.out.println("Błąd podczas komunikacji z serwerem: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();

            } finally {
                disconnect();
            }
        });
    }

    private void startAnswerTimer() {
        if (currentTimerTask != null && !currentTimerTask.isDone()) {
            currentTimerTask.cancel(true);
        }
        timeoutOccurred.set(false);

        currentTimerTask = executor.submit(() -> {
            try {
                if (currentTimeout <= 30000) {
                    Thread.sleep(currentTimeout);
                } else {
                    long remainingTime = currentTimeout;
                    long checkInterval = 15000;

                    while (remainingTime > 0 && waitingForAnswer.get() && !Thread.currentThread().isInterrupted()) {
                        Thread.sleep(Math.min(checkInterval, remainingTime));
                        remainingTime -= checkInterval;

                        if (waitingForAnswer.get() && remainingTime % 60000 <= checkInterval && remainingTime > 60000) {
                            System.out.println("\nPozostało jeszcze około " + (remainingTime/60000 + 1) + " minut na odpowiedź");
                            System.out.print("> ");
                        }

                        if (waitingForAnswer.get() && remainingTime <= 30000 && remainingTime > 15000) {
                            System.out.println("\nUWAGA: Pozostało tylko 30 sekund na odpowiedź!");
                            System.out.print("> ");
                        }
                    }
                }

                if (waitingForAnswer.get()) {
                    timeoutOccurred.set(true);
                }
            } catch (InterruptedException e) {
                // Ignorujemy przerwane wątku -> został przerwany specjalnie
            }
        });
    }

    private static boolean shouldRespond(String message) {
        return message.contains("d)") ||
                message.toLowerCase().contains("podaj swoje imię i nazwisko") ||
                message.toLowerCase().contains("napisz coś aby zacząć quiz");
    }

    private boolean connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            socket.setSoTimeout(600000);
            socket.setKeepAlive(true);

            // Inicjalizacja strumieni I/O
            serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverOut = new PrintWriter(socket.getOutputStream(), true);

            connected = true;
            return true;
        } catch (IOException e) {
            System.out.println("Błąd podczas łączenia z serwerem: " + e.getMessage());
            return false;
        }
    }

    private void disconnect() {
        if (!connected) return;

        connected = false;
        try {
            if (currentTimerTask != null && !currentTimerTask.isDone()) {
                currentTimerTask.cancel(true);
            }

            waitingForAnswer.set(false);
            timeoutOccurred.set(false);

            if (serverIn != null) serverIn.close();
            if (serverOut != null) serverOut.close();
            if (socket != null && !socket.isClosed()) socket.close();

            System.out.println("Rozłączono z serwerem.");
        } catch (IOException e) {
            System.out.println("Błąd podczas zamykania połączenia: " + e.getMessage());
        } finally {
            if (userInput != null) userInput.close();
            if (executor != null) {
                executor.shutdownNow();
                try {
                    executor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    System.out.println("Przerwano oczekiwanie na zakończenie zadań");
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}