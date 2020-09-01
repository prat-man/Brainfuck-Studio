package in.pratanumandal.brainfuck;

public class Memory {

    private Integer address;
    private Integer data;
    private Character character;

    public Memory(Integer address, Integer data, Character character) {
        this.address = address;
        this.data = data;
        this.character = character;
    }

    public Integer getAddress() {
        return address;
    }

    public void setAddress(Integer address) {
        this.address = address;
    }

    public Integer getData() {
        return data;
    }

    public void setData(Integer data) {
        this.data = data;
        this.character = (char) data.intValue();
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
        this.data = (int) character;
    }

}
