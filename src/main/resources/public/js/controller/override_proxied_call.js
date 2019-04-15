
app.controller('overrideProxiedCallController', function($scope, $uibModalInstance, $http, $timeout, globalVars, restClient, auth) {


    //
    // Labels
    $scope.heading = 'Proxy Override Manager';


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



    };


    //
    // Internal Functions


    //
    // Init page


});
