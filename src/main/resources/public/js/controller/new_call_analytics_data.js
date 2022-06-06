
app.controller('newCallAnalyticsDataController', function($scope, $window, $uibModalInstance, $http, $timeout, utils, globalVars, restClient) {


    //
    // Labels
    $scope.heading = 'New Call Analytic Batch';
    $scope.nameLabel = 'Batch Name';
    $scope.namePlaceholderTxt = 'Enter a name...';


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = 'Save';


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
    $scope.callAnalyticData = {
        "name" : null
    };


    //
    // Scoped Functions
    $scope.doSaveCallAnalyticData = function() {

        doCreateCallAnalyticData();
    };


    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };


    //
    // Internal Functions
    function doCreateCallAnalyticData() {

        // Validation
        if (utils.isBlank($scope.callAnalyticData.name)) {
            showAlert("'Batch Name' is required");
            return;
        }

        var reqBody = {
            "name" : $scope.callAnalyticData.name
        };

        // Send
        restClient.doPost($http, '/call-analytic', reqBody, function(status, data) {

            if (status == 201) {
                $uibModalInstance.close(true);
                return;
            } else if (status == 400) {
                showAlert(data.message);
                return;
            } else if (status == 409) {
                showAlert("Unable to save. The batch name '" + $scope.callAnalyticData.name + "' already exists");
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

});
