package Main;

public class Controller {


    private Object modelInstance;

    public Controller(String modelName) {
        //todo make reflective assigning to model instance
    }

    public Controller readDataFrom(String fname) {

        return this;
    }

    public Controller runModel() {
        return this;
    }

    public Controller runScriptFromFile(String fname) {
        return this;
    }

    public Controller runScript(String script) {
        return this;
    }

    public String getResultsAsTsv() {
        return "";
    }
}
