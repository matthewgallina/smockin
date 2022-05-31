
app.controller('callAnalyticDetailsController', function($scope, $location, $window, $http, restClient, globalVars, auth, utils) {

    //
    // Constants
    var extId = $location.search()["cid"];


    //
    // Labels
    $scope.heading = 'Call Analytic Details';
    $scope.nameLabel = 'Name';
    $scope.logsHeading = 'Call Log';


    //
    // Table Labels
    $scope.pathTableLabel = 'Path';
    $scope.typeTableLabel = 'Type';
    $scope.resultTableLabel = 'Result';
    $scope.dateCreatedTableLabel = 'Date Received';


    //
    // Buttons
    $scope.backButtonLabel = "Back";


    //
    // Data
    $scope.callAnalyticData = {};


    //
    // Scoped Functions
    $scope.doReturnBack = function() {
        $location.path("/call_analytics");
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
