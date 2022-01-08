
app.controller('mailEndpointInfoController', function($scope, $location, $uibModal, $http, $timeout, utils, globalVars, restClient, auth) {


    //
    // Constants
    var extId = $location.search()["eid"];
    var isNew = (extId == null);
    var mailPathPlaceHolderTxt = 'e.g. (max@smockin.com)';
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;


    //
    // Labels
    $scope.endpointHeading = (isNew) ? 'New Mail Inbox' : 'Mail Inbox';
    $scope.pathPlaceHolderTxt = mailPathPlaceHolderTxt;
    $scope.inboxAddressLabel = 'Inbox Address';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.endpointStatusLabel = 'Status:';


    //
    // Buttons
    $scope.deleteButtonLabel = 'Delete';
    $scope.saveButtonLabel = 'Save';
    $scope.cancelButtonLabel = 'Close';


    //
    // Alerts
    var alertPromise = null;
    $scope.alerts = [];

    var closeAlertFunc = function() {

        if (alertPromise != null) {
            $timeout.cancel( alertPromise );
            alertPromise = null;
        }

        $scope.alerts = [];
    };

    function showAlert(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        closeAlertFunc();
        $scope.alerts.push({ "type" : type, "msg" : msg });

        alertPromise = $timeout(closeAlertFunc, globalVars.AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;
    $scope.isNew = isNew;
    $scope.mockServerStatus = MockServerStoppedStatus;
    $scope.mockServerStoppedStatus = MockServerStoppedStatus;

    $scope.endpoint = {
        "address" : null,
        "status" : globalVars.ActiveStatus
    };

    if (!isNew) {

        var handleLoadedMock = function(endpoint) {

            extId = endpoint.extId;

            $scope.endpoint = {
                "extId" : endpoint.externalId,
                "address" : endpoint.address,
                "status" : endpoint.status,
                "dateCreated" : endpoint.dateCreated
            };

//            $scope.readOnly = (auth.isLoggedIn() && auth.getUserName() != $scope.endpoint.createdBy);

        };

        loadMockData(extId, handleLoadedMock);
    }

//    $scope.readOnly = (!isNew && auth.isLoggedIn() && auth.getUserName() != $scope.endpoint.createdBy);


    //
    // Scoped Functions
    $scope.doSetEndpointStatus = function(s) {
        $scope.endpoint.status = s;
    };

    $scope.doSaveMailAddress = function() {

        // Validation
        if (utils.isBlank($scope.endpoint.address)) {
            showAlert("'Address' is required");
            return;
        }
        if ($scope.endpoint.address.length < 10) {
            showAlert("Invalid mail 'Address' format (min 10 chars)");
            return;
        }
        if ($scope.endpoint.address.indexOf("@") == -1
                || $scope.endpoint.address.indexOf(".") == -1) {
            showAlert("Invalid mail 'Address' format");
            return;
        }

        // Send to Server
        if (isNew) {

            createNewAddress($scope.endpoint.address, $scope.endpoint.status, serverCallbackFuncFollowingNewRecord);

        } else {

            updateAddress($scope.endpoint.extId,
                          $scope.endpoint.address,
                          $scope.endpoint.status,
                            function (status, data) {

                                 utils.hideBlockingOverlay();

                                 if (status == 204) {
                                     showAlert('Mail Address successfully updated', 'success');
                                     return;
                                 }

                                 handleErrorResponse(status, data);
                             });

        }
    };

    $scope.doDelete = function() {
        doDeleteAddress();
    };

    $scope.doClose = function() {

        $location.path("/dashboard").search({
            "dv" : globalVars.MailServerMode
        });
    };


    //
    // Internal Functions
    function createNewAddress(address, status, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "address" : address,
            "status" : status
        };

        restClient.doPost($http, '/mailmock', reqData, callbackFunc);
    }

    function updateAddress(extId, address, status, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "address" : address,
            "status" : status
        };

        restClient.doPut($http, '/mailmock/' + extId, reqData, callbackFunc);
    }

    function doDeleteAddress() {

        utils.openDeleteConfirmation("Are you sure you wish to delete this mail address and all of it's content?", function (alertResponse) {
            if (alertResponse) {
                deleteAddress($scope.endpoint.extId);
            }
       });

    }

    function deleteAddress(extId) {

        utils.showBlockingOverlay();

        var serverCallbackFuncFollowingDelete = function (status, data) {

            utils.hideBlockingOverlay();

            if (status == 204) {

                $scope.doClose();
                return;
            }

            handleErrorResponse(status, data);
        };

        restClient.doDelete($http, '/mailmock/' + nodeType.toLowerCase() + '/' + extId, serverCallbackFuncFollowingDelete);

    }

    function loadMockData(extId, callback) {

        restClient.doGet($http, '/mailmock/' + extId, function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            callback(data);
        });

    }

    var serverCallbackFuncFollowingNewRecord = function (status, data) {

        utils.hideBlockingOverlay();

        if (status == 201) {
            $location.path("/dashboard").search({ "dv" : globalVars.MailServerMode });

            return;
        }

        handleErrorResponse(status, data);
    };

    function handleErrorResponse(status, data) {

        if (status == 400) {
            showAlert(data.message);
            return;
        } else if (status == 409) {
            showAlert("'" + $scope.endpoint.address + "' is already defined");
            return;
        }

        showAlert(globalVars.GeneralErrorMessage);
    }

    function loadMailServerStatus() {

        utils.checkMailServerStatus(function(running, port) {

            if (running == null) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.mockServerStatus = (running)
                ? MockServerRunningStatus
                : MockServerStoppedStatus;
        });

    }


    //
    // init page
    loadMailServerStatus();

});
