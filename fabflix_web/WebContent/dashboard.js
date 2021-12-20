let add_star_form = $("#add_star_form");
let metadata_button = $("#metadata_button");
let add_movie_form = $("#add_movie_form");

function handleAddStarResult(resultData) {
    let resultDataJson = JSON.parse(resultData);
    let addStarMessageElement = $("#add_star_message");

    // If login succeeds, it will redirect the user to index.html
    if (resultDataJson["status"] === "success") {
        addStarMessageElement.text("Success: star added! - StarID = " + resultDataJson['starId']);
        addStarMessageElement.removeAttr("hidden");
    } else {
        addStarMessageElement.text("Error: Star not added");
        addStarMessageElement.removeAttr("hidden");
    }
}

function submitStarForm(formSubmitEvent) {
    console.log("submitting add a star form");

    formSubmitEvent.preventDefault();

    // Checking if user entered required star name
    let starInput = $("#input-star-name");
    if (starInput && starInput.val() === "") {
        let addStarMessageElement = $("#add_star_message");
        addStarMessageElement.text("Error: Please enter a star name (required)");
        addStarMessageElement.removeAttr("hidden");
        return;
    }

    $.ajax(
        "api/dashboard", {
            method: "POST",
            // Serialize add_star_form to the data sent by POST request
            data: add_star_form.serialize(),
            success: handleAddStarResult
        }
    );
}

function handleMetadataResult(resultData) {
    let resultDataJson = JSON.parse(resultData);
    let metadataInfoElement = $("#metadata_info");
    let infoHTML = "";

    console.log(resultDataJson)
    for (let key in resultDataJson) {
        infoHTML += "<p>" + key + ": ";
        for (let i = 0; i < resultDataJson[key].length; i++) {
            if (i === 0) {
                infoHTML += resultDataJson[key][i]['name'] + " - " + resultDataJson[key][i]['type'];
            }
            else {
                infoHTML += ", " + resultDataJson[key][i]['name'] + " - " + resultDataJson[key][i]['type'];
            }
        }
        infoHTML += "</p>";
    }
    metadataInfoElement.html(infoHTML);
}

function clickedMetadataButton() {
    console.log("clicked metadata button");
    $.ajax(
        "api/dashboard", {
            method: "POST",
            data: {"op": "metadata"},
            success: handleMetadataResult,
        }
    );
}

function handleAddMovieResult(resultData) {
    let resultJSON = JSON.parse(resultData);
    console.log(resultJSON);
    let addMovieMessageElement = $("#add_movie_message");
    if (resultJSON['status'] === "success") {
        addMovieMessageElement.text("Success: Movie was added!" + " - " + "MovieID = " + resultJSON['confirmationMovieId']
        + ", GenreID = " + resultJSON['confirmationGenreId'] + ", starID = " + resultJSON['confirmationStarId']);
        addMovieMessageElement.removeAttr("hidden");
    }
    else {
        addMovieMessageElement.text("Error: Movie was not added - " + resultJSON['errorMessage']);
        addMovieMessageElement.removeAttr("hidden");
    }
}

function submitMovieForm(formSubmitEvent) {
    console.log("submitting add a movie form");

    formSubmitEvent.preventDefault();

    $.ajax(
        "api/dashboard", {
            method: "POST",
            // Serialize add_star_form to the data sent by POST request
            data: add_movie_form.serialize(),
            success: handleAddMovieResult
        }
    );
}

add_star_form.submit(submitStarForm);
metadata_button.click(clickedMetadataButton);
add_movie_form.submit(submitMovieForm);