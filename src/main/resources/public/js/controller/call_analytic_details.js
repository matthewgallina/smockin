
app.controller('callAnalyticDetailsController', function($scope, $location, $window, $http, restClient, globalVars, auth, utils) {

    //
    // Constants
    var extId = $location.search()["cid"];


    //
    // Labels
    $scope.heading = 'Call Analytic Details';
    $scope.nameLabel = 'Batch Name';
    $scope.pathLabel = 'URL Path Prefix';
    $scope.logsHeading = 'HTTP Call Log';
    $scope.logsCountHeading = 'Total Calls Received:';


    //
    // Table Labels
    $scope.noResultsFoundLabel = 'No Records Found';
    $scope.pathTableLabel = 'Path';
    $scope.typeTableLabel = 'Type';
    $scope.resultTableLabel = 'Result';
    $scope.dateCreatedTableLabel = 'Date Received';


    //
    // Buttons
    $scope.backButtonLabel = "Back";
    $scope.deleteCallAnalyticsButtonLabel = "Delete";


    //
    // Data
    $scope.callAnalyticData = {};


    //
    // Scoped Functions
    $scope.doReturnBack = function() {
        $location.path("/call_analytics");
    };

    $scope.doDeleteCallAnalytic = function() {

        utils.openDeleteConfirmation("Are you sure you wish to delete this call analytics batch?", function (alertResponse) {

            if (alertResponse) {

                restClient.doDelete($http, '/call-analytic/' + extId, function(status, data) {

                    if (status != 204) {
                        showAlert(globalVars.GeneralErrorMessage);
                        return;
                    }

                    $location.path("/call_analytics");
                });

            }

        });

    };

    //
    // Internal Functions
    function loadCallAnalyticData() {

        $scope.callAnalyticsData = [];

        restClient.doGet($http, '/call-analytic/' + extId, function(status, data) {

            if (status == 401) {
                redirectToHome();
                return;
            } else if (status != 200) {
                redirectToHome();
                return;
            }

            $scope.callAnalyticData = data;
        });

    }

    function redirectToHome() {
        $window.location.href = "/index.html";
    }


    //
    // Init Page
    loadCallAnalyticData();

});
