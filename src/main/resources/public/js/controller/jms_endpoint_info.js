
app.controller('jmsEndpointInfoController', function($scope, $rootScope, $route, $location, $uibModal, $http, $timeout, utils, globalVars, restClient) {


    //
    // Labels
    $scope.serverRestartInstruction = '(Please note, the jms mock server will need to be restarted for changes to take effect)';


    //
    // Buttons
    $scope.saveButtonLabel = 'Save';
    $scope.cancelButtonLabel = 'Cancel';


    //
    // Scoped Functions
    $scope.doCancel = function() {
        $location.path("/dashboard");
        clearEndpointData();
    };

    $scope.doSaveEndpoint = function() {

    };


    //
    // Internal Functions
    function clearEndpointData() {
        $rootScope.endpointData = null;
        $scope.extId = null;
    }

});
