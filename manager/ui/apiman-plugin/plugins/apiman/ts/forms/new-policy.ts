/// <reference path="../apimanPlugin.ts"/>
module Apiman {
    
    export var ConfigForms = {
        BASICAuthenticationPolicy: 'basic-auth.include',
        IgnoredResourcesPolicy: 'ignored-resources.include',
        IPBlacklistPolicy: 'ip-list.include',
        IPWhitelistPolicy: 'ip-list.include',
        RateLimitingPolicy: 'rate-limiting.include'
    };

    export var NewPolicyController = _module.controller("Apiman.NewPolicyController",
        ['$q', '$location', '$scope', 'OrgSvcs', 'ApimanSvcs', 'PageLifecycle', 'Logger', '$routeParams',
        ($q, $location, $scope, OrgSvcs, ApimanSvcs, PageLifecycle, Logger, $routeParams) => {
            var params = $routeParams;
            
            var promise = $q.all({
                policyDefs: $q(function(resolve, reject) {
                    ApimanSvcs.query({ entityType: 'policyDefs' }, function(policyDefs) {
                        $scope.selectedDefId = '__null__';
                        resolve(policyDefs);
                    }, reject);
                })
            });
            
            $scope.$watch('selectedDefId', function(newValue) {
                if (newValue) {
                    var newDef = undefined;
                    angular.forEach($scope.policyDefs, function(def) {
                        if (def.id == newValue) {
                            newDef = def;
                        }
                    });
                    $scope.selectedDef = newDef;
                }
            });
            
            $scope.$watch('selectedDef', function(newValue) {
                if (!newValue) {
                    $scope.include = undefined;
                } else {
                    $scope.config = new Object();
                    if ($scope.selectedDef.formType == 'JsonSchema') {
                        $scope.include = 'plugins/apiman/html/policyForms/JsonSchema.include';
                    } else {
                        var inc = ConfigForms[$scope.selectedDef.id];
                        if (!inc) {
                            inc = 'Default.include';
                        }
                        $scope.include = 'plugins/apiman/html/policyForms/' + inc;
                    }
                }
            });
            
            $scope.setValid = function(valid) {
                $scope.isValid = valid;
            };

            $scope.setConfig = function(config) {
                $scope.config = config;
            };
            
            $scope.addPolicy = function() {
                $scope.createButton.state = 'in-progress';
                var newPolicy = {
                    definitionId: $scope.selectedDefId,
                    configuration: JSON.stringify($scope.config)
                };
                var etype = params.type;
                if (etype == 'apps') {
                    etype = 'applications';
                }
                OrgSvcs.save({ organizationId: params.org, entityType: etype, entityId: params.id, versionsOrActivity: 'versions', version: params.ver, policiesOrActivity: 'policies' }, newPolicy, function(reply) {
                    PageLifecycle.redirectTo('/orgs/{0}/{1}/{2}/{3}/policies', params.org, params.type, params.id, params.ver);
                }, PageLifecycle.handleError);
            };
            
            PageLifecycle.loadPage('NewPolicy', promise, $scope, function() {
                PageLifecycle.setPageTitle('new-policy');
            });
        }]);

}
