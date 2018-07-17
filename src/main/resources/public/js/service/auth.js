
app.service('auth', function($uibModal) {

    this.doRequestAuth = function (callbackAction) {

        var modalInstance = $uibModal.open({
            templateUrl: 'authentication.html',
            controller: 'authenticationController',
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (data) {
            callbackAction(data);
        }, function () {
            callbackAction();
        });

    };

});
