
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
        .when("/endpoint", {
            templateUrl : "endpoint_info.html",
            controller : "endpointInfoController"
        });
});
