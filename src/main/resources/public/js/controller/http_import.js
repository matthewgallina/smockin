
app.controller('httpImportController', function($scope, $uibModalInstance, $timeout, globalVars, $http, uploadClient, restClient, utils, data) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    $scope.RamlImportType = "RAML";
    $scope.OpenApiImportType = "OPENAPI";
    $scope.StandardImportType = "sMockin";
    $scope.ImportTypes = [ $scope.StandardImportType, $scope.RamlImportType, $scope.OpenApiImportType ];
    $scope.ServerType = data.serverType;
    $scope.RestfulServerType = globalVars.RestfulServerType;


    //
    // Labels
    var serverTypeLabel = (($scope.ServerType == $scope.RestfulServerType)
                              ? 'HTTP'
                              : $scope.ServerType);
    $scope.importHeading = serverTypeLabel + ' Mocks - Import Manager';
    $scope.selectFileLabel = 'Select sMockin ' + serverTypeLabel + ' file...';
    $scope.importFeedbackLabel = 'Import Result';
    $scope.pleaseNoteLabel = 'Please note';
    $scope.existingEndpointsInfo = 'Any imported endpoints that conflict with an existing mock, will be prefixed with a timestamp (e.g /bob/raml_20180101120012000/hello)';
    $scope.ramlVersionInfo = "This facility supports most common RAML features, based on the 'RAML 100 & 200 Tutorials' at raml.org";
    $scope.openApiVersionInfo = "This facility supports OpenAPI 3.0 and Swagger 2.0 specifications (.yaml, .json, .zip)";
    $scope.importLabel = 'Import';
    $scope.orLabel = 'OR';
    $scope.importTypeLabel = 'Import Type:';
    $scope.keepExistingLabel = 'Keep existing versions (if present)';


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.importButtonLabel = 'Start Import';


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
    $scope.importType = $scope.StandardImportType;
    $scope.keepExisting = false;
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

    $scope.doSelectImportType = function(it) {
        $scope.importType = it;
        if (it == $scope.RamlImportType) {
            $scope.selectFileLabel = 'Select RAML API File...';
        } else if (it == $scope.OpenApiImportType) {
            $scope.selectFileLabel = 'Select OpenAPI/Swagger File...';
        } else {
            $scope.selectFileLabel = 'Select sMockin Zip File..';
        }
    };

    $scope.doUploadFile = function() {

        if ($scope.importType == $scope.RamlImportType) {
            doUploadApiRamlFile();
        } else if ($scope.importType == $scope.OpenApiImportType) {
            doUploadOpenApiFile();
        } else if ($scope.importType == $scope.StandardImportType) {
            doUploadStandardFile();
        }

    };

    function doUploadStandardFile() {

        $scope.importFeedback = "Awaiting import...";

        // Validation
        if ($scope.apiUploadFile.data == null
                || $scope.apiUploadFile.data.name.toLowerCase().indexOf(".zip") == -1) {
            showAlert("Please select a .zip based file to import");
            return;
        }

        $scope.disableForm = true;

        // Send data
        var fd = new FormData();
        fd.append('file', $scope.apiUploadFile.data);

       uploadClient.doPostWithHeaders($http, '/mock/import', fd, { 'KeepExisting' : $scope.keepExisting }, function(status, data) {

            $scope.apiUploadFile = {
                data : null
            }

            if (status != 200) {

                $scope.disableForm = false;

                if (status == 400) {
                    $scope.importFeedback = data.message;
                    showAlert("There is an issue with importing this file");
                    return;
                }

                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            showAlert("File successfully imported", "success");
            $scope.importFeedback = data.message;

            $scope.uploadCompleted = true;
            $scope.disableForm = false;
        });

    }

    function doUploadApiRamlFile() {

        $scope.importFeedback = "Awaiting import...";

        // Validation
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

        uploadClient.doPostWithHeaders($http, '/api/RAML/import', fd, { 'KeepExisting' : $scope.keepExisting }, function(status, data) {

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

    function doUploadOpenApiFile() {

        $scope.importFeedback = "Awaiting import...";

        // Validation
        if ($scope.apiUploadFile.data == null) {
            showAlert("Please select a .yaml, .json, or .zip file to import");
            return;
        }

        const fileName = $scope.apiUploadFile.data.name.toLowerCase();
        if (fileName.indexOf(".yaml") == -1
                && fileName.indexOf(".yml") == -1
                && fileName.indexOf(".json") == -1
                && fileName.indexOf(".zip") == -1) {
            showAlert("Invalid file type. Please select a .yaml, .yml, .json, or .zip based file");

            $scope.apiUploadFile = {
                data : null
            }

            return;
        }

        $scope.disableForm = true;

        // Send data
        var fd = new FormData();
        fd.append('file', $scope.apiUploadFile.data);

        uploadClient.doPostWithHeaders($http, '/api/OPENAPI/import', fd, { 'KeepExisting' : $scope.keepExisting }, function(status, data) {

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

            showAlert("OpenAPI/Swagger file successfully imported", "success");
            $scope.importFeedback = "All endpoints imported";

            $scope.uploadCompleted = true;
            $scope.disableForm = false;
        });

    };

});
