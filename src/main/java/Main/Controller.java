package Main;

import Annotations.Bind;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Controller {


    private Object modelInstance;
    private String years = "";

    public Controller(String modelName) {
        try {
            modelInstance = Class.forName(modelName).newInstance();
        }
        catch (Exception e) {
            System.err.println("Something went wrong during instantiation of a model");
            e.printStackTrace();
        }
    }

    public Controller readDataFrom(String fname) {
        //Map<String, Double[]> variables = new HashMap<>(); todo make enhanced reading of data using map
        try (BufferedReader br = new BufferedReader(new FileReader(fname))) {
            String line;
            int columnsAmount = 0;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");

                //getting exact field by name
                Field field = modelInstance.getClass().getDeclaredField(tokens[0].equals("LATA") ? "LL" : tokens[0]);
                field.setAccessible(true);

                if (field.getName().equals("LL")) {
                    //assigning years without the name of the variable
                    field.set(modelInstance, tokens.length - 1);
                    columnsAmount = tokens.length - 1;
                    for (int i = 1; i < tokens.length; i++) {
                        years += tokens[i] + " ";
                    }
                }
                else {
                    //setting all values and assigning them to a variable
                    double[] values = new double[columnsAmount];
                    for (int i = 0; i < values.length; i++) {
                        if (i + 1 < tokens.length) {
                            values[i] = Double.parseDouble(tokens[i + 1]);
                        }
                        else if (i == 0) {
                            values[i] = 0.0;
                        }
                        else {
                            values[i] = values[i - 1];
                        }
                    }
                    field.set(modelInstance, values);
                }
            }
        }
        catch (NoSuchFieldException e) {
            System.err.println("Some of the fields from the data doesn't exist in the given model" );
            //todo remake so that all field may be taken
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public Controller runModel() {
        try {
            modelInstance.getClass().getMethod("run").invoke(modelInstance);
        }
        catch (Exception e) {
            System.err.println("Something went wrong during model execution");
        }
        return this;
    }

    public Controller runScriptFromFile(String fname) {
        return this;
    }

    public Controller runScript(String script) {

        return this;
    }

    public String getResultsAsTsv() {
        String returnString = "";
        try {
            for (Field field : modelInstance.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Bind.class)) {
                    field.setAccessible(true);
                    String fieldName = field.getName();

                    if (fieldName.equals("LL")) {
                        returnString += fieldName + "\t" + years + "\n";
                    }
                    else {
                        double[] values = (double[]) field.get(modelInstance);
                        returnString += fieldName + "\t";
                        for (double value : values) {
                            returnString += value + " ";
                        }
                        returnString += "\n";
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return returnString;
    }
}
