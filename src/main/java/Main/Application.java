package Main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Application extends JLayeredPane {

    //panels
    JPanel mainLayer = new JPanel(new BorderLayout());
    JPanel scriptIDELayer = new JPanel();
    JPanel errorsLayer = new JPanel(new BorderLayout());

    //variables
    Controller controller;
    private final Map<String, String> modelsPaths = new HashMap<>();
    private final Map<String, String> dataPaths = new HashMap<>();
    private JTable calculationsTable;

    public Application() {
        this.setPreferredSize(new Dimension(800, 400));
        this.setLayout(null);

        //main panel
        mainLayer.setBounds(0, 0, (int)this.getPreferredSize().getWidth(), (int)this.getPreferredSize().getHeight());
        mainLayer.add(createModelsAndDataPanel(), BorderLayout.WEST);
        mainLayer.add(createTableAndScriptButtonsPanel(), BorderLayout.CENTER);

        //script IDE panel
        scriptIDELayer.setBounds(0, 0, (int)this.getPreferredSize().getWidth(), (int)this.getPreferredSize().getHeight());
        scriptIDELayer.setOpaque(false);

        //errors panel
        errorsLayer.setBounds(0, 0, (int)this.getPreferredSize().getWidth(), (int)this.getPreferredSize().getHeight());
        errorsLayer.setOpaque(false);

        //adding different layers to the pane
        this.add(mainLayer, Integer.valueOf(1));
        this.add(scriptIDELayer, Integer.valueOf(2));
        this.add(errorsLayer, Integer.valueOf(3));
    }

    private JPanel createModelsAndDataPanel() {
        JPanel returnPanel = new JPanel(new BorderLayout());

        //adding margin with title
        returnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        "Select model and data",
                        TitledBorder.CENTER,
                        TitledBorder.TOP
                ),
                BorderFactory.createEmptyBorder(10, 10, 10, 10) // Inner padding
        ));
        returnPanel.setPreferredSize(new Dimension(225, 0));

        JPanel listsPanel = new JPanel(new GridLayout(1, 2));

        //models list
        JList<String> modelList = new JList<>(parseModels());
        listsPanel.add(new JScrollPane(modelList));

        //data selection list
        JList<String> dataList = new JList<>(parseData());
        dataList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displayValuesOnTable(compressDataToTsv(dataPaths.get(dataList.getSelectedValue())));
            }
        });

        listsPanel.add(new JScrollPane(dataList));

        returnPanel.add(listsPanel, BorderLayout.CENTER);

        //run model
        JPanel buttonPanel = createButtonPanel(modelList, dataList);
        returnPanel.add(buttonPanel, BorderLayout.SOUTH);

        return returnPanel;
    }

    private JPanel createButtonPanel(JList<String> modelList, JList<String> dataList) {
        //creating button with listener
        JButton runModelButton = new JButton("Run model");

        runModelButton.addActionListener(e -> {
           if (modelList.getSelectedValue() != null) {
               try {
                   controller = new Controller(modelsPaths.get(modelList.getSelectedValue()));
                   if (dataList.getSelectedValue() != null) {
                       controller.readDataFrom(dataPaths.get(dataList.getSelectedValue())).runModel();
                       displayValuesOnTable(controller.getResultsAsTsv());
                   }
                   else {
                       showErrorDialog(errorsLayer, "No data source was selected");
                   }
               }
               catch (Exception ex) {
                   showErrorDialog(errorsLayer, ex.getMessage());
               }
           }
           else {
               showErrorDialog(errorsLayer, "No model was selected");
           }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(runModelButton);
        return buttonPanel;
    }

    private JPanel createTableAndScriptButtonsPanel() {
        JPanel returnPanel = new JPanel(new BorderLayout());

        //center panel for table
        calculationsTable = new JTable();
        returnPanel.add(new JScrollPane(calculationsTable), BorderLayout.CENTER);

        //bottom panel for buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton runScriptButton = new JButton("Run script from file");
        runScriptButton.addActionListener(e -> {
            if (controller != null) {
                JFileChooser fileChooser = new JFileChooser();

                //choosing staring directory and filter for files
                fileChooser.setCurrentDirectory(new File("src/res/scripts"));
                FileNameExtensionFilter extensionFilter = new FileNameExtensionFilter("Groovy script files (*.groovy)", "groovy");
                fileChooser.setFileFilter(extensionFilter);

                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    try {
                        controller.runScriptFromFile(fileChooser.getSelectedFile().getAbsolutePath());
                        displayValuesOnTable(controller.getResultsAsTsv());
                    }
                    catch (Exception ex) {
                        showErrorDialog(errorsLayer, ex.getMessage());
                    }
                }
            }
            else {
                showErrorDialog(errorsLayer, "No model was counted, script usage disabled");
            }
        });

        JButton createScriptButton = new JButton("Create and run ad hoc script");
        createScriptButton.addActionListener(e -> {
           if (controller != null) {
                createSmallIDE(scriptIDELayer);
           }
           else {
               showErrorDialog(errorsLayer, "No model was counted, script usage disabled");
           }
        });

        bottomPanel.add(runScriptButton);
        bottomPanel.add(createScriptButton);
        returnPanel.add(bottomPanel, BorderLayout.SOUTH);

        return returnPanel;
    }

    private void createSmallIDE(JPanel parentPanel) {
        JDialog scriptDialog = new JDialog((JFrame)null, "Script", true); // Modal dialog
        scriptDialog.setSize(400, 300);
        scriptDialog.setLayout(new BorderLayout());

        // Create a text area for script input
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        //adding the text area inside a scroll pane
        JScrollPane scrollPane = new JScrollPane(textArea);
        scriptDialog.add(scrollPane, BorderLayout.CENTER);

        //creating a panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            try {
                controller.runScript(textArea.getText());
                displayValuesOnTable(controller.getResultsAsTsv());
                scriptDialog.dispose();
            }
            catch (Exception ex) {
                showErrorDialog(errorsLayer, ex.getMessage());
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> scriptDialog.dispose());

        //adding buttons to the panel
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        //adding the button panel to the dialog
        scriptDialog.add(buttonPanel, BorderLayout.SOUTH);

        //showing the dialog
        scriptDialog.setLocationRelativeTo(parentPanel);
        scriptDialog.setVisible(true);
    }

    private String[] parseModels() {
        ArrayList<String> allModelsArrList = new ArrayList<>();
        String directoryPath = "src/main/java/Models";
        File modelsDirectory = new File(directoryPath);

        if (modelsDirectory.exists() && modelsDirectory.isDirectory()) {
            File[] files = modelsDirectory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.getName().toLowerCase().contains("model") && file.getName().endsWith(".java")) {
                        String nameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf("."));
                        allModelsArrList.add(nameWithoutExtension);
                        modelsPaths.put(nameWithoutExtension, "Models." + nameWithoutExtension);
                    }
                }
            }
        }
        return allModelsArrList.toArray(new String[0]);
    }

    private String[] parseData() {
        ArrayList<String> allDataArrList = new ArrayList<>();
        String directoryPath = "src/res/Data";
        File modelsDirectory = new File(directoryPath);

        if (modelsDirectory.exists() && modelsDirectory.isDirectory()) {
            File[] files = modelsDirectory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.getName().toLowerCase().contains("data") && file.getName().endsWith(".txt")) {
                        String nameWithoutExtension = file.getName().substring(0, file.getName().lastIndexOf("."));
                        allDataArrList.add(nameWithoutExtension);
                        dataPaths.put(nameWithoutExtension, file.getAbsolutePath());
                    }
                }
            }
        }
        return allDataArrList.toArray(new String[0]);
    }

    private String compressDataToTsv(String filePath) {
        StringBuilder returnString = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                returnString.append(line).append("\n");
            }
        }
        catch (IOException e) {
            System.err.println("Error during reading of the file.");
        }
        return returnString.toString();
    }

    private void displayValuesOnTable(String dataString) {
        String[] columns;
        String[][] data;

        //getting all lines with variables
        String[] variables = dataString.split("\n");

        //parsing columns
        columns = variables[0].split("\\s+");
        columns[0] = "";

        //parsing other data
        data = new String[variables.length - 1][];
        for (int i = 1; i < variables.length; i++) {
            data[i - 1] = variables[i].split("\\s+");
        }

        calculationsTable.setModel(new javax.swing.table.DefaultTableModel(data, columns));
    }

    private void showErrorDialog(JPanel parentPanel, String errMessage) {
        JDialog errorDialog = new JDialog((JFrame)null, "Error", true);

        //setting size and layout
        errorDialog.setSize(300, 150);
        errorDialog.setLayout(new BorderLayout());

        //adding label
        JLabel errorLabel = new JLabel("<html><div style='text-align: center;'>" + errMessage + "</div></html>");
        errorDialog.add(errorLabel, BorderLayout.CENTER);

        //adding OK button to exit
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> errorDialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        errorDialog.add(buttonPanel, BorderLayout.SOUTH);

        //setting location relative to provided JPanel(if exists), else setting to center of the screen
        errorDialog.setLocationRelativeTo(parentPanel);
        errorDialog.setVisible(true);
    }
}
