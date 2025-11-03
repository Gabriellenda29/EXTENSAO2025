package com.safemath.game;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    // Família da fonte carregada — inicializada em start(...)
    private String globalFontFamily = null;

    @Override
    public void start(Stage primaryStage) {
        // Tente carregar a fonte global antes de construir a UI para que todos os controles usem ela
        loadGlobalFont("/fonte/ARCADE_N.TTF"); // coloque sua fonte em src/main/resources/fonte/ARCADE_N.TTF (mude o nome se necessário)

        VBox menu = criarMenu(primaryStage);

        Scene scene = new Scene(menu, 800, 600);
        applyGlobalFontAndStyles(scene);

        primaryStage.setTitle("SafeMath 2D - Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadGlobalFont(String resourcePath) {
        try {
            if (getClass().getResourceAsStream(resourcePath) != null) {
                Font loaded = Font.loadFont(getClass().getResourceAsStream(resourcePath), 12);
                if (loaded != null) {
                    globalFontFamily = loaded.getFamily();
                    System.out.println("Fonte carregada: " + globalFontFamily + " (via " + resourcePath + ")");
                    return;
                }
            } else {
                System.err.println("Fonte não encontrada em resourcePath: " + resourcePath);
            }
        } catch (Exception ex) {
            System.err.println("Erro ao carregar fonte: " + ex.getMessage());
        }
        // fallback
        globalFontFamily = "Consolas";
        System.out.println("Usando fonte fallback: " + globalFontFamily);
    }


    private void applyGlobalFontAndStyles(Scene scene) {
        if (scene == null) return;
        if (globalFontFamily != null) {
            // Aplica apenas a família como estilo direto na root para garantir herança (sem forçar o tamanho)
            scene.getRoot().setStyle("-fx-font-family: '" + globalFontFamily + "';");
        }
        // Tenta carregar stylesheet opcional
        try {
            if (getClass().getResource("/styles/global.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/styles/global.css").toExternalForm());
            } else {
                // opcional: informe que o CSS não foi encontrado
                System.err.println("Aviso: /styles/global.css não encontrado (estilos globais não aplicados).");
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar global.css: " + e.getMessage());
        }
    }

    public VBox criarMenu(Stage stage) {
        VBox menu = new VBox(20);
        menu.setAlignment(Pos.CENTER);

        // Fundo
        BackgroundImage backgroundImage = new BackgroundImage(
                new Image(getClass().getResource("/images/SafeMath.png").toExternalForm(), 800, 600, false, true),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT
        );
        menu.setBackground(new Background(backgroundImage));

        // Setas
        ImageView setaMatematica = criarSeta();
        ImageView setaArcade = criarSeta();
        ImageView setaInfinito = criarSeta();

        // Botões
        HBox btnMatematicaBox = criarBotaoComSeta("MATEMÁTICA", setaMatematica);
        HBox btnArcadeBox = criarBotaoComSeta("ARCADE", setaArcade);
        HBox btnInfinitoBox = criarBotaoComSeta("INFINITO", setaInfinito);

        menu.getChildren().addAll(btnMatematicaBox, btnArcadeBox, btnInfinitoBox);

        // Eventos
        javafx.scene.control.Button btnMatematica = (javafx.scene.control.Button) btnMatematicaBox.getChildren().get(1);
        javafx.scene.control.Button btnArcade = (javafx.scene.control.Button) btnArcadeBox.getChildren().get(1);
        javafx.scene.control.Button btnInfinito = (javafx.scene.control.Button) btnInfinitoBox.getChildren().get(1);

        btnMatematica.setOnAction(e -> startGame(stage, "Matematica"));
        btnArcade.setOnAction(e -> startGame(stage, "Arcade"));
        btnInfinito.setOnAction(e -> startGame(stage, "Infinito"));

        return menu;
    }

    private javafx.scene.control.Button criarBotao(String texto) {
        javafx.scene.control.Button botao = new javafx.scene.control.Button(texto);
        // usa a fonte global se disponível, senão Consolas
        String family = (globalFontFamily != null) ? globalFontFamily : "Consolas";
        // reduzi o tamanho para 14 (mais próximo do padrão anterior e menos "grande")
        botao.setFont(Font.font(family, 12));
        botao.setTextFill(Color.WHITE);
        botao.setStyle("-fx-background-color: green; -fx-background-radius: 8; -fx-padding: 10 20;");
        return botao;
    }

    private ImageView criarSeta() {
        ImageView seta = new ImageView(new Image(getClass().getResource("/images/seta.png").toExternalForm()));
        seta.setFitWidth(24);
        seta.setFitHeight(24);
        seta.setVisible(false);
        return seta;
    }

    private HBox criarBotaoComSeta(String texto, ImageView seta) {
        javafx.scene.control.Button botao = criarBotao(texto);

        HBox hbox = new HBox(10, seta, botao);
        hbox.setAlignment(Pos.CENTER); // Centraliza seta + botão no meio da tela

        botao.setOnMouseEntered(e -> seta.setVisible(true));
        botao.setOnMouseExited(e -> seta.setVisible(false));

        return hbox;
    }


    private void startGame(Stage stage, String mode) {
        Game game = new Game();
        game.mode = mode;
        GamePane pane = new GamePane(game, this, stage); // passa referência do Main
        game.setPane(pane);

        Scene scene = new Scene(pane, 800, 600);
        applyGlobalFontAndStyles(scene);

        stage.setTitle("SafeMath 2D - " + mode);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}