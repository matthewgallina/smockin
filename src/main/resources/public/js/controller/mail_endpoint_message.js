
app.controller('mailEndpointMessageController', function($scope, $uibModalInstance, $timeout, $http, restClient, utils, globalVars, data) {


    //
    // Labels
    $scope.heading = 'Mail Message';
    $scope.senderLabel = 'From';
    $scope.dateReceivedLabel = 'Date Received';
    $scope.subjectLabel = 'Subject';
    $scope.bodyLabel = 'Message Body';
    $scope.noDataFoundMsg = 'No Attachments Found';


    //
    // Table Labels
    $scope.fileNameTableLabel = 'File Name';
    $scope.actionTableLabel = '';


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
    // Data
    $scope.attachments = [];

    //
    // Buttons
    $scope.closeButtonLabel = 'Close';
    $scope.downloadAttachmentButtonLabel = 'Download';


    //
    // Data Objects
    $scope.mailMessage = data.mailMessageData;


    //
    // Scoped Functions
    $scope.doClose = function() {
        $uibModalInstance.dismiss('cancel');
    };

    $scope.doDownloadFileAttachment = function(attachment) {

        var messageId = (data.mailMessageData.extId != null)
              ? data.mailMessageData.extId
              : data.mailMessageData.cacheID;

        var attachmentIdOrName = (attachment.extId != null)
              ? attachment.extId
              : attachment.name;

        loadAttachmentContent(data.mockMailExtId, messageId, attachmentIdOrName);
    };


    //
    // Internal Functions
    function loadAttachments(mailExtId, messageId) {

        restClient.doGet($http, '/mailmock/' + mailExtId + '/message/' + messageId + '/attachments', function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.attachments = data;
        });

    }

    function loadAttachmentContent(mailExtId, messageId, attachmentIdOrName) {

        restClient.doGet($http, '/mailmock/' + mailExtId + '/message/' + messageId + '/attachment/' + attachmentIdOrName, function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            utils.handleExportDownload(data.base64Content, data.name, data.mimeType);
        });

    }


    //
    // Init Page
    loadAttachments(
        data.mockMailExtId,
        (data.mailMessageData.extId != null)
            ? data.mailMessageData.extId
            : data.mailMessageData.cacheID);

});
