
app.controller('dashboardController', function($scope, $routeParams, $timeout, globalVars) {

    //
    // Constants
    var SelectedTab = $routeParams.tab;
    var tabIndexes = {
        'TCP' : 0,
        'JMS' : 1,
    }

    //
    // Labels
    $scope.tcpHeading = 'TCP';
    $scope.jmsHeading = 'JMS';


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
    $scope.selectedTab = tabIndexes.TCP;

    if (SelectedTab != null) {
        $scope.selectedTab = tabIndexes[SelectedTab];
    }

});
