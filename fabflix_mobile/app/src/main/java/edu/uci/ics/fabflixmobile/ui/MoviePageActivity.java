package edu.uci.ics.fabflixmobile.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Iterator;

import edu.uci.ics.fabflixmobile.R;
import edu.uci.ics.fabflixmobile.data.NetworkManager;

public class MoviePageActivity extends AppCompatActivity {

    private final String host = "ec2-18-118-136-1.us-east-2.compute.amazonaws.com";
    private final String port = "8443";
    private final String domain = "cs122b-fall21-project3";
    private final String baseURL = "https://" + host + ":" + port + "/" + domain;

    TextView tvTitle;
    TextView tvYear;
    TextView tvDirector;
    TextView tvGenres;
    TextView tvStars;

    private String movieId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_page);

        tvTitle = findViewById(R.id.movieTitle);
        tvYear = findViewById(R.id.movieYear);
        tvDirector = findViewById(R.id.movieDirector);
        tvGenres = findViewById(R.id.movieGenres);
        tvStars = findViewById(R.id.movieStars);

        // Getting movie id
        Bundle extras = getIntent().getExtras();
        movieId = extras.getString("movieId");

        // Send HTTP Request to SingleMovieServlet
        final RequestQueue queue = NetworkManager.sharedManager(this).queue;
        // request type is GET
        final StringRequest singleMovieRequest = new StringRequest(
                Request.Method.GET,
                baseURL + "/api/single-movie?id=" + URLEncoder.encode(movieId),
                response -> {
                    // TODO: should parse the json response to redirect to appropriate functions
                    //  upon different response value.
                    try {
                        Log.d("moviepage.success", response);
                        JSONArray jsonArray = new JSONArray(response);
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        Log.d("moviepage.success", (String) jsonObject.get("movie_title"));

                        tvTitle.setText((String) jsonObject.get("movie_title"));
                        tvYear.setText((String) jsonObject.get("movie_year"));
                        tvDirector.setText((String) jsonObject.get("movie_director"));

                        tvGenres.setText("Genres: " + (String) jsonObject.get("movie_genres"));

                        String stars = "Stars: ";
                        int i = 0;
                        JSONObject starsObject = (JSONObject) jsonObject.get("movie_actors");
                        Iterator keys = starsObject.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            if (i == 0) {
                                stars += starsObject.get(key);
                            }
                            else {
                                stars += ", " + starsObject.get(key);
                            }
                            i++;
                        }
                        tvStars.setText(stars);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // error
                    Log.d("moviepage.error", error.toString());
                });
        // important: queue.add is where request is actually sent
        queue.add(singleMovieRequest);
    }
}