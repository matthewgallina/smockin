
app.directive('windowDimensionsDirective', ['$window', function ($window) {
    return {
        link: link,
        restrict: 'A'
    };

    function link(scope, element, attrs) {
        scope.width = $window.innerWidth;
        scope.height = $window.innerHeight;

        function onResize() {

            if (scope.width !== $window.innerWidth) {
                scope.width = $window.innerWidth;
                scope.$digest();
            }

            if (scope.height !== $window.innerHeight) {
                scope.height = $window.innerHeight;
                scope.$digest();
            }

        };

        function cleanUp() {
            angular.element($window).off('resize', onResize);
        }

        angular.element($window).on('resize', onResize);
        scope.$on('$destroy', cleanUp);
    }

}]);
