package edu.uci.ics.fabflixmobile.ui.movielist;

import edu.uci.ics.fabflixmobile.R;
import edu.uci.ics.fabflixmobile.data.model.Movie;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class MovieListViewAdapter extends ArrayAdapter<Movie> {
    private final ArrayList<Movie> movies;

    // View lookup cache
    private static class ViewHolder {
        TextView title;
        TextView subtitle;
        TextView director;
        TextView genres;
        TextView stars;
    }

    public MovieListViewAdapter(Context context, ArrayList<Movie> movies) {
        super(context, R.layout.movielist_row, movies);
        this.movies = movies;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the movie item for this position
        Movie movie = movies.get(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            // If there's no view to re-use, inflate a brand new view for row
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.movielist_row, parent, false);
            viewHolder.title = convertView.findViewById(R.id.title);
            viewHolder.subtitle = convertView.findViewById(R.id.subtitle);
            viewHolder.director = convertView.findViewById(R.id.director);
            viewHolder.genres = convertView.findViewById(R.id.genres);
            viewHolder.stars = convertView.findViewById(R.id.stars);
            // Cache the viewHolder object inside the fresh view
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data from the data object via the viewHolder object
        // into the template view.
        viewHolder.title.setText(movie.getName());
        viewHolder.subtitle.setText(movie.getYear() + "");
        viewHolder.director.setText(movie.getDirector());

        // First three genres
        String genreNames = "";
        ArrayList<String> splitGenres = new ArrayList<>(Arrays.asList(movie.getGenres().split(",")));
        Collections.sort(splitGenres);

        for (int i = 0; i < splitGenres.size() && i < 3; i++) {
            if (i == 0) {
                genreNames += splitGenres.get(i);
            }
            else {
                genreNames += ", " + splitGenres.get(i);
            }
        }
        viewHolder.genres.setText("Genres: " + genreNames);

        // First three stars and display star names only
        String starNames = "";
        ArrayList<String> splitStars = new ArrayList<>(Arrays.asList(movie.getStars().split(";")));
        Collections.sort(splitStars, Comparator.comparingInt(s -> -Integer.parseInt(s.split(",")[2])));

        for (int i = 0; i < splitStars.size() && i < 3; i++) {
            String[] starPart = splitStars.get(i).split(",");
            if (i == 0) {
                starNames += starPart[1];
            }
            else {
                starNames += ", " + starPart[1];
            }
        }

        viewHolder.stars.setText("Stars: " + starNames);
        // Return the completed view to render on screen
        return convertView;
    }
}