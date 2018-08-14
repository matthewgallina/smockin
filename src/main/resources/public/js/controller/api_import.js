
app.controller('apiImportController', function($scope, $uibModalInstance, $timeout, globalVars, $http, restClient) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;


    //
    // Labels
    $scope.apiImportHeading = 'RAML API Importer';
    $scope.selectFileLabel = 'Select API RAML File...';
    $scope.importFeedbackLabel = 'Import Result';
    $scope.pleaseNoteLabel = 'Please note';
    $scope.existingEndpointsInfo = 'Any imported endpoints that conflict with an existing mock, will be prefixed with a timestamp (e.g /bob/raml_20180101120012000/hello)';
    $scope.ramlVersionInfo = "This facility supports most common RAML features, based on the 'RAML 100 & 200 Tutorials' at raml.org";


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.importButtonLabel = 'Run Import';


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

    $scope.importFeedback = "Awaiting import...";


    //
    // Scoped Functions
    $scope.doClose = function() {

        $uibModalInstance.close({
            "uploadCompleted" : $scope.uploadCompleted
        });

    };

    $scope.doUploadApiFile = function() {

        $scope.importFeedback = "Awaiting import...";

        if ($scope.apiUploadFile.data == null) {
            showAlert("Please select a .raml based file to import");
            return;
        }

        if ($scope.apiUploadFile.data.name.toLowerCase().indexOf(".raml") == -1) {
            showAlert("Invalid file type. Please select a .raml based file");

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

                $scope.apiUploadFile = {
                    data : null
                }

                if (status != 201) {

                    if (status == 400) {
                        $scope.importFeedback = data.message;
                        showAlert("There is an issue with this file");
                    } else {
                        showAlert(globalVars.GeneralErrorMessage);
                    }

                    $scope.disableForm = false;
                    return;
                }

                showAlert("RAML file successfully imported", "success");
                $scope.importFeedback = "All endpoints imported";

                $scope.uploadCompleted = true;
                $scope.disableForm = false;
            });

        };

    };

});
