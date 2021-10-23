
app.controller('s3EndpointFileUploaderController', function($scope, $uibModalInstance, $timeout, uploadClient, $http, globalVars, utils, data) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var currentNodeExtId = data.extId;
    var currentNodeName = data.nodeName;


    //
    // Labels
    $scope.heading = 'Upload File';
    $scope.selectFileLabel = 'Select File...';
    $scope.uploadDescription = 'This file will be uploaded to the directory ';
    $scope.uploadDestination = currentNodeName;


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.uploadButtonLabel = 'Upload';


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
    $scope.disableForm = false;
    $scope.s3UploadFile = {
        data : null
    };


    //
    // Scoped Functions
    $scope.doUploadFile = function() {

        // Validation
        if ($scope.s3UploadFile.data == null) {
            showAlert("Please select a file to upload");
            return;
        }

        doUploadStandardFile();

    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };


    //
    // Internal Functions
    function doUploadStandardFile() {

        $scope.disableForm = true;

        // Send data
        var fd = new FormData();
        fd.append('file', $scope.s3UploadFile.data);

       uploadClient.doPostWithHeaders($http, '/s3mock/' + currentNodeExtId + '/upload', fd, { }, function(status, data) {

            $scope.s3UploadFile = {
                data : null
            }

            if (status != 201) {

                $scope.disableForm = false;

                if (status == 400) {
                    showAlert("There is an issue with importing this file");
                    return;
                }

                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $uibModalInstance.close({
                "name" : $scope.node.name
            });

            $scope.disableForm = false;
        });

    }

});
