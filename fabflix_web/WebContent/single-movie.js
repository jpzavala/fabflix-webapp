function notifyError() {
    let shoppingElement = jQuery("#shopping-area");
    shoppingElement.html("<strong>Error: Movie was not added</strong>");
}

function notifySuccess () {
    let shoppingElement = jQuery("#shopping-area");
    shoppingElement.html("<p><strong>Added to cart successfully!</strong></p>")
}

function linkToSavedStatus (result) {
    $("a[href='movie-list.html']").attr('href', result);
}

function getSavedStatus() {
    jQuery.ajax ({
        method: "POST", // Setting request method
        // Setting request url, which is mapped by MoviesServlet in MoviesServlet.java
        url: "api/movies",
        success: (result) => linkToSavedStatus(result)
    })
}

/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */

function handleResult(resultData) {

    console.log("handleResult: populating movie info from resultData");

    // populate the movie info h3
    // find the empty h3 body by id "movie_info"
    let movieInfoElement = jQuery("#movie_info");

    // Format genres (ALL GENRES), sorted by alphabetical order
    let genres = resultData[0]["movie_genres"].split(',').sort();

    // each genre hyperlinked, as equivalent to browse by this genre
    let genreHTML = "";
    for (let i = 0; i < genres.length; i++) {
        if (i !== 0) {
            genreHTML += ", ";
        }
        genreHTML += '<a href="movie-list.html?op=browse&genre=' + genres[i] + '">';
        genreHTML += genres[i] + '</a>';
    }

    let ratingText = "";
    if (!resultData[0]["movie_rating"]) {
        ratingText = "N/A";
    }
    else {
        ratingText = resultData[0]["movie_rating"];
    }

    // append two html <p> created to the h3 body, which will refresh the page
    movieInfoElement.append("<h3 class='card-title'>" + resultData[0]["movie_title"] + "</h3>" +
        "<p>Year: " + resultData[0]["movie_year"] + "</p>" +
        "<p>Director: " + resultData[0]["movie_director"] + "</p>" +
        "<p>Genre(s): " + genreHTML + "</p>" +
        "<p>Rating: " + ratingText + "</p>");
    
    console.log("handleResult: populating stars table for single movie from resultData");

    // Populate the star table
    // Find the empty table body by id "star_table_body"
    let starTableBodyElement = jQuery("#star_table_body");

    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let key in resultData[0]["movie_actors"]) {
        // Concatenate the html tags with resultData jsonObject
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML +=
            "<th>" +
            // Add a link to single-star.html with id passed with GET url parameter
            '<a href="single-star.html?id=' + key + '">'
            + resultData[0]["movie_actors"][key] +     // display star's name for hyperlink
            '</a>' +
            "</th>";
        rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        starTableBodyElement.append(rowHTML);
    }
    getSavedStatus();

    $("#shopping-button").click(function() {
        $.ajax("api/shopping-cart", {
            method: "POST",
            data: {"op": "land", "movieId": resultData[0]['movie_id']},
            success: notifySuccess,
            error: notifyError
        });
    })
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let movieId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-movie?id=" + movieId, // Setting request url, which is mapped by SingleMovieServlet in SingleMovieServlet.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleMovieServlet
});