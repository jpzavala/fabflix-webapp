let login_employee_form = $("#login_employee_form");

function handleEmployeeLoginResult (resultDataString) {
    let resultJson = JSON.parse(resultDataString);

    console.log("handling employee login response");

    // If login succeeds, it will redirect the user to index.html
    if (resultJson["status"] === "success") {
        window.location.replace("dashboard.html");
    } else {
        // If login fails, the web page will display
        // error messages on <div> with id "login_error_message"
        let loginErrorElement = $("#login_error_message");
        loginErrorElement.text(resultJson["message"]);
        loginErrorElement.removeAttr("hidden");
    }
}

function submitEmployeeLoginForm(formSubmitEvent) {
    console.log("submit login form for employee");
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();

    $.ajax(
        "api/_dashboard", {
            method: "POST",
            // Serialize the login form to the data sent by POST request
            data: login_employee_form.serialize(),
            success: handleEmployeeLoginResult
        }
    );
}

// Bind the submit action of the form to a handler function
login_employee_form.submit(submitEmployeeLoginForm);
