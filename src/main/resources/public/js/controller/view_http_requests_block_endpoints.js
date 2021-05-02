
app.controller('viewHttpRequestsBlockEndpointsController', function($scope, $http, $timeout, $uibModalInstance, utils, restClient, globalVars, data) {

    //
    // Constants / Vars
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    $scope.httpMethods = globalVars.httpMethods;
    $scope.httpMethods = globalVars.httpMethods;


    //
    // Labels
    $scope.viewRequestsBlockedEndpointsHeading = 'Endpoints To Intercept';
    $scope.noEndpointsDefinedLabel = 'No Endpoints Defined';
    $scope.endpointMethodLabel = 'Method';
    $scope.endpointPathLabel = 'Path';


    //
    // Buttons
    $scope.addBlockingEndpointButtonLabel = 'Add';
    $scope.removeBlockingEndpointButtonLabel = 'Remove';
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
    $scope.endpoints = data.endpoints;
    $scope.paths = [];


    //
    // Scoped Functions
    $scope.doClose = function() {

        $uibModalInstance.close($scope.endpoints);
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
        });

    };

    $scope.doRemoveEndpointToBlock = function(endpoint) {

        $scope.closeAlert();

        var method = endpoint.method;
        var path = endpoint.path;

        var reqParams = '?method=' + method + '&path=' + path;

        restClient.doDelete($http, '/mockedserver/config/' + globalVars.RestfulServerType + '/live-logging-block/endpoint' + reqParams, function(status, data) {

            if (status != 204) {
                 showAlert(globalVars.GeneralErrorMessage);
                 return;
            }

            doRemoveNewEndpointRow(endpoint.id);
        });

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

    function loadPathData() {

        $scope.paths = [];

        restClient.doGet($http, '/restmock', function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            for (var d=0; d < data.length; d++) {
                $scope.paths.push(data[d].path);
            }

        });

    }

    function initPage() {

        if ($scope.endpoints.length == 0) {
            doAddNewEndpointRow();
        }

        loadPathData();

        //
        // Change width of second modal
        jQuery(function() {
            jQuery('.modal-dialog').first().addClass("blocked-endpoints-modal");
        });

    }


    //
    // Init Page
    initPage();

});
