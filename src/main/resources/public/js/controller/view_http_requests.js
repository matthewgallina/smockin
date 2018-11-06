
app.controller('viewHttpRequestsController', function($scope, $location, $timeout, $uibModalInstance, utils, globalVars) {


    //
    // Constants / Vars
    var LiveFeedUrl = "/httpMockLogFeed";
    var adminPort = 8000;
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var InitPageTimeoutMillis = 1500;


    //
    // Labels
    $scope.viewRequestsHeading = "HTTP Mock Activity";


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.clearFeedButtonLabel = "Clear";


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
    var wsSocket = null;
    $scope.remoteResponse = null;


    //
    // Scoped Functions
    $scope.doClearFeed = function() {
        $scope.remoteResponse = null;
        $scope.$digest();
    };

    $scope.doClose = function() {
        doTerminate();
        $uibModalInstance.close();
    };


    //
    // Internal Functions
    var doConnectFunc = function () {

        // Check the mock server is running and the port no
        utils.checkRestServerStatus(function(running, port) {

            $scope.remoteResponse = null;

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
                wsSocket = new WebSocket("ws://localhost:" + adminPort + LiveFeedUrl);

                applyWSListeners();

            } catch (err) {

                appendResponseMsg("Unable to establish connection to " + LiveFeedUrl);
                wsSocket = null;

                return;
            }

        });

    };

    function doTerminate() {

        if (wsSocket != null) {
            wsSocket.close();
            wsSocket = null;
        }

    };

    function applyWSListeners() {

       wsSocket.onopen = function (event) {
            $scope.remoteResponse = null;
            appendResponseMsg("Connected...");
        };

        wsSocket.onmessage = function (event) {
            var json = JSON.parse(event.data)
            appendResponseMsg(json.content);
        };

        wsSocket.onerror = function (event) {
            $scope.remoteResponse = null;
            appendResponseMsg("Unable to establish connection to " + LiveFeedUrl);
            wsSocket = null;
        };

        wsSocket.onclose = function (event) {
            appendResponseMsg("Connection closed");
            wsSocket = null;
        };

    }

    function appendResponseMsg(msg) {

        var res = $scope.remoteResponse;

        if (res == null) {
            res = "";
        }

        res = res.concat(msg);
        res = res.concat("\n\n");

        $scope.remoteResponse = res;
        $scope.$digest();

        var feedTextArea = jQuery('#remote-response-body');

        if (feedTextArea.length)
           feedTextArea.scrollTop(feedTextArea[0].scrollHeight - feedTextArea.height());
    }


    //
    // Init Page
    $scope.remoteResponse = "Establishing connection...";
    $timeout(doConnectFunc, InitPageTimeoutMillis);

});
