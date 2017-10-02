
app.controller('endpointInfoRuleConditionController', function($scope, $location, $uibModalInstance, $timeout, utils, globalVars, data) {


    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var TextDataType = globalVars.TextDataType;
    var NumericDataType = globalVars.NumericDataType;
    var PathVariableText = globalVars.PathVariableText;
    var PathVariableWildcardText = globalVars.PathVariableWildcardText;
    var RequestHeaderText = globalVars.RequestHeaderText;
    var RequestParamText = globalVars.RequestParamText;
    var RequestBodyText = globalVars.RequestBodyText;
    var EqualsText = globalVars.EqualsText;
    var ContainsText = globalVars.ContainsText;
    var IsMissingText = globalVars.IsMissingText;


    //
    // Labels
    $scope.newEndpointRuleConditionHeading = 'New Rule Condition';
    $scope.matchTypeLabel = 'Match On';
    $scope.selectDropDownLabel = 'Select...';
    $scope.comparatorLabel = 'Comparator';
    $scope.dataTypeLabel = 'Data Type';
    $scope.matchValueLabel = 'Match Value';
    $scope.caseSensitiveLabel = 'Is Case Sensitive';
    $scope.textDataTypeLabel = TextDataType;
    $scope.ruleConditionArgsTableLabel = "Condition Arguments";


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.addConditionArgButtonLabel = 'Add Arg';
    $scope.addGroupButtonLabel = 'Done';


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
    // Data Objects
    $scope.matchingTypes = globalVars.RuleMatchingTypes;

    $scope.comparators = [];
    $scope.conditionArgs = [];


    //
    // Functions
    $scope.doSelectMatchingType = function (m) {
        $scope.condition.matchType = m;

        //
        // Form Adjustments
        repopulateComparators(m.value);

        if (m.value == RequestBodyText) {
            $scope.condition.fieldName = null;
        }

    };

    $scope.doSelectComparator = function (c) {
        $scope.condition.comparator = c;

        //
        // Form Adjustments
        $scope.condition.dataType = c.dataType;

        if (c.value == IsMissingText) {
            $scope.condition.matchValue = null;
        }

    };

    $scope.doMoveConditionArgUp = function(index) {

        if (index == 0) {
            return;
        }

        var rule = $scope.conditionArgs[index];

        $scope.conditionArgs.splice(index, 1);
        $scope.conditionArgs.splice((index - 1), 0, rule);
    };

    $scope.doMoveConditionArgDown = function(index) {

        if ((index + 1) == $scope.conditionArgs.length) {
            return;
        }

        var rule = $scope.conditionArgs[index];

        $scope.conditionArgs.splice(index, 1);
        $scope.conditionArgs.splice((index + 1), 0, rule);
    };

    $scope.doRemoveConditionArg = function(index) {
        $scope.conditionArgs.splice(index, 1);
    };

    $scope.doAddRuleConditionArg = function() {

        if (utils.isBlank($scope.condition.matchType)) {
            showAlert("'Match On' is required");
            return;
        }

        if ( ( $scope.condition.matchType.value == PathVariableText
                || $scope.condition.matchType.value == RequestHeaderText
                || $scope.condition.matchType.value == RequestParamText )
                    && ( utils.isBlank($scope.condition.fieldName) )
                    ) {
            showAlert($scope.condition.matchType.name + " 'Key Name' is required");
            return;
        }

        if ( $scope.condition.matchType.value == PathVariableWildcardText
                && !utils.isNumeric($scope.condition.fieldName) ) {
            showAlert("The (numeric) position of the path variable wildcard arg is required");
            return;
        }

        if ($scope.condition.comparator == null) {
            showAlert("'Comparator' is required");
            return;
        }

        if ($scope.condition.comparator.value != IsMissingText
                && utils.isBlank($scope.condition.matchValue)) {
            showAlert("'Match Value' is required");
            return;
        }

        if ($scope.condition.comparator.dataType == NumericDataType
                && !utils.isNumeric($scope.condition.matchValue)) {
            showAlert("Invalid 'Match Value'. A numeric value is required");
            return;
        }

        $scope.conditionArgs.push($scope.condition);

        resetCondition();
    };

    $scope.doAddRuleGroup = function() {

        if ($scope.conditionArgs.length == 0) {
            showAlert("You have not added any rule condition arguments");
            return;
        }

        $uibModalInstance.close($scope.conditionArgs);
    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };

    function repopulateComparators(matchValue) {

        //
        // Default
        $scope.comparators = globalVars.RuleComparators;

        if (matchValue == RequestBodyText) {

            $scope.comparators = [
                { dropDownName : 'Equals ( TEXT )', tableName : 'equals', value : EqualsText, dataType: TextDataType },
                { dropDownName : 'Contains ( TEXT )', tableName : 'contains', value : ContainsText, dataType: TextDataType }
            ];

            $scope.condition.comparator = null;
            $scope.condition.dataType = null;

        } else if (matchValue == PathVariableText) {

            $scope.comparators = [
               { dropDownName : 'Equals ( TEXT )', tableName : 'equals', value : EqualsText, dataType: TextDataType },
               { dropDownName : 'Equals ( NUMBER )', tableName : '==', value : EqualsText, dataType: NumericDataType },
               { dropDownName : 'Contains ( TEXT )', tableName : 'contains', value : ContainsText, dataType: TextDataType }
            ];

            $scope.condition.comparator = null;
            $scope.condition.dataType = null;

        }

    }

    function resetCondition() {
        $scope.condition = {
            "matchType" : null,
            "fieldName" : null,
            "comparator" : null,
            "dataType" : null,
            "caseSensitive" : false,
            "matchValue" : null
        };
    }


    //
    // Initiate default page values
    repopulateComparators();
    resetCondition();

});
