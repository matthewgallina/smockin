
app.controller('s3EndpointInfoController', function($scope, $location, $uibModal, $http, $timeout, utils, globalVars, restClient, auth) {


    //
    // Constants
    var extId = $location.search()["eid"];
    var isNew = (extId == null);
    var s3PathPlaceHolderTxt = 'e.g. (my_cool_bucket)';


    //
    // Labels
    $scope.endpointHeading = (isNew) ? 'New S3 Bucket' : 'S3 Bucket';
    $scope.pathPlaceHolderTxt = s3PathPlaceHolderTxt;
    $scope.bucketLabel = 'Bucket Name';
    $scope.bucketStructureLabel = 'Directory Structure';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.endpointStatusLabel = 'Status:';
    $scope.addNodeButtonLabel = 'Add Directory';
    $scope.renameNodeButtonLabel = 'Rename';
    $scope.removeNodeButtonLabel = 'Remove';
    $scope.uploadFileButtonLabel = 'Upload File';


    //
    // Buttons
    $scope.saveButtonLabel = 'Save';
    $scope.deleteButtonLabel = 'Delete';
    $scope.cancelButtonLabel = 'Cancel';


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

    $scope.endpoint = {
        "bucket" : null,
        "status" : globalVars.ActiveStatus
    };

//    $scope.defaultCtxPathPrefix = (auth.isLoggedIn() && !auth.isSysAdmin()) ? ('/' + auth.getUserName()) : null;

    if (!isNew) {

        var handleLoadedMock = function(endpoint) {

            extId = endpoint.extId;

            $scope.endpoint = {
                "extId" : endpoint.extId,
                "bucket" : endpoint.bucket,
                "status" : endpoint.status,
                "createdBy" : endpoint.createdBy,
                "dateCreated" : endpoint.dateCreated,
                "userCtxPath" : endpoint.userCtxPath,
//                "parentExtId" : endpoint.parentExtId,
                "children" : endpoint.children
            };

//            $scope.defaultCtxPathPrefix = (!utils.isBlank(endpoint.userCtxPath)) ? ('/' + endpoint.userCtxPath) : null;
            $scope.readOnly = (auth.isLoggedIn() && auth.getUserName() != $scope.endpoint.createdBy);

            updateTree($scope.endpoint);

        };

        loadMockData(extId, handleLoadedMock);
    }

    $scope.readOnly = (!isNew && auth.isLoggedIn() && auth.getUserName() != $scope.endpoint.createdBy);


    //
    // Scoped Functions
    $scope.doSetEndpointStatus = function(s) {
        $scope.endpoint.status = s;
    };

    $scope.doDelete = function() {

        if (isNew) {
            return;
        }

        utils.openDeleteConfirmation("Are you sure you wish to delete this bucket and all of it's content?", function (alertResponse) {

            if (alertResponse) {

                utils.showBlockingOverlay();
                restClient.doDelete($http, '/s3mock/' + extId, serverCallbackFuncFollowingAddOrDelete);

            }

       });

    };

    $scope.doSaveEndpoint = function() {

        // Validation
        if (utils.isBlank($scope.endpoint.bucket)) {
            showAlert("'Bucket' is required");
            return;
        }

        // Send to Server
        createNewBucket($scope.endpoint.bucket, $scope.endpoint.status, serverCallbackFuncFollowingAddOrDelete);

/*
        utils.showBlockingOverlay();

        var reqData = {
            "bucket" : $scope.endpoint.bucket,
            "status" : $scope.endpoint.status
        };


        if (!isNew) {
            restClient.doPut($http, '/s3mock/' + extId, reqData, serverCallbackFuncFollowingAddOrDelete);
        } else {
            restClient.doPost($http, '/s3mock', reqData, serverCallbackFuncFollowingAddOrDelete);
        }
*/
    };

    $scope.doAddNode = function() {

        var nodeId;
        var selectedNode = $('#tree').treeview('getSelected', nodeId);

        // TODO prevent file selection
        if (selectedNode[0] == null || selectedNode[0].extId == null) {
            showAlert("Please select a directory");
            return;
        }

        var parentExtId = selectedNode[0].extId;

        var modalInstance = $uibModal.open({
            templateUrl: 's3_endpoint_node.html',
            controller: 's3EndpointNodeController',
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (data) {

            var nodeName = data.name;

            var serverCallbackFuncFollowingNewDir = function (status, data) {

                utils.hideBlockingOverlay();

                if (status == 201 || status == 204) {
                    var newNodeExtId = data.message;
                    appendNodeChild(parentExtId, $scope.endpoint, nodeName, newNodeExtId);
                    updateTree($scope.endpoint);
                    return;
                }

                if (status == 400) {
                    showAlert(data.message);
                    return;
                } else if (status == 409) {
                    showAlert("'" + $scope.endpoint.bucket + "' is already defined");
                    return;
                }

                showAlert(globalVars.GeneralErrorMessage);
            };

            createNewBucket(data.name, globalVars.ActiveStatus, parentExtId, serverCallbackFuncFollowingNewDir);

        }, function () {});

    };

    $scope.doRemoveNode = function() {

        var nodeId;
        var selectedNode = $('#tree').treeview('getSelected', nodeId);

        if (selectedNode[0] == null || selectedNode[0].extId == null) {
            showAlert("Please select a directory or file");
            return;
        }

        if (selectedNode[0].extId == $scope.endpoint.extId) {
            showAlert("You cannot remove the root bucket");
            return;
        }

        removeNodeChild(selectedNode[0].extId, $scope.endpoint);

        updateTree($scope.endpoint);

    };

    $scope.doUploadFile = function() {

        var nodeId;
        var selectedNode = $('#tree').treeview('getSelected', nodeId);

        // TODO prevent file selection
        if (selectedNode[0] == null || selectedNode[0].extId == null) {
            showAlert("Please select a directory in the tree");
            return;
        }

        var modalInstance = $uibModal.open({
            templateUrl: 's3_endpoint_file_uploader.html',
            controller: 's3EndpointFileUploaderController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                    return {
                        "extId" : selectedNode[0].extId,
                        "nodeName" : selectedNode[0].text
                    };
                }
            }
        });

        modalInstance.result.then(function (data) {
//            appendNodeChild(selectedNode[0].extId, $scope.endpoint, data.name);
//            updateTree($scope.endpoint);
        }, function () {});

    };

    $scope.doRenameNode = function() {

        var nodeId;
        var selectedNode = $('#tree').treeview('getSelected', nodeId);

        if (selectedNode[0] == null || selectedNode[0].extId == null) {
            showAlert("Please select a directory or file to rename");
            return;
        }

    };

    $scope.doCancel = function() {

        $location.path("/dashboard").search({ "dv" : globalVars.S3ServerMode });

    };


    //
    // Internal Functions
