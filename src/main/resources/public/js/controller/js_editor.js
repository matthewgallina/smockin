app.controller('jsEditorController', function($scope, $timeout, $uibModalInstance, globalVars, utils, data) {


    //
    // Constants / Vars
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;


    //
    // Labels
    $scope.jsEditorHeading = "Custom JS Editor";


    //
    // Buttons
    $scope.formatCustomJsButton = 'Format JS';
    $scope.cancelButtonLabel = 'Cancel';
    $scope.updateButtonLabel = 'Update';


    //
    // Alerts
    $scope.alerts = [];

    var closeAlertFunc = function() {
        $scope.alerts = [];
    };

   function showAlert(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        $scope.alerts = [];
        $scope.alerts.push({ "type" : type, "msg" : msg });

        $timeout(closeAlertFunc, AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data
    var jsEditor = null;


    //
    // Scoped Functions
    $scope.doCancel = function() {
        $uibModalInstance.dismiss();
    };

    $scope.doFormatCustomJS = function() {
        var formattedJS = js_beautify(jsEditor.getValue(), { indent_size: 2 });
        jsEditor.setValue(formattedJS);
    };

    $scope.doUpdate = function() {

        if (utils.isBlank(jsEditor.getValue())) {
            showAlert("Please enter tour JavaScript logic");
            return;
        }

        if (!jsEditor.getValue().trim().startsWith("function handleResponse")) {
            showAlert("The required function handleResponse(request, response) is not correctly defined");
            return;
        }

        if (JSHINT.errors.length > 0) {
            showAlert("There are errors in your JavaScript syntax that need to be fixed first.");
            return;
        }

        $uibModalInstance.close(jsEditor.getValue());
    };


    //
    // Internal Functions
    function initJSEditor(content) {

        if (jsEditor != null) {
            return;
        }

        jsEditor = CodeMirror.fromTextArea(document.getElementById('custom-js-syntax-modal'), {
          lineNumbers: true,
          styleActiveLine: true,
          matchBrackets: true,
          mode: "javascript",
          gutters: ["CodeMirror-lint-markers"],
          lint: true
        });

        jsEditor.setOption("theme", "darcula");

        updateJSEditor(content);
    }

    function updateJSEditor(content) {
        $timeout(function () {
            if (content != null) {
                jsEditor.setValue(content);
            }
            jsEditor.refresh();
        }, 500);
    }


    //
    // Init Page
    $uibModalInstance.opened.then(function() {
        $timeout(function () {
            initJSEditor(data.customJsSyntax);
        }, 500);
    });

});
