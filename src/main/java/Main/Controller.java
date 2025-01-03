package Main;

import Annotations.Bind;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Controller {


    private final Object modelInstance;
    private String years = "";
    private final Map<String, double[]> allScriptVariables = new HashMap<>();
    private static final Set<String> GROOVY_KEYWORDS = Set.of(
            "def", "new", "for", "if", "else", "while", "package", "class",
            "return", "double", "int", "float", "boolean", "char", "long",
            "short", "void", "true", "false", "null", "this", "super",
            "as", "in", "switch", "case", "break", "continue"
    );

    public Controller(String modelName) {
        try {
            //creating model according to its name
            modelInstance = Class.forName(modelName).getDeclaredConstructor().newInstance();
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + modelName);
        } catch (InstantiationException e) {
            throw new RuntimeException("Something went wrong while initializing the class: " + modelName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access while found during initialization of the class: " + modelName);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("No constructor found for class: " + modelName);
        }
    }

    public Controller readDataFrom(String fname) {
        //filling map with variables names and their values
        int LL = 0;
        Map<String, double[]> dataVariables = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fname))){
            String line;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");

                if (tokens[0].equals("LATA")) {
                    LL = tokens.length - 1;
                    for (int i = 1; i < tokens.length; i++) {
                        years += tokens[i] + " ";
                    }
                }
                else {
                    double[] values = new double[LL];
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
                    dataVariables.put(tokens[0], values);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found:\n" + fname);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while reading from file");
        }

        //assigning values only to variables which exist inside this model
        for (Field field : modelInstance.getClass().getDeclaredFields()) {
            try {
                if (field.isAnnotationPresent(Bind.class)) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    if (fieldName.equals("LL")) {
                        field.set(modelInstance, LL);
                    }
                    else {
                        field.set(modelInstance, dataVariables.get(fieldName) == null ? new double[LL] : dataVariables.get(fieldName));
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Illegal access while reading field: " + field.getName());
            }
        }
        return this;
    }

    public Controller runModel() {
        try {
            //executing run method
            modelInstance.getClass().getMethod("run").invoke(modelInstance);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error happened during invocation of run method");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access while invoking run method");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No run method found");
        }
        return this;
    }

    public Controller runScriptFromFile(String fname) {
        String script;

        //reading script
        try {
            script = Files.readString(Path.of(fname));
        }
        catch (IOException e) {
            throw new RuntimeException("Error while reading script file");
        }

        //executing script
        return runScript(script);
    }

    public Controller runScript(String script) {
        //creating groovy engine
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("groovy");
        if (scriptEngine == null) {
            throw new RuntimeException("Script engine not found");
        }

        //retrieving and filling all variables with data
        Set<String> scriptVariables = retrieveVariablesFromScript(script);
        passVariablesFromModel(scriptEngine, scriptVariables);

        //computing data from script
        try {
            scriptEngine.eval(script);
        } catch (ScriptException e) {
            throw new RuntimeException("Error while executing groovy script");
        }

        //writing computed data to the variable
        for (String variable : scriptVariables) {
            double[] value = (double[])scriptEngine.get(variable);
            if (value != null) {
                allScriptVariables.put(variable, value);
            }
        }

        return this;
    }

    public String getResultsAsTsv() {
        String returnString = "";

        //writing calculated data from model
        try {
            for (Field field : modelInstance.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Bind.class)) {
                    field.setAccessible(true);
                    String fieldName = field.getName();

                    if (fieldName.equals("LL")) {
                        returnString += fieldName + "\t" + years + "\n";
                    }
                    else if (field.get(modelInstance) != null) {
                        double[] values = (double[]) field.get(modelInstance);
                        returnString += fieldName + "\t";
                        for (double value : values) {
                            returnString += value + " ";
                        }
                        returnString += "\n";
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access while accessing model variables");
        }

        //writing additional data from scripts(if exists)
        for (String variable : allScriptVariables.keySet()) {
            returnString += variable + "\t";
            for (double value : allScriptVariables.get(variable)) {
                returnString += value + " ";
            }
            returnString += "\n";
        }

        return returnString;
    }

    private Set<String> retrieveVariablesFromScript(String script) {
        Set<String> variables = new HashSet<>();

        // Split the script into tokens by whitespace and special characters
        String[] tokens = script.split("[\\s\\W]+");

        for (String token : tokens) {
            // Add to variables if it's not a single lowercase letter and starts with a valid identifier character
            if (token.matches("[a-zA-Z_][a-zA-Z0-9_]*")
                    && !(token.length() == 1 && Character.isLowerCase(token.charAt(0)))
                    && !GROOVY_KEYWORDS.contains(token)) {
                variables.add(token);
            }
        }

        return variables;
    }

    private void passVariablesFromModel(ScriptEngine groovyEngine, Set<String> variables) {
        Iterator<String> iterator = variables.iterator();
        while (iterator.hasNext()) {
            String variable = iterator.next();
            try {
                Field field = modelInstance.getClass().getDeclaredField(variable);
                if (field.isAnnotationPresent(Bind.class)) {
                    field.setAccessible(true);

                    //passing the variable to the script engine
                    groovyEngine.put(variable, field.get(modelInstance));

                    //removing the variable using the iterator
                    iterator.remove();
                } else {
                    throw new NoSuchFieldException();
                }
            } catch (NoSuchFieldException e) {
                //ignored, as many fields won't be accessible or invisible because of having no annotation

                //also trying to find it map of variables from scripts
                if (allScriptVariables.containsKey(variable)) {
                    groovyEngine.put(variable, allScriptVariables.get(variable));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Variable you're trying to reach is inaccessible");
            }

        }
    }


}
