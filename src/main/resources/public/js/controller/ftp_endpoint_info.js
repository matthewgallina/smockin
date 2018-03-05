
app.controller('ftpEndpointInfoController', function($scope, $rootScope, $location, $http, $timeout, utils, globalVars, restClient) {


    //
    // Constants
    var isNew = ($rootScope.ftpEndpointData == null);


    //
    // Labels
    $scope.serverRestartInstruction = '(Please note, the ftp mock server will need to be restarted for changes to take effect)';
    $scope.endpointHeading = (isNew) ? 'New FTP Repository' : 'FTP Repository';
    $scope.nameLabel = 'Username:';
    $scope.usernamePlaceHolderTxt = 'Enter a username for the FTP repository';
    $scope.endpointStatusLabel = 'Status:';


    //
    // Buttons
    $scope.saveButtonLabel = 'Save';
    $scope.cancelButtonLabel = 'Cancel';


    //
    // Alerts
    var alertPromise = null;
    $scope.alerts = [];

    var closeAlertFunc = function() {

        if (alertPromise != null) {
            $timeout.cancel( alertPromise );
            alertPromise = null;
        }

        $scope.alerts = [];
    };

    function showAlert(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        closeAlertFunc();
        $scope.alerts.push({ "type" : type, "msg" : msg });

        alertPromise = $timeout(closeAlertFunc, globalVars.AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.endpoint = {
        "name" : null,
        "status" : globalVars.ActiveStatus
    };

    var extId = null;
    $scope.isNew = isNew;

    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;

    if (!isNew) {

        $scope.endpoint = {
            "name" : $rootScope.ftpEndpointData.name,
            "status" : $rootScope.ftpEndpointData.status
        };

        extId = $rootScope.ftpEndpointData.extId;
    }


    //
    // Scoped Functions
    $scope.doCancel = function() {

        $location.path("/dashboard").search({
            'tab' : 'FTP'
        });

        clearEndpointData();
    };

    $scope.doSaveFtpEndpoint = function() {

        closeAlertFunc();

        // Validation
        if (utils.isBlank($scope.endpoint.name)) {
            showAlert("'username' is required");
            return false;
        }

        var req = {
            "name" : $scope.endpoint.name,
            "status" : $scope.endpoint.status
        };

        // Save FTP endpoint
        if (!isNew) {
            restClient.doPut($http, '/ftpmock/' + extId, req, serverCallbackFunc);
        } else {
            restClient.doPost($http, '/ftpmock', req, serverCallbackFunc);
        }

    };

    $scope.doSetEndpointStatus = function(s) {
        $scope.endpoint.status = s;
    };


    //
    // Internal Functions
    function clearEndpointData() {
        $rootScope.ftpEndpointData = null;
    }

    function checkAutoRefreshStatus(callback) {

        restClient.doGet($http, '/mockedserver/config/' + globalVars.FtpServerType, function(status, data) {
            if (status == 200) {
                callback(data.autoRefresh);
                return;
            }

            callback();
        });

    }

    var serverCallbackFunc = function (status, data) {

        if (status == 201 || status == 204) {

            checkAutoRefreshStatus(function(autoRefresh) {

                var locParams = {
                    'tab' : 'FTP'
                };

                if (autoRefresh != null && autoRefresh) {
                    locParams.restart = 'true';
                }

                utils.hideBlockingOverlay();
                $location.path("/dashboard").search(locParams);
                clearEndpointData();
            });

            return;
        }

        utils.hideBlockingOverlay();
        showAlert(globalVars.GeneralErrorMessage);
    };

});
