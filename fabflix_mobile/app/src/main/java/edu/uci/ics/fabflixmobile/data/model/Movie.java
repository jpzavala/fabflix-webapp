package edu.uci.ics.fabflixmobile.data.model;

/**
 * Movie class that captures movie information for movies retrieved from MovieListActivity
 */
public class Movie {
    private final String name;
    private final short year;
    private final String id;
    private String director;
    private String genres;
    private String stars;

    public Movie(String name, short year, String id, String director, String genres, String stars) {
        this.name = name;
        this.year = year;
        this.id = id;
        this.director = director;
        this.genres = genres;
        this.stars = stars;
    }

    public String getName() {
        return name;
    }

    public short getYear() {
        return year;
    }

    public String getId() {
        return id;
    }

    public String getGenres() {
        return genres;
    }

    public String getStars() {
        return stars;
    }

    public String getDirector() {
        return director;
    }
}