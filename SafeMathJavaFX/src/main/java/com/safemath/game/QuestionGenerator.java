package com.safemath.game;

import java.util.Random;

public class QuestionGenerator {
    private Random rand = new Random();

    private int consecutiveCorrects = 0;


    public Question generate(String difficultyBase) {
        String base = (difficultyBase == null) ? "facil" : difficultyBase.toLowerCase();

        Question q = null;
        int attempts = 0;
        do {
            q = generateForBase(base);
            attempts++;
            if (attempts > 200) break;
        } while (isMultiplicationQuestion(q) && (Math.abs(q.answer) > 100)); // garantimos <= 100 para multiplicações

        return q;
    }

    private Question generateForBase(String base) {
        if ("infinito".equals(base)) {
            if (consecutiveCorrects < 5) return generateMedium();
            else if (consecutiveCorrects < 10) return generateHard();
            else return generateInsane();
        }

        switch (base) {
            case "facil":
                if (consecutiveCorrects < 3) return generateEasy();
                else if (consecutiveCorrects < 7) return generateMedium();
                else if (consecutiveCorrects < 12) return generateHard();
                else return generateInsane();
            case "medio":
                if (consecutiveCorrects < 2) return generateMedium();
                else if (consecutiveCorrects < 6) return generateHard();
                else return generateInsane();
            case "dificil":
                if (consecutiveCorrects < 3) return generateHard();
                else return generateInsane();
            default:
                return generateEasy();
        }
    }

    public void recordAnswer(boolean correct) {
        if (correct) consecutiveCorrects++;
        else consecutiveCorrects = 0;
    }

    public void resetProgress() {
        consecutiveCorrects = 0;
    }

    private Question generateEasy() {
        // Faixa pequena, adequada para 8-9 anos
        int a = rand.nextInt(20) + 1; // 1..20
        int b = rand.nextInt(20) + 1;
        if (rand.nextBoolean()) {
            // soma
            return new Question(a + " + " + b + " ?", (double) (a + b));
        } else {
            // subtração sem negativo: garante a >= b
            if (a < b) {
                int t = a; a = b; b = t;
            }
            return new Question(a + " - " + b + " ?", (double) (a - b));
        }
    }

    private Question generateMedium() {
        // Mistura de adição, subtração, multiplicação simples
        int op = rand.nextInt(3);
        if (op == 0) {
            int a = rand.nextInt(50) + 1; // até 50
            int b = rand.nextInt(50) + 1;
            return new Question(a + " + " + b + " ?", (double) (a + b));
        } else if (op == 1) {
            int a = rand.nextInt(50) + 1;
            int b = rand.nextInt(50) + 1;
            if (a < b) { int t = a; a = b; b = t; }
            return new Question(a + " - " + b + " ?", (double) (a - b));
        } else {
            // Multiplicação simples: fatores pequenos (<=12) para facilitar
            int a = rand.nextInt(12) + 1;
            int b = rand.nextInt(10) + 1;
            int attempts = 0;
            while ((long)a * b > 100 && attempts < 50) {
                a = rand.nextInt(12) + 1;
                b = rand.nextInt(10) + 1;
                attempts++;
            }
            return new Question(a + " × " + b + " ?", (double) (a * b));
        }
    }

    private Question generateHard() {
        // Evita parênteses. Divisões exatas com valores pequenos; multiplicações limitadas.
        int op = rand.nextInt(3);
        if (op == 0) {
            // soma/subtração valores moderados
            int a = rand.nextInt(120) + 1; // até 120
            int b = rand.nextInt(120) + 1;
            if (rand.nextBoolean()) {
                return new Question(a + " + " + b + " ?", (double) (a + b));
            }
            else {
                if (a < b) { int t = a; a = b; b = t; }
                return new Question(a + " - " + b + " ?", (double) (a - b));
            }
        } else if (op == 1) {
            // divisão exata: geramos quociente q e divisor d (pequenos), depois a = q * d
            int d = rand.nextInt(12) + 1; // divisor 1..12
            int q = rand.nextInt(12) + 1; // quociente 1..12
            int a = q * d;
            return new Question(a + " ÷ " + d + " ?", (double) q);
        } else {
            // multiplicação com produto <= 100
            int attempts = 0;
            while (attempts < 200) {
                int a = rand.nextInt(12) + 1; // 1..12
                int b = rand.nextInt(12) + 1; // 1..12
                long prod = (long) a * (long) b;
                if (prod <= 100) {
                    return new Question(a + " × " + b + " ?", (double) (a * b));
                }
                attempts++;
            }
            // fallback
            int a = rand.nextInt(10) + 1;
            int b = rand.nextInt(10) + 1;
            return new Question(a + " × " + b + " ?", (double) (a * b));
        }
    }

    private Question generateInsane() {
        int op = rand.nextInt(3);
        if (op == 0) {
            int a = rand.nextInt(200) + 1;
            int b = rand.nextInt(200) + 1;
            return new Question(a + " + " + b + " ?", (double) (a + b));
        } else if (op == 1) {
            int d = rand.nextInt(15) + 1; // divisor um pouco maior possível
            int q = rand.nextInt(20) + 1; // quociente
            int a = q * d;
            return new Question(a + " ÷ " + d + " ?", (double) q);
        } else {
            int attempts = 0;
            while (attempts < 300) {
                int a = rand.nextInt(15) + 1;
                int b = rand.nextInt(12) + 1;
                long prod = (long) a * (long) b;
                if (prod <= 100) {
                    return new Question(a + " × " + b + " ?", (double) (a * b));
                }
                attempts++;
            }
            int a = rand.nextInt(12) + 1;
            int b = rand.nextInt(10) + 1;
            return new Question(a + " × " + b + " ?", (double) (a * b));
        }
    }

    // Detecta se a pergunta é de multiplicação
    private boolean isMultiplicationQuestion(Question q) {
        if (q == null || q.text == null) return false;
        String t = q.text.toLowerCase();
        return t.contains("x") || t.contains("×") || t.contains("*") || t.contains("multiplic");
    }
}