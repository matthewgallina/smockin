
app.controller('jmsEndpointInfoController', function($scope, $rootScope, $location, $http, $timeout, utils, globalVars, restClient) {


    //
    // Constants
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
    $scope.jmsNamePlaceHolderTxt = 'Enter a name for this Queue or Topic';
    $scope.manageJmsQueueLabel = "Manage JMS Queue";
    $scope.manageJmsTopicLabel = "Manage JMS Topic";
    $scope.sendJMSMessageLabel = "Push Message to Queue";
    $scope.broadcastJMSMessageLabel = "Broadcast Topic Message";
    $scope.textMessageBodyLabel = "Text Message Body";
    $scope.clientIdHeading = "Subscriber Id";
    $scope.clientJoinDateHeading = "Join Date";


    //
    // Buttons
    $scope.saveButtonLabel = 'Save';
    $scope.cancelButtonLabel = 'Cancel';
    $scope.postJmsMessageButtonLabel = 'Push To Queue';
    $scope.postJmsTopicMessageButtonLabel = 'Broadcast';
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
    $scope.mockTypeDefinitions = MockTypeDefinitions;

    $scope.jmsMockTypes = [
       { "name" : "JMS Queue", "value" : MockTypeDefinitions.MockTypeQueue },
       { "name" : "JMS Topic", "value" : MockTypeDefinitions.MockTypeTopic }
    ];

    $scope.endpoint = {
        "name" : null,
        "status" : globalVars.ActiveStatus,
        "jmsMockType" : lookupJmsMockType(MockTypeDefinitions.MockTypeQueue)
    };

    var extId = null;
    $scope.isNew = isNew;

    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;

    $scope.jmsMessage = {
        "body" : null
    };

    $scope.jmsTopicMessage = {
        "body" : null
    };

    $scope.activeSubscribers = [];

    if (!isNew) {

        $scope.endpoint = {
            "name" : $rootScope.jmsEndpointData.name,
            "status" : $rootScope.jmsEndpointData.status,
            "jmsMockType" : lookupJmsMockType($rootScope.jmsEndpointData.jmsMockType)
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

    $scope.doSelectJmsMockType = function(mt) {
        $scope.endpoint.jmsMockType = mt;
    };

    $scope.doSaveJmsEndpoint = function() {

        closeAlertFunc();

        // Validation
        if (utils.isBlank($scope.endpoint.name)) {
            showAlert("'name' is required");
            return false;
        }

        var req = {
            "name" : $scope.endpoint.name,
            "status" : $scope.endpoint.status,
            "jmsMockType" : $scope.endpoint.jmsMockType.value
        };

        // Save JMS endpoint
        if (!isNew) {
            restClient.doPut($http, '/jmsmock/' + extId, req, serverCallbackFunc);
        } else {
            restClient.doPost($http, '/jmsmock', req, serverCallbackFunc);
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

        restClient.doPost($http, '/jms/queue', req, function(status, data) {

            if (status != 204) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            showAlert("Message posted to JMS queue: " + $scope.endpoint.name, "success");
        });

    };

    $scope.doClearJMSQueue = function() {

        var req = {
            "name" : $scope.endpoint.name
        };

        restClient.doPatch($http, '/jms/queue/clear', req, function(status, data) {

            if (status != 204) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            showAlert("JMS queue: " + $scope.endpoint.name + " has been cleared", "success");
        });

    };

    $scope.doPostJMSTopicBroadcastMessage = function() {

        // Validation
        if (utils.isBlank($scope.jmsTopicMessage.body)) {
            showAlert("'Text Message Body' is required");
            return false;
        }

        // Post Message to JMS Queue
        var req = {
            "name" : $scope.endpoint.name,
            "body" : $scope.jmsTopicMessage.body,
            "mimeType" : "text/plain"
        };

        restClient.doPost($http, '/jms/topic', req, function(status, data) {

            if (status != 204) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            showAlert("Message posted to JMS topic: " + $scope.endpoint.name, "success");
        });

    };


    //
    // Internal Functions
    function clearEndpointData() {
        $rootScope.jmsEndpointData = null;
    }

    function lookupJmsMockType(mockType) {
        for (var i=0; i < $scope.jmsMockTypes.length; i++) {
            if (mockType == $scope.jmsMockTypes[i].value) {
                return $scope.jmsMockTypes[i];
            }
        }

        return null;
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

        if (status == 409) {
            showAlert("'" + $scope.endpoint.name + "' is already in use");
            return;
        }

        showAlert(globalVars.GeneralErrorMessage);
    };

});
