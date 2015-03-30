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
        ['$q', '$location', '$scope', 'OrgSvcs', 'ApimanSvcs', 'PageLifecycle', 'Logger',
        ($q, $location, $scope, OrgSvcs, ApimanSvcs, PageLifecycle, Logger) => {
            var params = $location.search();
            
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
                OrgSvcs.save({ organizationId: params.org, entityType: params.type, entityId: params.id, versionsOrActivity: 'versions', version: params.ver, policiesOrActivity: 'policies' }, newPolicy, function(reply) {
                    var toPage = '/plan-policies.html';
                    var entityParam = 'plan';
                    if (params.type == 'services') {
                        toPage = '/service-policies.html';
                        entityParam = 'service';
                    }
                    if (params.type == 'applications') {
                        toPage = '/app-policies.html';
                        entityParam = 'app';
                    }
                    $location.url(Apiman.pluginName + toPage)
                        .search('org', reply.organizationId)
                        .search(entityParam, reply.entityId)
                        .search('version', reply.entityVersion);
                }, PageLifecycle.handleError);
            };
            
            PageLifecycle.loadPage('NewPolicy', promise, $scope, function() {
                PageLifecycle.setPageTitle('new-policy');
            });
        }]);

}
