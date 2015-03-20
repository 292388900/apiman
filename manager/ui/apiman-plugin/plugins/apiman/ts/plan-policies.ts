/// <reference path="apimanPlugin.ts"/>
/// <reference path="services.ts"/>
module Apiman {

    export var PlanPoliciesController = _module.controller("Apiman.PlanPoliciesController",
        ['$q', '$scope', '$location', 'OrgSvcs', 'ApimanSvcs', 'Logger', 'PageLifecycle', 'PlanEntityLoader', 'Dialogs', 
        ($q, $scope, $location, OrgSvcs, ApimanSvcs, Logger, PageLifecycle, PlanEntityLoader, Dialogs) => {
            var params = $location.search();
            $scope.organizationId = params.org;
            $scope.tab = 'policies';
            
            var removePolicy = function(policy) {
                angular.forEach($scope.policies, function(p, index) {
                    if (policy === p) {
                        $scope.policies.splice(index, 1);
                    }
                });
            };

            $scope.removePolicy = function(policy) {
                Dialogs.confirm('Confirm Remove Policy', 'Do you really want to remove this policy from the plan?', function() {
                    OrgSvcs.delete({ organizationId: params.org, entityType: 'plans', entityId: params.plan, versionsOrActivity: 'versions', version: params.version, policiesOrActivity: 'policies', policyId: policy.id }, function(reply) {
                        removePolicy(policy);
                    }, function(error) {
                        if (error.status == 409) {
                            $location.path('apiman/error-409.html');
                        } else {
                            alert("ERROR=" + error.status + " " + error.statusText);
                        }
                    });
                });
            };

            var dataLoad = PlanEntityLoader.getCommonData($scope, $location);
            angular.extend(dataLoad, {
                policies: $q(function(resolve, reject) {
                    OrgSvcs.query({ organizationId: params.org, entityType: 'plans', entityId: params.plan, versionsOrActivity: 'versions', version: params.version, policiesOrActivity: 'policies' }, function(policies) {
                        resolve(policies);
                    }, function(error) {
                        reject(error);
                    });
                })
            });
            var promise = $q.all(dataLoad);
            PageLifecycle.loadPage('PlanPolicies', promise, $scope);
        }])


}
