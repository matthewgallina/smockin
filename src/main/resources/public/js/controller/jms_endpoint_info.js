
app.controller('jmsEndpointInfoController', function($scope, $rootScope, $location, $http, $timeout, utils, globalVars, restClient) {


    //
    // Constant
    var MockTypeDefinitions = {
        MockTypeQueue : 'QUEUE',
        MockTypeTopic : 'TOPIC'
    };

    var isNew = ($rootScope.jmsEndpointData == null);


    //
    // Labels
    $scope.serverRestartInstruction = '(Please note, the jms mock server will need to be restarted for changes to take effect)';
    $scope.endpointHeading = (isNew) ? 'New JMS Endpoint' : 'JMS Endpoint';
    $scope.endpointStatusLabel = 'Status:';
    $scope.nameLabel = 'Name:';
    $scope.manageJmsQueueLabel = "Manage JMS Queue";
    $scope.sendJMSMessageLabel = "Push Message to Queue";
    $scope.textMessageBodyLabel = "Text Message Body";


    //
    // Buttons
    $scope.saveButtonLabel = 'Save';
    $scope.cancelButtonLabel = 'Cancel';
    $scope.postJmsMessageButtonLabel = 'Push To Queue';
    $scope.clearJmsQueueButtonLabel = 'Clear Queue';


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

    var extId = null;
    $scope.isNew = isNew;

    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;

    $scope.jmsMessage = {
        "body" : null
    };

    if (!isNew) {

        $scope.endpoint = {
            "name" : $rootScope.jmsEndpointData.name,
            "status" : $rootScope.jmsEndpointData.status,
            "jmsMockType" : $rootScope.jmsEndpointData.jmsMockType
        };

        extId = $rootScope.jmsEndpointData.extId;
    }


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
        if (!isNew) {
            restClient.doPut($http, '/jmsmock/' + extId, $scope.endpoint, serverCallbackFunc);
        } else {
            restClient.doPost($http, '/jmsmock', $scope.endpoint, serverCallbackFunc);
        }

    };

    $scope.doSetEndpointStatus = function(s) {
        $scope.endpoint.status = s;
    };

    $scope.doPostJMSMessage = function() {

        // Validation
        if (utils.isBlank($scope.jmsMessage.body)) {
            showAlert("'Text Message Body' is required");
            return false;
        }

        // Post Message to JMS Queue
        var req = {
            "name" : $scope.endpoint.name,
            "body" : $scope.jmsMessage.body,
            "mimeType" : "text/plain"
        };

        restClient.doPost($http, '/jms', req, function(status, data) {

            if (status != 204) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            showAlert("Message posted to JMS queue: " + $scope.endpoint.name, "success");
        });

    };

    $scope.doClearProxyQueue = function() {

        var req = {
            "name" : $scope.endpoint.name
        };

        restClient.doPatch($http, '/jms/clear', req, function(status, data) {

            if (status != 204) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            showAlert("JMS queue: " + $scope.endpoint.name + " has been cleared", "success");
        });


    };


    //
    // Internal Functions
    function clearEndpointData() {
        $rootScope.jmsEndpointData = null;
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

    var serverCallbackFunc = function (status, data) {

        if (status == 201 || status == 204) {

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

            return;
        }

        utils.hideBlockingOverlay();
        showAlert(globalVars.GeneralErrorMessage);
    };

});
