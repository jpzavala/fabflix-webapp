import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SuggestionsServlet", urlPatterns = "/movie_suggestion")
public class SuggestionsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/localdb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter out = response.getWriter();
        JsonArray jsonArray = new JsonArray();
        String query = request.getParameter("query");
        if (query == null || query.trim().isEmpty()) {
            out.write(jsonArray.toString());
        }

        System.out.println("Query is: " + query);

        StringBuilder queryText = new StringBuilder("'");
        String[] tokens = query.split(" ");
        for (String t : tokens) {
            queryText.append("+").append(t).append("* ");
        }
        queryText.append("'");

        try (Connection conn = dataSource.getConnection()) {
            String suggestionsQuery = "SELECT id, title from movies WHERE MATCH (title) AGAINST (? IN BOOLEAN MODE) LIMIT 10";
            PreparedStatement ps = conn.prepareStatement(suggestionsQuery);
            ps.setString(1, queryText.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                jsonArray.add(generateJsonObject(rs.getString("id"), rs.getString("title")));
            }

            rs.close();
            ps.close();

            // Fuzzy search for autosuggestion
            boolean activateFuzzy = true;
            if (jsonArray.size() < 10 && activateFuzzy) {

                // Calculate a good max distance to compare
                String limit = "" + (10 - jsonArray.size());
                int distance = (int) (0.4 * query.length());
                String fuzzySearchQuery = "SELECT id, title from movies WHERE title LIKE ? OR ed(title, ?) <= ? LIMIT " + limit;
                PreparedStatement psFuzzy = conn.prepareStatement(fuzzySearchQuery);
                psFuzzy.setString(1, "%" + query + "%");
                psFuzzy.setString(2, query);
                psFuzzy.setInt(3, distance);
                ResultSet rsFuzzy = psFuzzy.executeQuery();
                while (rsFuzzy.next()) {
                    jsonArray.add(generateJsonObject(rsFuzzy.getString("id"), rsFuzzy.getString("title")));
                }
                rsFuzzy.close();
                psFuzzy.close();
            }

            out.write(jsonArray.toString());
            System.out.println(jsonArray);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private static JsonObject generateJsonObject(String movieId, String movieTitle) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("value", movieTitle);

        JsonObject additionalDataJsonObject = new JsonObject();
        additionalDataJsonObject.addProperty("movieId", movieId);

        jsonObject.add("data", additionalDataJsonObject);
        return jsonObject;
    }
}
