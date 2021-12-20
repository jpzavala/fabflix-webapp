package edu.uci.ics.fabflixmobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;

import edu.uci.ics.fabflixmobile.R;
import edu.uci.ics.fabflixmobile.ui.movielist.MovieListActivity;

public class MainPageActivity extends AppCompatActivity {

    SearchView sv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        sv = (SearchView) findViewById(R.id.searchMainPage);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                sv.clearFocus();
                Log.d("fabflix_mobile", "submitted search: " + sv.getQuery());
                Intent MovieListPage = new Intent(MainPageActivity.this, MovieListActivity.class);
                MovieListPage.putExtra("query", sv.getQuery().toString());
                // activate the list page.
                startActivity(MovieListPage);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d("fabflix_mobile", "changed text");
                return true;
            }
        });
    }
}