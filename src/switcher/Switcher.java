package switcher;

import static switcher.DataHelper.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import switcher.Configurations.ConfigEntry;
import switcher.Configurations.GameEntry;

public class Switcher extends Application
{
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    public static Configurations configurations;
    
    private static FileChooser fileChooser = new FileChooser();
    private static DirectoryChooser dirChooser = new DirectoryChooser();

    private static ListView<String> gamesList = new ListView<String>();
    private static ListView<String> configurationsList = new ListView<String>();

    private static CheckBox debuggerBox = new CheckBox("Enable debugger");
    private static CheckBox breakOnEntryBox = new CheckBox("Break on entry");
    private static CheckBox forceSyncBox = new CheckBox("Enable force sync");
    private static CheckBox jitBox = new CheckBox("Enable JIT");
    private static CheckBox jitDebugBox = new CheckBox("Enable debug");
    private static CheckBox asyncLogBox = new CheckBox("Enable async");
    private static CheckBox toFile = new CheckBox("Write to file");
    private static CheckBox toStdOut = new CheckBox("Write to std out");
    private static CheckBox kernelTrace = new CheckBox("Enable kernel tracing");
    private static CheckBox branchTrace = new CheckBox("Enable branch tracing");

    private static TextField dirField = new TextField();
    private static TextField gamePathField = new TextField();

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        String configurationsText = readConfigurationsFile();
        
        // Read the configurations from our file
        if (configurationsText.isEmpty())
        {
            this.configurations = new Configurations();
        } else
        {
            this.configurations = GSON.fromJson(configurationsText, Configurations.class);
            decafConfigFile = new File(this.configurations.decafConfigPath);
            dirField.setText(decafConfigFile.getAbsolutePath());
        }

        ObservableList<String> gamesData = FXCollections.observableArrayList(this.configurations.gameEntries.keySet());
        ObservableList<String> configurationsData = FXCollections.observableArrayList(this.configurations.configEntries.keySet());

        Button browseBtn = new Button();
        browseBtn.setText("Browse");
        browseBtn.setAlignment(Pos.CENTER_RIGHT);
        browseBtn.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                decafConfigFile = fileChooser.showOpenDialog(primaryStage);

