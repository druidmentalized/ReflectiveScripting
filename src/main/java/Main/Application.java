package Main;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Application extends JPanel {

    private final Map<String, String> modelsPaths = new HashMap<>();
    private final Map<String, String> dataPaths = new HashMap<>();
    private JTable calculationsTable;

    public Application() {
        this.setPreferredSize(new Dimension(800, 400));
        this.setLayout(new BorderLayout());

        //filling panel
        this.add(createModelsAndDataPanel(), BorderLayout.WEST);
        this.add(createTableAndScriptButtonsPanel(), BorderLayout.CENTER);
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
                displayValuesOnTable(compressDataToString(dataPaths.get(dataList.getSelectedValue())));
            }
        });

        listsPanel.add(new JScrollPane(dataList));

        returnPanel.add(listsPanel, BorderLayout.CENTER);

        //run model button
        JButton runModelButton = new JButton("Run model");
        returnPanel.add(runModelButton, BorderLayout.SOUTH);

        return returnPanel;
    }

    private JPanel createTableAndScriptButtonsPanel() {
        JPanel returnPanel = new JPanel(new BorderLayout());

        //center panel for table
        calculationsTable = new JTable();
        returnPanel.add(new JScrollPane(calculationsTable), BorderLayout.CENTER);

        //bottom panel for buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton runScriptButton = new JButton("Run script from file");
        JButton createScriptButton = new JButton("Create and run ad hoc script");

        bottomPanel.add(runScriptButton);
        bottomPanel.add(createScriptButton);
        returnPanel.add(bottomPanel, BorderLayout.SOUTH);

        return returnPanel;
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
                        modelsPaths.put(nameWithoutExtension, file.getAbsolutePath());
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

    private String compressDataToString(String filePath) {
        StringBuilder returnString = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                returnString.append(line).append("\n");
            }
        }
        catch (IOException e) {
            System.err.println("Error during reading of the file.");
            e.printStackTrace();
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

    //todo (last step). make design
    //helper methods
}
