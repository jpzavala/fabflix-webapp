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

public class CastsDomParser {
    Map<String, List<StarParsed>> movieToStars = new TreeMap<>();
    Set<String> parsedTitles = new HashSet<>();
    Document dom;
    PrintWriter inconsistencyWriter;

    public void runParser() throws FileNotFoundException {
        inconsistencyWriter = new PrintWriter("inconsistencies_casts124.txt");
        parseXmlFile();
        parseDocument();
        try {
            insertData();
        }
        catch (Exception e) {
            System.out.println("error inserting data");
        }
        inconsistencyWriter.close();
        System.out.println("Finished parsing casts.xml");
    }

    private void parseXmlFile() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            dom = documentBuilder.parse("casts124.xml");
        }
        catch (ParserConfigurationException | SAXException | IOException error) {
            error.printStackTrace();
        }
    }

    private void parseDocument() {
        Element documentElement = dom.getDocumentElement();

        NodeList dirFilmNodes = documentElement.getElementsByTagName("dirfilms");
        if (dirFilmNodes != null) {
            for (int i = 0; i < dirFilmNodes.getLength(); i++) {
                Node dirFilmNode = dirFilmNodes.item(i); // <dirfilm>
                for (int j = 0; j < dirFilmNode.getChildNodes().getLength(); j++) {
                    Node dirFilmChild = dirFilmNode.getChildNodes().item(j); //<filmc>
                    if (dirFilmChild.getNodeName().equals("filmc")) {
                        String movieTitle = null;
                        ArrayList<StarParsed> featuredStars = null;

                        movieTitle = getMovieTitle(dirFilmChild);
                        featuredStars = getFeaturedStars(dirFilmChild);

                        if (movieTitle != null && featuredStars.size() > 0 && !parsedTitles.contains(movieTitle.toLowerCase())) {
                            movieToStars.put(movieTitle, featuredStars);
                            parsedTitles.add(movieTitle.toLowerCase());
                        }
                    }
                }
            }
        }
        parsedTitles = null;
        dom = null;
    }

    private String getMovieTitle(Node dirFilmChild) {
        NodeList mNodes = dirFilmChild.getChildNodes();
        if (mNodes == null || mNodes.getLength() == 0 || mNodes.item(1) == null) {
            return null;
        }

        NodeList mNodeChildren = mNodes.item(1).getChildNodes();
        for (int i = 0; i < mNodeChildren.getLength(); i++) {
            if (mNodeChildren.item(i).getNodeName().equals("t")) {
                String title = mNodeChildren.item(i).getTextContent();
                if (title != null && !title.isEmpty()) {
                    return title;
                }
                else {
                    System.out.println("Inconsistent Cast: movie title (<t>) is null or empty");
                }
            }
        }
        return null;
    }

    private ArrayList<StarParsed> getFeaturedStars(Node dirFilmChild) {
        ArrayList<StarParsed> movieCast = new ArrayList<>();
        NodeList mNodes = dirFilmChild.getChildNodes();

        for (int i = 1; i < mNodes.getLength(); i++) {
            NodeList mNodeChildren = mNodes.item(i).getChildNodes();
            for (int j = 0; j < mNodeChildren.getLength(); j++) {
                Node mNodeData = mNodeChildren.item(j);
                if (mNodeData.getNodeName().equals("a")) {
                    String starName = mNodeData.getTextContent();
                    StarParsed starObject = new StarParsed(starName);
                    if (starName != null && starName.trim().equals("s a")) {
                        inconsistencyWriter.println("Inconsistent Cast: star name (<a>) is 's a' which refers to unknown name (movie is added, but this actor is not)");
                    }
                    if (starName != null && !starName.trim().isEmpty() && !starName.trim().equals("s a") && !movieCast.contains(starObject)) {
                        movieCast.add(starObject);
                    }
                }
            }
        }
        if (movieCast.isEmpty()) {
            inconsistencyWriter.println("Inconsistent Cast: <filmc> does not have stars listed");
        }
        return movieCast;
    }

    private void insertData() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        String jdbcURL="jdbc:mysql://localhost:3306/moviedb";
        Connection conn = null;

        Statement sNewStarId = null;
        PreparedStatement psAddStar = null, psExistsStar = null;
        PreparedStatement psAddStarInMovie = null;

        try {
            conn = DriverManager.getConnection(jdbcURL,"mytestuser", "My6$Password");
            System.out.println("Connection established");
            conn.setAutoCommit(false);

            // Casts.xml includes stars that are not actors.xml . We can include those stars inserting them with null day of birth
            // Note however that if there is a star with that name already in the db I won't insert them

            System.out.println("Inserting new stars found in casts.xml");

            String newStarIdQuery = "SELECT MAX(id) AS id FROM stars";
            int newStarId;
            sNewStarId = conn.createStatement();
            ResultSet rsNewStarId = sNewStarId.executeQuery(newStarIdQuery);
            rsNewStarId.next();
            newStarId = Integer.parseInt(rsNewStarId.getString("id").substring(2)) + 1;
            rsNewStarId.close();
            sNewStarId.close();

            System.out.println("Grabbed new id for stars");

            Set<String> trackerStars = new HashSet<>();
            String starExistsQuery = "SELECT name FROM stars";
            psExistsStar = conn.prepareStatement(starExistsQuery);
            ResultSet rsExistsStar = psExistsStar.executeQuery();
            while (rsExistsStar.next()) {
                trackerStars.add(rsExistsStar.getString("name"));
            }

            rsExistsStar.close();
            psExistsStar.close();

            String addStarQuery = "INSERT INTO stars VALUES (?,?,?)";
            psAddStar = conn.prepareStatement(addStarQuery);

            int i = 1;
            for (Map.Entry<String, List<StarParsed>> entry : movieToStars.entrySet()) {
                for (StarParsed s : entry.getValue()) {
                    if (!trackerStars.contains(s.getName())) {
                        System.out.println("Adding " + s.getName());
                        psAddStar.setString(1, "nm" + newStarId);
                        psAddStar.setString(2, s.getName());
                        psAddStar.setNull(3, Types.INTEGER);
                        psAddStar.addBatch();
                        newStarId++;
                    }
                    if (i % 4000 == 0) {
                        System.out.println("Executing and clearing batch of 4000 inserts");
                        psAddStar.executeBatch();
                        psAddStar.clearBatch();
                    }
                    i++;
                }
            }

            psAddStar.executeBatch();
            psAddStar.close();

            System.out.println("Inserting stars_in_movies records");

            Map<String, String> starToId = new HashMap<>();
            Map<String, String> movieToId = new HashMap<>();

            String grabStarsQuery = "SELECT id, name, birthYear FROM stars";
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(grabStarsQuery);
            while (resultSet.next()) {
                starToId.put(resultSet.getString("name"), resultSet.getString("id"));
            }

            String grabMoviesQuery = "SELECT id, title FROM movies WHERE id > 'tt0499469'";
            Statement statement2 = conn.createStatement();
            ResultSet resultSet2 = statement2.executeQuery(grabMoviesQuery);
            while (resultSet2.next()) {
                movieToId.put(resultSet2.getString("title"), resultSet2.getString("id"));
            }

            resultSet2.close();
            statement2.close();
            System.out.println("Grabbed mappings");

            String addStarInMovieQuery = "INSERT INTO stars_in_movies VALUES (?,?)";
            psAddStarInMovie = conn.prepareStatement(addStarInMovieQuery);

            int j = 1;
            for (Map.Entry<String, List<StarParsed>> entry : movieToStars.entrySet()) {
                String movieId = movieToId.get(entry.getKey());
                if (movieId != null && movieId.compareTo("tt0499469") > 0) {
                    psAddStarInMovie.setString(2, movieId);
                    for (StarParsed s : entry.getValue()) {
                        String starId = starToId.get(s.getName());
                        if (starId != null) {
                            System.out.println(starId + "," + movieId);
                            psAddStarInMovie.setString(1, starId);
                            psAddStarInMovie.addBatch();
                        }
                        if (j % 4000 == 0) {
                            System.out.println("Executing and clearing batch of 5000 inserts");
                            psAddStarInMovie.executeBatch();
                            psAddStarInMovie.clearBatch();
                        }
                        j++;
                    }
                }
            }

            psAddStarInMovie.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (sNewStarId != null) sNewStarId.close();
            if (psAddStar != null) psAddStar.close();
            if (psExistsStar != null) psExistsStar.close();
            if (psAddStarInMovie != null) psAddStarInMovie.close();
            if (conn != null) conn.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CastsDomParser cdp = new CastsDomParser();
        try {
            cdp.runParser();
        }
        catch (Exception e) { }
    }
}
