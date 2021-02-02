
app.controller('viewHttpRequestsBlockEndpointsController', function($rootScope, $scope, $http, $timeout, $uibModalInstance, utils, restClient, globalVars, data) {

    //
    // Constants / Vars
    var wsSocket = data.wsSocket;
    var LiveLoggingAmendment = 'LIVE_LOGGING_AMENDMENT';
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    $scope.httpMethods = [
        'GET',
        'POST',
        'PUT',
        'DELETE',
        'PATCH'
    ];


    //
    // Labels
    $scope.viewRequestsBlockedEndpointsHeading = 'HTTP Live Feed - Endpoints To Block';
    $scope.noEndpointsDefinedLabel = 'No Endpoints Defined';
    $scope.endpointMethodLabel = 'Method';
    $scope.endpointPathLabel = 'Path';


    //
    // Buttons
    $scope.addBlockingEndpointButtonLabel = 'Add';
    $scope.removeBlockingEndpointButtonLabel = 'Remove';
    $scope.releaseBlockedResponseButtonLabel = 'Release';
    $scope.newEndpointRowButtonLabel = 'Apply Endpoint';
    $scope.removeEndpointRowButtonLabel = 'X';
    $scope.closeButtonLabel = 'Close';


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
    $scope.endpoints = [];
    $scope.responseData = {
        "receivedResponse" : false,
        "status" : null,
        "contentType" : null,
        "body" : null
    };


    //
    // Scoped Functions
    $scope.doClose = function() {
        $uibModalInstance.close();
    };

    $scope.doReleaseBlockedLog = function() {

        var req = {
            'type' : LiveLoggingAmendment,
            'payload' : {
                'status' : $scope.responseData.status,
                'contentType' : $scope.responseData.contentType,
                'headers' : {
                    'Foo-Bar': 'yo yo'
                },
                'body' : $scope.responseData.body
            }
        };

        if (wsSocket != null
                && wsSocket.readyState == wsSocket.OPEN) {

            wsSocket.send(JSON.stringify(req));

            $scope.responseData.status = null;
            $scope.responseData.contentType = null;
            $scope.responseData.body = null;
            $scope.responseData.receivedResponse = false;

            showAlert("Response released", "success");
        }

    };

    $scope.doAddEndpointToBlock = function() {

        var method = 'GET';
        var path = '/helloworld';

        var req = {
            'method' : method,
            'path' : path
        };

        restClient.doPost($http, '/mockedserver/config/' + globalVars.RestfulServerType + '/live-logging-block/endpoint', req, function(status, data) {

            if (status != 200) {
                 showAlert(globalVars.GeneralErrorMessage);
                 return;
            }

            doAddNewEndpointRow();

            showAlert("Added endpoint: " + method + ' ' + path, "success");
        });

    };

    $scope.doRemoveEndpointToBlock = function(id) {

        var method = 'GET';
        var path = '/helloworld';

        var reqParams = '?method=' + method + '&path=' + path;

        restClient.doDelete($http, '/mockedserver/config/' + globalVars.RestfulServerType + '/live-logging-block/endpoint' + reqParams, function(status, data) {

            if (status != 204) {
                 showAlert(globalVars.GeneralErrorMessage);
                 return;
            }

            doRemoveNewEndpointRow(id);

            showAlert("Removed endpoint: " + method + ' ' + path, "success");
        });

    };


    //
    // Internal Functions
    function doAddNewEndpointRow() {
        $scope.endpoints.push({ "id" : utils.generateUUID(), "method" : "GET", "path" : null });
    }

    function doRemoveNewEndpointRow(id) {

        for (var i=0; i < $scope.endpoints.length; i++) {
            if ($scope.endpoints[i].id == id) {
                $scope.endpoints.splice(i, 1);
            }
        }
    }

    //
    // Init Page
    doAddNewEndpointRow();


    //
    // Event Listener
    $rootScope.$on("LIVE_LOG_BLOCKED_RESPONSE_PAYLOAD", function(evt, data) {

        $scope.responseData.receivedResponse = true;
        $scope.responseData.status = data.content.status;
        $scope.responseData.body = data.content.body;
        $scope.responseData.contentType = data.content.headers['Content-Type'];

    });


    //
    // Change width of second modal
    jQuery(function() {
        jQuery('.modal-dialog').first().addClass("blocked-endpoints-modal");
    });

});
