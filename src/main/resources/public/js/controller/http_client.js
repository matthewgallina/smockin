
app.controller('httpClientController', function($scope, $location, $http, $timeout, utils, globalVars, restClient) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var RestfulServerType = globalVars.RestfulServerType;


    //
    // Labels
    $scope.clientHeading = "HTTP Client";
    $scope.urlLabel = "Path";
    $scope.urlPlaceholderTxt = "Endpoint URL to call";
    $scope.methodLabel = "Method";
    $scope.methodDropDownLabel = "Select...";
    $scope.requestBodyLabel = "Request Body";
    $scope.requestHeadersLabel = "Request Headers";
    $scope.removeRequestHeaderButtonLabel = 'X';
    $scope.addRequestHeaderButtonLabel = 'New Row';
    $scope.clientResponseLabel = "Response";


    //
    // Buttons
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
    $scope.httpMethods = [
        'GET',
        'POST',
        'PUT',
        'DELETE',
        'PATCH'
    ];

    $scope.clientRequest = {
        "url" : null,
        "method" : null,
        "body" : null,
        "headers" : []
    };

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

        console.log($scope.clientRequest);

        if (utils.isBlank($scope.clientRequest.url)) {
            showAlert("'Path' is required");
            return;
        }

        if (utils.isBlank($scope.clientRequest.method)) {
            showAlert("'Method' is required");
            return;
        }

        for (var h=0; h < $scope.clientRequest.headers.length; h++) {
            if (utils.isBlank($scope.clientRequest.headers[h].name)
                    || utils.isBlank($scope.clientRequest.headers[h].value)) {
                showAlert("You have incomplete 'Request Headers'");
                return;
            }
        }

        var reqData = {
            "method" : $scope.clientRequest.method,
            "headers" : {},
            "url" : $scope.clientRequest.url,
            "body" : $scope.clientRequest.body
        };

        for (var h=0; h < $scope.clientRequest.headers.length; h++) {
            reqData.headers[$scope.clientRequest.headers[h].name] = $scope.clientRequest.headers[h].value;
        }

        var serverCallbackFunc = function (status, data) {

            if (status == 200) {

                $scope.clientResponse = data.status
                    + "\n" + data.contentType
                    + "\n" + data.body
                    + "\n" + data.headers;

                return;
            }

            showAlert("Oops looks like something went wrong!");
        };

        restClient.doPost($http, '/httpclientcall', reqData, serverCallbackFunc);

    };

    $scope.doClose = function() {
        angular.element( document.getElementById("http-client") ).css('display', 'none');
    };

    function loadMockServerDetails() {

        restClient.doGet($http, '/mockedserver/config/' + RestfulServerType, function(status, data) {

            if (status == 200) {
                $scope.clientRequest.url = "http://localhost:" + data.port + "/";
                return;
            }

            showAlert("Oops looks like something went wrong!");
        });

    }

    loadMockServerDetails();

});
