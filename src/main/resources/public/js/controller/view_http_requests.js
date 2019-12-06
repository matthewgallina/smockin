
app.controller('viewHttpRequestsController', function($scope, $location, $timeout, $uibModal, $uibModalInstance, utils, globalVars) {


    //
    // Constants / Vars
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var InitPageTimeoutMillis = 1500;
    var WebSocketHeartBeatMillis = 30000;
    var RequestDirectionValue = 'REQUEST';
    var ResponseDirectionValue = 'RESPONSE';
    var LiveFeedUrl = "ws://"
        + location.host
        + "/liveLoggingFeed";


    //
    // Labels
    $scope.viewRequestsHeading = "HTTP Live Feed";
    $scope.noActivityData = 'Listening for activity...';
    $scope.proxyLabel = '(via sMockin Proxy)';
    $scope.headersLabel = 'Headers';
    $scope.bodyLabel = 'Body';
    $scope.requestSearchPlaceholderText = 'Enter a keyword to filter results below...';
    $scope.connectionStatusLabel = 'Connection Status';
    $scope.connectionStatusOfflineLabel = 'OFFLINE';
    $scope.connectionStatusOnlineLabel = 'ONLINE';
    $scope.noFeedRecordSelected = 'Nothing Selected';
    $scope.requestLabel = 'Request';
    $scope.responseLabel = 'Response';
    $scope.mockedResponseLabel = 'Substituted (mock)';
    $scope.originalResponseLabel = 'Original';
    $scope.usingOverrideLabel = "using override!";
    $scope.overrideResponseLabel = "Override Response";


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.clearFeedButtonLabel = "Clear List";


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
    $scope.selectedFeedData = null;
    $scope.proxiedOverrides = [];


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

    $scope.doViewFeedRow = function(f) {
        $scope.selectedFeedData = f;
    };

    $scope.doClearFeed = function() {
        $scope.activityFeed = [];
        $scope.selectedFeedData = null;
    };

    $scope.doClose = function() {
        doTerminate();
        $uibModalInstance.close();
    };

    $scope.getRowStyle = function(recordId) {
        return ($scope.selectedFeedData != null && $scope.selectedFeedData.id == recordId) ? 'info' : '';
    };

    $scope.isUsingOverride = function(feed) {

        var requestKey = feed.request.method + feed.request.url;

        for (var p=0; p < $scope.proxiedOverrides.length; p++) {
            if ($scope.proxiedOverrides[p].requestKey == requestKey) {
                return true;
            }
        }

        return false;
    };

    $scope.doOpenOverride = function() {

        var modalInstance = $uibModal.open({
            templateUrl: 'override_proxied_call.html',
            controller: 'overrideProxiedCallController',
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (response) {

        }, function () {

        });

    };


    //
    // Internal Functions
    var doConnectFunc = function () {

        // Check the mock server is running and the port no
        utils.checkRestServerStatus(function(running, port) {

            $scope.doClearFeed();

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
                wsSocket = new WebSocket(LiveFeedUrl);

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
            $scope.doClearFeed();
            $scope.noActivityData = 'Listening for activity...';
            $scope.wsEstablished = true;
            $scope.$digest();

            keepWsAlive();
        };

        wsSocket.onmessage = function (event) {
            handleResponseMsg(JSON.parse(event.data));
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

            cancelKeepAlive();
        };

    }

    var wsHbTimerPromise = null;

    var keepWsAlive = function() {

        if (wsSocket != null
                && wsSocket.readyState == wsSocket.OPEN) {
            wsSocket.send('');
        } else {
            cancelKeepAlive();
        }

        wsHbTimerPromise = $timeout(keepWsAlive, WebSocketHeartBeatMillis);
    };

    function cancelKeepAlive() {

        if (wsHbTimerPromise != null) {
            $timeout.cancel(wsHbTimerPromise);
        }
    }

    function handleResponseMsg(liveLog) {

        if (liveLog.direction == RequestDirectionValue) {
            buildInitialRequest(liveLog);
        } else if (liveLog.direction == ResponseDirectionValue) {
            appendResponse(liveLog);
        }

    }

    function buildInitialRequest(req) {

        req.content.date = req.date;

        var data = {
            'id' : req.id,
            'proxied' : req.proxied,
            'request' : req.content,
            'response' : null
        };

        $scope.activityFeed.push(data);
        $scope.$digest();
    }

    function appendResponse(resp) {

        for (var i=0; i < $scope.activityFeed.length; i++) {
            if ($scope.activityFeed[i].id == resp.id) {
                resp.content.date = resp.date;
                $scope.activityFeed[i].response = resp.content;
                $scope.$digest();
                break;
            }
        }
    }


    //
    // Init Page
    $scope.noActivityData = 'Establishing connection...';
    $timeout(doConnectFunc, InitPageTimeoutMillis);

});
