
app.controller('userKvpDataController', function($scope, $window, $uibModalInstance, $http, $timeout, utils, globalVars, restClient, data) {


    //
    // Labels
    $scope.heading = 'New Key/Value Data Entry';
    $scope.keyLabel = 'Key';
    $scope.valueLabel = 'Value';
    $scope.keyPlaceholderTxt = 'Enter the key of this data pair...';


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = 'Save';
    $scope.deleteButtonLabel = "Delete";


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

        $timeout(closeAlertFunc, globalVars.AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.kvpData = {
        "extId" : null,
        "key" : null,
        "value" : null
    };

    if (data != null) {

        $scope.heading = 'View Key Value Data';

        $scope.kvpData.extId = data.extId;
        $scope.kvpData.key = data.key;
        $scope.kvpData.value = data.value;

    };


    //
    // Scoped Functions
    $scope.doSaveKvp = function() {

        if (data != null) {
            doUpdateKvp();
        } else {
            doCreateKvp();
        }

    };

    $scope.doDeleteKvp = function() {

        if (data == null) {
            return;
        }

        utils.openDeleteConfirmation("Are you sure you wish to delete this data pair?", function(result) {

            if (!result) {
                return;
            }

            restClient.doDelete($http, '/keyvaluedata/' + data.extId, function(status, data) {

                if (status == 204) {
                    $uibModalInstance.close("ok");
                    return;
                }

                showAlert(globalVars.GeneralErrorMessage);
            });

        });

    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };


    //
    // Internal Functions
    function doCreateKvp() {

        // Validation
        if (utils.isBlank($scope.kvpData.key)) {
            showAlert("'Key' is required");
            return;
        }
        if (utils.isBlank($scope.kvpData.value)) {
            showAlert("'Value' is required");
            return;
        }

        var reqBody = {
            "extId" : null,
            "key" : $scope.kvpData.key,
            "value" : $scope.kvpData.value
        };

        // Send
        restClient.doPost($http, '/keyvaluedata', reqBody, function(status, data) {

            if (status == 201) {
                $uibModalInstance.close("ok");
                return;
            } else if (status == 400) {
                showAlert(data.message);
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

    function doUpdateKvp() {

        // Validation
        if (utils.isBlank($scope.kvpData.key)) {
            showAlert("'Key' is required");
            return;
        }
        if (utils.isBlank($scope.kvpData.value)) {
            showAlert("'Value' is required");
            return;
        }

        var reqBody = {
            "extId" : $scope.kvpData.extId,
            "key" : $scope.kvpData.key,
            "value" : $scope.kvpData.value
        };

        // Send
        restClient.doPut($http, '/keyvaluedata/' + data.extId, reqBody, function(status, data) {

            if (status == 204) {
                $uibModalInstance.close("ok");
                return;
            } else if (status == 400) {
                showAlert(data.message);
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

});
