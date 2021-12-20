import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


// Declaring a WebServlet called MoviesServlet, which maps to url "/api/movies"
@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
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

        // Setup to measure TS
        long startTs = System.nanoTime();

        String contextPath = request.getServletContext().getRealPath("/");
        String logFilePath = contextPath + "\\jmeter-log.txt";

        System.out.println(logFilePath);

        File myFile = new File(logFilePath);
        if (!myFile.exists()) {
            myFile.createNewFile();
        }
        PrintWriter pout = new PrintWriter(new FileWriter(myFile, true));

        response.setContentType("application/json"); // Response mime type

        HttpSession session = request.getSession();

        StringBuilder savedURL = new StringBuilder("movie-list.html?");

        // Saving URL in session
        Set<String> paramNames = request.getParameterMap().keySet();

        int count = 0;
        for (String name : paramNames) {
            String value = request.getParameter(name);
            if (count == 0) {
                savedURL.append(name).append("=").append(value);
            }
            else {
                savedURL.append("&").append(name).append("=").append(value);
            }
            count++;
        }

        session.setAttribute("saved-url", savedURL.toString());

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        String mode = request.getParameter("op");

        String query = "";
        Map<Integer, String> parameterMap = new HashMap<>();

        // TJ setup
        long startTj = System.nanoTime();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {

            // Processing movie list results according to browse/search/parameters
            if (mode.equals("fulltext")) {
                String queryWords = request.getParameter("clues");

                System.out.println(queryWords);

                if (queryWords == null || queryWords.trim().isEmpty()) {
                    JsonArray jsonArray = new JsonArray();
                    out.write(jsonArray.toString());
                    return;
                }

                StringBuilder queryText = new StringBuilder("'");
                String[] tokens = queryWords.split(" ");
                for (String t : tokens) {
                    queryText.append("+").append(t).append("* ");
                }
                queryText.append("'");

                JsonArray jsonArray = new JsonArray();
                String fullTextQuery = "SELECT id, title from movies WHERE MATCH (title) AGAINST (? IN BOOLEAN MODE) OR title LIKE ? OR ed(title, ?) <= ?";

                // Pagination
                String numRecordParameter = request.getParameter("numRecords");
                String offset = request.getParameter("offset");

                fullTextQuery += " LIMIT " + numRecordParameter;
                fullTextQuery += " OFFSET " + offset;

                PreparedStatement ps = conn.prepareStatement(fullTextQuery);
                ps.setString(1, queryText.toString());
                ps.setString(2, "%" + queryWords + "%");
                ps.setString(3, queryWords);

                // Calculate a good max distance to compare
                int distance = (int) (0.4 * queryWords.length());
                ps.setInt(4, distance);

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String resultId = rs.getString("id");
                    String searchByIdQuery = "SELECT M.id, M.title, M.year, M.director, group_concat(DISTINCT G.name) as genres, R.rating, group_concat(DISTINCT S.id, ',', S.name, ',', (SELECT COUNT(*) from stars_in_movies WHERE stars_in_movies.starId =  SM.starId) SEPARATOR ';') AS actors" +
                            " FROM movies M LEFT JOIN ratings R ON M.id = R.movieId, genres_in_movies GM, genres G, stars S, stars_in_movies SM" +
                            " WHERE M.id = GM.movieId AND G.id = GM.genreId AND S.id = SM.starId AND SM.movieId = M.id AND M.id = ?";
                    PreparedStatement resultPs = conn.prepareStatement(searchByIdQuery);
                    resultPs.setString(1, resultId);
                    ResultSet movieDataRs = resultPs.executeQuery();

                    while (movieDataRs.next()) {
                        // Getting all columns
                        String movieId = movieDataRs.getString("id");
                        String movieTitle = movieDataRs.getString("title");
                        String movieYear = movieDataRs.getString("year");
                        String movieDirector = movieDataRs.getString("director");
                        String movieRating = movieDataRs.getString("rating");

                        String movieGenres = movieDataRs.getString("genres");

                        String movieStars = movieDataRs.getString("actors");

                        // Creating JsonObject
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("movie_id", movieId);
                        jsonObject.addProperty("movie_title", movieTitle);
                        jsonObject.addProperty("movie_year", movieYear);
                        jsonObject.addProperty("movie_director", movieDirector);
                        jsonObject.addProperty("movie_rating", movieRating);
                        jsonObject.addProperty("movie_genres", movieGenres);
                        jsonObject.addProperty("movie_actors", movieStars);

                        jsonArray.add(jsonObject);
                    }

                    movieDataRs.close();
                    resultPs.close();
                }

                rs.close();
                ps.close();
                out.write(jsonArray.toString());
                response.setStatus(200);
                return;
            }
            else if (mode.equals("search")) {
                String titleParameter = request.getParameter("title");
                String yearParameter = request.getParameter("year");
                String directorParameter = request.getParameter("director");
                String starNameParameter = request.getParameter("star");

                // Query joining all tables to get all the required info
                query = "SELECT M.id, M.title, M.year, M.director, group_concat(DISTINCT G.name) as genres, R.rating, group_concat(DISTINCT S.id, ',', S.name, ',', (SELECT COUNT(*) from stars_in_movies WHERE stars_in_movies.starId =  SM.starId) SEPARATOR ';') AS actors" +
                        " FROM movies M LEFT JOIN ratings R ON M.id = R.movieId, genres_in_movies GM, genres G, stars S, stars_in_movies SM" +
                        " WHERE M.id = GM.movieId AND G.id = GM.genreId AND S.id = SM.starId AND SM.movieId = M.id";

                if (!titleParameter.isEmpty()) {
                    parameterMap.put(parameterMap.size() + 1, "%" + titleParameter + "%");
                    query += " AND title LIKE ?";
                }
                if (!yearParameter.isEmpty()) {
                    parameterMap.put(parameterMap.size() + 1, yearParameter);
                    query += " AND year = ?";
                }
                if (!directorParameter.isEmpty()) {
                    parameterMap.put(parameterMap.size() + 1, "%" + directorParameter + "%");
                    query += " AND director LIKE ?";
                }
                if (!starNameParameter.isEmpty()) {
                    parameterMap.put(parameterMap.size() + 1, "%" + starNameParameter + "%");
                    query += " AND M.id IN " +
                            "(SELECT movies.id " +
                            "FROM movies, stars_in_movies, stars " +
                            "WHERE movies.id = stars_in_movies.movieId AND stars.id = stars_in_movies.starId AND stars.name LIKE ?)";
                }
                query += " GROUP BY M.id";
            }
            else if (mode.equals("browse")) {
                String genreParameter = request.getParameter("genre");
                String startsWithParameter = request.getParameter("title");
                if (!genreParameter.equals("null")) {

                    parameterMap.put(parameterMap.size() + 1, genreParameter);
                    query = "SELECT SUBQ.id, SUBQ.title, SUBQ.year, SUBQ.director, SUBQ.genres, R.rating, " +
                            "group_concat(DISTINCT S.id, ',', S.name, ',', (SELECT COUNT(*) from stars_in_movies WHERE stars_in_movies.starId =  SM.starId) SEPARATOR ';') AS actors" +
                            " FROM (SELECT M.id, M.title, M.year, M.director, group_concat(G.name) as genres" +
                            " FROM movies M, genres G, genres_in_movies GM WHERE M.id = GM.movieId AND G.id = GM.genreId" +
                            " GROUP BY M.id" +
                            " HAVING count(CASE WHEN G.name = ? THEN 0 END) > 0) AS SUBQ LEFT JOIN ratings R ON SUBQ.id = R.movieId, stars S, stars_in_movies SM" +
                            " WHERE SUBQ.id = SM.movieId AND S.id = SM.starId" +
                            " GROUP BY SUBQ.id";
                }
                else if (!startsWithParameter.equals("null")) {

                    query = "SELECT M.id, M.title, M.year, M.director, group_concat(DISTINCT G.name) as genres, R.rating, group_concat(DISTINCT S.id, ',', S.name, ',', (SELECT COUNT(*) from stars_in_movies WHERE stars_in_movies.starId =  SM.starId) SEPARATOR ';') AS actors" +
                            " FROM movies M LEFT JOIN ratings R ON M.id = R.movieId, genres_in_movies GM, genres G, stars S, stars_in_movies SM" +
                            " WHERE M.id = GM.movieId AND G.id = GM.genreId AND S.id = SM.starId AND SM.movieId = M.id";

                    if (startsWithParameter.equals("*")) {
                        query += " AND title REGEXP '^[^0-9A-Za-z]'";
                    }
                    else {
                        parameterMap.put(parameterMap.size() + 1, startsWithParameter + "%");
                        query += " AND title LIKE ?";
                    }
                    query += " GROUP BY M.id";
                }
                else {
                    throw new Exception();
                }
            }
            else {
                throw new Exception();
            }

            // sortBy and order parameters
            String sortByParameter = request.getParameter("sortBy");
            String orderParameter = request.getParameter("order");
            if (sortByParameter.equals("title")) {
                switch (orderParameter) {
                    case "asc_asc":
                        query += " ORDER BY title ASC, rating ASC";
                        break;
                    case "asc_desc":
                        query += " ORDER BY title ASC, rating DESC";
                        break;
                    case "desc_asc":
                        query += " ORDER BY title DESC, rating ASC";
                        break;
                    case "desc_desc":
                        query += " ORDER BY title DESC, rating DESC";
                        break;
                }
            }
            else if (sortByParameter.equals("rating")) {
                switch (orderParameter) {
                    case "asc_asc":
                        query += " ORDER BY rating ASC, title ASC";
                        break;
                    case "asc_desc":
                        query += " ORDER BY rating ASC, title DESC";
                        break;
                    case "desc_asc":
                        query += " ORDER BY rating DESC, title ASC";
                        break;
                    case "desc_desc":
                        query += " ORDER BY rating DESC, title DESC";
                        break;
                }
            }

            // Pagination
            String numRecordParameter = request.getParameter("numRecords");
            String offset = request.getParameter("offset");

            query += " LIMIT " + numRecordParameter;
            query += " OFFSET " + offset;

            request.getServletContext().log(query);

            PreparedStatement statement = conn.prepareStatement(query);

            if (!parameterMap.isEmpty()) {
                for (Map.Entry<Integer, String> entry : parameterMap.entrySet()) {
                    statement.setString(entry.getKey(), entry.getValue());
                }
            }

            ResultSet rs = statement.executeQuery();

            // End TJ measure
            long endTj = System.nanoTime();
            long elapsedTj = endTj - startTj;

            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                // Getting all columns
                String movieId = rs.getString("id");
                String movieTitle = rs.getString("title");
                String movieYear = rs.getString("year");
                String movieDirector = rs.getString("director");
                String movieRating = rs.getString("rating");

                String movieGenres = rs.getString("genres");

                String movieStars = rs.getString("actors");

                // Creating JsonObject
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_director", movieDirector);
                jsonObject.addProperty("movie_rating", movieRating);
                jsonObject.addProperty("movie_genres", movieGenres);
                jsonObject.addProperty("movie_actors", movieStars);

                jsonArray.add(jsonObject);
            }

            rs.close();
            statement.close();

            // Log to localhost log
            request.getServletContext().log("getting " + jsonArray.size() + " results");

            // Write JSON string to output
            out.write(jsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

            // End TS measure and write
            long endTs = System.nanoTime();
            long elapsedTs = endTs - startTs;

            pout.println(elapsedTs + "," + elapsedTj);
            pout.close();

        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getServletContext().log("entered post method");
        String savedUrl = (String) request.getSession().getAttribute("saved-url");
        PrintWriter out = response.getWriter();
        response.setStatus(200);
        if (savedUrl == null || savedUrl.isEmpty()) {
            out.write("index.html");
        }
        else {
            out.write(savedUrl);
        }
    }
}
