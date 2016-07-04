package switcher;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import switcher.Configurations.ConfigEntry;

public class Switcher extends Application
{
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	private static final Path CONFIGURATIONS_PATH = Paths.get(ClassLoader.getSystemClassLoader().getResource(".").getPath().substring(1).replace("%20", " ") + "configurations.json");
	
	private File decafConfigFile = null;
	private Configurations configurations;
	
	private ListView<String> list = new ListView<String>();
	
	private CheckBox debuggerBox = new CheckBox("Enable debugger");
	private CheckBox breakOnEntryBox = new CheckBox("Break on entry");
	private CheckBox forceSyncBox = new CheckBox("Enable force sync");
	private CheckBox jitBox = new CheckBox("Enable JIT");
	private CheckBox jitDebugBox = new CheckBox("Enable debug");
	private CheckBox asyncLogBox = new CheckBox("Enable async");
	private CheckBox toFile = new CheckBox("Write to file");
	private CheckBox toStdOut = new CheckBox("Write to std out");
	private CheckBox kernelTrace = new CheckBox("Enable kernel tracing");
	private CheckBox branchTrace = new CheckBox("Enable branch tracing");
	
	private TextField dirField = new TextField();
	
	@Override
	public void start(Stage primaryStage) throws Exception 
	{
		String configurationsText = readConfigurationsFile();
		
		//Read the configurations from our file
		if (configurationsText.isEmpty())
		{
			this.configurations = new Configurations();
		}
		else
		{
			this.configurations = GSON.fromJson(configurationsText, Configurations.class);
			decafConfigFile = new File(this.configurations.decafConfigPath);
			dirField.setText(decafConfigFile.getAbsolutePath());
		}
		
		ContextMenu listContextMenu = new ContextMenu();
		
		TextInputDialog addDialog = new TextInputDialog();
		addDialog.setTitle("Add configuration");
		addDialog.setHeaderText("Please enter your configuration name");
		addDialog.setContentText("Configuration name:");

		MenuItem addMenuItem = new MenuItem("Add");
		addMenuItem.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				Optional<String> result = addDialog.showAndWait();
				
				if (result.isPresent())
				{
					configurations.configEntries.put(result.get(), new ConfigEntry());
					list.getItems().add(result.get());
					saveStateToFiles();
				}
			} 
		});
		
		MenuItem removeMenuItem = new MenuItem("Remove");
		removeMenuItem.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				String selectedItem = list.getSelectionModel().getSelectedItem();
				
				if (list.getSelectionModel().getSelectedItem() != null)
				{
					configurations.configEntries.remove(selectedItem);
					list.getItems().remove(list.getSelectionModel().getSelectedItem());
					saveStateToFiles();
				}
			} 
		});
		listContextMenu.getItems().addAll(addMenuItem, removeMenuItem);
		
		list.setContextMenu(listContextMenu);
		list.setOnMouseClicked(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event) 
			{
				if (list.getSelectionModel().getSelectedItem() != null)
				{
					updateOptionsForSelection();
					saveStateToFiles();
				}
			}
		});
		
		ObservableList<String> data = FXCollections.observableArrayList(this.configurations.configEntries.keySet());
		
		Label dirLabel = new Label();
		dirLabel.setText("Config path: ");
		dirLabel.setFont(Font.font(14));
		
		FileChooser fileChooser = new FileChooser();
		
		Button browseBtn = new Button();
		browseBtn.setText("Browse");
		browseBtn.setAlignment(Pos.CENTER_RIGHT);
		browseBtn.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				decafConfigFile = fileChooser.showOpenDialog(primaryStage);
			
				//There may be no file selected
				if (decafConfigFile != null)
				{
					dirField.setText(decafConfigFile.getAbsolutePath());
					configurations.decafConfigPath = decafConfigFile.getAbsolutePath();
					saveStateToFiles();
				}
			}
		});
		
		//Bottom row
		//Box containing the directory field on top and the button row on the bottom
        VBox dirBox = new VBox(2);
        dirBox.getChildren().addAll(dirField);

		HBox dirRow = new HBox(2);
		dirRow.setPadding(new Insets(5, 2, 0, 2));
		dirRow.setHgrow(dirBox, Priority.ALWAYS);
		dirRow.getChildren().addAll(dirLabel, dirBox, browseBtn);
		
		//Right side options pane
		VBox optionColumn = new VBox(2);
		optionColumn.setPadding(new Insets(5, 5, 0, 5));
		addSettingsElements(optionColumn);
		
		//Row for the selection list and corresponding options
		HBox upperRow = new HBox(2);
		list.setItems(data);
		upperRow.getChildren().addAll(list, optionColumn);
		
		//Directory row on the bottom
        VBox box = new VBox(2);
        box.getChildren().addAll(upperRow, dirRow);
        box.setVgrow(upperRow, Priority.ALWAYS);
        
		Scene scene = new Scene(box, 700, 500);
		
		primaryStage.setTitle("Decaf Config Switcher");
		primaryStage.setScene(scene);

		primaryStage.show();
	}
	
	private String readFromFile(Path path) throws IOException
	{
		if (Files.exists(path))
		{
			String result = "";
			
			for (String string : Files.readAllLines(path))
			{
				result += string + "\n";
			}
			
			return result;
		}
		
		Files.createFile(path);
		return "";
	}
	
	private String readConfigurationsFile() throws IOException
	{
		return readFromFile(CONFIGURATIONS_PATH);
	}
	
	private ConfigEntry mergeConfigEntries(ConfigEntry customConfigEntry, ConfigEntry decafConfigEntry)
	{
		decafConfigEntry.debugger = customConfigEntry.debugger;
		decafConfigEntry.gpu = customConfigEntry.gpu;
		decafConfigEntry.jit = customConfigEntry.jit;
		customConfigEntry.log.level = decafConfigEntry.log.level;
		decafConfigEntry.log = customConfigEntry.log;
		
		return decafConfigEntry;
	}
	
	private void saveStateToFiles()
	{
		//Write to our own configurations file
		String jsonText = GSON.toJson(this.configurations);
		
		try {
			Files.write(CONFIGURATIONS_PATH, jsonText.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Write to the decaf configuration file for the current entry
		ConfigEntry entry = getSelectedConfigEntry();
		
		if (entry != null)
		{
			String decafConfigText = "";

			try 
			{
				decafConfigText = readFromFile(this.decafConfigFile.toPath());
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}

			ConfigEntry decafEntry = GSON.fromJson(decafConfigText, ConfigEntry.class);
			String newConfigText = GSON.toJson(mergeConfigEntries(entry, decafEntry));

			try {
				Files.write(this.decafConfigFile.toPath(), newConfigText.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void updateOptionsForSelection()
	{
		ConfigEntry selectedEntry = getSelectedConfigEntry();
		
		debuggerBox.setSelected(selectedEntry.debugger.enabled);
		breakOnEntryBox.setSelected(selectedEntry.debugger.breakOnEntry);
		forceSyncBox.setSelected(selectedEntry.gpu.forceSync);
		jitBox.setSelected(selectedEntry.jit.enabled);
		jitDebugBox.setSelected(selectedEntry.jit.debug);
		asyncLogBox.setSelected(selectedEntry.log.async);
		toFile.setSelected(selectedEntry.log.toFile);
		toStdOut.setSelected(selectedEntry.log.toStdout);
		kernelTrace.setSelected(selectedEntry.log.kernelTrace);
		branchTrace.setSelected(selectedEntry.log.branchTrace);
	}
	
	private ConfigEntry getSelectedConfigEntry()
	{
		String selectedItem = this.list.getSelectionModel().getSelectedItem();
		return selectedItem != null ? configurations.getOrCreateConfigEntry(selectedItem) : null;
	}
	
	private void addSettingsElements(VBox optionColumn)
	{
		//Debugger options elements
		Label debuggerOptionsLabel = new Label();
		debuggerOptionsLabel.setText("Debugger Options");
		debuggerOptionsLabel.setFont(Font.font(15));
		debuggerOptionsLabel.setPadding(new Insets(5, 0, 0, 0));

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

		//GPU options elements
		Label gpuOptionsLabel = new Label();
		gpuOptionsLabel.setText("GPU Options");
		gpuOptionsLabel.setFont(Font.font(15));
		gpuOptionsLabel.setPadding(new Insets(5, 0, 0, 0));
		
		//JIT options elements
		Label jitOptionsLabel = new Label();
		jitOptionsLabel.setText("JIT Options");
		jitOptionsLabel.setFont(Font.font(15));
		jitOptionsLabel.setPadding(new Insets(5, 0, 0, 0));
		
		//Logging options elements
		Label loggingOptionsLabel = new Label();
		loggingOptionsLabel.setText("Logging Options");
		loggingOptionsLabel.setFont(Font.font(15));
		loggingOptionsLabel.setPadding(new Insets(5, 0, 0, 0));
		
		branchTrace.setPadding(new Insets(0, 0, 5, 0));
		
		optionColumn.getChildren().addAll(debuggerOptionsLabel, debuggerBox, breakOnEntryBox);
		optionColumn.getChildren().addAll(gpuOptionsLabel, forceSyncBox);
		optionColumn.getChildren().addAll(jitOptionsLabel, jitBox, jitDebugBox);
		optionColumn.getChildren().addAll(loggingOptionsLabel, asyncLogBox, toFile, toStdOut, kernelTrace, branchTrace);
	}
	
	public static void main(String[] args)
	{
		launch(args);
	}
}
