
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
        .when("/mail_endpoint", {
            templateUrl : "mail_endpoint_info.html",
            controller : "mailEndpointInfoController"
        })
        .when("/manage_users", {
            templateUrl : "manage_users.html",
            controller : "manageUsersController"
        })
        .when("/manage_user_kvp_data", {
            templateUrl : "manage_user_kvp_data.html",
            controller : "manageUserKvpDataController"
        })
        .when("/call_analytics", {
            templateUrl : "call_analytics.html",
            controller : "callAnalyticsController"
        })
        .when("/call_analytic_details", {
            templateUrl : "call_analytic_details.html",
            controller : "callAnalyticDetailsController"
        });
});
