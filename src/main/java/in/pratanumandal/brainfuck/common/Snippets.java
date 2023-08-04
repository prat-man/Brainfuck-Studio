package in.pratanumandal.brainfuck.common;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;

@XmlRootElement(name = "snippets")
@XmlAccessorType(XmlAccessType.FIELD)
public class Snippets {

    @XmlElement(name = "snippet")
    private ObservableList<Snippet> snippets;

    public Snippets() {
        this.snippets = FXCollections.observableArrayList();
    }

    public ObservableList<Snippet> getSnippets() {
        return snippets;
    }

    @Override
    public String toString() {
        return "Snippets{" +
                "snippets=" + snippets +
                '}';
    }

    @XmlRootElement(name = "snippet")
    @XmlAccessorType (XmlAccessType.FIELD)
    public static class Snippet {

        private String name;
        private String description;
        private String code;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
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
            JAXBContext jaxbContext = JAXBContext.newInstance(Snippets.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            return (Snippets) jaxbUnmarshaller.unmarshal(new File(Constants.SNIPPETS_FILE));
        }
        catch (Exception e) {
            Snippets snippets = defaultSnippets();
            saveSnippets(snippets);
            return snippets;
        }
    }

    public static void saveSnippets(Snippets snippets) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Snippets.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(snippets, new File(Constants.SNIPPETS_FILE));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Snippets defaultSnippets() {
        Snippets snippets = new Snippets();

        Snippet snippet;

        snippet = new Snippet();
        snippet.setName("Print newline");
        snippet.setCode("[-]++++++++++.");
        snippets.getSnippets().add(snippet);

        snippet = new Snippet();
        snippet.setName("Print number");
        snippet.setDescription("Prints value in current cell as decimal number. Requires 7 empty cells to the right. Leaves starting cell unchanged.");
        snippet.setCode(">[-]>[-]+>[-]+<[>[-<-<<[->+>+<<]>[-<+>]>>]++++++++++>[-]+>[-]>[-]>[-]<<<<<[->-[>+>>]>[[-<+>]+>+>>]<<<<<]>>-[-<<+>>]<[-]++++++++[-<++++++>]>>[-<<+>>]<<]<[.[-]<]<");
        snippets.getSnippets().add(snippet);

        return snippets;
    }

}
