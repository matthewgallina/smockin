
app.config(function($routeProvider) {
    $routeProvider
        .when("/", {
            templateUrl : "dashboard.html",
            controller : "dashboardController"
        })
        .when("/dashboard", {
            templateUrl : "dashboard.html",
            controller : "dashboardController"
        })
        .when("/tcp_endpoint", {
            templateUrl : "tcp_endpoint_info.html",
            controller : "tcpEndpointInfoController"
        })
        .when("/jms_endpoint", {
            templateUrl : "jms_endpoint_info.html",
            controller : "jmsEndpointInfoController"
        })
        .when("/ftp_endpoint", {
            templateUrl : "ftp_endpoint_info.html",
            controller : "ftpEndpointInfoController"
        });

});
