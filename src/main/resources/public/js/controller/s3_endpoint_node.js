
app.controller('s3EndpointNodeController', function($scope, $uibModalInstance, $timeout, globalVars, utils, data) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var extId = data.extId;
    var nodeName = data.nodeName;


    //
    // Labels
    $scope.heading = (extId != null)
        ? 'Rename Directory'
        : 'Add Directory';
    $scope.namePlaceholderTxt = 'Enter a name for this directory...';


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.addButtonLabel = (extId != null)
        ? 'Update'
        : 'Add';


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
    $scope.node = {
        "name" : nodeName
    };


    //
    // Scoped Functions
    $scope.doAddNode = function() {

        if (utils.isBlank($scope.node.name)) {
            showAlert("Directory name required");
            return;
        }

        $uibModalInstance.close({
            "name" : $scope.node.name
        });

    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };


});
