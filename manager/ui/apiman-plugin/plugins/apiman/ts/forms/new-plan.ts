/// <reference path="../apimanPlugin.ts"/>
module Apiman {

    export var NewPlanController = _module.controller("Apiman.NewPlanController",
        ['$q', '$location', '$scope', 'CurrentUserSvcs', 'OrgSvcs', 'PageLifecycle', '$rootScope',
        ($q, $location, $scope, CurrentUserSvcs, OrgSvcs, PageLifecycle, $rootScope) => {
            var recentOrg = $rootScope.mruOrg;

            var promise = $q.all({
                organizations: $q(function(resolve, reject) {
                    CurrentUserSvcs.query({ what: 'planorgs' }, function(orgs) {
                        if (recentOrg) {
                            $scope.selectedOrg = recentOrg;
                        } else if (orgs.length > 0) {
                            $scope.selectedOrg = orgs[0];
                        }
                        resolve(orgs);
                    }, function(error) {
                        reject(error);
                    });
                })
            });
            
            $scope.setOrg = function(org) {
                $scope.selectedOrg = org;
            };
            $scope.saveNewPlan = function() {
                $scope.createButton.state = 'in-progress';
                OrgSvcs.save({ organizationId: $scope.selectedOrg.id, entityType: 'plans' }, $scope.plan, function(reply) {
                    $location.url(Apiman.pluginName + '/plan-overview.html').search('org', reply.organization.id).search('plan', reply.name).search('version', $scope.plan.initialVersion);
                }, function(error) {
                    if (error.status == 409) {
                        $location.url('apiman/error-409.html');
                    } else {
                        $scope.createButton.state = 'error';
                        alert("ERROR=" + error.status + " " + error.statusText);
                    }
                });
            };
            
            // Initialize the model - the default initial version for a new plan is always 1.0
            $scope.plan = {
                initialVersion: '1.0'
            };
            
            PageLifecycle.loadPage('NewPlan', promise, $scope, function() {
                PageLifecycle.setPageTitle('new-plan');
                $('#apiman-entityname').focus();
            });
        }]);

}
