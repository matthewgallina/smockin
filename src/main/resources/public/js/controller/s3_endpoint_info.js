
app.controller('s3EndpointInfoController', function($scope, $location, $uibModal, $http, $timeout, utils, globalVars, restClient, auth) {


    //
    // Constants
    var extId = $location.search()["eid"];
    var isNew = (extId == null);
    var s3PathPlaceHolderTxt = 'e.g. (my_cool_bucket)';
    var NodeTypeDir = 'DIR';
    var NodeTypeFile = 'FILE';


    //
    // Labels
    $scope.endpointHeading = (isNew) ? 'New S3 Bucket' : 'S3 Bucket';
    $scope.pathPlaceHolderTxt = s3PathPlaceHolderTxt;
    $scope.bucketLabel = 'Bucket Name';
    $scope.bucketStructureLabel = 'Directory Structure';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.endpointStatusLabel = 'Status:';
    $scope.addNodeButtonLabel = 'Add Dir';
    $scope.renameNodeButtonLabel = 'Rename Dir';
    $scope.removeNodeButtonLabel = 'Remove';
    $scope.uploadFileButtonLabel = 'Upload File';


    //
    // Buttons
    $scope.saveButtonLabel = 'Create';
    $scope.deleteButtonLabel = 'Delete Bucket';
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
                "children" : endpoint.children,
                "files" : endpoint.files
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

    $scope.doSaveEndpoint = function() {

        // Validation
        if (utils.isBlank($scope.endpoint.bucket)) {
            showAlert("'Bucket' is required");
            return;
        }

        // Send to Server
        createNewBucket($scope.endpoint.bucket, $scope.endpoint.status, null, serverCallbackFuncFollowingNewBucket);
    };

    $scope.doAddNode = function() {

        if (isNew) {
            return;
        }

        var nodeId;
        var selectedNode = $('#tree').treeview('getSelected', nodeId);

        if (selectedNode[0] == null
                || selectedNode[0].extId == null
                || selectedNode[0].nodeType == NodeTypeFile) {
            showAlert("Please select a directory");
            return;
        }

        var parentExtId = selectedNode[0].extId;

        var modalInstance = $uibModal.open({
            templateUrl: 's3_endpoint_node.html',
            controller: 's3EndpointNodeController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                    return {};
                }
            }
        });

        modalInstance.result.then(function (data) {

            var nodeName = data.name;

            var serverCallbackFuncFollowingNewDir = function (status, data) {

                utils.hideBlockingOverlay();

                if (status == 201) {
                    var newNodeExtId = data.message;
                    appendNodeChild(parentExtId, $scope.endpoint, nodeName, newNodeExtId);
                    updateTree($scope.endpoint);
                    return;
                }

                handleErrorResponse(status, data);
            };

            createNewBucket(data.name, globalVars.ActiveStatus, parentExtId, serverCallbackFuncFollowingNewDir);

        }, function () {});

    };

    $scope.doRemoveNode = function() {

        if (isNew) {
            return;
        }

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

        utils.openDeleteConfirmation("Are you sure you wish to delete this directory and all of it's content?", function (alertResponse) {
            if (alertResponse) {
                deleteNode(selectedNode[0].extId, selectedNode[0].nodeType, false);
            }
       });

    };

    $scope.doUploadFile = function() {

        if (isNew) {
            return;
        }

        var nodeId;
        var selectedNode = $('#tree').treeview('getSelected', nodeId);

        if (selectedNode[0] == null
                || selectedNode[0].extId == null
                || selectedNode[0].nodeType == NodeTypeFile) {
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
            appendNodeChild(selectedNode[0].extId, $scope.endpoint, data.name, data.extId);
            updateTree($scope.endpoint);
        }, function () {});

    };

    $scope.doRenameNode = function() {

        if (isNew) {
            return;
        }

        var nodeId;
        var selectedNode = $('#tree').treeview('getSelected', nodeId);

        if (selectedNode[0] == null
                || selectedNode[0].extId == null
                || selectedNode[0].nodeType == NodeTypeFile) {
            showAlert("Please select a directory to rename");
            return;
        }

        var extId = selectedNode[0].extId;
        var nodeName = selectedNode[0].text;

        var modalInstance = $uibModal.open({
            templateUrl: 's3_endpoint_node.html',
            controller: 's3EndpointNodeController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                    return {
                        "extId" : extId,
                        "nodeName" : nodeName
                    };
                }
            }
        });

        modalInstance.result.then(function (data) {

            var nodeName = data.name;

            var serverCallbackFuncFollowingUpdate = function (status, data) {

                utils.hideBlockingOverlay();

                if (status == 204) {
                    amendNodeChildDir(extId, nodeName, $scope.endpoint);
                    updateTree($scope.endpoint);
                    return;
                }

                handleErrorResponse(status, data);
            };

            updateBucket(extId, nodeName, globalVars.ActiveStatus, serverCallbackFuncFollowingUpdate);

        }, function () {});

    };

    $scope.doDeleteBucket = function () {

        utils.openDeleteConfirmation("Are you sure you wish to delete this S3 bucket and all of it's content?", function (alertResponse) {
            if (alertResponse) {
                deleteNode($scope.endpoint.extId, NodeTypeDir, true);
            }
       });

    };

    $scope.doClose = function() {

        $location.path("/dashboard").search({
            "dv" : globalVars.S3ServerMode
        });
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

    function updateBucket(extId, bucket, status, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "bucket" : bucket,
            "status" : status
        };

        restClient.doPut($http, '/s3mock/' + extId, reqData, callbackFunc);
    }

    function deleteNode(extId, nodeType, isRoot) {

        utils.showBlockingOverlay();

        var serverCallbackFuncFollowingDelete = function (status, data) {

            utils.hideBlockingOverlay();

            if (status == 204) {

                if (isRoot) {
                    $scope.doClose();
                    return;
                }

                removeNodeChild(extId, $scope.endpoint, nodeType);
                updateTree($scope.endpoint);

                return;
            }

            handleErrorResponse(status, data);
        };

        restClient.doDelete($http, '/s3mock/' + extId + '?type=' + nodeType, serverCallbackFuncFollowingDelete);

    }

    function amendNodeChildDir(extIdToMatch, newNodeName, endpoint) {

        if (extIdToMatch == endpoint.extId) {

            endpoint.bucket = newNodeName;

        } else {

            for (var c=0; c < endpoint.children.length; c++) {
                amendNodeChildDir(extIdToMatch, newNodeName, endpoint.children[c]);
            }

        }

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

    function removeNodeChild(extIdToMatch, endpoint, nodeType) {

        if (nodeType == NodeTypeDir) {

            for (var c=0; c < endpoint.children.length; c++) {
                if (extIdToMatch == endpoint.children[c].extId) {
                    endpoint.children.splice(c, 1);
                    return;
                }
            }

            for (var c=0; c < endpoint.children.length; c++) {
                removeNodeChild(extIdToMatch, endpoint.children[c], nodeType);
            }

        } else if (nodeType == NodeTypeFile) {

            for (var c=0; c < endpoint.files.length; c++) {
                if (extIdToMatch == endpoint.files[c].extId) {
                    endpoint.files.splice(c, 1);
                    return;
                }
            }

            for (var c=0; c < endpoint.files.length; c++) {
                removeNodeChild(extIdToMatch, endpoint.files[c], nodeType);
            }

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

    var serverCallbackFuncFollowingNewBucket = function (status, data) {

        utils.hideBlockingOverlay();

        if (status == 201) {
            $location.path("/dashboard").search({ "dv" : globalVars.S3ServerMode });
            return;
        }

        handleErrorResponse(status, data);
    };

    function handleErrorResponse(status, data) {

        if (status == 400) {
            showAlert(data.message);
            return;
        } else if (status == 409) {
            showAlert("'" + $scope.endpoint.bucket + "' is already defined");
            return;
        }

        showAlert(globalVars.GeneralErrorMessage);
    }


    //
    // Tree View
    function buildTree(endpoint) {

        // Current node
        var treeNode = buildTreeNode(endpoint.bucket, endpoint.extId, endpoint.expanded, endpoint.files);

        // Children
        for (var c=0; c < endpoint.children.length; c++) {
            treeNode.nodes.push(buildTree(endpoint.children[c]));
        }

        return treeNode;
    }

    function buildTreeNode(bucket, extId, expanded, nodeFiles) {

        var node = {
            extId: extId,
            text: bucket,
            nodeType: NodeTypeDir,
            nodes: [],
            //            parentExtId: parentExtId,
            state: {
                expanded: (expanded != null && expanded)
            }
        };

        if (nodeFiles != null) {
            for (var n=0; n < nodeFiles.length; n++) {
                node.nodes.push({
                    extId: nodeFiles[n].extId,
                    nodeType: NodeTypeFile,
                    text: nodeFiles[n].name,
                    icon: 'glyphicon glyphicon-file'
                });
            }
        }

        return node;
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
            expandIcon: 'glyphicon glyphicon-folder-close',
            collapseIcon : 'glyphicon glyphicon-folder-open',
            onNodeExpanded: function(event, data) {
                updateTreeView(data.extId, $scope.endpoint, true);
            },
            onNodeCollapsed: function(event, data) {
                updateTreeView(data.extId, $scope.endpoint, false);
            }
        });

    }

});
