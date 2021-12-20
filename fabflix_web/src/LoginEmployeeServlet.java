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

@WebServlet(name = "LoginEmployeeServlet", urlPatterns = "/api/_dashboard")
public class LoginEmployeeServlet extends HttpServlet {

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
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        JsonObject responseJsonObject = new JsonObject();

        try {
            String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
            System.out.println("gRecaptchaResponse=" + gRecaptchaResponse);
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);

        } catch (Exception e) {
            responseJsonObject.addProperty("status", "fail");
            request.getServletContext().log("Login failed: reCAPTCHA not completed");
            responseJsonObject.addProperty("message", "Please verify that you are not a robot");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        // Talking to database to verify user login
        try (Connection conn = dataSource.getConnection()) {

            HttpSession session = request.getSession();

            // Executing query, gets row with email and password, empty if username is not DB
            String query = "SELECT * FROM employees WHERE email = ?";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                // A row is returned, user exists
                if (VerifyPassword.verifyEmployeeCredentials(username, password)) {
                    // If password matches, login success
                    request.getSession().setAttribute("employee", new Employee(username));
                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty("message", "success");
                    //session.setAttribute("customerId", rs.getString("id"));
                }
                else {
                    // Password does not match
                    responseJsonObject.addProperty("status", "fail");
                    request.getServletContext().log("Login failed, wrong password");
                    responseJsonObject.addProperty("message", "Wrong password");
                }
            }
            else {
                // ResultSet is empty, username does NOT exist
                responseJsonObject.addProperty("status", "fail");
                request.getServletContext().log("Login failed, username does not exist");
                responseJsonObject.addProperty("message", "user" + username + " does not exist");
            }
            response.getWriter().write(responseJsonObject.toString());

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            response.getWriter().write(jsonObject.toString());
            request.getServletContext().log("Critical error (maybe db connection):", e);
            response.setStatus(500);
        }
    }
}
