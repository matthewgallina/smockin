
app.controller('myAccountController', function($scope, $http, $timeout, $uibModalInstance, restClient, auth, utils, globalVars) {


    //
    // Constants / Vars
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;


    //
    // Labels
    $scope.configHeading = "Configuration";
    $scope.enableUsersLabel = "Enable Users";
    $scope.adminWarnText = "Note, admin authorisation is required to enable this feature";


    //
    // Buttons
    $scope.closeButtonLabel = "Cancel";


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
    $scope.config = {
        usersEnabled: false
    };


    //
    // Scoped Functions
    $scope.doClose = function() {
        $uibModalInstance.close();
    };

});
