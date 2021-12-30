
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
        .when("/s3_endpoint", {
            templateUrl : "s3_endpoint_info.html",
            controller : "s3EndpointInfoController"
        })
        .when("/manage_users", {
            templateUrl : "manage_users.html",
            controller : "manageUsersController"
        })
        .when("/manage_user_kvp_data", {
            templateUrl : "manage_user_kvp_data.html",
            controller : "manageUserKvpDataController"
        });
});
