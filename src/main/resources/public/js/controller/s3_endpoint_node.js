
app.controller('s3EndpointNodeController', function($scope, $uibModalInstance, $timeout, globalVars, utils, data) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var NodeTypeBucket = globalVars.NodeTypeBucket;
    var extId = data.extId;
    var nodeName = data.nodeName;
    var nodeType = data.nodeType;
    var parentName = data.parentName;


    //
    // Labels
    var nodeTypeLabel = (nodeType == NodeTypeBucket)
        ? "Bucket"
        : "Directory";
    $scope.heading = (extId != null)
        ? 'Rename ' + nodeTypeLabel
        : 'Add ' + nodeTypeLabel;
    $scope.namePlaceholderText = 'Enter a Name...';
    $scope.dirDestinationText = 'This directory will be added to ';
    $scope.dirParentName = parentName;


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
            showAlert("A name required");
            return;
        }
        if (nodeType == NodeTypeBucket
                && utils.matchesRegex(globalVars.S3BucketNameRegex, $scope.node.name)) {
            showAlert("Invalid S3 'Bucket' name (lowercase letters, numbers & hyphens only)");
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
