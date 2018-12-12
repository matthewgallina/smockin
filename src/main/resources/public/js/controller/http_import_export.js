
app.controller('httpImportExportController', function($scope, $uibModalInstance, $timeout, globalVars, $http, uploadClient, restClient, utils, data) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    $scope.ImportType = "IMPORT";
    $scope.ExportType = "EXPORT";


    //
    // Labels
    $scope.importExportHeading = 'Import / Export';
    $scope.selectFileLabel = 'Select API RAML File...';
    $scope.importFeedbackLabel = 'Import Result';
    $scope.pleaseNoteLabel = 'Please note';
    $scope.existingEndpointsInfo = 'Any imported endpoints that conflict with an existing mock, will be prefixed with a timestamp (e.g /bob/raml_20180101120012000/hello)';
    $scope.ramlVersionInfo = "This facility supports most common RAML features, based on the 'RAML 100 & 200 Tutorials' at raml.org";
    $scope.importLabel = 'Import';
    $scope.exportLabel = 'Export';
    $scope.exportInstructions = "Please select how you wish to export your HTTP mocks...";
    $scope.orLabel = 'OR';


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.importButtonLabel = 'Run Import';
    $scope.exportSelectButtonLabel = 'Select Mocks To Export...';
    $scope.downloadAllButtonLabel = 'Download All Mocks';
    $scope.downloadSelectionButtonLabel = 'Download Selection';


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
    $scope.mode = $scope.ImportType;
    $scope.disableForm = false;
    $scope.uploadCompleted = false;
    $scope.exportSelection = [];

    $scope.apiUploadFile = {
        data : null
    };

    $scope.importFeedback = "Awaiting import...";


    if (data != null) {

        if (data.mode != null) {
            $scope.mode = data.mode;
        }

        if (data.exportSelection != null) {
            $scope.exportSelection = data.exportSelection;
        }

    }


    //
    // Scoped Functions
    $scope.doClose = function() {

        $uibModalInstance.close({
            "uploadCompleted" : $scope.uploadCompleted
        });

    };

    $scope.doInitExport = function() {

        utils.openWarningConfirmation("Are you sure you wish to create this export?", function (alertResponse) {

            if (alertResponse) {

                restClient.doPost($http, '/mock/export/RESTFUL', $scope.exportSelection, function(status, data) {

                    if (status != 200) {
                        showAlert(globalVars.GeneralErrorMessage);
                        return;
                    }

                    $uibModalInstance.close({
                        "generatedExportData" : data
                    });

                });

            }

        });

    };

    $scope.doSelectExports = function() {

        $uibModalInstance.close({
            "initExportSelection" : true
        });

    };

    $scope.doUploadApiFile = function() {

        $scope.importFeedback = "Awaiting import...";

        if ($scope.apiUploadFile.data == null) {
            showAlert("Please select a .raml based file to import");
            return;
        }

        if ($scope.apiUploadFile.data.name.toLowerCase().indexOf(".raml") == -1
                && $scope.apiUploadFile.data.name.toLowerCase().indexOf(".zip") == -1) {
            showAlert("Invalid file type. Please select a .raml or .zip based file");

            $scope.apiUploadFile = {
                data : null
            }

            return;
        }

        $scope.disableForm = true;

        // Send data
        var fd = new FormData();
        fd.append('file', $scope.apiUploadFile.data);

        uploadClient.doPost($http, '/api/RAML/import', fd, function(status, data) {

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

});
