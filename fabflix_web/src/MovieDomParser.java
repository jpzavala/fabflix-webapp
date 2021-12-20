import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

public class MovieDomParser {
    Set<MovieParsed> movies = new TreeSet<>();
    Set<GenreParsed> collectedGenres = new HashSet<>();
    Document dom;
    PrintWriter inconsistencyWriter;

    public void runParser() throws FileNotFoundException {
        inconsistencyWriter = new PrintWriter("inconsistencies_mains234.txt");
        parseXmlFile();
        parseDocument();
        System.out.println("Finished parseDocument()");
        dom = null;
        try {
            insertMovieData();
            insertGenreData();
            insertGenresInMoviesData();
        }
        catch (Exception e) {
            System.out.println("error inserting data");
        }
        inconsistencyWriter.close();
        System.out.println("Finished parsing mains.xml");
    }

    private void parseXmlFile() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            dom = documentBuilder.parse("mains243.xml");
        }
        catch (ParserConfigurationException | SAXException | IOException error) {
            error.printStackTrace();
        }
    }

    private void parseDocument() {
        // get the document root Element
        Element documentElement = dom.getDocumentElement();

        // get a nodelist of directorfilms Elements, parse each to get director and movies
        NodeList directorFilmsNodes = documentElement.getElementsByTagName("directorfilms");
        if (directorFilmsNodes != null) {
            for (int i = 0; i < directorFilmsNodes.getLength(); i++) {

                Node directorFilmNode = directorFilmsNodes.item(i);

                String director = null;
                ArrayList<MovieParsed> filmsParsed = null;

                // Iterate child notes of director tag
                for (int j = 0; j < directorFilmNode.getChildNodes().getLength(); j++) {
                    Node childNode = directorFilmNode.getChildNodes().item(j);
                    if (childNode.getNodeName().equals("director")) {
                        // Call getDirector to parse
                        director = getDirector(childNode);
                    }
                    else if (childNode.getNodeName().equals("films")) {
                        // Call getMovies to parse
                        filmsParsed = getMovies(childNode, director);
                    }
                }

                if (filmsParsed != null && filmsParsed.size() > 0) {
                    movies.addAll(filmsParsed);
                }
            }
        }
    }

    private String getDirector(Node directorNode) {
        // Director name is one of the child nodes in director node
        NodeList directorChildNodes = directorNode.getChildNodes();
        for (int i = 0; i < directorChildNodes.getLength(); i++) {
            if (directorChildNodes.item(i).getNodeName().equals("dirname")) {
                return directorChildNodes.item(i).getTextContent();
            }
        }
        return null;
    }

    private ArrayList<MovieParsed> getMovies(Node filmsNode, String director) {
        ArrayList<MovieParsed> extractedMovies = new ArrayList<>();
        if (director == null || director.isEmpty()) {
            inconsistencyWriter.println("Inconsistent Movie: No director <dirname> element is empty");
            return extractedMovies;
        }

        NodeList filmNodes = filmsNode.getChildNodes();
        for (int i = 0; i < filmNodes.getLength(); i++) {
            Node filmNode = filmNodes.item(i);
            NodeList filmNodeChildren = filmNode.getChildNodes();

            String title = null;
            int year = -1;
            List<GenreParsed> movieGenres = new ArrayList<>();

            for (int j = 0; j < filmNodeChildren.getLength(); j++) {
                Node filmData = filmNodeChildren.item(j);

                if (filmData.getNodeName().equals("t")) {
                    title = filmData.getTextContent();
                    if (title == null || title.isEmpty()) {
                        inconsistencyWriter.println("Inconsistent Movie: title (<t>) element is empty or null");
                    }
                }
                else if (filmData.getNodeName().equals("year")) {
                    try {
                        year = Integer.parseInt(filmData.getTextContent());
                    }
                    catch (Exception e) {}
                    if (year == -1) {
                        inconsistencyWriter.println("Inconsistent Movie: year (<year>) element not a number");
                    }
                }
                else if (filmData.getNodeName().equals("cats")) {
                    NodeList catsChildren = filmData.getChildNodes();
                    for (int k = 0; k < catsChildren.getLength(); k++) {
                        String genreParsed = catsChildren.item(k).getTextContent();
                        if (genreParsed == null || genreParsed.trim().isEmpty()) {
                            inconsistencyWriter.println("Inconsistent Movie element: genre (<cat>) is empty");
                            continue;
                        }
                        GenreParsed genreObject = new GenreParsed(genreParsed.toLowerCase().trim());

                        if (!movieGenres.contains(genreObject)) {
                            movieGenres.add(genreObject);
                        }

                        collectedGenres.add(genreObject);
                    }

                }
            }

            if (title == null || title.isEmpty()) { }
            else if (year == -1) { }
            else if (movieGenres.isEmpty()) {
                inconsistencyWriter.println("Inconsistent Movie: genres (<cats>) element is empty");
            }
            else {
                MovieParsed extractedMovie = new MovieParsed(title, year, director, movieGenres);
                extractedMovies.add(extractedMovie);
            }
        }

        return extractedMovies;
    }

    private void insertMovieData() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        String jdbcURL = "jdbc:mysql://localhost:3306/moviedb";
        Connection conn = null;
        PreparedStatement psAddMovie = null, psMovieExists = null;
        Statement sNewId = null;
        ResultSet rsNewId = null, rsMovieExists = null;
        try {
            conn = DriverManager.getConnection(jdbcURL, "mytestuser", "My6$Password");

            conn.setAutoCommit(false);

            System.out.println("Inserting movies");

            // Inserting movies into moviedb.movies
            String newIdMovieQuery = "SELECT MAX(id) AS id FROM movies";
            String addMovieQuery = "INSERT INTO movies VALUES(?,?,?,?)";
            String movieExistsQuery = "SELECT id FROM movies WHERE title = ? AND year = ? AND director = ?";
            int newId;

            sNewId = conn.createStatement();
            rsNewId = sNewId.executeQuery(newIdMovieQuery);
            rsNewId.next();
            newId = Integer.parseInt(rsNewId.getString("id").substring(2)) + 1;

            sNewId.close();
            rsNewId.close();

            psMovieExists = conn.prepareStatement(movieExistsQuery);
            psAddMovie = conn.prepareStatement(addMovieQuery);

            int i = 1;
            for (MovieParsed m : movies) {
                psMovieExists.setString(1, m.getTitle());
                psMovieExists.setInt(2, m.getYear());
                psMovieExists.setString(3, m.getDirector());
                rsMovieExists = psMovieExists.executeQuery();
                if (!rsMovieExists.next()) {
                    System.out.println("adding " + m.getTitle());
                    psAddMovie.setString(1, "tt0" + newId);
                    psAddMovie.setString(2, m.getTitle());
                    psAddMovie.setInt(3, m.getYear());
                    psAddMovie.setString(4, m.getDirector());
                    psAddMovie.addBatch();
                    newId++;
                }
                if (i % 4000 == 0) {
                    System.out.println("Executing and clearing batch of 4000 inserts");
                    psAddMovie.executeBatch();
                    psAddMovie.clearBatch();
                }
                i++;
            }

            psAddMovie.executeBatch();

            psMovieExists.close();
            psAddMovie.close();

            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (rsNewId != null) rsNewId.close();
            if (rsMovieExists != null) rsMovieExists.close();
            if (psAddMovie != null) psAddMovie.close();
            if (psMovieExists != null) psMovieExists.close();
            if (sNewId != null) sNewId.close();
            if (conn != null) conn.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void insertGenreData() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        String jdbcURL = "jdbc:mysql://localhost:3306/moviedb";
        Connection conn = null;
        PreparedStatement psAddGenre = null, psGenreExists = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(jdbcURL, "mytestuser", "My6$Password");

            conn.setAutoCommit(false);

            System.out.println("Inserting genres");

            // Inserting genres to moviedb.genres
            String addGenreQuery = "INSERT INTO genres VALUES(?,?)";
            String genreExistsQuery = "SELECT id FROM genres WHERE name = ?;";

            psAddGenre = conn.prepareStatement(addGenreQuery);
            psGenreExists = conn.prepareStatement(genreExistsQuery);

            for (GenreParsed g : collectedGenres) {
                psGenreExists.setString(1, g.getName());
                rs = psGenreExists.executeQuery();
                if (!rs.next()) {
                    psAddGenre.setNull(1, Types.INTEGER);
                    psAddGenre.setString(2, g.getName());
                    psAddGenre.executeUpdate();
                }
            }

            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (rs != null) rs.close();
            if (psAddGenre != null) psAddGenre.close();
            if (psGenreExists != null) psGenreExists.close();
            if (conn != null) conn.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    private void insertGenresInMoviesData() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        String jdbcURL="jdbc:mysql://localhost:3306/moviedb";
        Connection conn = null;
        PreparedStatement psAddGenresInMovie = null, psGenresInMovies = null;
        ResultSet searchRs = null;
        try {
            conn = DriverManager.getConnection(jdbcURL,"mytestuser", "My6$Password");

            conn.setAutoCommit(false);

            System.out.println("Inserting genres_in_movies");

            // Insert movie and genre relations in moviedb.genres_in_movies
            String genresInMoviesQuery = "SELECT movies.id AS movieId, genres.id AS genreId FROM movies, genres " +
                    "WHERE movies.title = ? AND movies.year =  ? AND genres.name = ?";
            String addGenresInMovieQuery = "INSERT INTO genres_in_movies VALUES(?,?)";

            psGenresInMovies = conn.prepareStatement(genresInMoviesQuery);
            psAddGenresInMovie = conn.prepareStatement(addGenresInMovieQuery);

            int i = 1;
            for (MovieParsed m : movies) {
                psGenresInMovies.setString(1, m.getTitle());
                psGenresInMovies.setInt(2, m.getYear());
                for (GenreParsed g : m.getGenreList()) {
                    psGenresInMovies.setString(3, g.getName());
                    searchRs = psGenresInMovies.executeQuery();
                    if (searchRs.next()) {
                        String genreId = searchRs.getString("genreId");
                        String movieId = searchRs.getString("movieId");
                        System.out.println(genreId + "," + movieId);
                        psAddGenresInMovie.setString(1, genreId);
                        psAddGenresInMovie.setString(2, movieId);
                        psAddGenresInMovie.addBatch();
                        if (i % 5000 == 0) {
                            System.out.println("Executing and clearing batch of 5000 inserts");
                            psAddGenresInMovie.executeBatch();
                            psAddGenresInMovie.clearBatch();
                        }
                        i++;
                    }
                }
            }

            psAddGenresInMovie.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (searchRs != null) searchRs.close();
            if (psGenresInMovies != null) psGenresInMovies.close();
            if (psAddGenresInMovie != null) psAddGenresInMovie.close();
            if (conn != null) conn.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MovieDomParser mdp = new MovieDomParser();
        try {
            mdp.runParser();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
