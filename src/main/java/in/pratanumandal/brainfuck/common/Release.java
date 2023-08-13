package in.pratanumandal.brainfuck.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;

public class Release {

    private Version version;

    private LocalDate date;

    private String notes;

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Release{" +
                "version=" + version +
                ", date=" + date +
                ", notes='" + notes + '\'' +
                '}';
    }

    public static class Version {

        private int major;
        private int minor;

        public int getMajor() {
            return major;
        }

        public void setMajor(int major) {
            this.major = major;
        }

        public int getMinor() {
            return minor;
        }

        public void setMinor(int minor) {
            this.minor = minor;
        }

        @Override
        public String toString() {
            return this.major + "." + this.minor;
        }

    }

    public static Release getRelease() throws IOException {
        URL url = new URL(Constants.RELEASE_URL);

        YAMLFactory factory = new YAMLFactory();
        factory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE);
        ObjectMapper objectMapper = new ObjectMapper(factory);
        objectMapper.registerModule(new JavaTimeModule());

        return objectMapper.readValue(url, Release.class);
    }

}
