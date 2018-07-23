app.service('auth', function($uibModal, globalVars) {

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

    this.isLoggedIn = function () {
        return (this.getToken() != null);
    };

    this.saveToken = function (token) {
        localStorage.setItem(globalVars.SmockinAuthToken, token);
    };

    this.getToken = function () {
        return localStorage.getItem(globalVars.SmockinAuthToken);
    };

    this.clearToken = function () {
    	localStorage.removeItem(globalVars.SmockinAuthToken);
    };

});
