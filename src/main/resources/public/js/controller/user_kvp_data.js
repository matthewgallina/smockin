
app.controller('userKvpDataController', function($scope, $window, $uibModalInstance, $http, $timeout, utils, globalVars, restClient, data) {


    //
    // Labels
    $scope.heading = 'New Key/Value Data Entry';
    $scope.keyLabel = 'Key';
    $scope.valueLabel = 'Value';
    $scope.keyPlaceholderTxt = 'Enter the key of this data pair...';
    $scope.bulkValuePlaceholderTxt = 'Please enter your key/value pairs, 1 pair per line using the \'=\' operator like so:\n\nname=joe\nage=32\nlang=en';


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = 'Save';
    $scope.deleteButtonLabel = 'Delete';
    $scope.singleEntryButtonLabel = 'Single Entry';
    $scope.bulkEntryButtonLabel = 'Bulk Entry';


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
        "value" : null,
        "bulkValue" : null
    };

    if (data != null) {

        $scope.heading = 'View Key Value Data';

        $scope.kvpData.extId = data.extId;
        $scope.kvpData.key = data.key;
        $scope.kvpData.value = data.value;

    };

    $scope.singleEntryMode = 'SINGLE';
    $scope.bulkEntryMode = 'BULK';
    $scope.entryMode = $scope.singleEntryMode;


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

        var reqBody = [];

        if ($scope.entryMode == $scope.singleEntryMode) {

            // Validation
            if (utils.isBlank($scope.kvpData.key)) {
                showAlert("'Key' is required");
                return;
            }
            if (utils.isBlank($scope.kvpData.value)) {
                showAlert("'Value' is required");
                return;
            }

            reqBody.push({
                "extId" : null,
                "key" : $scope.kvpData.key,
                "value" : $scope.kvpData.value
            });

        } else if ($scope.entryMode == $scope.bulkEntryMode) {

            // Validation
            if (utils.isBlank($scope.kvpData.bulkValue)
                    || ($scope.kvpData.bulkValue.indexOf("\n") == -1
                            && $scope.kvpData.bulkValue.indexOf("=") == -1)) {
                showAlert("At least 1 key/value pair entry is required");
                return;
            }

            var pairs = $scope.kvpData.bulkValue.split("\n");

            for (var i=0; i < pairs.length; i++) {

                var kvp = pairs[i].split("=");

                if (kvp == null
                        || kvp.length != 2
                        || utils.isBlank(kvp[0])
                        || utils.isBlank(kvp[1])) {
                    showAlert("Invalid key/value pair entry found");
                    return;
                }

                reqBody.push({
                    "extId" : null,
                    "key" : kvp[0].trim(),
                    "value" : kvp[1].trim()
                });

            }

        }

        // Send
        restClient.doPost($http, '/keyvaluedata', reqBody, function(status, data) {

            if (status == 201) {
                $uibModalInstance.close("ok");
                return;
            } else if (status == 400) {
                showAlert(data.message);
                return;
            } else if (status == 409) {

                if ($scope.entryMode == $scope.singleEntryMode) {
                    showAlert("Unable to save. The key '" + $scope.kvpData.key + "' already exists");
                } else {
                    showAlert("Unable to save. One of your keys already exists");
                }

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
