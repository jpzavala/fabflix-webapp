function notifyError() {
    let movieMessageElement = jQuery("#" + movieId + "-message");
    movieMessageElement.html("<strong>Error: Movie was not added</strong>");
}

function notifySuccess(movieId) {
    let movieMessageElement = jQuery("#" + movieId + "-message");
    movieMessageElement.html("<strong>Added to cart successfully</strong>");
}

function bindButton(movieId) {
    let identifier = "#" + movieId + "-button";
    $(identifier).click(function() {
        $.ajax("api/shopping-cart", {
            method: "POST",
            data: {"op": "increase", "movieId": movieId},
            success: function () {
                notifySuccess(movieId);
                console.log("movie added successfully to cart");
            },
            error: function () {
                notifyError();
                console.log(movieId + " could not be added")
            }
        });
    })
}

function linkToSavedStatus (result) {
    $("a[href='#']").attr('href', result);
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

function createPrevAndNext(resultLength, offset, numRecords) {
    // Modify url to create hyperlinks
    let url = new URL(window.location.href);
    let searchParams = new URLSearchParams(url.search);

    // prev
    if (offset !== '0' && offset !== null) {
        let newOffset = Number(offset) - Number(numRecords);
        searchParams.set('offset', newOffset.toString());
        jQuery('#prev').html('<a href="movie-list.html?' + searchParams.toString() + '"> ' +
            '<button class="btn btn-primary"> PREV </button></a>');
    }

    let pageNum;
    if (offset === '0') {
        pageNum = 1;
    }
    else {
        pageNum = Number(offset) / Number(numRecords) + 1;
    }
    jQuery('#pageNumber').html('<strong>Page ' + pageNum.toString() + '</strong>');
    console.log("in prev and next");
    //next (if the length of the results for this page is less than numRecords then no next button)
    if (resultLength >= Number(numRecords)) {
        let newOffset = Number(offset) + Number(numRecords);
        searchParams.set('offset', newOffset.toString());
        jQuery('#next').html('<a href="movie-list.html?' + searchParams.toString() + '"> ' +
            '<button class="btn btn-primary"> NEXT </button></a>');
    }
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 * @param offset String
 * @param numRecords String
 */
function handleMovieResult(resultData, offset, numRecords) {
    let movieRankingElement = jQuery("#movie_ranking");
    for (let i = 0; i < resultData.length; i++) {

        // // first three genres, sorted by alphabetical order, each hyperlinked, as equivalent to browse by this genre.
        let genres = resultData[i]["movie_genres"].split(',');
        let genres_edited = genres.sort().slice(0,3);

        let genreHTML = "";
        for (let j = 0; j < genres_edited.length; j++) {
            if (j !== 0) {
                genreHTML += ", ";
            }
            genreHTML += '<a href="movie-list.html?op=browse&genre=' + genres_edited[j] + '">';
            genreHTML += genres_edited[j] + '</a>';
        }

        let rowHTML = "";

        let rankin = Number(offset) + i + 1;

        rowHTML += '<div class="row justify-content-center">\n' +
            '            <div class="col-1 rank"><h4>' + rankin + '</h4></div>\n' +
            '            <div class="col-9">\n' +
            '                <div class="jumbotron">\n' +
                                '<a href="single-movie.html?id=' + resultData[i]["movie_id"] + '">' +
            '                       <h1 class="display-4">' + resultData[i]["movie_title"] + '</h1>\n' +
                                '</a>' +
            '                    <p class="lead">Year: ' + resultData[i]["movie_year"] + '</p>\n' +
            '                    <p class="lead">Director: ' + resultData[i]["movie_director"] + '</p>\n' +
            '                    <p class="lead">Genre: ' + genreHTML + '</p>\n';

        if (!resultData[i]["movie_rating"]) {
            rowHTML += '<p class="lead">Rating: N/A ' + '</p>\n';
        }
        else {
            rowHTML += '<p class="lead">Rating: ' + resultData[i]["movie_rating"] + '</p>\n';
        }

        rowHTML += '<hr class="my-4">\n';

        let count = 0; // keeps track of how many stars are inserted
        rowHTML += '<p class="lead">Stars: ';

        // Each (starId, star_name) is separated by ';'
        let stars = resultData[i]["movie_actors"].split(';').sort(
            function(a,b) {
                let split_a = a.split(',');
                let split_b = b.split(',');
                // If same number of movies played then sort by name
                if (split_a[2] === split_b[2]) {
                    return split_a[1] > split_b[1] ? 1 : -1;
                }
                return split_b[2] - split_a[2];
            }
        )
        for (let j = 0; j < stars.length; j++) {
            if (count > 0) {
                rowHTML += ", ";
            }

            // Data is separated by (',')
            let star_data = stars[j].split(',');

            rowHTML += '<a href="single-star.html?id=' + star_data[0] + '">'
                + star_data[1] + '</a>';

            count++;
            if (count > 2) { // check for three stars
                break;
            }
        }
        rowHTML += '</p>'

        rowHTML += '<button type="button" class="btn btn-primary" id="' +
            resultData[i]["movie_id"] + '-button">' +
            'Add to Shopping Cart' + '</button>';
        rowHTML += '<br>';
        rowHTML += '<div id="' + resultData[i]["movie_id"] + '-message">' + '</div>'
        rowHTML += '</div>' + '</div>' + '</div>';
        movieRankingElement.append(rowHTML);

        bindButton(resultData[i]["movie_id"]);
    }
    createPrevAndNext(resultData.length, offset, numRecords);
    getSavedStatus();
}

function createSortingOptions() {
    // Grab element by id
    let sortingOptionsElement = jQuery("#sorting_options");

    let rowHTML = "";

    // Modify url to create hyperlinks
    let url = new URL(window.location.href);
    let searchParams = new URLSearchParams(url.search);

    // rating ASC, title ASC
    searchParams.set('sortBy', 'rating');
    searchParams.set('order', 'asc_asc');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> Rating (asc), Title (asc) </a>';

    // rating ASC, title DESC
    searchParams.set('sortBy', 'rating');
    searchParams.set('order', 'asc_desc');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> Rating (asc), Title (desc) </a>';

    // rating DESC, title ASC
    searchParams.set('sortBy', 'rating');
    searchParams.set('order', 'desc_asc');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> Rating (desc), Title (asc) </a>';

    // rating DESC, title DESC
    searchParams.set('sortBy', 'rating');
    searchParams.set('order', 'desc_desc');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> Rating (desc), Title (desc) </a>';

    // title ASC, rating ASC
    searchParams.set('sortBy', 'title');
    searchParams.set('order', 'asc_asc');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> Title (asc), Rating(asc) </a>';

    // title ASC, rating DESC
    searchParams.set('sortBy', 'title');
    searchParams.set('order', 'asc_desc');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> Title (asc), Rating(desc) </a>';

    // title DESC, rating ASC
    searchParams.set('sortBy', 'title');
    searchParams.set('order', 'desc_asc');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> Title (desc), Rating(asc) </a>';

    // title DESC, rating DESC
    searchParams.set('sortBy', 'title');
    searchParams.set('order', 'desc_desc');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> Title (desc), Rating(desc) </a>';

    sortingOptionsElement.append(rowHTML);
}

function createNOptions() {
    let NOptionsElement = jQuery("#n_options");

    let rowHTML = "";

    // Modify url to create hyperlinks
    let url = new URL(window.location.href);
    let searchParams = new URLSearchParams(url.search);

    // N = 10
    searchParams.set('numRecords', '10');
    searchParams.set('offset', '0');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> N = 10 </a>';

    // N = 25
    searchParams.set('numRecords', '25');
    searchParams.set('offset', '0');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> N = 25 </a>';

    // N = 50
    searchParams.set('numRecords', '50');
    searchParams.set('offset', '0');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> N = 50 </a>';

    // N = 100
    searchParams.set('numRecords', '100');
    searchParams.set('offset', '0');
    rowHTML += '<a class="dropdown-item" href="movie-list.html?' + searchParams.toString();
    rowHTML += '"> N = 100 </a>';
    NOptionsElement.append(rowHTML);
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */
createSortingOptions();
createNOptions();

let urlToSend = "api/movies?op=";
let op = getParameterByName('op');
urlToSend += op;

if (op === "fulltext") {
    urlToSend += "&clues=" + getParameterByName("clues");
}
else if (op === "search") {
    let title = getParameterByName('title');
    let year = getParameterByName('year');
    let director = getParameterByName('director');
    let starName = getParameterByName('star');
    urlToSend += "&title=" + title;
    urlToSend += "&year=" + year;
    urlToSend += "&director=" + director;
    urlToSend += "&star=" + starName;
}
else {
    let genreP = getParameterByName('genre');
    let starsWith = getParameterByName('startsWith');
    urlToSend += "&genre=" + genreP;
    urlToSend += "&title=" + starsWith;
}

// sort by certain data and order
let sortBy = getParameterByName('sortBy');
urlToSend += "&sortBy=" + sortBy;
let order = getParameterByName('order');
urlToSend += "&order=" + order;

// num of results and offset (pagination)
let numRecords = getParameterByName('numRecords');
if (!numRecords) {
    urlToSend += "&numRecords=100";
    numRecords = '100';
}
else {
    urlToSend += "&numRecords=" + numRecords;
}

let offset = getParameterByName('offset');
if (!offset) {
    urlToSend += "&offset=0";
    offset = '0';
}
else {
    urlToSend += "&offset=" + offset;
}

console.log(urlToSend);

// Makes the HTTP GET request and registers on success callback function handleStarResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    // Setting request url, which is mapped by MoviesServlet in MoviesServlet.java
    url: urlToSend,
    success: (resultData) => handleMovieResult(resultData, offset, numRecords)
})
