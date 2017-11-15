app.controller('wsSendMessageController', function($scope, $timeout, $http, restClient, $uibModalInstance, globalVars, utils, data) {


    //
    // Constants / Vars
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;


    //
    // Labels
    $scope.clientHeading = "Push WS Message" + ((data.sessionId == null) ? " (All Clients)" : "");
    $scope.pathLabel = "Path";
    $scope.sessionIdLabel = "Session Id";
    $scope.bodyLabel = "Message Body";


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.sendButtonLabel = 'Send';


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
    // Data
    $scope.pushData = {
        "path" : data.path,
        "sessionId" : data.sessionId,
        "body" : null
    };


    //
    // Scoped Functions
    $scope.doClose = function() {
        $uibModalInstance.dismiss();
    };

    $scope.doPushMessage = function() {

        $scope.alerts = [];

        // Validation
        if (utils.isBlank($scope.pushData.body)) {
            showAlert("'Message Body' is required");
            return false;
        }

        // Send proxy SSE message
        var reqData = {
            "path" : $scope.pushData.path,
            "body" : $scope.pushData.body
        };

        var url = '/ws' + ((data.sessionId != null) ? '/' + data.sessionId : '');

        restClient.doPost($http, url, reqData, function(status, data) {

            if (status != 204) {
                 showAlert(globalVars.GeneralErrorMessage);
                 return;
            }

            $uibModalInstance.close();
        });

    };

});
