
app.controller('jmsEndpointInfoController', function($scope, $rootScope, $location, $http, $timeout, utils, globalVars, restClient) {


    //
    // Constant
    var MockTypeDefinitions = {
        MockTypeQueue : 'QUEUE',
        MockTypeTopic : 'TOPIC'
    };

    var isNew = true;


    //
    // Labels
    $scope.serverRestartInstruction = '(Please note, the jms mock server will need to be restarted for changes to take effect)';
    $scope.endpointHeading = (isNew) ? 'New JMS Endpoint' : 'View JMS Endpoint';
    $scope.endpointStatusLabel = 'Status:';
    $scope.nameLabel = 'Name:';


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
        "status" : globalVars.ActiveStatus,
        "jmsMockType" : MockTypeDefinitions.MockTypeQueue
    };

    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;


    //
    // Scoped Functions
    $scope.doCancel = function() {

        $location.path("/dashboard").search({
            'tab' : 'JMS'
        });

        clearEndpointData();
    };

    $scope.doSaveJmsEndpoint = function() {

        closeAlertFunc();

        // Validation
        if (utils.isBlank($scope.endpoint.name)) {
            showAlert("'name' is required");
            return false;
        }

        // Save JMS endpoint
        restClient.doPost($http, '/jmsmock', $scope.endpoint, function(status, data) {

            if (status != 201) {
                 showAlert(globalVars.GeneralErrorMessage);
                 return;
            }

            checkAutoRefreshStatus(function(autoRefresh) {

                var locParams = {
                    'tab' : 'JMS'
                };

                if (autoRefresh != null && autoRefresh) {
                    locParams.restart = 'true';
                }

                utils.hideBlockingOverlay();
                $location.path("/dashboard").search(locParams);
                clearEndpointData();
            });

        });

    };

    $scope.doSetEndpointStatus = function(s) {
        $scope.endpoint.status = s;
    };


    //
    // Internal Functions
    function clearEndpointData() {
        $rootScope.jmsEndpointData = null;
        $scope.extId = null;
    }

    function checkAutoRefreshStatus(callback) {

        restClient.doGet($http, '/mockedserver/config/' + globalVars.JmsServerType, function(status, data) {
            if (status == 200) {
                callback(data.autoRefresh);
                return;
            }

            callback();
        });

    }

});
