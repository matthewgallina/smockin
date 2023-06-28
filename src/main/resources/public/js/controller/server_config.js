
app.controller('serverConfigController', function($scope, $location, $uibModal, $uibModalInstance, $http, $timeout, utils, globalVars, restClient, auth, data) {


    //
    // Constants
    var ServerType = data.serverType;
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    $scope.RestfulServerType = globalVars.RestfulServerType;
    $scope.MailServerType = globalVars.MailServerType;
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    $scope.isLoggedIn = auth.isLoggedIn();


    //
    // Labels
    var ServerTypeLabel = (ServerType == globalVars.RestfulServerType)?"HTTP":ServerType;
    $scope.serverConfigHeading = ServerTypeLabel + ' Mock Server Config';
    $scope.portLabel = 'Port';
    $scope.maxThreadsLabel = 'Max Threads';
    $scope.minThreadsLabel = 'Min Threads';
    $scope.timeOutMillisLabel = 'Idle Time out';
    $scope.autoStartLabel = 'Auto start on application launch';
    $scope.enableCorsLabel = 'Enable Cross-Origin Resource Sharing (across all endpoints)';
    $scope.autoGenInboxesLabel = 'Auto create inboxes where anonymous mail is received (requires restart)';
    $scope.portPlaceholderTxt = "The Port this mock server will run off";
    $scope.maxThreadsPlaceholderTxt = 'The Maximum Threads (Concurrent Requests) allowed';
    $scope.minThreadsPlaceholderTxt = 'The Minimum Threads (Concurrent Requests) allowed';
    $scope.timeOutMillisPlaceholderTxt = 'Connection Idle Time Out (in Milliseconds)';
    $scope.ngrokAuthTokenLabel = 'Ngrok Auth Token';
    $scope.ngrokAuthTokenLabelPlaceholderTxt = 'Enter Your Ngrok Auth Token...';


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = 'Update Server';
    $scope.purgeMailMessagesInCacheButtonLabel = 'Clear all messages on Mail Server';
    $scope.purgeMailMessagesInDatabaseButtonLabel = 'Delete all messages in Database';


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
    $scope.serverConfig = {
        "serverType" : null,
        "port" : 0,
        "maxThreads" : 0,
        "minThreads" : 0,
        "timeOutMillis" : 0,
        "autoStart" : false,
        "enableCors" : false,
        "autoGenInboxes" : false,
        "ngrokAuthToken" : null,
    };


    //
    // Scoped Functions
    $scope.doSaveConfig = function() {

        if ($scope.readOnly) {
            return;
        }

        // Validation
        if (utils.isBlank($scope.serverConfig.port)
                || !utils.isNumeric($scope.serverConfig.port)) {
            showAlert("'Port' is required and must be numeric");
            return;
        }

        if (ServerType == globalVars.RestfulServerType) {

            if (utils.isBlank($scope.serverConfig.maxThreads)
                    || !utils.isNumeric($scope.serverConfig.maxThreads)) {
                showAlert("'Max Threads' is required and must be numeric");
                return;
            }

            if (utils.isBlank($scope.serverConfig.minThreads)
                    || !utils.isNumeric($scope.serverConfig.minThreads)) {
                showAlert("'Min Threads' is required and must be numeric");
                return;
            }

            if (utils.isBlank($scope.serverConfig.timeOutMillis)
                    || !utils.isNumeric($scope.serverConfig.timeOutMillis)) {
                showAlert("'Idle Time out' is required and must be numeric");
                return;
            }

        }

        var req = {
            "serverType" : $scope.serverConfig.serverType,
            "port" : $scope.serverConfig.port,
            "maxThreads" : $scope.serverConfig.maxThreads,
            "minThreads" : $scope.serverConfig.minThreads,
            "timeOutMillis" : $scope.serverConfig.timeOutMillis,
            "autoStart" : $scope.serverConfig.autoStart,
            "nativeProperties" : {}
        }

        // Handle Native Server Properties
        if (ServerType == globalVars.RestfulServerType) {
            req.nativeProperties = {
                "ENABLE_CORS" : ($scope.serverConfig.enableCors) ? "TRUE" : "FALSE"
            };
        }
        if (ServerType == globalVars.RestfulServerType) {
            req.nativeProperties = {
                "NGROK_AUTH_TOKEN" : $scope.serverConfig.ngrokAuthToken
            };
        }
        if (ServerType == globalVars.MailServerType) {
            req.nativeProperties = {
                "AUTO_GEN_INBOXES" : ($scope.serverConfig.autoGenInboxes) ? "TRUE" : "FALSE"
            };
        }

        // Send update
        restClient.doPut($http, '/mockedserver/config/' + ServerType, req, function(status, data) {

            if (status == 204) {
                $uibModalInstance.close({
                    "restartReq" : true
                });

                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.doPurgeAllMailMessages = function (storeType) {

        utils.openDeleteConfirmation("Are you sure wish to delete all mail messages from the " + storeType + "?", function (alertResponse) {

            if (alertResponse) {

                restClient.doDelete($http, '/mockedserver/mail/clear/' + storeType, function(status, data) {

                    if (status == 204) {

                        $uibModalInstance.close({
                            "reload" : true
                        });

                        return;
                    }

                    showAlert(globalVars.GeneralErrorMessage);
                });
            }
        });

    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };


    //
    // Internal Functions
    function loadServerConfig() {

        restClient.doGet($http, '/mockedserver/config/' + ServerType, function(status, data) {

            // Always expect server config to be present.
            if (status == 200) {

                $scope.serverConfig = {
                    "serverType" : data.serverType,
                    "port" : data.port,
                    "maxThreads" : data.maxThreads,
                    "minThreads" : data.minThreads,
                    "timeOutMillis" : data.timeOutMillis,
                    "autoStart" : data.autoStart,
                    "enableCors" : (data.nativeProperties.ENABLE_CORS != null && data.nativeProperties.ENABLE_CORS.toUpperCase() == "TRUE"),
                    "autoGenInboxes" : (data.nativeProperties.AUTO_GEN_INBOXES != null && data.nativeProperties.AUTO_GEN_INBOXES.toUpperCase() == "TRUE"),
                    "ngrokAuthToken" : data.nativeProperties.NGROK_AUTH_TOKEN
                };

                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }


    //
    // Init page
    loadServerConfig();

});
