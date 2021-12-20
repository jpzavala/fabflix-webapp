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
import java.sql.*;


// Declaring a WebServlet called MoviesServlet, which maps to url "/api/movies"
@WebServlet(name = "DashboardServlet", urlPatterns = "/api/dashboard")
public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/masterdb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            String op = request.getParameter("op");
            if (op.equals("addStar")) {

                // Add a star dashboard operation
                String starName = request.getParameter("starName");
                String birthYear = request.getParameter("birthYear");

                // Getting highest star id in db
                String query = "SELECT id FROM stars ORDER BY id DESC LIMIT 1;";
                Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery(query);
                rs.next();

                // Add 1 to get a new star id
                String highestId = rs.getString("id");
                String newStarId = "nm" + (Integer.parseInt(highestId.substring(2)) + 1);
                System.out.println("new star id is: " + newStarId);

                rs.close();
                statement.close();

                // Insert star into stars
                query = "INSERT INTO stars VALUES (?, ?, ?)";
                PreparedStatement pstatement = conn.prepareStatement(query);
                pstatement.setString(1, newStarId);
                pstatement.setString(2, starName);

                // Have to check if birthYear is empty
                if (birthYear == null || birthYear.isEmpty()) {
                    pstatement.setNull(3, Types.INTEGER);
                }
                else {
                    pstatement.setInt(3, Integer.parseInt(birthYear));
                }

                pstatement.executeUpdate();
                pstatement.close();

                JsonObject responseJsonObject = new JsonObject();
                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("starId", newStarId);
                response.getWriter().write(responseJsonObject.toString());
            }
            else if (op.equals("metadata")) {

                // Get metadata operation
                JsonObject jsonObject = new JsonObject();

                DatabaseMetaData metaData = conn.getMetaData();
                ResultSet rs = metaData.getTables("moviedb" , null, "%", null);
                while (rs.next()) {
                    String tableName = rs.getString(3);
                    ResultSet rsColumn = metaData.getColumns("moviedb", null, tableName, null);

                    JsonArray columnDataArray = new JsonArray();

                    while (rsColumn.next()) {
                        JsonObject columnData = new JsonObject();
                        String columnName = rsColumn.getString(4);
                        String columnType = rsColumn.getString(6);
                        columnData.addProperty("name", columnName);
                        columnData.addProperty("type", columnType);
                        columnDataArray.add(columnData);
                    }

                    jsonObject.add(tableName, columnDataArray);
                }

                rs.close();
                System.out.println(jsonObject);
                response.getWriter().write(jsonObject.toString());
            }
            else if (op.equals("addMovie")) {
                String movieTitle = request.getParameter("movieTitle");
                String movieDirector = request.getParameter("movieDirector");
                String movieYear = request.getParameter("movieYear");
                String movieGenre = request.getParameter("movieGenre");
                String movieStar = request.getParameter("movieStar");
                if (movieTitle == null || movieTitle.isEmpty() ||
                        movieDirector == null || movieDirector.isEmpty() ||
                        movieYear == null || movieYear.isEmpty() ||
                        movieGenre == null || movieGenre.isEmpty() ||
                        movieStar == null || movieStar.isEmpty()) {
                    throw new Exception("One parameter given to add a movie cannot be empty");
                }

                // Duplicate movies should not be added, a movie is identified by (title, year, director).
                String query = "SELECT * FROM movies WHERE title = ? AND year = ? AND director = ?";
                PreparedStatement movieExistsStatement = conn.prepareStatement(query);
                movieExistsStatement.setString(1, movieTitle);
                movieExistsStatement.setInt(2, Integer.parseInt(movieYear));
                movieExistsStatement.setString(3, movieDirector);
                ResultSet rs = movieExistsStatement.executeQuery();
                if (rs.next()) {
                    // Movie already exists
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("errorMessage", "Movie already exists in database");
                    jsonObject.addProperty("status", "failure");
                    response.getWriter().write(jsonObject.toString());
                    request.getServletContext().log("Movie already exists in database");
                    return;
                }

                movieExistsStatement.close();
                rs.close();

                // To add the movie, get a new movieId for the stored procedure
                query = "SELECT id FROM movies ORDER BY id DESC LIMIT 1;";
                Statement statementForMovieId = conn.createStatement();
                ResultSet rsForMovieId = statementForMovieId.executeQuery(query);
                rsForMovieId.next();

                String highestMovieId = rsForMovieId.getString("id");
                String newMovieId = "";
                if (highestMovieId.startsWith("tt0")) {
                    newMovieId = "tt0" + (Integer.parseInt(highestMovieId.substring(2)) + 1);
                }
                else {
                    newMovieId = "tt" + (Integer.parseInt(highestMovieId.substring(2)) + 1);
                }

                statementForMovieId.close();
                rsForMovieId.close();

                // Now get candidate starId for the stored procedure
                query = "SELECT id FROM stars ORDER BY id DESC LIMIT 1;";
                Statement statementForStarId = conn.createStatement();
                ResultSet rsForStarId = statementForStarId.executeQuery(query);
                rsForStarId.next();

                String highestStarId = rsForStarId.getString("id");
                String newStarId = "nm" + (Integer.parseInt(highestStarId.substring(2)) + 1);

                statementForStarId.close();
                rsForStarId.close();

                // Call stored procedure
                CallableStatement callableStatement = conn.prepareCall("{call add_movie(?, ?, ?, ?, ?, ?, ?)}");
                callableStatement.setString(1, newMovieId);
                callableStatement.setString(2, movieTitle);
                callableStatement.setInt(3, Integer.parseInt(movieYear));
                callableStatement.setString(4, movieDirector);
                callableStatement.setString(5, newStarId);
                callableStatement.setString(6, movieStar);
                callableStatement.setString(7, movieGenre);
                ResultSet rsProcedure = callableStatement.executeQuery();

                if (rsProcedure.next()) {
                    System.out.println("Procedure returned: " + rsProcedure.getString(1));
                }

                rsProcedure.close();
                callableStatement.close();

                // Get movieId, starId, genreId for confirmation message
                query = "SELECT movies.id, movies.title, genres.id, stars.id FROM movies, genres, genres_in_movies, stars, stars_in_movies" +
                        " WHERE movies.id = genres_in_movies.movieId AND genres_in_movies.genreId = genres.id " +
                        "AND movies.id = stars_in_movies.movieId AND stars.id = stars_in_movies.starId AND movies.id = ?";
                PreparedStatement confirmationStatement = conn.prepareStatement(query);
                confirmationStatement.setString(1, newMovieId);
                ResultSet rsConfirmation = confirmationStatement.executeQuery();
                if (rsConfirmation.next()) {
                    String confirmationMovieId = rsConfirmation.getString("movies.id");
                    String confirmationGenreId = rsConfirmation.getString("genres.id");
                    String confirmationStarId = rsConfirmation.getString("stars.id");
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("confirmationMovieId", confirmationMovieId);
                    jsonObject.addProperty("confirmationGenreId", confirmationGenreId);
                    jsonObject.addProperty("confirmationStarId", confirmationStarId);
                    jsonObject.addProperty("status", "success");
                    response.getWriter().write(jsonObject.toString());
                }
                else {
                    throw new Exception("stored procedure returned but new movie was not found in db");
                }
            }
            else {
                throw new Exception("access attempt to dashboard servlet without command");
            }
        }
        catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            jsonObject.addProperty("status", "failure");
            response.getWriter().write(jsonObject.toString());
            request.getServletContext().log("Critical error (maybe db connection):", e);
        }
    }
}
