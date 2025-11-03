package com.safemath.game;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GamePane extends BorderPane {
    private Game game;

    // Topo: turno, pergunta, resposta
    private final Label turnoLabel = new Label();
    private final Label questionLabel = new Label();
    private final TextField answerField = new TextField();
    private final Button btnSubmit = new Button("Responder");

    // Player sprite + animal ao lado (não sobreposto)
    private final ImageView basePlayerImage = new ImageView();
    private final ImageView animalSideImage = new ImageView();
    private final ProgressBar playerHealthBar = new ProgressBar();
    private final Label playerDamageLabel = new Label();

    // Boss + vida
    private final ImageView bossImage = new ImageView();
    private final ProgressBar bossHealthBar = new ProgressBar();
    private final Label bossDamageLabel = new Label();

    // Seleção de animais (cards)
    private final HBox animalSelectionBox = new HBox(6);

    // Botão de pause (imagem)
    private final Button settingsButton = new Button();

    // Stack onde o jogo e overlays vivem (ESSENCIAL para centralização)
    private final StackPane gameStack = new StackPane();

    // Overlays (dentro do gameStack)
    private final StackPane pauseOverlay = new StackPane();
    private final StackPane centerMessageLayer = new StackPane();

    private final Main mainApp;
    private final Stage stage;

    // Campo novo: animal que está "locked" por ter entrado em cooldown
    private Animal lockedAnimal = null;

    // Questão atual armazenada como campo para uso na lambda
    private Question currentQuestion = null;

    private boolean gameActive = false;
    private Animal selectedAnimal;

    // Paths formatados (suas pastas: images/matematica, images/arcade, images/infinito)
    private static final String[] MATEMATICA_BOSSES = {
            "/images/matematica/boss1.png",
            "/images/matematica/boss2.png",
            "/images/matematica/boss3.png",
            "/images/matematica/boss4.png",
            "/images/matematica/boss5.png"
    };

    private static final String[] ARCADE_BOSSES = {
            "/images/arcade/boss1.png",
            "/images/arcade/boss2.png",
            "/images/arcade/boss3.png"
    };

    private static final String[] INFINITO_BOSSES = {
            "/images/infinito/boss1.png"
    };

    public GamePane(Game game, Main mainApp, Stage stage) {
        this.game = game;
        this.mainApp = mainApp;
        this.stage = stage;
        game.setPane(this);

        // ---------- Estilos básicos ----------
        this.setStyle("-fx-background-color: #7fe6e6;");

        // ---------- Topo (título + pergunta + input) ----------
        turnoLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #0b3f2b;");
        questionLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #0b3f2b;");
        VBox questionBox = new VBox(6, turnoLabel, questionLabel);
        questionBox.setAlignment(Pos.CENTER);

        answerField.setMaxWidth(240);
        answerField.setPromptText("Resposta");
        answerField.setStyle(
                "-fx-background-color: white; -fx-background-radius: 18; -fx-border-radius: 18; " +
                        "-fx-border-color: #00000055; -fx-border-width: 4; -fx-padding: 8; -fx-font-size: 18;"
        );
        // Permite submeter a resposta pressionando Enter no TextField
        answerField.setOnAction(e -> btnSubmit.fire());

        btnSubmit.setStyle("-fx-font-size: 14px;");
        VBox topBox = new VBox(10, questionBox, answerField, btnSubmit);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(14));
        topBox.setPrefHeight(220);

        // ---------- Player + animal ao lado ----------
        basePlayerImage.setFitWidth(64);
        basePlayerImage.setFitHeight(64);
        basePlayerImage.setPreserveRatio(true);

        animalSideImage.setFitWidth(60);
        animalSideImage.setFitHeight(60);
        animalSideImage.setPreserveRatio(true);
        animalSideImage.setVisible(false);

        playerHealthBar.setPrefWidth(90);
        playerDamageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 10px;");
        playerDamageLabel.setOpacity(0);

        HBox playerWithAnimal = new HBox(6);
        playerWithAnimal.setAlignment(Pos.BOTTOM_LEFT);
        playerWithAnimal.getChildren().addAll(basePlayerImage, animalSideImage);
        animalSideImage.setTranslateX(6);

        VBox playerPortraitBox = new VBox(6, playerWithAnimal, playerHealthBar, playerDamageLabel);
        playerPortraitBox.setAlignment(Pos.CENTER_LEFT);

        // Ajustes no seletor: menos padding, espaçamento menor e largura máxima para caber antes do boss
        animalSelectionBox.setPadding(new Insets(4));
        animalSelectionBox.setSpacing(6);
        animalSelectionBox.setStyle("-fx-background-color: transparent;");
        animalSelectionBox.setMaxWidth(420); // limitar largura total do seletor (não invade área do boss)

        VBox leftGroup = new VBox(8, playerPortraitBox, animalSelectionBox);
        leftGroup.setAlignment(Pos.BOTTOM_LEFT);
        leftGroup.setPadding(new Insets(0,0,18,18));

        // ---------- Boss (lado direito inferior) ----------
        // Default boss size (will be adjusted for modes via adjustBossSizeForMode)
        bossImage.setFitWidth(260);
        bossImage.setFitHeight(180);
        bossImage.setPreserveRatio(true);
        bossImage.setSmooth(true);
        bossHealthBar.setPrefWidth(140);
        bossDamageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 10px;");
        bossDamageLabel.setOpacity(0);

        VBox bossGroup = new VBox(6, bossImage, bossHealthBar, bossDamageLabel);
        bossGroup.setAlignment(Pos.BOTTOM_RIGHT);
        bossGroup.setPadding(new Insets(0,18,18,0));

        // ---------- Arena ----------
        AnchorPane arenaPane = new AnchorPane();
        AnchorPane.setLeftAnchor(leftGroup, 8.0);
        AnchorPane.setBottomAnchor(leftGroup, 8.0);
        // reserva espaço à direita para o boss para evitar sobreposição
        AnchorPane.setRightAnchor(leftGroup, 320.0);
        AnchorPane.setRightAnchor(bossGroup, 8.0);
        AnchorPane.setBottomAnchor(bossGroup, 8.0);
        arenaPane.getChildren().addAll(leftGroup, bossGroup);

        // ---------- Construção do conteúdo do jogo ----------
        VBox gameContent = new VBox();
        gameContent.getChildren().addAll(topBox, arenaPane);
        VBox.setVgrow(arenaPane, Priority.ALWAYS);
        gameContent.setAlignment(Pos.TOP_CENTER);

        // ---------- Botão de pause (imagem) ----------
        Image pauseIconImg = loadImageOrPlaceholder("/images/icons/pause.png");
        if (pauseIconImg != null) {
            ImageView pauseIcon = new ImageView(pauseIconImg);
            pauseIcon.setFitWidth(28);
            pauseIcon.setFitHeight(28);
            settingsButton.setGraphic(pauseIcon);
            settingsButton.setText("");
        } else {
            settingsButton.setText("❚❚");
            settingsButton.setStyle("-fx-font-size: 12px;");
        }
        settingsButton.setStyle("-fx-background-radius: 10; -fx-padding: 6;");
        settingsButton.setOnAction(e -> showPauseOverlay());

        // ---------- Background de batalha ----------
        this.setStyle("-fx-background-color: transparent;");
        Image bg = loadImageOrPlaceholder("/images/backgrounds/battle_bg.png");
        if (bg == null) bg = loadImageOrPlaceholder("/images/BackgroundLutas.png");
        if (bg != null) {
            ImageView bgView = new ImageView(bg);
            bgView.setPreserveRatio(false);
            bgView.setSmooth(false);
            bgView.setCache(true);
            bgView.setManaged(false);
            bgView.setMouseTransparent(true);
            bgView.fitWidthProperty().bind(gameStack.widthProperty());
            bgView.fitHeightProperty().bind(gameStack.heightProperty());
            gameStack.getChildren().add(0, bgView);
            StackPane.setAlignment(bgView, Pos.CENTER);
        } else {
            gameStack.setStyle("-fx-background-color: linear-gradient(to bottom, #7fe6e6, #6fd6d6);");
        }

        // ---------- Stack + overlays ----------
        gameStack.getChildren().add(gameContent);
        StackPane.setAlignment(gameContent, Pos.TOP_CENTER);

        gameStack.getChildren().add(settingsButton);
        StackPane.setAlignment(settingsButton, Pos.TOP_LEFT);
        StackPane.setMargin(settingsButton, new Insets(8,0,0,8));

        setupPauseOverlay();
        setupCenterMessageLayer();

        gameStack.getChildren().addAll(pauseOverlay, centerMessageLayer);
        StackPane.setAlignment(pauseOverlay, Pos.CENTER);
        StackPane.setAlignment(centerMessageLayer, Pos.CENTER);

        gameStack.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        gameStack.prefWidthProperty().bind(this.widthProperty());
        gameStack.prefHeightProperty().bind(this.heightProperty());

        this.setCenter(gameStack);

        // binds overlays
        pauseOverlay.prefWidthProperty().bind(gameStack.widthProperty());
        pauseOverlay.prefHeightProperty().bind(gameStack.heightProperty());
        centerMessageLayer.prefWidthProperty().bind(gameStack.widthProperty());
        centerMessageLayer.prefHeightProperty().bind(gameStack.heightProperty());

        // listeners / init
        btnSubmit.setOnAction(e -> {}); // será setado por nextTurn
        loadPlaceholders();             // carrega player & placeholder, não sobrescreve boss real
        syncInitialBossImage();         // garante boss correto conforme game.mode/current state
        updateAll();
        updateAnimalSelection();
        startGame();
    }

    private void setupCenterMessageLayer() {
        centerMessageLayer.setPickOnBounds(false);
        centerMessageLayer.setMouseTransparent(true);
        centerMessageLayer.setManaged(false);
    }

    private void setupPauseOverlay() {
        pauseOverlay.setStyle("-fx-background-color: transparent;");
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);

        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(420);
        panel.setPrefHeight(260);

        panel.setStyle(
                "-fx-background-color: linear-gradient(to bottom, rgba(127,230,230,0.95), rgba(111,214,214,0.95));" +
                        "-fx-border-color: #0b3f2b; -fx-border-width: 6;" +
                        "-fx-background-radius: 10; -fx-border-radius: 10;"
        );

        Label title = new Label("PAUSADO");
        title.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #08392b;");

        Button btnResume = new Button("RETOMAR");
        Button btnMenu = new Button("MENU");

        String btnStyle =
                "-fx-font-family: 'Monospaced'; -fx-font-size: 12px; " +
                        "-fx-background-color: linear-gradient(#ffffff, #e6fbff);" +
                        "-fx-border-color: #7fb4c7; -fx-border-width: 2;" +
                        "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 8 14;";

        btnResume.setStyle(btnStyle);
        btnMenu.setStyle(btnStyle);

        btnResume.setPrefWidth(140);
        btnMenu.setPrefWidth(140);

        btnResume.setOnAction(e -> hidePauseOverlay());
        btnMenu.setOnAction(e -> {
            hidePauseOverlay();
            backToMenu();
        });

        panel.getChildren().addAll(title, btnResume, btnMenu);
        pauseOverlay.getChildren().add(panel);
        StackPane.setAlignment(panel, Pos.CENTER);

        panel.prefWidthProperty().bind(gameStack.widthProperty().multiply(0.60));
        panel.prefHeightProperty().bind(gameStack.heightProperty().multiply(0.45));
    }

    private void showPauseOverlay() {
        gameActive = false;
        pauseOverlay.setVisible(true);
        pauseOverlay.setMouseTransparent(false);
        pauseOverlay.setManaged(true);
        pauseOverlay.toFront();
        centerMessageLayer.toFront();
    }

    private void hidePauseOverlay() {
        pauseOverlay.setVisible(false);
        pauseOverlay.setMouseTransparent(true);
        pauseOverlay.setManaged(false);
        gameActive = true;
    }

    // carrega apenas player e placeholder; não pré-carrega boss do outro modo
    private void loadPlaceholders() {
        try {
            Image playerImg = loadImageOrPlaceholder("/images/player.png");
            if (playerImg != null) basePlayerImage.setImage(playerImg);

            Image bossPlaceholder = loadImageOrPlaceholder("/images/bosses/placeholder.png");
            if (bossPlaceholder != null) bossImage.setImage(bossPlaceholder);
        } catch (Exception e) {
            System.err.println("Erro ao carregar imagens: " + e.getMessage());
        }
    }

    // sincroniza a imagem do boss logo após o GamePane ser criado (cobre o caso Game.spawnBoss() já ter sido chamado)
    private void syncInitialBossImage() {
        if (game == null) return;
        String mode = game.mode != null ? game.mode : "Matematica";
        int idx;
        switch (mode) {
            case "Matematica":
                idx = game.animals != null && game.currentAnimal != null ? game.animals.indexOf(game.currentAnimal) + 1 : 1;
                if (idx < 1) idx = 1;
                if (idx > MATEMATICA_BOSSES.length) idx = MATEMATICA_BOSSES.length;
                setBossImageForMode("Matematica", idx);
                break;
            case "Arcade":
                idx = Math.max(1, Math.min(game.arcadeStage + 1, ARCADE_BOSSES.length));
                setBossImageForMode("Arcade", idx);
                break;
            case "Infinito":
                setBossImageForMode("Infinito", 1);
                break;
            default:
                setBossImageForMode("Matematica", 1);
        }
    }

    private Image loadImageOrPlaceholder(String path) {
        if (path == null) return null;
        InputStream in = getClass().getResourceAsStream(path);
        if (in != null) return new Image(in);
        String alt = path.startsWith("/") ? path.substring(1) : "/" + path;
        InputStream in2 = getClass().getResourceAsStream(alt);
        if (in2 != null) return new Image(in2);
        URL res = getClass().getResource(path);
        if (res == null) res = getClass().getResource(alt);
        if (res == null) {
            System.err.println("loadImageOrPlaceholder: recurso não encontrado: " + path + " (tentadas também: " + alt + ")");
            return null;
        }
        try {
            return new Image(res.openStream());
        } catch (Exception ex) {
            System.err.println("Erro ao abrir imagem: " + ex.getMessage());
            return null;
        }
    }

    private void trySetBossImage(String resourcePath) {
        Image img = loadImageOrPlaceholder(resourcePath);
        if (img != null) {
            bossImage.setImage(img);
        } else {
            System.err.println("trySetBossImage: imagem não encontrada em: " + resourcePath);
            Image fallback = loadImageOrPlaceholder("/images/bosses/placeholder.png");
            if (fallback != null) bossImage.setImage(fallback);
        }
    }

    // Ajusta o tamanho do boss conforme o modo (aumenta no modo Infinito)
    private void adjustBossSizeForMode(String mode) {
        if ("Infinito".equalsIgnoreCase(mode)) {
            // maior no modo infinito — escolha valores que cabem na sua tela sem cobrir totalmente a arena
            bossImage.setFitWidth(380);
            bossImage.setFitHeight(260);
            bossHealthBar.setPrefWidth(200);
        } else {
            // padrão/tradicional
            bossImage.setFitWidth(260);
            bossImage.setFitHeight(180);
            bossHealthBar.setPrefWidth(140);
        }
    }

    // MATEMATICA
    public void setBossImageByIndex(int index) {
        if (index < 1 || index > MATEMATICA_BOSSES.length) index = 1;
        String path = MATEMATICA_BOSSES[index - 1];
        trySetBossImage(path);
    }

    // ARCADE (usa array ARCADE_BOSSES)
    public void setBossImageByArcadeIndex(int index) {
        if (index < 1 || index > ARCADE_BOSSES.length) index = 1;
        String path = ARCADE_BOSSES[index - 1];
        trySetBossImage(path);
    }

    // GENERIC — agora ajusta tamanho antes de setar a imagem
    public void setBossImageForMode(String mode, int index) {
        if (mode == null) return;
        adjustBossSizeForMode(mode); // <-- muda o tamanho do boss conforme o modo (Infinito maior)
        switch (mode) {
            case "Matematica" -> setBossImageByIndex(index);
            case "Arcade" -> setBossImageByArcadeIndex(index);
            case "Infinito" -> {
                int idx = Math.max(1, Math.min(index, INFINITO_BOSSES.length));
                trySetBossImage(INFINITO_BOSSES[idx - 1]);
            }
            default -> {}
        }
    }

    public void onModeVictory() {
        if (game == null) return;
        String mode = game.mode != null ? game.mode : "MODO";
        if ("Matematica".equalsIgnoreCase(mode) || "Arcade".equalsIgnoreCase(mode)) {
            showCenteredMessage("VOCÊ VENCEU O MODO " + mode.toUpperCase() + "!", 3.0);
            PauseTransition t = new PauseTransition(Duration.seconds(3.6));
            t.setOnFinished(e -> backToMenu());
            t.play();
        }
    }

    private void updatePlayerPortrait() {
        if (selectedAnimal != null) {
            String imgPath = switch (normalizeName(selectedAnimal.name)) {
                case "coelho" -> "/images/coelho.png";
                case "gato" -> "/images/gato.png";
                case "cao" -> "/images/cachorro.png";
                case "leao" -> "/images/leao.png";
                case "tigre" -> "/images/tigre.png";
                default -> "/images/animals/placeholder.png";
            };
            Image img = loadImageOrPlaceholder(imgPath);
            if (img != null) {
                animalSideImage.setImage(img);
                animalSideImage.setVisible(true);
            }
        } else {
            animalSideImage.setVisible(false);
        }
    }

    private String normalizeName(String s) {
        return s == null ? "" : s.toLowerCase()
                .replace("ã", "a").replace("â", "a").replace("á", "a").replace("à", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                .replace("ú", "u")
                .replace("ç", "c");
    }

    private void backToMenu() {
        VBox menu = mainApp.criarMenu(stage);
        double w = 800, h = 600;
        if (stage.getScene() != null) {
            w = stage.getScene().getWidth();
            h = stage.getScene().getHeight();
        } else {
            w = stage.getWidth() > 0 ? stage.getWidth() : w;
            h = stage.getHeight() > 0 ? stage.getHeight() : h;
        }
        menu.setPrefSize(w, h);
        Scene menuScene = new Scene(menu, w, h);
        stage.setScene(menuScene);
        stage.setTitle("SafeMath 2D - Menu");
    }

    public void log(String text) {
        // intentionally empty to avoid bottom log UI
    }

    public void updateAll() {
        updatePlayerHealth();
        updateBossHealth();
        updatePlayerPortrait();
    }

    public void updatePlayerHealth() {
        if (game.player == null) {
            playerHealthBar.setProgress(1.0);
            return;
        }
        double perc = (double) game.player.getHealth() / game.player.getMaxHealth();
        playerHealthBar.setProgress(Math.max(0, perc));
    }

    public void updateBossHealth() {
        bossHealthBar.setProgress(currentBossProgress());
    }

    private double currentBossProgress() {
        if (game.currentBoss == null) return 0;
        double vidaMax = 1.0;
        switch (game.mode) {
            case "Matematica" -> vidaMax = game.currentAnimal.force * 3.0;
            case "Arcade" -> vidaMax = game.currentBoss.maxHealth;
            case "Infinito" -> vidaMax = 999999;
        }
        return Math.max(0, (double) game.currentBoss.health / vidaMax);
    }

    private void startGame() {
        gameActive = true;
        nextTurn();
    }

    public void disableTurns() {
        gameActive = false;
    }

    public void updateAnimalSelection() {
        animalSelectionBox.getChildren().clear();
        animalSelectionBox.setAlignment(Pos.CENTER_LEFT);

        List<Animal> available = new ArrayList<>();
        if ("Matematica".equals(game.mode)) {
            int index = game.animals.indexOf(game.currentAnimal);
            for (int i = 0; i <= index; i++) available.add(game.animals.get(i));
        } else {
            available.addAll(game.animals);
        }

        if (selectedAnimal == null && !available.isEmpty()) selectedAnimal = available.get(0);
        if (selectedAnimal != null && selectedAnimal.cooldown > 0) {
            lockedAnimal = selectedAnimal;
            selectedAnimal = findPreviousAvailable(selectedAnimal, available);
        }

        for (Animal a : available) {
            Pane card = buildAnimalCard(a);
            animalSelectionBox.getChildren().add(card);
        }

        updatePlayerPortrait();
    }

    // Substitua este método se quiser ajustes adicionais (largura, fonte, badge)
    private Pane buildAnimalCard(Animal a) {
        ImageView iv = new ImageView();
        iv.setFitWidth(44);
        iv.setFitHeight(44);
        iv.setPreserveRatio(true);

        String imgPath = switch (normalizeName(a.name)) {
            case "coelho" -> "/images/coelho.png";
            case "gato" -> "/images/gato.png";
            case "cao" -> "/images/cachorro.png";
            case "leao" -> "/images/leao.png";
            case "tigre" -> "/images/tigre.png";
            default -> "/images/placeholder.png";
        };
        Image img = loadImageOrPlaceholder(imgPath);
        if (img != null) iv.setImage(img);

        // label com apenas o nome (sem "(CD: x)")
        Label name = new Label(a.name);
        // Fonte reduzida para evitar truncamento e estilo consistente
        name.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #0b3f2b;");
        name.setMaxWidth(92); // limita a largura para caber no card
        name.setEllipsisString("...");
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setAlignment(Pos.CENTER);

        // content box (imagem + nome)
        VBox contentBox = new VBox(4, iv, name);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(6, 8, 6, 8));
        contentBox.setPrefWidth(110);
        contentBox.setMaxWidth(110);
        contentBox.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 6; -fx-border-color: #00000022; -fx-border-radius: 6;");

        // stack para permitir posicionar badge no canto superior direito
        StackPane cardStack = new StackPane(contentBox);
        cardStack.setPrefWidth(110);
        cardStack.setMaxWidth(110);

        // cooldown badge (top-right) — aparece somente se cooldown > 0
        if (a.cooldown > 0) {
            Label badge = new Label(String.valueOf(a.cooldown));
            badge.setStyle(
                    "-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;" +
                            "-fx-padding: 2 6; -fx-background-radius: 10;"
            );
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(6, 6, 0, 0));
            cardStack.getChildren().add(badge);
            cardStack.setOpacity(0.75); // levemente esmaecido quando em cooldown
        } else {
            cardStack.setOpacity(1.0);
        }

        // aplica borda azul quando selecionado
        if (selectedAnimal != null && selectedAnimal.equals(a)) {
            cardStack.setStyle("-fx-background-color: transparent; -fx-border-color: #3b82f6; -fx-border-width: 3; -fx-border-radius: 6; -fx-background-radius: 6;");
        }

        // cursor pointer para indicar clicável
        cardStack.setCursor(Cursor.HAND);

        // clique no card: seleciona (se não estiver em cooldown)
        cardStack.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> {
            if (a.cooldown > 0) {
                showCenteredToast(a.name + " em cooldown", 1.0);
                return;
            }
            selectedAnimal = a;
            if (lockedAnimal == a) lockedAnimal = null;
            updateAnimalSelection();   // rebuild cards to update border visuals
            updatePlayerPortrait();    // atualiza animal ao lado do player
            showCenteredToast("Selecionado: " + a.name, 0.9);
        });

        return cardStack;
    }

    private Animal findPreviousAvailable(Animal from, List<Animal> available) {
        int idx = available.indexOf(from);
        if (idx == -1) {
            return game.currentAnimal;
        }
        for (int i = idx - 1; i >= 0; i--) {
            Animal a = available.get(i);
            if (a.cooldown == 0) return a;
        }
        for (int i = idx + 1; i < available.size(); i++) {
            Animal a = available.get(i);
            if (a.cooldown == 0) return a;
        }
        return game.currentAnimal != null ? game.currentAnimal : available.get(0);
    }

    private void checkLockedAnimalRelease() {
        if (lockedAnimal == null) return;
        if (lockedAnimal.cooldown <= 0) {
            selectedAnimal = lockedAnimal;
            lockedAnimal = null;
            updateAnimalSelection();
            updatePlayerPortrait();
        }
    }

    public void showCenteredMessage(String msg, double seconds) {
        Label label = new Label(msg);
        label.setStyle(
                "-fx-background-color: rgba(0,0,0,0.85); -fx-text-fill: white; -fx-padding: 18 28; " +
                        "-fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold; -fx-alignment: center;"
        );
        label.setWrapText(true);
        label.setOpacity(0);

        centerMessageLayer.getChildren().add(label);
        StackPane.setAlignment(label, Pos.CENTER);

        centerMessageLayer.setMouseTransparent(false);
        centerMessageLayer.setManaged(true);
        centerMessageLayer.toFront();
        label.toFront();

        FadeTransition in = new FadeTransition(Duration.millis(220), label);
        in.setFromValue(0);
        in.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.seconds(seconds));

        FadeTransition out = new FadeTransition(Duration.millis(240), label);
        out.setFromValue(1);
        out.setToValue(0);
        out.setOnFinished(ev -> {
            centerMessageLayer.getChildren().remove(label);
            if (centerMessageLayer.getChildren().isEmpty()) {
                centerMessageLayer.setMouseTransparent(true);
                centerMessageLayer.setManaged(false);
            }
        });

        in.setOnFinished(ev -> hold.play());
        hold.setOnFinished(ev -> out.play());
        in.play();
    }

    public void showCenteredToast(String msg, double seconds) {
        Label label = new Label(msg);
        label.setStyle(
                "-fx-background-color: rgba(0,0,0,0.75); -fx-text-fill: white; -fx-padding: 10 16; " +
                        "-fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: bold;"
        );
        label.setWrapText(true);
        label.setOpacity(0);

        centerMessageLayer.getChildren().add(label);
        StackPane.setAlignment(label, Pos.CENTER);

        centerMessageLayer.setMouseTransparent(true);
        centerMessageLayer.setManaged(true);
        centerMessageLayer.toFront();
        label.toFront();

        FadeTransition in = new FadeTransition(Duration.millis(160), label);
        in.setFromValue(0);
        in.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.seconds(seconds));

        FadeTransition out = new FadeTransition(Duration.millis(200), label);
        out.setFromValue(1);
        out.setToValue(0);
        out.setOnFinished(ev -> {
            centerMessageLayer.getChildren().remove(label);
            if (centerMessageLayer.getChildren().isEmpty()) {
                centerMessageLayer.setMouseTransparent(true);
                centerMessageLayer.setManaged(false);
            }
        });

        in.setOnFinished(ev -> hold.play());
        hold.setOnFinished(ev -> out.play());
        in.play();
    }

    private String formatAnswer(double a) {
        if (Math.abs(a - Math.round(a)) < 0.0001) return String.valueOf((long)Math.round(a));
        return String.format("%.3f", a).replace(',', '.');
    }

    private void nextTurn() {
        if (!gameActive) return;
        if (!game.player.isAlive() || !game.currentBoss.isAlive()) return;

        String turno = game.isPlayerTurn() ? "ATAQUE" : "DEFESA";
        turnoLabel.setText(turno);

        Question q = game.qGen.generate(game.difficultyForMode());
        this.currentQuestion = q;

        questionLabel.setText(currentQuestion.text);

        answerField.clear();
        answerField.requestFocus();

        btnSubmit.setOnAction(e -> processAnswer(this.currentQuestion));
    }

    private void processAnswer(Question q) {
        if (q == null) return;

        String textRaw = answerField.getText();
        if (textRaw == null) textRaw = "";
        String normalized = textRaw.trim().replace(',', '.');

        boolean correct = false;
        try {
            double userAns = Double.parseDouble(normalized);
            correct = Math.abs(userAns - q.answer) < 0.001;
        } catch (NumberFormatException ignored) {
            correct = false;
        }

        game.qGen.recordAnswer(correct);

        if (game.isPlayerTurn()) {
            if (selectedAnimal == null) selectedAnimal = game.currentAnimal;

            if (selectedAnimal.canAttack()) {
                if (correct) {
                    int dmg = selectedAnimal.force;

                    boolean bossDied = false;
                    if (game.currentBoss != null) {
                        game.currentBoss.takeDamage(dmg);
                        bossDied = !game.currentBoss.isAlive();
                    }

                    showBossDamage(dmg);
                    updateAll();

                    selectedAnimal.cooldown = Math.max(1, (int) Math.round(selectedAnimal.force * 0.1));
                    if (selectedAnimal.cooldown > 0) {
                        lockedAnimal = selectedAnimal;
                        List<Animal> available = new ArrayList<>();
                        if ("Matematica".equals(game.mode)) {
                            int index = game.animals.indexOf(game.currentAnimal);
                            for (int i = 0; i <= index; i++) available.add(game.animals.get(i));
                        } else {
                            available.addAll(game.animals);
                        }
                        selectedAnimal = findPreviousAvailable(lockedAnimal, available);
                    }

                    if (bossDied) {
                        // Ajuste: quando o chefe morre, atualizamos o estado (spawn do próximo chefe pode ocorrer em checkBossDefeat)
                        // e garantimos que a pergunta mude imediatamente para evitar a mesma questão repetida.
                        game.checkBossDefeat();
                        updateAll();

                        // Se o jogo não acabou / não voltou ao menu, geramos uma nova questão adequada ao novo estado.
                        if (game.currentBoss != null && game.player.isAlive() && gameActive) {
                            Question newQ = game.qGen.generate(game.difficultyForMode());
                            this.currentQuestion = newQ;
                            questionLabel.setText(newQ.text);
                            answerField.clear();
                            answerField.requestFocus();
                            btnSubmit.setOnAction(e -> processAnswer(this.currentQuestion));
                        }
                        return;
                    }
                } else {
                    showCenteredMessage("ERRADO!\nResposta correta: " + formatAnswer(q.answer), 1.8);
                }
            }
            game.decrementAllCooldowns();
            checkLockedAnimalRelease();
            updateAnimalSelection();
        } else {
            int maxDmg = switch (game.mode) {
                case "Arcade" -> 50 + game.arcadeStage * 50;
                case "Infinito" -> 20;
                default -> 50;
            };
            int dmg = game.getRandomDamage(maxDmg);

            if (correct) {
                showCenteredMessage("DEFESA PERFEITA!", 1.2);
            } else {
                game.player.takeDamage(dmg);
                showPlayerDamage(dmg);
                showCenteredMessage("DEFESA FALHOU! Recebeu " + dmg + " de dano.\nResposta correta: " + formatAnswer(q.answer), 2.2);

                if (!game.player.isAlive()) {
                    showCenteredMessage("DERROTADO", 3.0);
                    disableTurns();
                    PauseTransition t = new PauseTransition(Duration.seconds(3.6));
                    t.setOnFinished(ev -> backToMenu());
                    t.play();
                    return;
                }
            }
        }

        updateAll();
        game.togglePlayerTurn();
        nextTurn();
    }

    private void showBossDamage(int dmg) {
        String text = "-" + dmg;
        bossDamageLabel.setText(text);
        bossDamageLabel.setOpacity(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(900), bossDamageLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(Duration.millis(300));
        fade.setOnFinished(e -> bossDamageLabel.setText(""));
        fade.play();
    }

    private void showPlayerDamage(int dmg) {
        String text = "-" + dmg;
        playerDamageLabel.setText(text);
        playerDamageLabel.setOpacity(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(900), playerDamageLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(Duration.millis(300));
        fade.setOnFinished(e -> playerDamageLabel.setText(""));
        fade.play();
    }
}