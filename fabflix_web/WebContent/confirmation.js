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

function handleResult(result) {
    result = JSON.parse(result);
    let saleElement = jQuery("#sale_info");
    console.log(result);
    saleElement.append("<p>SalesID: ");
    for (let i = 0; i < result.length; i++) {
        let saleid = Number(result[i]["saleId"]) - i;
        saleElement.append(saleid.toString() + " ");
    }
    saleElement.append("</p>");
    saleElement.append("<p>Movies: </p>");
    for (let i = 0; i < result.length; i++) {
        saleElement.append("<p>" + result[i]["title"] + ": " + result[i]["qty"] + "</p>");
    }
    saleElement.append("<p>Final Price: " + result[0]["finalPrice"] + "</p>")
}

getSavedStatus();

jQuery.ajax ({
    method: "POST",
    datatype: "json",
    data: {"op": "confirmSale"},
    url: "api/checkout",
    success: (result) => handleResult(result)
})