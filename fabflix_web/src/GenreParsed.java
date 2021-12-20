public class GenreParsed {
    private String name;

    public GenreParsed(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object otherGenre) {
        if (otherGenre == this)
            return true;
        if (!(otherGenre instanceof GenreParsed))
            return false;
        GenreParsed g = (GenreParsed) otherGenre;
        return g.getName().equalsIgnoreCase(getName());
    }

    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    public String toString() {
        return getName();
    }

}
