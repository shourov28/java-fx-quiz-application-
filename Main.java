package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends Application {
    private List<Question> questions;
    private List<Integer> selectedOptions; // Store selected options for each question
    private int currentIndex;
    private ToggleGroup optionsGroup;
    private int correctAnswersCounter = 0;

    private Map<String, String> credentials = new HashMap<>(); // Map to store username-password pairs

    @Override
    public void start(Stage primaryStage) {
        // Initialize credentials for admin and user
        credentials.put("admin", "1234");
        credentials.put("user", "1234");

        // Login screen
        VBox loginBox = new VBox(10);
        loginBox.setPadding(new javafx.geometry.Insets(20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Button loginButton = new Button("Login");

        Label loginMessage = new Label();

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (authenticate(username, password)) {
                if (username.equals("admin")) {
                    // Admin panel
                    primaryStage.setScene(adminPanel(primaryStage));
                } else {
                    // User panel
                    primaryStage.setScene(userPanel(primaryStage));
                }
            } else {
                loginMessage.setText("Invalid username or password.");
            }
        });

        loginBox.getChildren().addAll(usernameField, passwordField, loginButton, loginMessage);

        Scene loginScene = new Scene(loginBox, 300, 200);

        primaryStage.setScene(loginScene);
        primaryStage.setTitle("Login");
        primaryStage.show();
    }

 // Admin panel
    private Scene adminPanel(Stage primaryStage) {
        VBox root = new VBox(10);

        Label questionLabel = new Label("Admin Panel - All Questions");
        TextArea questionTextArea = new TextArea();
        questionTextArea.setEditable(false);
        Button backButton = new Button("Back to Login");

        try (BufferedReader reader = new BufferedReader(new FileReader("resources/questions.txt"))) {
            StringBuilder allQuestions = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                allQuestions.append(line).append("\n");
            }
            questionTextArea.setText(allQuestions.toString());
        } catch (IOException e) {
            questionTextArea.setText("Error: Failed to load questions from file.");
            e.printStackTrace();
        }

        backButton.setOnAction(e -> primaryStage.setScene(new Scene(new VBox(), 300, 200))); // Go back to login screen

        root.getChildren().addAll(questionLabel, questionTextArea, backButton);

        return new Scene(root, 600, 400);
    }

    // User panel
    private Scene userPanel(Stage primaryStage) {
        VBox root = new VBox(10);

        Label questionLabel = new Label();
        optionsGroup = new ToggleGroup();
        VBox optionsBox = new VBox();
        Label scoreLabel = new Label();
        Button nextButton = new Button("Next");
        Button previousButton = new Button("Previous");
        Button submitButton = new Button("Submit");

        nextButton.setOnAction(e -> moveToNextQuestion(optionsGroup, optionsBox, questionLabel, scoreLabel, nextButton, previousButton, submitButton));
        previousButton.setOnAction(e -> moveToPreviousQuestion(optionsGroup, optionsBox, questionLabel, scoreLabel, nextButton, previousButton, submitButton));
        submitButton.setOnAction(e -> submitQuiz(scoreLabel)); // Add action for Submit button

        root.getChildren().addAll(questionLabel, optionsBox, scoreLabel, nextButton, previousButton, submitButton);

        try {
            loadQuestions("resources/questions.txt", optionsBox);
        } catch (IOException e) {
            e.printStackTrace();
            Platform.exit();
            return null;
        }

        displayCurrentQuestion(questionLabel, optionsBox, optionsGroup, nextButton, previousButton);

        Scene scene = new Scene(root, 500, 500);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("User Panel");
        primaryStage.show();

        return scene;
    }

    // Load questions
    private void loadQuestions(String filePath, VBox optionsBox) throws IOException {
        questions = new ArrayList<>();
        selectedOptions = new ArrayList<>(); // Initialize selected options list
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int optionIndex = 0;
            while ((line = reader.readLine()) != null) {
                String questionText = line;
                List<String> options = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    if ((line = reader.readLine()) != null) {
                        options.add(line);
                    } else {
                        throw new IOException("Unexpected end of file while reading options.");
                    }
                }
                if ((line = reader.readLine()) != null) {
                    System.out.println("Correct answer index line: " + line);
                    try {
                        int correctAnswerIndex = Integer.parseInt(line.trim());
                        Question question = new Question(questionText, options, correctAnswerIndex);
                        questions.add(question);

                        for (int i = 0; i < options.size(); i++) {
                            RadioButton optionButton = new RadioButton(options.get(i));
                            optionButton.setToggleGroup(optionsGroup);
                            optionsBox.getChildren().add(optionButton);
                        }
                    } catch (NumberFormatException e) {
                        throw new IOException("Correct answer index is not a valid integer: " + line);
                    }
                } else {
                    throw new IOException("Unexpected end of file while reading correct answer index.");
                }
            }
        }
    }

    // Display current question
    private void displayCurrentQuestion(Label questionLabel, VBox optionsBox, ToggleGroup optionsGroup, Button nextButton, Button previousButton) {
        Question question = questions.get(currentIndex);
        questionLabel.setText(question.getQuestionText());

        optionsBox.getChildren().clear();
        optionsGroup.getToggles().clear();

        List<String> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            RadioButton optionButton = new RadioButton(options.get(i));
            optionButton.setToggleGroup(optionsGroup);
            optionsBox.getChildren().add(optionButton);
            // Check if this option was selected before
            if (selectedOptions.size() > currentIndex && selectedOptions.get(currentIndex) == i) {
                optionButton.setSelected(true);
            }
        }

        nextButton.setDisable(currentIndex == questions.size() - 1);
        previousButton.setDisable(currentIndex == 0);
    }

    // Move to the next question
    private void moveToNextQuestion(ToggleGroup optionsGroup, VBox optionsBox, Label questionLabel, Label scoreLabel, Button nextButton, Button previousButton, Button submitButton) {
        RadioButton selectedOption = (RadioButton) optionsGroup.getSelectedToggle();
        if (selectedOption == null) {
            scoreLabel.setText("Please select an option before proceeding.");
            return;
        }

        int selectedIndex = optionsBox.getChildren().indexOf(selectedOption);

        if (selectedIndex == questions.get(currentIndex).getCorrectAnswerIndex()) {
            scoreLabel.setText("Correct! " + selectedOption.getText() + " is the right answer.");
            correctAnswersCounter++;
        } else {
            scoreLabel.setText("Incorrect! The correct answer is: " + questions.get(currentIndex).getOptions().get(questions.get(currentIndex).getCorrectAnswerIndex()));
        }

        // Store selected option
        selectedOptions.add(currentIndex, selectedIndex);

        // Check if this is the last question
        if (currentIndex == questions.size() - 1) {
            // Calculate score and display it
            submitQuiz(scoreLabel);
            nextButton.setDisable(true);
            previousButton.setDisable(true);
            submitButton.setDisable(true);
        } else {
            currentIndex++;
            displayCurrentQuestion(questionLabel, optionsBox, optionsGroup, nextButton, previousButton);
        }
    }

    // Move to the previous question
    private void moveToPreviousQuestion(ToggleGroup optionsGroup, VBox optionsBox, Label questionLabel, Label scoreLabel, Button nextButton, Button previousButton, Button submitButton) {
        // Store selected option before moving to the previous question
        RadioButton selectedOption = (RadioButton) optionsGroup.getSelectedToggle();
        if (selectedOption != null) {
            int selectedIndex = optionsBox.getChildren().indexOf(selectedOption);
            selectedOptions.set(currentIndex, selectedIndex);
        }

        currentIndex--;
        displayCurrentQuestion(questionLabel, optionsBox, optionsGroup, nextButton, previousButton);
    }

    // Submit the quiz
    private void submitQuiz(Label scoreLabel) {
        scoreLabel.setText("Final Score: " + correctAnswersCounter + " out of " + questions.size());
    }

    // Authentication method
    private boolean authenticate(String username, String password) {
        return credentials.containsKey(username) && credentials.get(username).equals(password);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Question {
    private String questionText;
    private List<String> options;
    private int correctAnswerIndex;

    public Question(String questionText, List<String> options, int correctAnswerIndex) {
        this.questionText = questionText;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }
}
