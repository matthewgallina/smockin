
app.controller('s3EndpointInfoController', function($scope, $location, $uibModal, $http, $timeout, utils, globalVars, restClient, auth) {


    //
    // Constants
    var extId = $location.search()["eid"];
    var isNew = (extId == null);
    var s3PathPlaceHolderTxt = 'e.g. (my_cool_bucket)';
    var NodeTypeBucket = globalVars.NodeTypeBucket;
    var NodeTypeDir = globalVars.NodeTypeDir;
    var NodeTypeFile = globalVars.NodeTypeFile;
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;


    //
    // Labels
    $scope.endpointHeading = (isNew) ? 'New S3 Bucket' : 'S3 Bucket';
    $scope.pathPlaceHolderTxt = s3PathPlaceHolderTxt;
    $scope.bucketLabel = 'Bucket Name';
    $scope.bucketStructureLabel = 'S3 Bucket Content';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.endpointStatusLabel = 'Status:';



    //
    // Buttons
    $scope.addNodeButtonLabel = 'Add Dir';
    $scope.renameNodeButtonLabel = 'Rename';
    $scope.removeNodeButtonLabel = 'Delete';
    $scope.uploadFileButtonLabel = 'Upload';
    $scope.syncBucketButtonLabel = 'Re-Sync';
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

    $scope.doSaveS3Bucket = function() {

        // Validation
        if (utils.isBlank($scope.endpoint.bucket)) {
            showAlert("'Bucket' is required");
            return;
        }
        if (utils.matchesRegex(globalVars.S3BucketNameRegex, $scope.endpoint.bucket)) {
            showAlert("Invalid S3 'Bucket' name (lowercase letters, numbers & hyphens only)");
            return;
        }

        // Send to Server
        if (isNew) {

            createNewBucket($scope.endpoint.bucket, $scope.endpoint.status, null, serverCallbackFuncFollowingNewBucket);

        } else {

            updateBucket($scope.endpoint.extId,
                         $scope.endpoint.bucket,
                         $scope.endpoint.status,
                             function (status, data) {

                                 utils.hideBlockingOverlay();

                                 if (status == 204) {
                                     showAlert('Bucket status successfully updated', 'success');
                                     return;
                                 }

                                 handleErrorResponse(status, data);
                             });

        }
    };

    $scope.doDelete = function() {
        doDeleteBucket();
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
        var parentName = selectedNode[0].actualName;
        var nodeType = selectedNode[0].nodeType;

        var modalInstance = $uibModal.open({
            templateUrl: 's3_endpoint_node.html',
            controller: 's3EndpointNodeController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                    return {
                        "extId" : null,
                        "nodeName" : null,
                        "nodeType" : NodeTypeDir,
                        "parentName" : parentName
                    };
                }
            }
        });

        modalInstance.result.then(function (data) {

            var nodeName = data.name;

            var serverCallbackFuncFollowingNewDir = function (status, data) {

                utils.hideBlockingOverlay();

                if (status == 201) {
                    var newNodeExtId = data.message;
                    appendNodeChild(parentExtId, $scope.endpoint, nodeName, newNodeExtId, NodeTypeDir);
                    updateTree($scope.endpoint);
                    return;
                }

                handleErrorResponse(status, data);
            };

            createNewDir(data.name,
                         (nodeType == NodeTypeBucket) ? parentExtId : null,
                         (nodeType == NodeTypeDir) ? parentExtId : null,
                         serverCallbackFuncFollowingNewDir);

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
            showAlert("To delete this S3 bucket, please use the main 'Delete' button below");
            return;
        }

        utils.openDeleteConfirmation("Are you sure you wish to delete this directory and all of it's content?", function (alertResponse) {
            if (alertResponse) {
                deleteNode(selectedNode[0].extId, selectedNode[0].nodeType, false);
            }
       });

    };

    $scope.doSyncBucket = function() {

        if ($scope.mockServerStatus != MockServerRunningStatus) {
            return;
        }

        utils.openWarningConfirmation("Are you sure you wish to re-synchronize the mock bucket contents?", function (alertResponse) {
            if (alertResponse) {
                reSyncBucketContent();
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
                        "nodeName" : selectedNode[0].actualName,
                        "nodeType" : selectedNode[0].nodeType
                    };
                }
            }
        });

        modalInstance.result.then(function (data) {
            appendNodeChild(selectedNode[0].extId, $scope.endpoint, data.name, data.extId, NodeTypeFile);
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
        var nodeName = selectedNode[0].actualName;
        var nodeType = selectedNode[0].nodeType;

        var modalInstance = $uibModal.open({
            templateUrl: 's3_endpoint_node.html',
            controller: 's3EndpointNodeController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                    return {
                        "extId" : extId,
                        "nodeName" : nodeName,
                        "nodeType" : nodeType,
                        "parentName" : null
                    };
                }
            }
        });

        modalInstance.result.then(function (data) {

            var nodeName = data.name;

            var serverCallbackFuncFollowingUpdate = function (status, data) {

                utils.hideBlockingOverlay();

                if (status == 204) {

                    if (nodeType == NodeTypeBucket
                            && extId == $scope.endpoint.extId) {
                        $scope.endpoint.bucket = nodeName;
                    } else if (nodeType == NodeTypeDir) {
                        amendNodeChildDir(extId, nodeName, $scope.endpoint);
                    }

                    updateTree($scope.endpoint);
                    return;
                }

                handleErrorResponse(status, data);
            };

            if (nodeType == NodeTypeBucket) {
                updateBucket(extId, nodeName, globalVars.ActiveStatus, serverCallbackFuncFollowingUpdate);
            } else if (nodeType == NodeTypeDir) {
                updateDir(extId, nodeName, serverCallbackFuncFollowingUpdate);
            }

        }, function () {});

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

    function reSyncBucketContent() {

        // Send to Server
        utils.showBlockingOverlay();

        var serverCallbackFuncFollowingResync = function (status, data) {

            utils.hideBlockingOverlay();

            if (status == 204) {
                showAlert("Bucket Synchronization Complete", "success");
                return;
            }

            handleErrorResponse(status, data);
        };

        var reqData = {
            "bucketExtId" : $scope.endpoint.extId
        };

        restClient.doPost($http, '/s3mock/bucket/' + $scope.endpoint.extId + '/sync', reqData, serverCallbackFuncFollowingResync);

    }

    function createNewBucket(bucket, status, parentId, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "bucket" : bucket,
            "status" : status
        };

        restClient.doPost($http, '/s3mock/bucket', reqData, callbackFunc);
    }

    function createNewDir(name, bucketParentId, dirParentId, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "name" : name,
            "bucketExtId" : bucketParentId,
            "parentDirExtId" : dirParentId
        };

        restClient.doPost($http, '/s3mock/dir', reqData, callbackFunc);
    }

    function updateBucket(extId, bucket, status, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "bucket" : bucket,
            "status" : status
        };

        restClient.doPut($http, '/s3mock/bucket/' + extId, reqData, callbackFunc);
    }

    function updateDir(extId, name, callbackFunc) {

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "name" : name,
            "bucketExtId" : null,
            "parentDirExtId" : null
        };

        restClient.doPut($http, '/s3mock/dir/' + extId, reqData, callbackFunc);
    }

    function doDeleteBucket() {

        utils.openDeleteConfirmation("Are you sure you wish to delete this S3 bucket and all of it's content?", function (alertResponse) {
            if (alertResponse) {
                deleteNode($scope.endpoint.extId, NodeTypeBucket, true);
            }
       });

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

        restClient.doDelete($http, '/s3mock/' + nodeType.toLowerCase() + '/' + extId, serverCallbackFuncFollowingDelete);

    }

    function amendNodeChildDir(extIdToMatch, newNodeName, endpoint) {

        if (extIdToMatch == endpoint.extId) {
            endpoint.name = newNodeName;
        } else {

            for (var c=0; c < endpoint.children.length; c++) {
                amendNodeChildDir(extIdToMatch, newNodeName, endpoint.children[c]);
            }

        }

    }

    function appendNodeChild(extIdToMatch, endpoint, newNodeName, newNodeExtId, newNodeType) {

        if (extIdToMatch == endpoint.extId) {

            endpoint.expanded = true;

            if (newNodeType == NodeTypeDir) {

                endpoint.children.push({
                    "extId" : newNodeExtId,
                    "name" : newNodeName,
                    "children" : [],
                    "files" : [],
                    "expanded" : false
                });

            } else if (newNodeType == NodeTypeFile) {

                endpoint.files.push({
                    "extId" : newNodeExtId,
                    "name" : newNodeName
                });

            }

        } else {

            for (var c=0; c < endpoint.children.length; c++) {
                appendNodeChild(extIdToMatch, endpoint.children[c], newNodeName, newNodeExtId, newNodeType);
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

        } else if (nodeType == NodeTypeFile) {

            for (var c=0; c < endpoint.files.length; c++) {
                if (extIdToMatch == endpoint.files[c].extId) {
                    endpoint.files.splice(c, 1);
                    return;
                }
            }

        }

        for (var c=0; c < endpoint.children.length; c++) {
            removeNodeChild(extIdToMatch, endpoint.children[c], nodeType);
        }

    }

    function loadMockData(extId, callback) {

        restClient.doGet($http, '/s3mock/bucket/' + extId, function(status, data) {

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
    function initTree(endpoint) {

        // Current node
        var treeNode = buildTreeNode(endpoint.bucket, endpoint.extId, NodeTypeBucket, endpoint.expanded, endpoint.files, endpoint.children.length);

        // Children
        for (var c=0; c < endpoint.children.length; c++) {
            treeNode.nodes.push(addTreeDir(endpoint.children[c]));
        }

        return treeNode;
    }

    function addTreeDir(directory) {

        // Current node
        var treeNode = buildTreeNode(directory.name, directory.extId, NodeTypeDir, directory.expanded, directory.files, directory.children.length);

        // Children
        for (var c=0; c < directory.children.length; c++) {
            treeNode.nodes.push(addTreeDir(directory.children[c]));
        }

        return treeNode;
    }

    function buildTreeNode(name, extId, nodeType, expanded, nodeFiles, childNodeCount) {

        var nodeTotal = childNodeCount;

        if (nodeFiles != null) {
            nodeTotal = ( nodeTotal + nodeFiles.length);
        }

        var node = {
            extId: extId,
            text: ' ' + name,
            tags: [ nodeTotal ],
            actualName: name,
            nodeType: nodeType,
            nodes: [],
            icon: 'glyphicon glyphicon-folder-close',
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
                    text: ' ' + nodeFiles[n].name,
                    actualName: nodeFiles[n].name,
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
                initTree(endpoint)
            ],
            expandIcon: 'glyphicon glyphicon-chevron-right',
            collapseIcon : 'glyphicon glyphicon-chevron-down',
            showTags: true,
            onNodeExpanded: function(event, data) {
                updateTreeView(data.extId, $scope.endpoint, true);
            },
            onNodeCollapsed: function(event, data) {
                updateTreeView(data.extId, $scope.endpoint, false);
            }
        });

    }

    function loadS3ServerStatus() {

        utils.checkS3ServerStatus(function(running, port) {

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
    loadS3ServerStatus();

});
