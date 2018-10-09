
app.controller('serverConfigController', function($scope, $location, $uibModal, $uibModalInstance, $http, $timeout, utils, globalVars, restClient, auth, data) {


    //
    // Constants
    var ServerType = data.serverType;
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    $scope.RestfulServerType = globalVars.RestfulServerType;
    $scope.JmsServerType = globalVars.JmsServerType;
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());

    //
    // Labels
    var ServerTypeLabel = (ServerType == globalVars.RestfulServerType)?"HTTP":ServerType;
    $scope.serverConfigHeading = ServerTypeLabel + ' Mock Server Config';
    $scope.portLabel = 'Port';
    $scope.maxThreadsLabel = 'Max Threads';
    $scope.minThreadsLabel = 'Min Threads';
    $scope.timeOutMillisLabel = 'Idle Time out';
    $scope.autoStartLabel = 'Auto start on application launch';
    $scope.autoRefreshLabel = 'Auto restart after endpoint updates';
    $scope.enableCorsLabel = 'Enable Cross-Origin Resource Sharing (across all endpoints)';
    $scope.enableProxyServerLabel = 'Enable Proxy Server (mock interceptor) on port 8010';
    $scope.manageProxyPriorityPathsLabel = '(' + (($scope.readOnly) ? 'view' : 'manage') +  ' priority paths)';

    $scope.portPlaceholderTxt = "The Port this mock server will run off";
    $scope.maxThreadsPlaceholderTxt = 'The Maximum Threads (Concurrent Requests) allowed';
    $scope.minThreadsPlaceholderTxt = 'The Minimum Threads (Concurrent Requests) allowed';
    $scope.timeOutMillisPlaceholderTxt = 'Connection Idle Time Out (in Milliseconds)';

    $scope.restartServerMessage = "(Note, saving will cause the server to restart if currently running)"


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = 'Save';


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
        "autoRefresh" : false,
        "enableCors" : false,
        "enableProxyServer" : false
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
            showAlert("'port' is required and must be numeric");
            return;
        }

        if (utils.isBlank($scope.serverConfig.maxThreads)
                || !utils.isNumeric($scope.serverConfig.maxThreads)) {
            showAlert("'maxThreads' is required and must be numeric");
            return;
        }

        if (utils.isBlank($scope.serverConfig.minThreads)
                || !utils.isNumeric($scope.serverConfig.minThreads)) {
            showAlert("'minThreads' is required and must be numeric");
            return;
        }

        if (utils.isBlank($scope.serverConfig.timeOutMillis)
                || !utils.isNumeric($scope.serverConfig.timeOutMillis)) {
            showAlert("'timeOutMillis' is required and must be numeric");
            return;
        }

        var req = {
            "serverType" : $scope.serverConfig.serverType,
            "port" : $scope.serverConfig.port,
            "maxThreads" : $scope.serverConfig.maxThreads,
            "minThreads" : $scope.serverConfig.minThreads,
            "timeOutMillis" : $scope.serverConfig.timeOutMillis,
            "autoStart" : $scope.serverConfig.autoStart,
            "autoRefresh" : $scope.serverConfig.autoRefresh,
            "nativeProperties" : {}
        }

        // Handle Native Server Properties
        if (ServerType == globalVars.RestfulServerType) {
            req.nativeProperties = {
                "ENABLE_CORS" : ($scope.serverConfig.enableCors)?"TRUE":"FALSE"
            };
            req.nativeProperties = {
                "PROXY_SERVER_ENABLED" : ($scope.serverConfig.enableProxyServer)?"TRUE":"FALSE"
            };
        } else if (ServerType == globalVars.JmsServerType) {
            req.nativeProperties = {
                "BROKER_URL" : "tcp://localhost:"
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

    $scope.doOpenProxyPathPriorityManager = function() {

        var modalInstance = $uibModal.open({
            templateUrl: 'proxy_path_conflict_manager.html',
            controller: 'proxyPathConflictManagerController',
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (response) {

        }, function () {

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
                    "autoRefresh" : data.autoRefresh,
                    "enableCors" : (data.nativeProperties.ENABLE_CORS != null && data.nativeProperties.ENABLE_CORS.toUpperCase() == "TRUE"),
                    "enableProxyServer" : (data.nativeProperties.PROXY_SERVER_ENABLED != null && data.nativeProperties.PROXY_SERVER_ENABLED.toUpperCase() == "TRUE")
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
