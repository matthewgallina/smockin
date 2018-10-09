
app.controller('proxyPathConflictManagerController', function($scope, $uibModalInstance, $http, $timeout, globalVars, restClient, auth) {


    //
    // Labels
    $scope.heading = 'Proxy Priority Path Manager';
    $scope.instructionsLineLabel = 'Please specify which mock the proxy server should use where there is matching path:';


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
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    $scope.pathDuplicates = [];


    //
    // Scoped Functions
    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };

    $scope.doSave = function() {

        if ($scope.readOnly) {
            return;
        }

        var pathDuplicatePriorities = [];

        // Validation
        for (var i=0; i < $scope.pathDuplicates.length; i++) {

            if ($scope.pathDuplicates[i].priorityExtId == null) {
                showAlert("Please specify which mock to use for path: " + $scope.pathDuplicates[i].path);
                return;
            }

            pathDuplicatePriorities.push($scope.pathDuplicates[i].priorityExtId);
        }

        // Send Request
        var reqData = {
            "proxyPriorityMockIds" : pathDuplicatePriorities
        };

        restClient.doPost($http, '/proxyconfig/duplicate', reqData, function(status, data) {

            if (status != 204) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $uibModalInstance.close();
        });

    };


    //
    // Internal Functions
    function loadDuplicates() {

        $scope.pathDuplicates = [];

        restClient.doGet($http, '/proxyconfig/duplicate', function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.pathDuplicates = data;

            for (var p=0; p < $scope.pathDuplicates.length; p++) {

                $scope.pathDuplicates[p].priorityExtId = null;

                for (var m=0; m < $scope.pathDuplicates[p].mocks.length; m++) {
                    if ($scope.pathDuplicates[p].mocks[m].proxyPriority) {
                        $scope.pathDuplicates[p].priorityExtId = $scope.pathDuplicates[p].mocks[m].extId;
                    }
                }

            }

        });

    }


    //
    // Init page
    loadDuplicates();

});
