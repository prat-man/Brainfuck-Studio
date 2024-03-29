package in.pratanumandal.brainfuck.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Snippets {

    private List<Snippet> snippets;

    @JsonIgnore
    private ObservableList<Snippet> observableSnippets;

    public Snippets() {
        this.snippets = new ArrayList<>();
    }

    public ObservableList<Snippet> getSnippets() {
        if (observableSnippets == null) {
            observableSnippets = FXCollections.observableList(snippets);
        }
        return observableSnippets;
    }

    @Override
    public String toString() {
        return "Snippets{" +
                "snippets=" + snippets +
                '}';
    }

    public static class Snippet {

        private final SimpleStringProperty name;
        private final SimpleStringProperty description;
        private final SimpleStringProperty code;

        public Snippet() {
            name = new SimpleStringProperty();
            description = new SimpleStringProperty();
            code = new SimpleStringProperty();
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public StringProperty nameProperty() {
            return name;
        }

        public String getDescription() {
            return description.get();
        }

        public void setDescription(String description) {
            this.description.set(description);
        }

        public StringProperty descriptionProperty() {
            return description;
        }

        public String getCode() {
            return code.get();
        }

        public void setCode(String code) {
            this.code.set(code);
        }

        public StringProperty codeProperty() {
            return code;
        }

        @Override
        public String toString() {
            return "Snippet{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", code='" + code + '\'' +
                    '}';
        }

    }

    public static Snippets loadSnippets() {
        try {
            File file = new File(Constants.SNIPPETS_FILE);
            if (!file.exists()) {
                Path path = Paths.get(Constants.SNIPPETS_FILE);
                Files.copy(Snippets.class.getClassLoader().getResourceAsStream("json/snippets.json"), path, StandardCopyOption.REPLACE_EXISTING);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(file, Snippets.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void saveSnippets(Snippets snippets) {
        try {
            DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentObjectsWith(indenter);
            printer.indentArraysWith(indenter);

            File file = new File(Constants.SNIPPETS_FILE);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writer(printer).writeValue(file, snippets);
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
