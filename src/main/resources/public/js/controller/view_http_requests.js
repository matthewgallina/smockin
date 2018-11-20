
app.controller('viewHttpRequestsController', function($scope, $location, $timeout, $uibModalInstance, utils, globalVars) {


    //
    // Constants / Vars
    var LiveFeedUrl = "/liveLoggingFeed";
    var adminPort = 8000;
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var InitPageTimeoutMillis = 1500;


    //
    // Labels
    $scope.viewRequestsHeading = "HTTP Live Feed";
    $scope.noActivityData = 'Listening for activity...';
    $scope.requestIdLabel = 'Trace Id';
    $scope.inboundArrowLabel = '----->';
    $scope.outboundArrowLabel = '<-----';
    $scope.proxyLabel = 'Proxy';
    $scope.headersLabel = 'Headers';
    $scope.bodyLabel = 'Body';
    $scope.requestSearchPlaceholderText = 'Enter a keyword to filter results below...';
    $scope.connectionStatusLabel = 'Connection Status';
    $scope.connectionStatusOfflineLabel = 'OFFLINE';
    $scope.connectionStatusOnlineLabel = 'ONLINE';

    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.clearFeedButtonLabel = "Clear Activity";


    //
    // Alerts
    var timeOutPromise = null;
    $scope.alerts = [];

    var closeAlertFunc = function() {
        $scope.alerts = [];
    };

   function showAlert(msg, type) {

        if (timeOutPromise != null) {
            $timeout.cancel(timeOutPromise);
        }

        closeAlertFunc();

        if (type == null) {
            type = 'danger';
        }

        $scope.alerts.push({ "type" : type, "msg" : msg });

        timeOutPromise = $timeout(closeAlertFunc, AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    var wsSocket = null;
    $scope.activityFeed = [];
    $scope.wsEstablished = false;
    $scope.sortType = 'name';
    $scope.sortReverse = false;
    $scope.search = '';


    //
    // Scoped Functions
    $scope.doConvertHttpHeaders = function(headers) {

        var allHeaders = [];

        if (headers == null || headers.length == 0) {
            return allHeaders;
        }

        for (var h in headers) {
            if (headers.hasOwnProperty(h)) {
              allHeaders.push(h + ": " + headers[h]);
            }
        }

        return allHeaders;
    };

    $scope.doClearFeed = function() {
        $scope.activityFeed = [];
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

            $scope.activityFeed = [];

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
            $scope.activityFeed = [];
            $scope.noActivityData = 'Listening for activity...';
            $scope.wsEstablished = true;
            $scope.$digest();
        };

        wsSocket.onmessage = function (event) {
            appendResponseMsg(JSON.parse(event.data));
        };

        wsSocket.onerror = function (event) {
            showAlert("Unable to establish connection to " + LiveFeedUrl);
            wsSocket = null;
            $scope.wsEstablished = false;
            $scope.$digest();
        };

        wsSocket.onclose = function (event) {
            wsSocket = null;
            $scope.wsEstablished = false;
            $scope.$digest();
        };

    }

    function appendResponseMsg(liveLog) {
        $scope.activityFeed.push(liveLog);
        $scope.$digest();
    }


    //
    // Init Page
    $scope.noActivityData = 'Establishing connection...';
    $timeout(doConnectFunc, InitPageTimeoutMillis);

});
