package com.unINTELIJIbadIDEA.server.init;

import com.unINTELIJIbadIDEA.server.model.Question;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QuestionsLoader {

    public static List<Question> loadQuestions(String filePath) {
        List<Question> questions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int totalQuestions = Integer.parseInt(reader.readLine().trim());

            for (int i = 0; i < totalQuestions; i++) {
                String line = reader.readLine();
                try {
                    questions.add(createQuestion(line));
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Błąd podczas wczytywania pliku z pytaniami: " + e.getMessage());
        }

        return questions;
    }

    private static Question createQuestion(String line){
        if (line == null || line.isEmpty()){
            return null;
        }

        String[] parts = line.split(";");
        if (parts.length != 6) {
            throw new RuntimeException("Nieprawidłowy format pytania: " + line);
        }

        String questionText = parts[0];
        String answerA = parts[1];
        String answerB = parts[2];
        String answerC = parts[3];
        String answerD = parts[4];
        String correctAnswer = parts[5].trim().toLowerCase();

        return new Question(questionText, answerA, answerB, answerC, answerD, correctAnswer);
    }


}
