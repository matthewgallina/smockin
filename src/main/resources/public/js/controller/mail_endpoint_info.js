
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
    $scope.saveReceivedMailLabel = 'Auto-Save messages';
    $scope.includeMailMessagesInSavePromptLabel1 = 'Include';
    $scope.includeMailMessagesInSavePromptLabel2 = 'message(s) currently on mail server?';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.endpointStatusLabel = 'Status:';
    $scope.inboxMessagesLabel = 'Mail Messages';
    $scope.noDataFoundMsg = 'No Messages Found';
    $scope.reloadMessagesLabel = 'refresh';
    $scope.savedToDatabaseLabel = 'Saved to DB';
    $scope.savedToMailServerLabel = 'Mail Server';
    $scope.mailServerLabel = 'server';
    $scope.offlineLabel = 'offline';
    $scope.selectAllEndpointsHeading = 'select all';
    $scope.deselectAllEndpointsHeading = 'clear selection';


    //
    // Table Labels
    $scope.senderTableLabel = 'From';
    $scope.subjectTableLabel = 'Subject';
    $scope.dateReceivedTableLabel = 'Date Received';
    $scope.locationLabel = 'Location';
    $scope.attachmentsLabel = 'Attachments';
    $scope.actionTableLabel = '';


    //
    // Buttons
    $scope.deleteButtonLabel = 'Delete';
    $scope.saveButtonLabel = 'Save';
    $scope.cancelButtonLabel = 'Close';
    $scope.viewMessageButtonLabel = 'View';
    $scope.deleteMessagesButtonLabel = 'Delete Messages';


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
    var currentSaveReceivedMailState = false;
    $scope.showIncludeMailMessagesInSavePrompt = false;
    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;
    $scope.isNew = isNew;
    $scope.mockServerStatus = MockServerStoppedStatus;
    $scope.mockServerRunningStatus = MockServerRunningStatus;
    $scope.mockServerStoppedStatus = MockServerStoppedStatus;
    $scope.endpoint = {
        "address" : null,
        "status" : globalVars.ActiveStatus,
        "retainCachedMail" : false,
        "saveReceivedMail" : false
    };
    $scope.mailMessages = [];
    $scope.messagesSelection = [];
    $scope.mailServerMessageCount = 0;


    //
    // Scoped Functions
    $scope.doSetEndpointStatus = function(s) {
        $scope.endpoint.status = s;
    };

    $scope.doToggleIncludeMailMessagesInSavePrompt = function() {

        if (isNew
                || $scope.mailServerMessageCount == 0) {
            return;
        }

        if (!$scope.endpoint.saveReceivedMail) {
            $scope.showIncludeMailMessagesInSavePrompt = false;
            $scope.endpoint.retainCachedMail = false;
            return;
        }

        if (!$scope.showIncludeMailMessagesInSavePrompt
                && !currentSaveReceivedMailState) {
            $scope.showIncludeMailMessagesInSavePrompt = true;
        }

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

            createNewAddress($scope.endpoint.address,
                             $scope.endpoint.status,
                             $scope.endpoint.saveReceivedMail,
                             function (status, data) {

                                 utils.hideBlockingOverlay();

                                 if (status == 201) {
                                     $location.path("/dashboard").search({ "dv" : globalVars.MailServerMode });
                                     return;
                                 }

                                 handleErrorResponse(status, data);
                             });

        } else {

            updateAddress($scope.endpoint.extId,
                  $scope.endpoint.address,
                  $scope.endpoint.status,
                  $scope.endpoint.saveReceivedMail,
                  $scope.endpoint.retainCachedMail,
                  function (status, data) {

                     utils.hideBlockingOverlay();

                     if (status == 204) {
                         showAlert('Mail Inbox successfully updated', 'success');
                         $scope.showIncludeMailMessagesInSavePrompt = false;
                         loadMock();
                         return;
                     }

                     handleErrorResponse(status, data);
                  });

        }
    };

    $scope.doDelete = function() {
        doDeleteAddress();
    };

    $scope.doReloadMock = function() {
        loadMock();
    };

    $scope.doClose = function() {

        $location.path("/dashboard").search({
            "dv" : globalVars.MailServerMode
        });
    };

    $scope.doOpenMailMessage = function(mailMessage) {

        var modalInstance = $uibModal.open({
            templateUrl: 'mail_endpoint_message.html',
            controller: 'mailEndpointMessageController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                    return {
                        "mockMailExtId" : $scope.endpoint.extId,
                        "mailMessageData" : mailMessage
                    };
                }
            }
        });

        modalInstance.result.then(function() {
        }, function () {
        });

    };

    $scope.doSelectAllMessages = function() {

        $scope.messagesSelection = [];

        for (var m=0; m < $scope.mailMessages.length; m++) {

            var message = $scope.mailMessages[m];

            var messageId = (message.extId != null)
                ? message.extId
                : message.cacheID;

            $scope.messagesSelection.push(messageId);
        }

    };

    $scope.doesMessagesSelectionContain = function(message) {

        var messageId = (message.extId != null)
                            ? message.extId
                            : message.cacheID;

        for (var m=0; m < $scope.messagesSelection.length; m++) {
            if ($scope.messagesSelection[m] == messageId) {
                return true;
            }
        }

        return false;
    };

    $scope.doClearAllMessages = function() {
        $scope.messagesSelection = [];
    };

    $scope.doDeleteSelection = function() {

        if ($scope.messagesSelection.length == 0) {
            return;
        }

        utils.openDeleteConfirmation("Are you sure wish to delete the selected messages?", function (alertResponse) {

            if (alertResponse) {

                for (let m=0; m < $scope.messagesSelection.length; m++) {

                    let containsErrors = false;

                    var deleteMsgCallback = function(status, data) {

                        if (!containsErrors && status != 204) {
                            containsErrors = true;
                        }

                        if ((m+1) == $scope.messagesSelection.length) {

                            $timeout(function () {

                                loadMock();

                                if (containsErrors) {
                                    showAlert(globalVars.GeneralErrorMessage + ' not all messages were deleted');
                                    return;
                                }

                                showAlert('Selected messages have been deleted', 'success');

                            }, 1000);

                        }

                    }

                    restClient.doDelete($http, '/mailmock/' + $scope.endpoint.extId + '/inbox/' + $scope.messagesSelection[m], deleteMsgCallback);
                }

            }

        });

    };

    $scope.toggleMessageSelection = function(message) {

        var messageId = (message.extId != null)
            ? message.extId
            : message.cacheID;

        var pos = $scope.messagesSelection.indexOf(messageId);

        if (pos == -1) {
            $scope.messagesSelection.push(messageId);
            return;
        }

        $scope.messagesSelection.splice(pos, 1);

    };


    //
    // Internal Functions
    function loadMock() {

        loadMockData(extId, function(endpoint) {

            $scope.endpoint = {
                "extId" : endpoint.externalId,
                "address" : endpoint.address,
                "status" : endpoint.status,
                "dateReceived" : endpoint.dateReceived,
                "saveReceivedMail" : endpoint.saveReceivedMail
            };

            currentSaveReceivedMailState = endpoint.saveReceivedMail;

            // saved messages from DB
            $scope.mailMessages = endpoint.messages;

            // messages from mail server
            loadMailServerStatus(function() {
                loadInboxMessages();
            });

        });

    }

    function createNewAddress(address, status, saveReceivedMail, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "address" : address,
            "status" : status,
            "saveReceivedMail" : saveReceivedMail
        };

        restClient.doPost($http, '/mailmock', reqData, callbackFunc);
    }

    function updateAddress(extId, address, status, saveReceivedMail, retainCachedMail, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "address" : address,
            "status" : status,
            "saveReceivedMail" : saveReceivedMail
        };

        var retainCachedMailReqParam = (retainCachedMail != null)
            ? '?retainCachedMail=' + retainCachedMail
            : '';

        restClient.doPut($http, '/mailmock/' + extId + retainCachedMailReqParam, reqData, callbackFunc);
    }

    function doDeleteAddress() {

        if (isNew) {
            return;
        }

        utils.openDeleteConfirmation("Are you sure you wish to delete this mail address and all of it's content?", function (alertResponse) {
            if (alertResponse) {
                deleteAddress($scope.endpoint.extId);
            }
       });

    }

    function deleteAddress(extId) {

        if (isNew) {
            return;
        }

        utils.showBlockingOverlay();

        var serverCallbackFuncFollowingDelete = function (status, data) {

            utils.hideBlockingOverlay();

            if (status == 204) {

                $scope.doClose();
                return;
            }

            handleErrorResponse(status, data);
        };

        restClient.doDelete($http, '/mailmock/' + extId, serverCallbackFuncFollowingDelete);

    }

    function loadMockData(extId, callback) {

        if (isNew) {
            return;
        }

        restClient.doGet($http, '/mailmock/' + extId, function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            callback(data);
        });

    }

    function loadInboxMessages() {

        $scope.mailServerMessageCount = 0;

        if (isNew || $scope.mockServerStatus == MockServerStoppedStatus) {
            return;
        }

        restClient.doGet($http, '/mailmock/' + extId + '/inbox', function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.mailServerMessageCount = data.length;
            $scope.mailMessages = $scope.mailMessages.concat(data);
        });

    }

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

    function loadMailServerStatus(callbackFunc) {

        utils.checkMailServerStatus(function(running, port) {

            if (running == null) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.mockServerStatus = (running)
                ? MockServerRunningStatus
                : MockServerStoppedStatus;

            if (callbackFunc != null) {
                callbackFunc();
            }

        });

    }


    //
    // init page
    if (!isNew) {
        loadMock();
    }

});
