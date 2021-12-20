package edu.uci.ics.fabflixmobile.ui.movielist;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.uci.ics.fabflixmobile.R;
import edu.uci.ics.fabflixmobile.data.NetworkManager;
import edu.uci.ics.fabflixmobile.data.model.Movie;
import edu.uci.ics.fabflixmobile.ui.MoviePageActivity;

import java.net.URLEncoder;
import java.util.ArrayList;

public class MovieListActivity extends AppCompatActivity {

    private final String host = "ec2-18-118-136-1.us-east-2.compute.amazonaws.com";
    private final String port = "8443";
    private final String domain = "cs122b-fall21-project3";
    private final String baseURL = "https://" + host + ":" + port + "/" + domain;

    private int currentOffset = 0;

    private String query;
    final ArrayList<Movie> movies = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movielist);
        // TODO: this should be retrieved from the backend server

        // Getting query
        Bundle extras = getIntent().getExtras();
        query = extras.getString("query");

        fillList();

        Button prevButton = findViewById(R.id.prevButton);
        Button nextButton = findViewById(R.id.nextButton);

        prevButton.setOnClickListener(v -> {
            ListView listView = findViewById(R.id.list);
            listView.setAdapter(null);
            movies.clear();
            currentOffset -= 20;
            fillList();
        });

        nextButton.setOnClickListener(v -> {
            ListView listView = findViewById(R.id.list);
            listView.setAdapter(null);
            movies.clear();
            currentOffset += 20;
            fillList();
        });
    }

    private void fillList() {
        // HTTP request to MovieList
        final RequestQueue queue = NetworkManager.sharedManager(this).queue;
        // request type is GET
        final StringRequest moviesRequest = new StringRequest(
                Request.Method.GET,
                baseURL + "/api/movies?op=fulltext&numRecords=20"
                        + "&offset=" + currentOffset
                        + "&clues=" + URLEncoder.encode(query),
                response -> {
                    // TODO: should parse the json response to redirect to appropriate functions
                    //  upon different response value.
                    try {
                        JSONArray jsonArray = new JSONArray(response);

                        Button prevButton = findViewById(R.id.prevButton);
                        Button nextButton = findViewById(R.id.nextButton);

                        if (currentOffset == 0) {
                            prevButton.setVisibility(View.INVISIBLE);
                        }
                        else {
                            prevButton.setVisibility(View.VISIBLE);
                        }

                        if (jsonArray.length() == 20) {
                            nextButton.setVisibility(View.VISIBLE);
                        }
                        else {
                            nextButton.setVisibility(View.INVISIBLE);
                        }

                        Log.d("movielist.success", response);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            movies.add(new Movie(
                                    (String) jsonObject.get("movie_title"),
                                    Short.parseShort((String) jsonObject.get("movie_year")),
                                    (String) jsonObject.get("movie_id"),
                                    (String) jsonObject.get("movie_director"),
                                    (String) jsonObject.get("movie_genres"),
                                    (String) jsonObject.get("movie_actors")));
                            MovieListViewAdapter adapter = new MovieListViewAdapter(this, movies);
                            ListView listView = findViewById(R.id.list);
                            listView.setAdapter(adapter);
                            listView.setOnItemClickListener((parent, view, position, id) -> {
                                Movie movie = movies.get(position);
                                Log.d("movielist.clicked", "clicked on: " + movie.getId());
                                Intent MoviePage = new Intent(MovieListActivity.this, MoviePageActivity.class);
                                MoviePage.putExtra("movieId", movie.getId());
                                startActivity(MoviePage);
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // error
                    Log.d("movielist.error", error.toString());
                });
        // important: queue.add is where request is actually sent
        queue.add(moviesRequest);
    }
}