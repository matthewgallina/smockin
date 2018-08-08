
app.controller('apiImportController', function($scope, $uibModalInstance, $timeout, globalVars, $http, restClient) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;


    //
    // Labels
    $scope.apiImportHeading = 'RAML API Importer';
    $scope.selectFileLabel = 'Select API RAML File...';


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.importButtonLabel = 'Import';


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
    $scope.uploadCompleted = false;

    $scope.apiUploadFile = {
        data : null
    };


    //
    // Scoped Functions
    $scope.doClose = function() {

        $uibModalInstance.close({
            "uploadCompleted" : $scope.uploadCompleted
        });

    };

    $scope.doUploadApiFile = function() {

        if ($scope.apiUploadFile.data == null) {
            showAlert("Please select the RAML file to import");
            return;
        }

        if ($scope.apiUploadFile.data.name.indexOf(".raml") == -1) {
            showAlert("Invalid file type. Please select a .raml file");

            $scope.apiUploadFile = {
                data : null
            }

            return;
        }

        var reader = new FileReader();
        reader.readAsText($scope.apiUploadFile.data);

        reader.onload = function () {

            $scope.disableForm = true;

            // Send data
            var reqData = {
                "type" : "RAML",
                "content" : btoa(reader.result),
                "config" : {
                    "keepExisting" : true,
                    "keepStrategy" : "RENAME_NEW"
                }
            };

            restClient.doPost($http, '/api/import', reqData, function(status, data) {

                if (status != 201) {
                    showAlert((status == 400) ? data.message : globalVars.GeneralErrorMessage);
                    $scope.disableForm = false;
                    return;
                }

                $scope.apiUploadFile = {
                    data : null
                }

                showAlert("RAML file successfully imported", "success");

                $scope.uploadCompleted = true;
                $scope.disableForm = false;
            });

        };

    };

});
