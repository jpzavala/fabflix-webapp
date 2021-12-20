public class StarParsed {
    private String name;
    private int birthYear;

    public StarParsed(String name) {
        this.name = name;
        birthYear = -1;
    }

    public StarParsed(String name, int birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBirthYear() {
        return this.birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public boolean equals(Object otherStar) {
        if (otherStar == this)
            return true;
        if (!(otherStar instanceof StarParsed))
            return false;
        StarParsed s = (StarParsed) otherStar;
        return s.getName().equalsIgnoreCase(getName());
    }

    public String toString() {
        return getName() + " - " + getBirthYear();
    }
}
