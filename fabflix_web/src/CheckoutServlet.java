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
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;

/**
 * This IndexServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /api/index.
 */
@WebServlet(name = "CheckoutServlet", urlPatterns = "/api/checkout")
public class CheckoutServlet extends HttpServlet {

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/masterdb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Sale confirmation (receives POST from confirmation.js)
        if (request.getParameter("op") != null && request.getParameter("op").equals("confirmSale")) {
            try (Connection conn = dataSource.getConnection()) {
                JsonArray jsonArray = new JsonArray();
                String saleId = "";
                //Getting saleId
                Statement statementSale = conn.createStatement();
                String query = "SELECT * FROM sales ORDER BY id DESC LIMIT 1";
                ResultSet rs = statementSale.executeQuery(query);
                if (rs.next()) {
                    saleId = rs.getString("id");
                }
                else {
                    throw new Exception();
                }

                statementSale.close();
                rs.close();

                // Get movie titles from movieIDs in shopping cart to display in sale confirmation
                HttpSession session = request.getSession();
                Map<String, Integer> previousItems = (Map<String, Integer>) session.getAttribute("previousItems");

                query = "SELECT title FROM movies WHERE id = ?";
                PreparedStatement statement1 = conn.prepareStatement(query);

                for (Map.Entry<String, Integer> entry : previousItems.entrySet()) {
                    statement1.setString(1, entry.getKey());
                    ResultSet rs1 = statement1.executeQuery();
                    if (rs1.next()) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("saleId", saleId);
                        jsonObject.addProperty("finalPrice", (String) session.getAttribute("total-price"));
                        jsonObject.addProperty("title", rs1.getString("title"));
                        jsonObject.addProperty("qty", entry.getValue());
                        jsonArray.add(jsonObject);
                    }
                    rs1.close();
                }
                statement1.close();
                response.getWriter().write(jsonArray.toString());
            }
            catch (Exception e) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("status", "fail");
                jsonObject.addProperty("errorMessage", e.getMessage());
                response.getWriter().write(jsonObject.toString());
                request.getServletContext().log("Error:", e);
                response.setStatus(500);
            }
        }
        else { // Payment operation (POST from payment.js for Payment Page)
            try (Connection conn = dataSource.getConnection()) {
                JsonObject responseJsonObject = new JsonObject();

                String creditCard = request.getParameter("credit_card_number");
                String firstName = request.getParameter("first_name");
                String lastName = request.getParameter("last_name");
                String expirationDate = request.getParameter("expiration_date");

                String query = "SELECT * FROM creditcards WHERE id = ?";
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, creditCard);
                ResultSet rs = statement.executeQuery();

                request.getServletContext().log(query);

                if (rs.next()) {
                    // A row has returned = credit card is in db, need to check all fields match otherwise fail
                    if (rs.getString("firstName").equals(firstName)
                            && rs.getString("lastName").equals(lastName)
                            && rs.getString("expiration").equals(expirationDate))
                    {
                        // Insert record into moviedb.sales
                        HttpSession session = request.getSession();
                        Map<String, Integer> previousItems = (Map<String, Integer>) session.getAttribute("previousItems");

                        // Get customerId
                        String customerId = (String) session.getAttribute("customerId");

                        for (Map.Entry<String, Integer> entry : previousItems.entrySet()) {
                            String movieId = entry.getKey();
                            LocalDate currentDate = LocalDate.now();

                            String insertSQL = "INSERT INTO sales (customerId, movieId, saleDate, qty) VALUES (?, ?, ?, ?)";
                            PreparedStatement preparedStatement = conn.prepareStatement(insertSQL);
                            preparedStatement.setString(1, customerId);
                            preparedStatement.setString(2, movieId);
                            preparedStatement.setString(3, currentDate.toString());
                            preparedStatement.setString(4, entry.getValue().toString());
                            preparedStatement.executeUpdate();
                        }

                        // Success payment
                        responseJsonObject.addProperty("status", "success");
                        responseJsonObject.addProperty("message", "success");
                    }
                    else {
                        responseJsonObject.addProperty("status", "fail");
                        request.getServletContext().log("payment information is wrong");
                        responseJsonObject.addProperty("message", "Please re-enter payment information");
                    }
                }
                else {
                    // ResultSet is empty so no credit card number was entered
                    responseJsonObject.addProperty("status", "fail");
                    request.getServletContext().log("credit card number not found");
                    responseJsonObject.addProperty("message", "Please re-enter payment information");
                }

                rs.close();
                statement.close();

                response.getWriter().write(responseJsonObject.toString());
            }
            catch (Exception e) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("errorMessage", e.getMessage());
                response.getWriter().write(jsonObject.toString());
                request.getServletContext().log("Error:", e);
                response.setStatus(500);
            }
        }
    }
}