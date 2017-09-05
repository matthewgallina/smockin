
app.controller('wsClientController', function($scope, $location, $http, $timeout, $filter, $uibModalInstance, utils, globalVars, data) {


    //
    // Constants / Vars
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;

    //
    // Labels
    $scope.clientHeading = "WebSocket Client";
    $scope.urlLabel = "Path";
    $scope.urlPlaceholderTxt = "WebSocket endpoint URL to call (e.g /chat)";
    $scope.requestBodyLabel = "Message Body";
    $scope.remoteResponseLabel = "Remote Response";


    //
    // Buttons
    $scope.clearButtonLabel = 'Clear';
    $scope.closeButtonLabel = 'Close';
    $scope.sendButtonLabel = 'Send';
    $scope.terminateButtonLabel = 'Terminate';
    $scope.connectButtonLabel = 'Connect';


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
    $scope.clientRequest = {
        "url" : null,
        "body" : null
    };

    if (data.state != null) {
        $scope.clientRequest = {
            "url" : data.state.url,
            "body" : data.state.body
        };
    }

    $scope.remoteResponse = null;

    var wsSocket = null;


    //
    // Functions
    $scope.doConnect = function() {

        // Validate
        if (utils.isBlank($scope.clientRequest.url)) {
            showAlert("'Path' is required");
            return;
        }

        if (!$scope.clientRequest.url.startsWith("/")) {
            showAlert("Invalid 'Path'. (expected format: /chat)");
            return;
        }

        // Check the mock server is running and the port no
        utils.checkServerStatus(function(running, port) {

            if (running == null) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            if (!running) {
                showAlert("Cannot establish connection. The mock server is not currently running.");
                return;
            }

            try {

                // Establish connection to WS endpoint
                wsSocket = new WebSocket("ws://localhost:" + port + $scope.clientRequest.url);

            } catch (err) {

                appendResponseMsg("Unable to establish connection to " + $scope.clientRequest.url);
                wsSocket = null;

                return;
            }


            // WebSocket event listeners
            wsSocket.onopen = function (event) {
                clearResponseFunc();
                appendResponseMsg("Connection established", 'dd/MM/yy HH:mm:ss');
            };

            wsSocket.onmessage = function (event) {
                appendResponseMsg(event.data);
            };

            wsSocket.onerror = function (event) {

                appendResponseMsg("Unable to establish connection to " + $scope.clientRequest.url);
                wsSocket = null;

                return;
            };

            wsSocket.onclose = function (event) {

console.log(event.code);

                appendResponseMsg("Connection closed");
                wsSocket = null;
            };

        });

    };

    $scope.doTerminate = function() {

        if (wsSocket != null) {
            wsSocket.close();
        }

    };

    $scope.doSend = function() {

        // Validate
        if (utils.isBlank($scope.clientRequest.body)) {
            showAlert("'Message Body' is required");
            return;
        }

        if (wsSocket == null) {
            showAlert("No active websocket connection detected");
            return;
        }

        // Send message
        wsSocket.send($scope.clientRequest.body);

        $scope.clientRequest.body = "";
    };

    var clearResponseFunc = function() {
        $scope.remoteResponse = null;
    };

    $scope.doClearResponse = clearResponseFunc;

    function appendResponseMsg(msg, dateFormat) {

        var res = $scope.remoteResponse;

        if (res == null) {
            res = "";
        }

        res = res.concat($filter('date')(new Date(), (dateFormat != null)?dateFormat:'HH:mm:ss'));
        res = res.concat("  -  ");
        res = res.concat(msg);
        res = res.concat("\n\n");

        $scope.remoteResponse = res;
        $scope.$digest()
    }

    $scope.doClose = function() {
        $uibModalInstance.close($scope.clientRequest);
    };

});
