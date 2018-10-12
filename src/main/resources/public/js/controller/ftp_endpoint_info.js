
app.controller('ftpEndpointInfoController', function($scope, $rootScope, $location, $http, $timeout, utils, globalVars, restClient, uploadClient) {


    //
    // Constants
    var isNew = ($rootScope.ftpEndpointData == null);


    //
    // Labels
    $scope.serverRestartInstruction = '(Please note, the ftp mock server will need to be restarted for changes to take effect)';
    $scope.endpointHeading = (isNew) ? 'New FTP Repository' : 'FTP Repository';
    $scope.nameLabel = 'Username (& Password):';
    $scope.usernamePlaceHolderTxt = 'Enter a username for the FTP repository';
    $scope.endpointStatusLabel = 'Status:';
    $scope.manageFtpLabel = 'Manage FTP Repository';
    $scope.currentRepositoryFilesLabel = 'Current Repository Files';
    $scope.uploadFTPMessageLabel = 'Upload File to Repository';
    $scope.selectFileLabel = 'Select File...';
    $scope.noFilesFoundLabel = 'No Files Found';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";


    //
    // Buttons
    $scope.uploadFTPButtonLabel = 'Upload to Repository';
    $scope.deleteButtonLabel = 'Delete';
    $scope.saveButtonLabel = 'Save';
    $scope.cancelButtonLabel = 'Cancel';


    //
    // Alerts
    var alertPromise = null;
    $scope.alerts = [];

    var closeAlertFunc = function() {

        if (alertPromise != null) {
            $timeout.cancel( alertPromise );
            alertPromise = null;
        }

        $scope.alerts = [];
    };

    function showAlert(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        closeAlertFunc();
        $scope.alerts.push({ "type" : type, "msg" : msg });

        alertPromise = $timeout(closeAlertFunc, globalVars.AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.endpoint = {
        "name" : null,
        "status" : globalVars.ActiveStatus
    };

    var extId = null;
    $scope.isNew = isNew;

    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;
    $scope.disableForm = false;

    if (!isNew) {

        $scope.endpoint = {
            "name" : $rootScope.ftpEndpointData.name,
            "status" : $rootScope.ftpEndpointData.status
        };

        extId = $rootScope.ftpEndpointData.extId;
    }

    $scope.ftpUploadFile = {
        data : null
    };

    $scope.ftpFileTreeData = [];


    //
    // Scoped Functions
    $scope.doCancel = function() {

        $location.path("/dashboard").search({
            'tab' : 'FTP'
        });

        clearEndpointData();
    };

    $scope.doSaveFtpEndpoint = function() {

        closeAlertFunc();

        // Validation
        if (utils.isBlank($scope.endpoint.name)) {
            showAlert("'username' is required");
            return;
        }

        var req = {
            "name" : $scope.endpoint.name,
            "status" : $scope.endpoint.status
        };

        // Save FTP endpoint
        if (!isNew) {
            restClient.doPut($http, '/ftpmock/' + extId, req, saveMockCallbackFunc);
        } else {
            restClient.doPost($http, '/ftpmock', req, saveMockCallbackFunc);
        }

    };

    $scope.doDelete = function() {

        if (isNew) {
            return
        }

        utils.openDeleteConfirmation("Are you sure you wish to delete this endpoint?", function (alertResponse) {

            if (alertResponse) {

                restClient.doDelete($http, '/ftpmock/' + extId, function(status, data) {

                    if (status != 204) {
                        showAlert(globalVars.GeneralErrorMessage);
                        return;
                    }

                    $location.path("/dashboard").search({
                        'tab' : 'FTP'
                    });

                });

            }

       });

    };

    $scope.doSetEndpointStatus = function(s) {
        $scope.endpoint.status = s;
    };

    $scope.doUploadFileToFTP = function() {

        closeAlertFunc();

        if (isNew) {
            return;
        }

        // Validation
        if ($scope.ftpUploadFile.data == null) {
            showAlert("Please select a file to upload");
            return;
        }

        // Send data
        var fd = new FormData();
        fd.append('file', $scope.ftpUploadFile.data);

        $scope.disableForm = true;

        uploadClient.doPost($http, '/ftpmock/' + extId + '/file/upload', fd, function(status, data) {

            if (status != 201) {
                showAlert(globalVars.GeneralErrorMessage);
                $scope.disableForm = false;
                return;
            }

            $scope.ftpUploadFile = {
                data : null
            }

            showAlert("File uploaded", "success");
            $scope.disableForm = false;
        });

    };

    $scope.doDeleteUploadedFile = function(fileUri) {

        closeAlertFunc();

        if (isNew) {
            return;
        }

        var deletionConfirmationMsg = "Delete the file '" + fileUri + "'?";

        if (fileUri.endsWith("/")) {
            deletionConfirmationMsg = "Delete the directory '" + fileUri + "' and any content?";
        }

        utils.openWarningConfirmation(deletionConfirmationMsg, function (alertResponse) {

            if (alertResponse) {

                restClient.doDelete($http, '/ftpmock/' + extId + '/file?uri=' + encodeURIComponent(fileUri), function(status, data) {

                    if (status != 204) {
                        showAlert(globalVars.GeneralErrorMessage);
                        return;
                    }

                    loadFtpUserFiles();
                    showAlert("File deleted", "success");
                });

            }

        });

    };


    //
    // Internal Functions
    function clearEndpointData() {
        $rootScope.ftpEndpointData = null;
    }

    function checkAutoRefreshStatus(callback) {

        restClient.doGet($http, '/mockedserver/config/' + globalVars.FtpServerType, function(status, data) {
            if (status == 200) {
                callback(data.autoRefresh);
                return;
            }

            callback();
        });

    }

    var saveMockCallbackFunc = function (status, data) {

        if (status == 201 || status == 204) {

            checkAutoRefreshStatus(function(autoRefresh) {

                var locParams = {
                    'tab' : 'FTP'
                };

                if (autoRefresh != null && autoRefresh) {
                    locParams.restart = 'true';
                }

                utils.hideBlockingOverlay();
                $location.path("/dashboard").search(locParams);
                clearEndpointData();
            });

            return;
        }

        utils.hideBlockingOverlay();

        if (status == 409) {
            showAlert("'" + $scope.endpoint.name + "' is already in use");
            return;
        }

        showAlert(globalVars.GeneralErrorMessage);
    };

    function loadFtpUserFiles() {

        restClient.doGet($http, '/ftpmock/' + extId + '/file', function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.ftpFileTreeData = data;
        });

    }


    //
    // Init page
    if (!isNew) {
        loadFtpUserFiles();
    }

});
