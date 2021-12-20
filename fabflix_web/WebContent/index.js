/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleGenresResult (resultData) {
    //console.log(resultData);

    // Generates genre list, each genre hyperlinked
    let genreListElement = jQuery("#genre-list");
    let rowHTML = "<p><strong>By Genre:</strong></p>";
    for (let i = 0; i < resultData.length; i++) {
        rowHTML += '<a href="movie-list.html?op=browse&genre=' +
            resultData[i]["genre-name"] + '">';
        rowHTML += resultData[i]["genre-name"] + ' </a>';
    }
    genreListElement.append(rowHTML);
}

// Populates list to browse by (0,1,2,3..A,B,C...X,Y,Z) characters
function populateFirstCharacterList() {
    let firstCharacterElement = jQuery("#first-character-list");
    let rowHTML = "<p><strong>By Title:</strong></p>";
    for (let i = 0; i < 10; i++) {
        rowHTML += '<a href="movie-list.html?op=browse&startsWith=' +
            i + '">';
        rowHTML += i + ' </a>';
    }
    firstCharacterElement.append(rowHTML);
    let alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ*".split("");
    rowHTML = "";
    for (let i = 0; i < alphabet.length; i++) {
        rowHTML += '<a href="movie-list.html?op=browse&startsWith=' +
            alphabet[i] + '">';
        rowHTML += alphabet[i] + ' </a>';
    }
    firstCharacterElement.append(rowHTML);
}


// --- Full Text Search / Autocomplete ---

function handleLookupAjaxSuccess(data, query, doneCallback) {
    // TODO: if you want to cache the result into a global variable you can do it here
    localStorage.setItem(query, data);

    // parse the string into JSON
    let jsonData = JSON.parse(data);
    console.log(jsonData);

    // call the callback function provided by the autocomplete library
    // add "{suggestions: jsonData}" to satisfy the library response format according to
    //   the "Response Format" section in documentation
    doneCallback( { suggestions: jsonData } );
}

function handleLookup(query, doneCallback) {
    console.log("Autocomplete search initiated (after delay)");
    // TODO: if you want to check past query results first, you can do it here
    if (localStorage.getItem(query) !== null) {
        console.log("Autocomplete query found in cache!");
        handleLookupAjaxSuccess(localStorage.getItem(query), query, doneCallback);
        return;
    }

    console.log("Sending ajax request to server (NOT cached)")

    // sending the HTTP GET request to the Java Servlet endpoint hero-suggestion
    // with the query data
    jQuery.ajax({
        "method": "GET",
        // generate the request url from the query.
        // escape the query string to avoid errors caused by special characters
        "url": "movie_suggestion?query=" + escape(query),
        "success": function(data) {
            // pass the data, query, and doneCallback function into the success handler
            handleLookupAjaxSuccess(data, query, doneCallback);
        },
        "error": function(errorData) {
            console.log("lookup ajax error");
            console.log(errorData);
        }
    })
}

function handleSelectSuggestion(suggestion) {
    console.log(suggestion);
    window.location.href = "single-movie.html?id=" + suggestion['data']['movieId'];
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Makes the HTTP GET request and registers on success callback function handleStarResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/mainpage", // Setting request url, which is mapped by MoviesServlet in MoviesServlet.java
    success: (resultData) => handleGenresResult(resultData)
});

// Write browsing list
populateFirstCharacterList();


// --- Full Text Search / Autocomplete ---

$('#autocomplete').autocomplete({
    // documentation of the lookup function can be found under the "Custom lookup function" section
    lookup: function (query, doneCallback) {
        handleLookup(query, doneCallback);
    },
    onSelect: function(suggestion) {
        handleSelectSuggestion(suggestion);
    },
    // set delay time
    deferRequestBy: 300,
    // there are some other parameters that you might want to use to satisfy all the requirements
    // TODO: add other parameters, such as minimum characters
    minChars: 3
});


