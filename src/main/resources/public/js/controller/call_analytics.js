
app.controller('callAnalyticsController', function($scope, $location, $window, $uibModal, $http, restClient, globalVars, auth, utils) {


    //
    // Labels
    $scope.heading = 'Call Analytics';
    $scope.nameTableLabel = "Name";
    $scope.httpCountTableLabel = "HTTP Call Count";
    $scope.s3countTableLabel = "S3 Call Count";
    $scope.mailCountTableLabel = "Mail Call Count";
    $scope.actionTableLabel = "Action";
    $scope.noDataFoundLabel = "No Data Found";


    //
    // Buttons
    $scope.addCallBatchButtonLabel = "Add Call Analytics Batch";
    $scope.viewCallAnalyticsButtonLabel = "View";
    $scope.deleteCallAnalyticsButtonLabel = "Delete";


    //
    // Data
    $scope.callAnalyticsData = [];


    //
    // Scoped Functions
    $scope.doDeleteCallAnalytic = function(callAnalyticData) {

        utils.openDeleteConfirmation("Are you sure you wish to delete this call analytics batch?", function (alertResponse) {

            if (alertResponse) {

                restClient.doDelete($http, '/call-analytic/' + callAnalyticData.externalId, function(status, data) {

                    if (status != 204) {
                        showAlert(globalVars.GeneralErrorMessage);
                        return;
                    }

                    loadCallAnalyticsTableData();
                });

            }

        });

    };

    $scope.doOpenCreateAnalyticsCall = function() {

        var modalInstance = $uibModal.open({
            templateUrl: 'new_call_analytics_data.html',
            controller: 'newCallAnalyticsDataController',
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (response) {
            if (response != null && response) {
                loadCallAnalyticsTableData();
            }
        }, function () {  });

    };

    $scope.doViewCallAnalytic = function(callAnalyticData) {

        $location.path("/call_analytic_details").search({ "cid" : callAnalyticData.externalId });;

    };


    //
    // Internal Functions
    function loadCallAnalyticsTableData() {

        $scope.callAnalyticsData = [];

        restClient.doGet($http, '/call-analytic', function(status, data) {

            if (status == 401) {
                redirectToHome();
                return;
            } else if (status != 200) {
                redirectToHome();
                return;
            }

            $scope.callAnalyticsData = data;
        });

    }

    function redirectToHome() {
        $window.location.href = "/index.html";
    }


    //
    // Init Page
    loadCallAnalyticsTableData();

});