/*
    function getUserCtxPushPath(endpoint) {
        return ($scope.defaultCtxPathPrefix != null)
            ? ($scope.defaultCtxPathPrefix + endpoint.path)
            : endpoint.path;
    }
*/

    function createNewBucket(bucket, status, parentId, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "bucket" : bucket,
            "status" : status,
            "parentExtId" : parentId
        };

        restClient.doPost($http, '/s3mock', reqData, callbackFunc);
    }

    function appendNodeChild(extIdToMatch, endpoint, newNodeName, newNodeExtId) {

        if (extIdToMatch == endpoint.extId) {

            endpoint.expanded = true;

            // Use dateCreated field to determine if node is new or not.
            endpoint.children.push({
                "extId" : newNodeExtId,
//                "parentExtId" : endpoint.extId,
                "bucket" : newNodeName,
                "children" : [],
                "expanded" : false
            });

        } else {

            for (var c=0; c < endpoint.children.length; c++) {
                appendNodeChild(extIdToMatch, endpoint.children[c], newNodeName, newNodeExtId);
            }

        }

    }

    function removeNodeChild(extIdToMatch, endpoint) {

// console.log("extIdToMatch " + extIdToMatch);

        for (var c=0; c < endpoint.children.length; c++) {
// console.log("endpoint.children[c].extId :" + endpoint.children[c].bucket + " " + endpoint.children[c].extId);
            if (extIdToMatch == endpoint.children[c].extId) {
                endpoint.children.splice(c, 1);
                return;
            }
        }

        for (var c=0; c < endpoint.children.length; c++) {
            removeNodeChild(extIdToMatch, endpoint.children[c], endpoint);
        }

    }

    function loadMockData(extId, callback) {

        restClient.doGet($http, '/s3mock/' + extId, function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            callback(data);
        });

    }

    var serverCallbackFuncFollowingAddOrDelete = function (status, data) {

        utils.hideBlockingOverlay();

        if (status == 201 || status == 204) {
            $location.path("/dashboard").search(locParams);
            return;
        }

        if (status == 400) {
            showAlert(data.message);
            return;
        } else if (status == 409) {
            showAlert("'" + $scope.endpoint.bucket + "' is already defined");
            return;
        }

        showAlert(globalVars.GeneralErrorMessage);
    };


    //
    // Tree View
    function buildTree(endpoint) {

        // Current node
        var treeNode = buildTreeNode(endpoint.bucket, endpoint.extId, endpoint.expanded);

        // Children
        for (var c=0; c < endpoint.children.length; c++) {
            treeNode.nodes.push(buildTree(endpoint.children[c]));
        }

        return treeNode;
    }

    function buildTreeNode(bucket, extId, expanded) {

        return {
            extId: extId,
            text: bucket,
            nodes: [],
//            parentExtId: parentExtId,
            state: {
                expanded: (expanded != null && expanded)
            }
       };

    }

    function updateTreeView(extId, endpoint, open) {

        if (extId == endpoint.extId) {
            endpoint.expanded = open;
            return;
        }

        for (var c=0; c < endpoint.children.length; c++) {
            updateTreeView(extId, endpoint.children[c], open);
        }

    }

    function updateTree(endpoint) {

        $('#tree').treeview({
            data: [
                buildTree(endpoint)
            ],
            onNodeExpanded: function(event, data) {
                updateTreeView(data.extId, $scope.endpoint, true);
            },
            onNodeCollapsed: function(event, data) {
                updateTreeView(data.extId, $scope.endpoint, false);
            }
        });

    }

});
