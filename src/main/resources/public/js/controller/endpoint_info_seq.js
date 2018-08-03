
app.controller('endpointInfoSeqController', function($scope, $location, $uibModalInstance, $http, $timeout, utils, globalVars, restClient, data, auth) {


    //
    // Constants
    var isNew = (data.seq == null);
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;


    //
    // Labels
    $scope.newEndpointSeqHeading = ((isNew)?'New ':'') + 'Sequenced Response';
    $scope.contentTypeLabel = 'Content Type';
    $scope.contentTypePlaceholderTxt = 'e.g. (application/json)';
    $scope.httpStatusCodeLabel = 'HTTP Status Code';
    $scope.httpStatusCodePlaceholderTxt = 'e.g. (200, 201, 404)';
    $scope.responseBodyLabel = 'Response Body';
    $scope.frequencyLabel = 'Occurrence';
    $scope.frequencyPlaceholderTxt = 'No of times this response will be returned, before moving onto the next response';
    $scope.responseHeadersLabel = 'Response Headers';
    $scope.orderNoLabel = 'Order';
    $scope.responseHeaderNameLabel = 'Name';
    $scope.responseHeaderValueLabel = 'Value';


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = (isNew)?'Add ':'Amend';
    $scope.removeResponseHeaderButtonLabel = 'X';
    $scope.addResponseHeaderButtonLabel = 'New Row';


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
    $scope.contentTypes = globalVars.ContentMimeTypes;

    $scope.responseHeaderList = [];

    $scope.seqResponse = {
        "extId" : null,
        "orderNo" : 0,
        "responseContentType" : null,
        "httpStatusCode" : 200,
        "responseBody" : null,
        "sleepInMillis" : 0,
        "frequencyCount" : 1,
        "responseHeaders" : {}
    };

    $scope.isNew = isNew;
    $scope.readOnly = (!isNew && auth.isLoggedIn() && auth.getUserName() != data.createdBy);

    if (!isNew) {

        $scope.seqResponse = data.seq;

        angular.forEach($scope.seqResponse.responseHeaders, function(v, k) {
            $scope.responseHeaderList.push({ 'name' : k, 'value' : v });
        });

    }


    //
    // Scoped Functions
    $scope.doAddResponseHeaderRow = function() {
        $scope.responseHeaderList.push({ "name" : null, "value" : null });
    };

    $scope.doRemoveResponseHeaderRow = function(index) {
        $scope.responseHeaderList.splice(index, 1);
    };

    $scope.doSaveSeq = function() {

        $scope.seqResponse.responseHeaders = {};

        if (utils.isBlank($scope.seqResponse.responseContentType)) {
            showAlert("'Content Type' is required");
            return;
        }

        if (utils.isBlank($scope.seqResponse.httpStatusCode)
                || !utils.isNumeric($scope.seqResponse.httpStatusCode)) {
            showAlert("'Http Status Code' is required and must be numeric");
            return;
        }

        if (utils.isBlank($scope.seqResponse.frequencyCount)
                || !utils.isNumeric($scope.seqResponse.frequencyCount)
                || $scope.seqResponse.frequencyCount < 1) {
            showAlert("'Occurrence' is required, must be numeric and be at least 1");
            return;
        }

        for (var r=0; r < $scope.responseHeaderList.length; r++) {
            var rhName = $scope.responseHeaderList[r].name;
            var rhValue = $scope.responseHeaderList[r].value;

            if (utils.isBlank(rhName) || utils.isBlank(rhValue)) {
                showAlert("You have blank 'Response Header' fields. Please amend or remove these.");
                return;
            }

            if ($scope.seqResponse.responseHeaders[rhName] != null) {
                showAlert("The 'Response Header' field '" + rhName + "' is defined more then once.");
                return;
            }

            $scope.seqResponse.responseHeaders[rhName] = rhValue;
        }

        $uibModalInstance.close($scope.seqResponse);
    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };

});
