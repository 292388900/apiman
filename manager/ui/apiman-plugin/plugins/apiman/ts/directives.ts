/// <reference path="../../includes.ts"/>
module Apiman {

    _module.directive('apimanActionBtn',
        ['Logger', function(Logger) {
            return {
                restrict: 'A',
                link: function(scope, element, attrs) {
                    var actionVar = attrs.field;
                    var actionText = attrs.placeholder;
                    var icon = attrs.icon;
                    Logger.debug("Action button initializing state variable [{0}].", actionVar);
                    scope[actionVar] = {
                        state: 'ready',
                        html: $(element).html(),
                        actionHtml: '<i class="fa fa-spin ' + icon + '"></i> ' + actionText
                    };
                    scope.$watch(actionVar + '.state', function() {
                        var newVal = scope[actionVar];
                        if (newVal.state == 'in-progress') {
                            $(element).prop('disabled', true);
                            $(element).html(newVal.actionHtml);
                        } else {
                            $(element).prop('disabled', false);
                            $(element).html(newVal.html);
                        }
                    });
                }
            };
        }]);

    
    _module.directive('apimanPermission',
        ['Logger', 'CurrentUser', function(Logger, CurrentUser) {
            return {
                restrict: 'A',
                link: function(scope, element, attrs) {
                    scope.$watch('organizationId', function(newValue, oldValue) {
                        var orgId = newValue;
                        if (orgId) {
                            var permission = attrs.apimanPermission;
                            Logger.debug('Checking authorization :: permission {0}/{1}.', orgId, permission);
                            if (!CurrentUser.hasPermission(orgId, permission)) {
                                $(element).hide();
                            }
                        } else {
                            Logger.error('Missing organizationId from $scope - authorization disabled.');
                        }
                    });
                }
            };
        }]);

    
    _module.directive('apimanStatus',
        ['Logger', function(Logger) {
            return {
                restrict: 'A',
                link: function(scope, element, attrs) {
                    scope.$watch('entityStatus', function(newValue, oldValue) {
                        var entityStatus = newValue;
                        if (entityStatus) {
                            var validStatuses = attrs.apimanStatus.split(',');
                            var statusIsValid = false;
                            Logger.debug('Checking status {0} against valid statuses {1}.', entityStatus, '' + validStatuses);
                            for (var i = 0; i < validStatuses.length; i++) {
                                if (validStatuses[i] == entityStatus) {
                                    statusIsValid = true;
                                    break;
                                }
                            }
                            if (!statusIsValid) {
                                $(element).hide();
                            }
                        } else {
                            Logger.error('Missing entityStatus from $scope - hide/show based on entity status feature is disabled.');
                        }
                    });
                }
            };
        }]);

    
    _module.directive('apimanEntityStatus',
        ['Logger', function(Logger) {
            return {
                restrict: 'A',
                link: function(scope, element, attrs) {
                    var toWatch = attrs.apimanEntityStatus;
                    if (!toWatch) {
                        toWatch = 'entityStatus';
                    }
                    scope.$watch(toWatch, function(newValue, oldValue) {
                        var entityStatus = newValue;
                        if (entityStatus) {
                            $(element).html(entityStatus);
                            $(element).removeClass();
                            $(element).addClass('apiman-label');
                            
                            if (entityStatus == 'Created' || entityStatus == 'Ready') {
                                $(element).addClass('apiman-label-warning');
                            } else if (entityStatus == 'Retired') {
                                $(element).addClass('apiman-label-default');
                            } else {
                                $(element).addClass('apiman-label-success');
                            }
                        }
                    });
                }
            };
        }]);

    
    _module.directive('apimanSearchBox',
        ['Logger', function(Logger) {
            return {
                restrict: 'E',
                templateUrl: 'plugins/apiman/html/directives/searchBox.html',
                scope: {
                    searchFunction: '=function'
                },
                link: function(scope, element, attrs) {
                    scope.placeholder = attrs.placeholder;
                    scope.doSearch = function() {
                        $(element).find('button i').removeClass('fa-search');
                        $(element).find('button i').removeClass('fa-close');
                        if (scope.value) {
                            $(element).find('button i').addClass('fa-close');
                        } else {
                            $(element).find('button i').addClass('fa-search');
                        }
                        scope.searchFunction(scope.value);
                    };
                    scope.onClick = function() {
                        if (scope.value) {
                            scope.value = '';
                            $(element).find('button i').removeClass('fa-search');
                            $(element).find('button i').removeClass('fa-close');
                            $(element).find('button i').addClass('fa-search');
                        }
                        scope.searchFunction(scope.value);
                    };
                }
            };
        }]);

    _module.directive('apimanConfirmModal',
        ['Logger', function(Logger) {
            return {
                templateUrl: 'plugins/apiman/html/directives/confirmModal.html',
                restrict: 'E',
                transclude: true,
                link: function(scope, element, attrs) {
                    scope.title = attrs.title;

                    $(element).on('hidden.bs.modal', function() {
                        Logger.debug('hidden.bs.modal fired');
                        $(element).remove();
                    });
                }
            };
        }]);
}
