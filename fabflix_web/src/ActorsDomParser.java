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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActorsDomParser {
    List<StarParsed> collectedStars = new ArrayList<>();
    Document dom;
    PrintWriter inconsistencyWriter;

    public void runParser() throws FileNotFoundException {
        inconsistencyWriter = new PrintWriter("inconsistencies_actors63.txt");
        parseXmlFile();
        parseDocument();
        try {
            insertData();
        }
        catch (Exception e) {
            System.out.println("error inserting data");
        }
        inconsistencyWriter.close();
        System.out.println("Finished parsing actors.xml");
    }

    private void parseXmlFile() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            dom = documentBuilder.parse("actors63.xml");
        }
        catch (ParserConfigurationException | SAXException | IOException error) {
            error.printStackTrace();
        }
    }

    private void parseDocument() {
        Element documentElement = dom.getDocumentElement();

        NodeList actorNodes = documentElement.getElementsByTagName("actor");
        if (actorNodes != null) {
            for (int i = 0; i < actorNodes.getLength(); i++) {

                Element actorElement = (Element) actorNodes.item(i);
                StarParsed extractedStar = getStar(actorElement);
                if (extractedStar != null) {
                    collectedStars.add(extractedStar);
                }
            }
        }
    }

    private StarParsed getStar(Element actorElement) {
        String starName = getTextValue(actorElement, "stagename");
        int dayOfBirth = getIntValue(actorElement, "dob");
        if (starName == null || starName.isEmpty()) {
            inconsistencyWriter.println("Inconsistent Star: star name (<stagename>) element is empty");
            return null;
        }
        else if (dayOfBirth == -1) {
            inconsistencyWriter.println(starName + "Inconsistent Star: star dayBirth (<dob>) element is not numeric or is empty");
        }
        return new StarParsed(starName, dayOfBirth);
    }

    private String getTextValue(Element element, String tagName) {
        String textVal = null;
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            // Cannot assume the first child is going to be there like in the example
            Node firstChild = nodeList.item(0).getFirstChild();
            if (firstChild != null) {
                textVal = firstChild.getNodeValue();
            }
        }
        return textVal;
    }

    private int getIntValue(Element ele, String tagName) {
        // in production application you would catch the exception
        try {
            return Integer.parseInt(getTextValue(ele, tagName));
        }
        catch (Exception e) {
            return -1;
        }
    }

    private void writeResults() throws FileNotFoundException {
        PrintWriter out = new PrintWriter("stars_output.txt");

        for (StarParsed star : collectedStars) {
            out.println(star);
        }

        // Close the file.
        out.close();
    }

    private void insertData() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        String jdbcURL="jdbc:mysql://localhost:3306/moviedb";
        Connection conn = null;

        Statement sNewStarId = null;
        PreparedStatement psStarExists = null, psAddStar = null;

        try {
            conn = DriverManager.getConnection(jdbcURL,"mytestuser", "My6$Password");

            conn.setAutoCommit(false);

            // Get new candidate starId for new stars
            String newStarIdQuery = "SELECT MAX(id) AS id FROM stars";
            int newStarId;
            sNewStarId = conn.createStatement();
            ResultSet rsNewStarId = sNewStarId.executeQuery(newStarIdQuery);
            rsNewStarId.next();
            newStarId = Integer.parseInt(rsNewStarId.getString("id").substring(2)) + 1;

            Set<String> trackerStars = new HashSet<>();
            String starExistsQuery = "SELECT name FROM stars";
            psStarExists = conn.prepareStatement(starExistsQuery);
            ResultSet resultSet = psStarExists.executeQuery();
            while (resultSet.next()) {
                trackerStars.add(resultSet.getString("name"));
            }

            String addStarQuery = "INSERT INTO stars VALUES(?,?,?)";
            psAddStar = conn.prepareStatement(addStarQuery);

            for (StarParsed s : collectedStars) {
                if (!trackerStars.contains(s.getName())) {
                    psAddStar.setString(1, "nm" + newStarId);
                    psAddStar.setString(2, s.getName());
                    if (s.getBirthYear() == -1) {
                        psAddStar.setNull(3, Types.INTEGER);
                    }
                    else {
                        psAddStar.setInt(3, s.getBirthYear());
                    }
                    psAddStar.addBatch();
                    newStarId++;
                }
            }

            psAddStar.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (sNewStarId != null) sNewStarId.close();
            if (psAddStar != null) psAddStar.close();
            if (psStarExists != null) psStarExists.close();
            if (conn != null) conn.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ActorsDomParser adp = new ActorsDomParser();
        try {
            adp.runParser();
            adp.writeResults();
        }
        catch (Exception e) { }
    }
}
