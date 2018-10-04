
app.controller('proxyPathConflictManagerController', function($scope, $uibModalInstance, $http, $timeout, globalVars, restClient, auth) {


    //
    // Constants



    //
    // Labels
    $scope.heading = 'Proxy Path Conflict Manager';
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());


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



    //
    // Scoped Functions
    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };

    $scope.doSave = function() {
        $uibModalInstance.close();
    };


    //
    // Init page


});
