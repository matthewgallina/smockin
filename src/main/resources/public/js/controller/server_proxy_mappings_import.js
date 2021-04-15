
app.controller('serverProxyMappingsImportController', function($scope, $uibModalInstance, $timeout, globalVars, $http, uploadClient) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var RestfulServerType = globalVars.RestfulServerType;


    //
    // Labels
    $scope.importHeading = 'Path to URL Mappings - Import Manager';
    $scope.selectFileLabel = 'Select mappings file...';
    $scope.keepExistingLabel = "Keep Existing Mappings";


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.importButtonLabel = 'Import & Save';


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
    $scope.keepExisting = true;
    $scope.proxyMappingsUploadFile = {
        data : null
    };


    //
    // Scoped Functions
    $scope.doClose = function() {

        $uibModalInstance.close();
    };

    $scope.doUploadFile = function() {

        // Validation
        if ($scope.proxyMappingsUploadFile.data == null
                || $scope.proxyMappingsUploadFile.data.name.toLowerCase().indexOf(".json") == -1) {
            showAlert("Please select a .json based file to import");
            return;
        }

        $scope.disableForm = true;

        // Send data
        var fd = new FormData();
        fd.append('file', $scope.proxyMappingsUploadFile.data);

        uploadClient.doPostWithHeaders($http, '/mockedserver/config/' + RestfulServerType + '/proxy/mappings/import', fd, { 'KeepExisting' : $scope.keepExisting }, function(status, data) {

            $scope.proxyMappingsUploadFile = {
                data : null
            }

            $scope.disableForm = false;

            if (status == 400) {
                showAlert((data.message != null)
                            ? data.message
                            : "There is an issue with importing this file");
                return;
            }

            if (status != 204) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $uibModalInstance.close({
                "uploadCompleted" : true
            });

        });

    }

    function initPage() {

        // Change width of second modal
        jQuery(function() {
            jQuery('.modal-dialog').first().addClass("proxy-mappings-import-endpoint-modal");
        });

    }


    //
    // Init Page
    initPage();

});
