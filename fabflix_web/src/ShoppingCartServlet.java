import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This IndexServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /api/index.
 */
@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/localdb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * handles GET requests to store session information
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();

        try (Connection conn = dataSource.getConnection()) {

            // Gets items from shopping cart (previousItems), creates it if there isn't one in user session
            Map<String, Integer> previousItems = (Map<String, Integer>) session.getAttribute("previousItems");
            if (previousItems == null) {
                previousItems = new LinkedHashMap<>();
            }

            // Log to localhost log
            request.getServletContext().log("getting " + previousItems.size() + " items from shopping cart");

            JsonArray jsonArray = new JsonArray();

            String query = "SELECT * FROM movies WHERE movies.id = ?";
            PreparedStatement statement = conn.prepareStatement(query);

            // Get movie titles from movieIDs in previousItems to display in shopping cart page
            for (Map.Entry<String, Integer> entry : previousItems.entrySet()) {

                String movieTitle = "";
                statement.setString(1, entry.getKey());

                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    movieTitle = rs.getString("title");
                }

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", entry.getKey());
                jsonObject.addProperty("quantity", entry.getValue());
                jsonObject.addProperty("movie_title", movieTitle);

                jsonArray.add(jsonObject);
                rs.close();
            }

            statement.close();

            // write all the data into the jsonObject
            response.getWriter().write(jsonArray.toString());
        }
        catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            response.getWriter().write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        }
    }

    /**
     * handles POST requests to add and show the item list information
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession();

        // op=save-total : Save total price of shopping cart using session
        if (request.getParameter("op").equals("save-total")) {
            session.setAttribute("total-price", request.getParameter("total-price"));
            return;
        }

        // op=get-total : Return total price of shopping cart from session
        if (request.getParameter("op").equals("get-total")) {
            String totalPrice = (String) session.getAttribute("total-price");
            response.getWriter().write(totalPrice);
            return;
        }

        // Shopping cart operations
        String item = request.getParameter("movieId");
        request.getServletContext().log(item);
        Map<String, Integer> previousItems = (Map<String, Integer>) session.getAttribute("previousItems");

        if (request.getParameter("op").equals("decrease")) {
            if (previousItems.get(item) > 0) {
                previousItems.put(item, previousItems.get(item) - 1);
            }
            if (previousItems.get(item) == 0) {
                previousItems.remove(item);
            }
        }
        else if (request.getParameter("op").equals("delete")) {
            previousItems.remove(item);
        }
        else { // Increase and land
            // get the previous items in a ArrayList
            if (previousItems == null) {
                previousItems = new LinkedHashMap<>();
                previousItems.put(item, 1);
                session.setAttribute("previousItems", previousItems);
            } else {
                // prevent corrupted states through sharing under multi-threads
                // will only be executed by one thread at a time
                synchronized (previousItems) {
                    if (previousItems.containsKey(item)) {
                        previousItems.put(item, previousItems.get(item) + 1);
                    }
                    else {
                        previousItems.put(item, 1);
                    }
                }
            }

            Gson gson = new Gson();
            String itemsJson = gson.toJson(previousItems);

            // write all the data
            response.getWriter().write(itemsJson);
        }
    }
}