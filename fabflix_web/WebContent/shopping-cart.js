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

function createButtons (resultData) {
    console.log("in function create buttons");
    for (let i = 0; i < resultData.length; i++) {

        let identifier = "#" + resultData[i]["movie_id"] + "-inc-button";
        $(identifier).click(function() {
            $.ajax("api/shopping-cart", {
                method: "POST",
                data: {"op": "increase", "movieId": resultData[i]["movie_id"]},
                success: function () {
                    location.reload();
                }
            });
        })

        identifier = "#" + resultData[i]["movie_id"] + "-dec-button";
        $(identifier).click(function() {
            $.ajax("api/shopping-cart", {
                method: "POST",
                data: {"op": "decrease", "movieId": resultData[i]["movie_id"]},
                success: function () {
                    location.reload();
                }
            });
        })

        identifier = "#" + resultData[i]["movie_id"] + "-del-button";
        $(identifier).click(function() {
            $.ajax("api/shopping-cart", {
                method: "POST",
                data: {"op": "delete", "movieId": resultData[i]["movie_id"]},
                success: function () {
                    location.reload();
                }
            });
        })
    }
}

function handleShoppingCart(resultData) {
    let shoppingCartElement = jQuery("#shopping_table_body");
    console.log(resultData)
    let rowHTML = '<tbody id="shopping_table_body">';
    let totalPrice = 0;
    for (let i = 0; i < resultData.length; i++) {
        rowHTML += "<tr>";
        rowHTML += "<th>" + resultData[i]["movie_title"] + "</th>";
        rowHTML += "<th>" + resultData[i]["quantity"] + "</th>";
        rowHTML += "<th> $10 </th>";
        rowHTML += "<th><button class='btn btn-primary' id='" + resultData[i]["movie_id"] + "-inc-button'>" + "Increase" + "</button></th>";
        rowHTML += "<th><button class='btn btn-primary' id='" + resultData[i]["movie_id"] + "-dec-button'>" + "Decrease" + "</button></th>";
        rowHTML += "<th><button class='btn btn-primary' id='" + resultData[i]["movie_id"] + "-del-button'>" + "Delete" + "</button></th>";
        rowHTML += "</tr>";

        totalPrice += Number(resultData[i]["quantity"]) * 10;
    }
    rowHTML += '</tbody>';
    shoppingCartElement.replaceWith(rowHTML);
    createButtons(resultData);

    let totalPriceElement = jQuery("#total_price");
    totalPriceElement.append("<p>Total Price: $" + totalPrice + "</p>");

    $.ajax("api/shopping-cart", {
        method: "POST",
        data: {"op": "save-total", "total-price": totalPrice.toString()},
        success: function() {
            console.log("total price saved");
        }
    });
}

/* Script starts */
getSavedStatus();

$.ajax("api/shopping-cart", {
    dataType: "json",
    method: "GET",
    data: {"op": "land"},
    success: handleShoppingCart
});