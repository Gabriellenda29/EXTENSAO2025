package com.safemath.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    public GamePane pane;
    public Animal currentAnimal;
    public Boss currentBoss;
    public Player player;

    public List<Animal> animals = new ArrayList<>();
    public QuestionGenerator qGen = new QuestionGenerator();

    public String mode = "Matematica";
    private Random rand = new Random();
    public int arcadeStage = 0;

    private boolean isPlayerTurn = true;

    public Game() {
        animals.add(new Animal("Coelho", 10));
        animals.add(new Animal("Gato", 20));
        animals.add(new Animal("Cao", 30));
        animals.add(new Animal("Leão", 40));
        animals.add(new Animal("Tigre", 50));

        currentAnimal = animals.get(0);
        player = new Player(200);
        spawnBoss();
    }

    public void setPane(GamePane pane) {
        this.pane = pane;
    }

    public void spawnBoss() {
        int vida = 0;
        switch (mode) {
            case "Matematica": vida = currentAnimal.force * 3; break;
            case "Arcade":
                if (arcadeStage == 0) vida = 200;
                else if (arcadeStage == 1) vida = 300;
                else vida = 400;
                break;
            case "Infinito": vida = 999999; break;
        }
        currentBoss = new Boss("Chefe", vida);
        // Resetar progresso de perguntas quando um novo chefe surgir
        qGen.resetProgress();
        if (pane != null) {
            pane.updateBossHealth();
            // Define a imagem do chefe dependendo do modo:
            // Matemática: 5 chefes -> use index 1..5 (mapeado ao animal atual)
            if ("Matematica".equals(mode)) {
                int idx = animals.indexOf(currentAnimal) + 1; // 1..5
                if (idx < 1) idx = 1;
                if (idx > 5) idx = 5;
                pane.setBossImageForMode("Matematica", idx);
            }
            // Arcade: 3 chefes -> use arcadeStage 0..2 => boss1..boss3
            else if ("Arcade".equals(mode)) {
                int idx = arcadeStage + 1; // 1..3
                if (idx < 1) idx = 1;
                if (idx > 3) idx = 3;
                pane.setBossImageForMode("Arcade", idx);
            }
            // Infinito: 1 chefe
            else if ("Infinito".equals(mode)) {
                pane.setBossImageForMode("Infinito", 1);
            }
        }
    }

    public void togglePlayerTurn() { isPlayerTurn = !isPlayerTurn; }
    public boolean isPlayerTurn() { return isPlayerTurn; }

    public String difficultyForMode() {
        switch (mode) {
            case "Matematica": return "facil";
            case "Arcade": return "medio";
            case "Infinito": return "infinito";
            default: return "facil";
        }
    }

    public void checkBossDefeat() {
        if (currentBoss != null && !currentBoss.isAlive()) {
            if (mode.equals("Matematica")) {
                int index = animals.indexOf(currentAnimal);
                if (index < animals.size() - 1) {
                    // Desbloqueia próximo animal
                    currentAnimal = animals.get(index + 1);
                    if (pane != null) {
                        pane.showCenteredMessage("Novo animal desbloqueado:\n" + currentAnimal.name, 2.2);
                        pane.updateAnimalSelection();
                    }
                    spawnBoss();
                } else {
                    if (pane != null) {
                        // mostra a mensagem e volta automaticamente ao menu
                        pane.onModeVictory();
                    }
                    endGame();
                }
            } else if (mode.equals("Arcade")) {
                if (pane != null) {
                    pane.showCenteredMessage("Você derrotou o chefe!", 1.8);
                }
                arcadeStage++;
                if (arcadeStage < 3) {
                    spawnBoss();
                } else {
                    if (pane != null) {
                        pane.onModeVictory();
                    }
                    endGame();
                }
            } else if (mode.equals("Infinito")) {
                // Infinito não termina — apenas respawna
                if (pane != null) pane.log("🔁 Chefe re-spawn (Infinito)");
                spawnBoss();
            }
        }
    }

    private void endGame() {
        if (pane != null) {
            pane.disableTurns();
            // log é no-op na UI, mas mantemos para debug em console se quiser implementar
            pane.log("🎮 O jogo terminou!");
        }
    }

    public int getRandomDamage(int max) { return rand.nextInt(max) + 1; }

    public void decrementAllCooldowns() {
        for (Animal a : animals) a.decrementCooldown();
    }
}