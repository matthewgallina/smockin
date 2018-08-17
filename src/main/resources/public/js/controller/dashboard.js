
app.controller('dashboardController', function($scope, $routeParams, $timeout, globalVars) {

    //
    // Constants
    var SelectedTab = $routeParams.tab;
    var tabIndexes = {
        'HTTP' : 0,
        'JMS' : 1,
        'FTP' : 2
    }

    //
    // Labels
    $scope.tcpHeading = 'HTTP';
    $scope.jmsHeading = 'JMS';
    $scope.ftpHeading = 'FTP';


    //
    // Alerts
    $scope.alerts = [];

    var closeAlertFunc = function() {
        $scope.alerts = [];
    };

    $scope.showAlert = function(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        $scope.alerts = [];
        $scope.alerts.push({ "type" : type, "msg" : msg });

        $timeout(closeAlertFunc, globalVars.AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data
    $scope.selectedTab = tabIndexes.HTTP;

    if (SelectedTab != null) {
        $scope.selectedTab = tabIndexes[SelectedTab];
    }

});
