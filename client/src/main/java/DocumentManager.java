import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class DocumentManager {
    private SceneManager sceneManager;

    public DocumentManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public File openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Document Text File");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Text Files", "*.txt"));
        File selectedFile = fileChooser.showOpenDialog(sceneManager.getStage());

        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
        }

        return selectedFile;
    }

    public boolean saveFile(File file, String content) {
        try {
            Files.write(file.toPath(), content.getBytes());
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save file: " + e.getMessage());
            return false;
        }
    }

    public String loadFileContent(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes);
        } catch (IOException e) {
            System.err.println("Failed to load file: " + e.getMessage());
            return "";
        }
    }
}
