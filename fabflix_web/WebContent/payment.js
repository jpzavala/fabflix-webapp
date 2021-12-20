let paymentForm = $("#payment-form");

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

function handlePaymentResult(resultData) {
    let resultDataJson = JSON.parse(resultData);

    // If login succeeds, it will redirect the user to index.html
    if (resultDataJson["status"] === "success") {
        window.location.replace("confirmation.html");
    } else {
        // If login fails, the web page will display
        // error messages on <div> with id "login_error_message"
        $("#payment_error_message").text("Error:" + resultDataJson["message"]);
    }
}

function submitPaymentForm(formSubmitEvent) {
    formSubmitEvent.preventDefault();
    $.ajax(
        "api/checkout", {
            method: "POST",
            // Serialize the login form to the data sent by POST request
            data: paymentForm.serialize(),
            success: handlePaymentResult
        }
    );
}

/* Script starts */
getSavedStatus();

$.ajax("api/shopping-cart", {
    method: "POST",
    data: {"op": "get-total"},
    success: resultData => {
        let totalPriceElement = jQuery("#total-price-area")
        totalPriceElement.append("<p>Total Price (Amount to Pay): $" + resultData + "</p>");
    }
});

paymentForm.submit(submitPaymentForm);