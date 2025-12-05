
package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.PhotoAdapter;
import com.example.myapplication.model.Album;
import com.example.myapplication.model.Photo;
import com.example.myapplication.model.TagType;
import com.example.myapplication.util.SearchManager;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private List<Album> albums;
    private Spinner tagType1Spinner, tagType2Spinner, operatorSpinner;
    private AutoCompleteTextView tagValue1Input, tagValue2Input;
    private RecyclerView resultsGrid;
    private PhotoAdapter resultsAdapter;
    private TextView noResultsMessage;
    private TextView resultsTitle;
    private List<Photo> searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Load albums from centralized DataStore
        albums = com.example.myapplication.util.DataStore.getAlbums(this);
        if (albums == null) albums = new ArrayList<>();

        searchResults = new ArrayList<>();

        // Setup spinners
        tagType1Spinner = findViewById(R.id.tag_type_spinner);
        tagType2Spinner = findViewById(R.id.tag_type_spinner2);
        operatorSpinner = findViewById(R.id.operator_spinner);

        setupSpinner(tagType1Spinner, new String[]{"Person", "Location"});
        setupSpinner(tagType2Spinner, new String[]{"Person", "Location"});
        setupSpinner(operatorSpinner, new String[]{"AND", "OR"});

        // Setup autocomplete
        tagValue1Input = findViewById(R.id.tag_value_input);
        tagValue2Input = findViewById(R.id.tag_value_input2);

        updateAutocompleteSuggestions();

        // Setup results
        resultsGrid = findViewById(R.id.search_results_grid);
        noResultsMessage = findViewById(R.id.no_results_message);
        resultsTitle = findViewById(R.id.results_title);
        resultsAdapter = new PhotoAdapter(this, searchResults, this);
        resultsGrid.setLayoutManager(new GridLayoutManager(this, 3));
        resultsGrid.setAdapter(resultsAdapter);

        // Setup buttons
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        Button searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> performSearch());

        // Update autocomplete when tag type changes
        tagType1Spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateTagValue1Suggestions();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        tagType2Spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateTagValue2Suggestions();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void updateAutocompleteSuggestions() {
        updateTagValue1Suggestions();
        updateTagValue2Suggestions();
    }

    private void updateTagValue1Suggestions() {
        TagType type = getSelectedTagType(tagType1Spinner);
        List<String> suggestions = SearchManager.getTagValueSuggestions(albums, type);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, suggestions);
        tagValue1Input.setAdapter(adapter);
        tagValue1Input.setThreshold(1);

        // Update suggestions dynamically as user types
        tagValue1Input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String prefix = s.toString();
                List<String> filtered = SearchManager.getAutocompleteSuggestions(albums, type, prefix);
                ArrayAdapter<String> newAdapter = new ArrayAdapter<>(SearchActivity.this,
                        android.R.layout.simple_dropdown_item_1line, filtered);
                tagValue1Input.setAdapter(newAdapter);
                tagValue1Input.showDropDown();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void updateTagValue2Suggestions() {
        TagType type = getSelectedTagType(tagType2Spinner);
        List<String> suggestions = SearchManager.getTagValueSuggestions(albums, type);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, suggestions);
        tagValue2Input.setAdapter(adapter);
        tagValue2Input.setThreshold(1);

        tagValue2Input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String prefix = s.toString();
                List<String> filtered = SearchManager.getAutocompleteSuggestions(albums, type, prefix);
                ArrayAdapter<String> newAdapter = new ArrayAdapter<>(SearchActivity.this,
                        android.R.layout.simple_dropdown_item_1line, filtered);
                tagValue2Input.setAdapter(newAdapter);
                tagValue2Input.showDropDown();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private TagType getSelectedTagType(Spinner spinner) {
        String selected = spinner.getSelectedItem().toString();
        return TagType.fromString(selected);
    }

    private void performSearch() {
        String tagValue1 = tagValue1Input.getText().toString().trim();

        if (tagValue1.isEmpty()) {
            Toast.makeText(this, "Enter at least one tag value", Toast.LENGTH_SHORT).show();
            return;
        }

        TagType type1 = getSelectedTagType(tagType1Spinner);
        String tagValue2 = tagValue2Input.getText().toString().trim();

        if (tagValue2.isEmpty()) {
            // Single criterion search
            searchResults = SearchManager.searchByTag(albums, type1, tagValue1);
        } else {
            // Dual criterion search
            TagType type2 = getSelectedTagType(tagType2Spinner);
            String operatorStr = operatorSpinner.getSelectedItem().toString();
            SearchManager.SearchOperator operator = operatorStr.equals("AND") ?
                    SearchManager.SearchOperator.AND : SearchManager.SearchOperator.OR;

            searchResults = SearchManager.searchByTags(albums, type1, tagValue1, type2, tagValue2, operator);
        }

        resultsAdapter.updatePhotos(searchResults);
        updateResultsDisplay();
    }

    private void updateResultsDisplay() {
        if (searchResults.isEmpty()) {
            resultsGrid.setVisibility(RecyclerView.GONE);
            noResultsMessage.setVisibility(TextView.VISIBLE);
            resultsTitle.setVisibility(TextView.GONE);
        } else {
            resultsGrid.setVisibility(RecyclerView.VISIBLE);
            noResultsMessage.setVisibility(TextView.GONE);
            resultsTitle.setVisibility(TextView.VISIBLE);
            resultsTitle.setText("Found " + searchResults.size() + " photo(s)");
        }
    }

    @Override
    public void onPhotoClick(Photo photo) {
        // Find which album this photo belongs to
        Album photoAlbum = null;
        for (Album album : albums) {
            if (album.getPhotos().contains(photo)) {
                photoAlbum = album;
                break;
            }
        }

        if (photoAlbum != null) {
            Intent intent = new Intent(this, PhotoActivity.class);
            intent.putExtra("album", photoAlbum);
            // pass the full albums list so PhotoActivity can update/save consistently
            intent.putExtra("allAlbums", new java.util.ArrayList<>(albums));
            intent.putExtra("photoIndex", photoAlbum.getPhotos().indexOf(photo));
            startActivity(intent);
        }
    }

    @Override
    public void onPhotoLongClick(Photo photo) {
        // Not used in search view
    }
}
