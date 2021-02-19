
app.controller('viewHttpRequestsBlockEndpointsController', function($rootScope, $scope, $http, $timeout, $uibModalInstance, utils, restClient, globalVars, data) {

    //
    // Constants / Vars
    var wsSocket = data.wsSocket;
    var LiveLoggingAmendment = 'LIVE_LOGGING_AMENDMENT';
    var LiveLoggingAmendmentCancel = 'LIVE_LOGGING_AMENDMENT_CANCEL';
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    $scope.JsonContentType = globalVars.JsonContentType;
    $scope.XmlContentType = globalVars.XmlContentType;
    $scope.httpMethods = globalVars.httpMethods;


    //
    // Labels
    $scope.viewRequestsBlockedEndpointsHeading = 'HTTP Live Feed - Endpoints To Block';
    $scope.noEndpointsDefinedLabel = 'No Endpoints Defined';
    $scope.endpointMethodLabel = 'Method';
    $scope.endpointPathLabel = 'Path';
    $scope.formatJsonLabel = 'Validate & Format JSON';
    $scope.formatXmlLabel = 'Validate & Format XML';


    //
    // Buttons
    $scope.addBlockingEndpointButtonLabel = 'Add';
    $scope.removeBlockingEndpointButtonLabel = 'Remove';
    $scope.releaseBlockedResponseButtonLabel = 'Release';
    $scope.removeEndpointRowButtonLabel = 'X';
    $scope.closeButtonLabel = 'Close';
    $scope.releaseInterceptedResponseButton = 'Release Response';


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
    $scope.httpMethods = globalVars.httpMethods;
    $scope.contentTypes = globalVars.ContentMimeTypes;
    $scope.responseData = {
        "receivedResponse" : false,
        "status" : null,
        "contentType" : null,
        "body" : null
    };


    //
    // Scoped Functions
    $scope.doClose = function() {

        if ($scope.responseData.receivedResponse) {

            var req = {
                'type' : LiveLoggingAmendmentCancel,
                'payload' : null
            }

            wsSocket.send(JSON.stringify(req));
        }

        $uibModalInstance.close();

    };

    $scope.doReleaseBlockedLog = function() {

        $scope.closeAlert();

        var req = {
            'type' : LiveLoggingAmendment,
            'payload' : {
                'status' : $scope.responseData.status,
                'contentType' : $scope.responseData.contentType,
                'headers' : {
//                    'Foo-Bar': 'yo yo'
                },
                'body' : $scope.responseData.body
            }
        };

        if (wsSocket != null
                && wsSocket.readyState == wsSocket.OPEN) {

            wsSocket.send(JSON.stringify(req));

            $scope.responseData.receivedResponse = false;
            $scope.responseData.status = null;
            $scope.responseData.contentType = null;
            $scope.responseData.body = null;

            showAlert("Response released", "success");
        }

    };

    $scope.doAddEndpointToBlock = function(endpoint) {

        var method = endpoint.method;
        var path = endpoint.path;

        $scope.closeAlert();

        if (utils.isBlank(method)) {
            showAlert("Method is required");
            return;
        }
        if (utils.isBlank(path)) {
            showAlert("Path is required");
            return;
        }
        if (!path.startsWith("/")) {
            showAlert("Path requires a forward slash prefix");
            return;
        }

        var req = {
            'method' : method,
            'path' : path
        };

        restClient.doPost($http, '/mockedserver/config/' + globalVars.RestfulServerType + '/live-logging-block/endpoint', req, function(status, data) {

            if (status != 200) {
                 showAlert(globalVars.GeneralErrorMessage);
                 return;
            }

            endpoint.id = utils.generateUUID();
            doAddNewEndpointRow();

            showAlert("Added endpoint: " + method + ' ' + path, "success");
        });

    };

    $scope.doRemoveEndpointToBlock = function(id) {

        $scope.closeAlert();

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

    $scope.doFormatJson = function() {

        $scope.closeAlert();

        if ($scope.responseData.body == null) {
            return;
        }

        var validationOutcome = utils.validateJson($scope.responseData.body);

        if (validationOutcome != null) {
            showAlert(validationOutcome);
            return;
        }

        $scope.responseData.body = utils.formatJson($scope.responseData.body);
    };

    $scope.doFormatXml = function() {

        $scope.closeAlert();

        if ($scope.responseData.body == null) {
            return;
        }

        var validationOutcome = utils.validateAndFormatXml($scope.responseData.body);

        if (validationOutcome == null) {
            showAlert("Unable to format XML. Invalid syntax");
            return;
        }

        if (validationOutcome[0] == 'ERROR') {
            showAlert("Unable to format XML: " + validationOutcome[1]);
            return;
        }

        $scope.responseData.body = validationOutcome[1];
    };


    //
    // Internal Functions
    function doAddNewEndpointRow() {

        $scope.endpoints.push({
            "id" : null,
            "method" : null,
            "path" : null
        });
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