                // There may be no file selected
                if (decafConfigFile != null)
                {
                    dirField.setText(decafConfigFile.getAbsolutePath());
                    configurations.decafConfigPath = decafConfigFile.getAbsolutePath();
                    saveStateToFiles();
                }
            }
        });
        
        // Lists and options boxes in the top row

        VBox leftPane = new VBox(5);
        leftPane.setPadding(new Insets(5, 5, 0, 10));
        addLeftPaneElements(leftPane, primaryStage);
        
        gamesList.setItems(gamesData);
        configurationsList.setItems(configurationsData);
        
        VBox rightPane = new VBox(5);
        rightPane.setPadding(new Insets(5, 5, 0, 10));
        addRightPaneElements(rightPane);
        
        VBox.setVgrow(gamesList, Priority.ALWAYS);
        VBox.setVgrow(configurationsList, Priority.ALWAYS);
        
        HBox upperRow = new HBox(2);
        upperRow.getChildren().addAll(leftPane, rightPane);

        // Directory row on the bottom
        
        Label dirLabel = new Label();
        dirLabel.setText("Config path: ");
        dirLabel.setFont(Font.font(14));
              
        HBox dirRow = new HBox(2);
        dirRow.setPadding(new Insets(5, 2, 0, 2));
        HBox.setHgrow(dirField, Priority.ALWAYS);
        dirRow.getChildren().addAll(dirLabel, dirField, browseBtn);
        
        VBox box = new VBox(2);
        box.getChildren().addAll(upperRow, dirRow);
        VBox.setVgrow(upperRow, Priority.ALWAYS);

        Scene scene = new Scene(box, 700, 500);

        primaryStage.setTitle("Decaf Config Switcher");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    private void updateOptionsForSelection()
    {
        ConfigEntry selectedEntry = getSelectedConfigEntry();

        debuggerBox.setSelected(selectedEntry.debugger.enabled);
        breakOnEntryBox.setSelected(selectedEntry.debugger.breakOnEntry);
        gamePathField.setText(selectedEntry.game.path);
        forceSyncBox.setSelected(selectedEntry.gpu.forceSync);
        jitBox.setSelected(selectedEntry.jit.enabled);
        jitDebugBox.setSelected(selectedEntry.jit.debug);
        asyncLogBox.setSelected(selectedEntry.log.async);
        toFile.setSelected(selectedEntry.log.toFile);
        toStdOut.setSelected(selectedEntry.log.toStdout);
        kernelTrace.setSelected(selectedEntry.log.kernelTrace);
        branchTrace.setSelected(selectedEntry.log.branchTrace);
    }

    public static ConfigEntry getSelectedConfigEntry()
    {
        String selectedItem = configurationsList.getSelectionModel().getSelectedItem();
        return selectedItem != null ? configurations.getOrCreateConfigEntry(selectedItem) : null;
    }
    
    private void addLeftPaneElements(VBox leftPane, Stage stage)
    {
        //
        // Games list context menu
        //
        
        ContextMenu gameContextMenu = new ContextMenu();
        
        TextInputDialog addGameDialog = new TextInputDialog();
        addGameDialog.setTitle("Add configuration");
        addGameDialog.setHeaderText("Please enter your game's name");
        addGameDialog.setContentText("Game name:");
        
        MenuItem addGameMenuItem = new MenuItem("Add");
        addGameMenuItem.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                Optional<String> result = addGameDialog.showAndWait();

                if (result.isPresent())
                {
                    File gamePath = dirChooser.showDialog(stage);
                    
                    //The user may have closed the chooser without selecting a file
                    if (gamePath != null)
                    {
                        GameEntry entry = new GameEntry(gamePath.getAbsolutePath());

                        configurations.gameEntries.put(result.get(), entry);
                        gamesList.getItems().add(result.get());
                        saveStateToFiles();
                    }
                }
            }
        });

        MenuItem removeGameMenuItem = new MenuItem("Remove");
        removeGameMenuItem.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                String selectedItem = gamesList.getSelectionModel().getSelectedItem();

                if (gamesList.getSelectionModel().getSelectedItem() != null)
                {
                    configurations.gameEntries.remove(selectedItem);
                    gamesList.getItems().remove(gamesList.getSelectionModel().getSelectedItem());
                    saveStateToFiles();
                }
            }
        });
        gameContextMenu.getItems().addAll(addGameMenuItem, removeGameMenuItem);

        gamesList.setContextMenu(gameContextMenu);
        gamesList.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                if (gamesList.getSelectionModel().getSelectedItem() != null && getSelectedConfigEntry() != null)
                {
                    String selectedGame = gamesList.getSelectionModel().getSelectedItem();
                    String path = configurations.gameEntries.get(selectedGame).path;
                    
                    gamePathField.setText(path);
                    getSelectedConfigEntry().game.path = path;
                    saveStateToFiles();
                }
            }
        });
        
        
        //
        // Config list context menu
        //
        
        ContextMenu configContextMenu = new ContextMenu();
        
        TextInputDialog addConfigDialog = new TextInputDialog();
        addConfigDialog.setTitle("Add configuration");
        addConfigDialog.setHeaderText("Please enter your configuration name");
        addConfigDialog.setContentText("Configuration name:");
        
        MenuItem addConfigMenuItem = new MenuItem("Add");
        addConfigMenuItem.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                Optional<String> result = addConfigDialog.showAndWait();

                if (result.isPresent())
                {
                    configurations.configEntries.put(result.get(), new ConfigEntry());
                    configurationsList.getItems().add(result.get());
                    saveStateToFiles();
                }
            }
        });

        MenuItem removeConfigMenuItem = new MenuItem("Remove");
        removeConfigMenuItem.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                String selectedItem = configurationsList.getSelectionModel().getSelectedItem();

                if (configurationsList.getSelectionModel().getSelectedItem() != null)
                {
                    configurations.configEntries.remove(selectedItem);
                    configurationsList.getItems().remove(configurationsList.getSelectionModel().getSelectedItem());
                    saveStateToFiles();
                }
            }
        });
        configContextMenu.getItems().addAll(addConfigMenuItem, removeConfigMenuItem);

        configurationsList.setContextMenu(configContextMenu);
        configurationsList.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                if (configurationsList.getSelectionModel().getSelectedItem() != null)
                {
                    updateOptionsForSelection();
                    saveStateToFiles();
                }
            }
        });
        
        Label gamesLabel = new Label();
        gamesLabel.setText("Games: ");
        gamesLabel.setFont(Font.font(14));
        
        Label configurationsLabel = new Label();
        configurationsLabel.setText("Configurations: ");
        configurationsLabel.setFont(Font.font(14));

        leftPane.getChildren().addAll(gamesLabel, gamesList, configurationsLabel, configurationsList);
    }

    private void addRightPaneElements(VBox rightPane)
    {
        debuggerBox.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().debugger.enabled = debuggerBox.isSelected();
                    saveStateToFiles();
                }
            }
        });

        breakOnEntryBox.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().debugger.breakOnEntry = breakOnEntryBox.isSelected();
                    saveStateToFiles();
                }
            }
        });

        forceSyncBox.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().gpu.forceSync = forceSyncBox.isSelected();
                    saveStateToFiles();
                }
            }
        });

        jitBox.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().jit.enabled = jitBox.isSelected();
                    saveStateToFiles();
                }
            }
        });

        jitDebugBox.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().jit.debug = jitDebugBox.isSelected();
                    saveStateToFiles();
                }
            }
        });

        asyncLogBox.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().log.async = asyncLogBox.isSelected();
                    saveStateToFiles();
                }
            }
        });

        toFile.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().log.toFile = toFile.isSelected();
                    saveStateToFiles();
                }
            }
        });

        toStdOut.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().log.toStdout = toStdOut.isSelected();
                    saveStateToFiles();
                }
            }
        });

        kernelTrace.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().log.kernelTrace = kernelTrace.isSelected();
                    saveStateToFiles();
                }
            }
        });

        branchTrace.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                if (getSelectedConfigEntry() != null)
                {
                    getSelectedConfigEntry().log.branchTrace = branchTrace.isSelected();
                    saveStateToFiles();
                }
            }
        });
        
        //
        // Section labels for the various options
        //
        
        Label debuggerOptionsLabel = new Label();
        debuggerOptionsLabel.setText("Debugger Options");
        debuggerOptionsLabel.setFont(Font.font(14));
        debuggerOptionsLabel.setPadding(new Insets(5, 0, 0, 0));

        // Game options elements
        
        Label gameOptionsLabel = new Label();
        gameOptionsLabel.setText("Game Options");
        gameOptionsLabel.setFont(Font.font(14));
        gameOptionsLabel.setPadding(new Insets(5, 0, 0, 0));
        
        Label gamePathLabel = new Label();
        gamePathLabel.setText("Path: ");
        gamePathLabel.setFont(Font.font(12));
        gamePathLabel.setPadding(new Insets(5, 0, 0, 0));
        
        gamePathField.setEditable(false);

        
        HBox gamePathBox = new HBox();
        gamePathBox.getChildren().addAll(gamePathLabel, gamePathField);
        
        // GPU options elements
        
        Label gpuOptionsLabel = new Label();
        gpuOptionsLabel.setText("GPU Options");
        gpuOptionsLabel.setFont(Font.font(14));
        gpuOptionsLabel.setPadding(new Insets(5, 0, 0, 0));

        Label jitOptionsLabel = new Label();
        jitOptionsLabel.setText("JIT Options");
        jitOptionsLabel.setFont(Font.font(14));
        jitOptionsLabel.setPadding(new Insets(5, 0, 0, 0));

        Label loggingOptionsLabel = new Label();
        loggingOptionsLabel.setText("Logging Options");
        loggingOptionsLabel.setFont(Font.font(14));
        loggingOptionsLabel.setPadding(new Insets(5, 0, 0, 0));

        branchTrace.setPadding(new Insets(0, 0, 5, 0));

        rightPane.getChildren().addAll(debuggerOptionsLabel, debuggerBox, breakOnEntryBox);
        rightPane.getChildren().addAll(gameOptionsLabel, gamePathBox);
        rightPane.getChildren().addAll(gpuOptionsLabel, forceSyncBox);
        rightPane.getChildren().addAll(jitOptionsLabel, jitBox, jitDebugBox);
        rightPane.getChildren().addAll(loggingOptionsLabel, asyncLogBox, toFile, toStdOut, kernelTrace, branchTrace);
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
