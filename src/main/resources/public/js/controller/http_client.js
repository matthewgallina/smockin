
app.controller('httpClientController', function($scope, $location, $http, $timeout, $uibModalInstance, utils, globalVars, restClient, data) {


    //
    // Constants / Vars
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;

    //
    // Labels
    $scope.clientHeading = "HTTP Client";
    $scope.urlLabel = "Request Path";
    $scope.urlPlaceholderTxt = "HTTP endpoint URL to call (e.g /hello)";
    $scope.methodDropDownLabel = "Select...";
    $scope.requestBodyLabel = "Request Body";
    $scope.requestHeadersLabel = "Request Headers";
    $scope.requestHeaderNameLabel = 'Name';
    $scope.requestHeaderValueLabel = 'Value';
    $scope.removeRequestHeaderButtonLabel = 'X';
    $scope.addRequestHeaderButtonLabel = 'New Row';
    $scope.clientResponseLabel = "Response";
    $scope.noHeadersDefinedLabel = "No Headers Defined";


    //
    // Buttons
    $scope.clearButtonLabel = 'Clear';
    $scope.closeButtonLabel = 'Close';
    $scope.sendButtonLabel = 'Send';


    //
    // Alerts
    $scope.alerts = [];

    var closeAlertFunc = function() {
        $scope.alerts = [];
    };

   function showAlert(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        $scope.alerts = [];
        $scope.alerts.push({ "type" : type, "msg" : msg });

        $timeout(closeAlertFunc, AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.httpMethods = globalVars.httpMethods;

    $scope.clientRequest = {
        "url" : null,
        "method" : null,
        "body" : null,
        "headers" : []
    };

    if (data.state != null) {
        $scope.clientRequest = {
            "url" : data.state.url,
            "method" : data.state.method,
            "body" : data.state.body,
            "headers" : data.state.headers
        };
    }

    $scope.clientResponse = null;

    //
    // Functions
    $scope.doAddRequestHeaderRow = function() {
        $scope.clientRequest.headers.push({ "name" : null, "value" : null });
    };

    $scope.doRemoveRequestHeaderRow = function(index) {
        $scope.clientRequest.headers.splice(index, 1);
    };

    $scope.doSelectHttpMethod = function(m) {
        $scope.clientRequest.method = m;
    };

    $scope.doSend = function() {

        $scope.clientResponse = "";

        // Validation
        if (utils.isBlank($scope.clientRequest.url)) {
            showAlert("'Request Path' is required");
            return;
        }

        if (!$scope.clientRequest.url.startsWith("/")) {
            showAlert("Invalid 'Request Path'. (expected format: /hello)");
            return;
        }

        if (utils.isBlank($scope.clientRequest.method)) {
            showAlert("'Request Method' is required");
            return;
        }

        if (($scope.clientRequest.method == "POST"
            || $scope.clientRequest.method == "PUT"
            || $scope.clientRequest.method == "PATCH")
                && $scope.clientRequest.body == null) {
            showAlert("'Request Body' is required");
            return;
        }

        for (var h=0; h < $scope.clientRequest.headers.length; h++) {
            if (utils.isBlank($scope.clientRequest.headers[h].name)
                    || utils.isBlank($scope.clientRequest.headers[h].value)) {
                showAlert("You have incomplete 'Request Headers'");
                return;
            }
        }

        // Build Request
        var reqData = {
            "method" : $scope.clientRequest.method,
            "headers" : {},
            "url" : $scope.clientRequest.url,
            "body" : $scope.clientRequest.body
        };

        for (var h=0; h < $scope.clientRequest.headers.length; h++) {
            reqData.headers[$scope.clientRequest.headers[h].name] = $scope.clientRequest.headers[h].value;
        }

        // Response Handler
        var serverCallbackFunc = function (status, data) {

            if (status == -1) {
                showAlert("Request time out. The server is taking too long to respond.");
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            var responseBodyPart = "";

            if (data.status == 404) {

                responseBodyPart =
                    "Error communicating with url:"
                    + "\n"
                    + (" " + reqData.url)
                    + "\n\n"
                    + "- Is " + reqData.url + " a valid endpoint?"
                    + "\n"
                    + "- Is the mock server running?";

            } else {

                var headersText = objToString(data.headers);

                responseBodyPart =
                    "Headers:"
                    + "\n"
                    + headersText
                    + "\n"
                    + data.body;

            }

            $scope.clientResponse =
                "HTTP Status Code: " + data.status
                + "\n\n"
                + responseBodyPart;

        };

        // Send Request
        restClient.doPost($http, '/httpclientcall', reqData, serverCallbackFunc);

    };

    $scope.doClear = function() {

        $scope.clientRequest = {
            "url" : null,
            "method" : null,
            "body" : null,
            "headers" : []
        };

        $scope.clientResponse = "";
    };

    $scope.doClose = function() {
        $uibModalInstance.close($scope.clientRequest);
    };

    function objToString (obj) {
        var str = '';
        for (var p in obj) {
            if (obj.hasOwnProperty(p)) {
                str += '   ' + p + ': ' + obj[p] + '\n';
            }
        }
        return str;
    }

});
