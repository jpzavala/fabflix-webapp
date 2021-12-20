import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MovieParsed implements Comparable<MovieParsed> {
    private String title;
    private int year;
    private String director;
    private List<GenreParsed> genres;

    public MovieParsed(String title, int year, String director) {
        this.title = title;
        this.year = year;
        this.director = director;
        genres = new ArrayList<>();
    }

    public MovieParsed(String title, int year, String director, List<GenreParsed> genres) {
        this.title = title;
        this.year = year;
        this.director = director;
        this.genres = genres;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public List<GenreParsed> getGenreList() {
        return genres;
    }

    public void setGenreList(List<GenreParsed> parsedGenres) {
        this.genres = parsedGenres;
    }

    public void addGenre(GenreParsed genre) {
        this.genres.add(genre);
    }

    public void clearGenreList() {
        this.genres.clear();
    }

    public String toString() {
        return title + ": " + year + ", " + director + ", " + genres;
    }

    @Override
    public int compareTo(MovieParsed o) {
        return this.title.toLowerCase().trim().compareTo(o.getTitle().toLowerCase().trim());
    }
}
