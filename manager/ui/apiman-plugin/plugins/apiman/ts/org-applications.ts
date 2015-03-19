/// <reference path="apimanPlugin.ts"/>
/// <reference path="services.ts"/>
module Apiman {

    export var OrgAppsController = _module.controller("Apiman.OrgAppsController",
        ['$q', '$scope', '$location', 'OrgSvcs', 'PageLifecycle', '$rootScope', 
        ($q, $scope, $location, OrgSvcs, PageLifecycle, $rootScope) => {
            var params = $location.search();
            $scope.organizationId = params.org;
            
            $scope.filterApps = function(value) {
                if (!value) {
                    $scope.filteredApps = $scope.apps;
                } else {
                    var filtered = [];
                    for (var i = 0; i < $scope.apps.length; i++) {
                        var app = $scope.apps[i];
                        if (app.name.toLowerCase().indexOf(value) > -1) {
                            filtered.push(app);
                        }
                    }
                    $scope.filteredApps = filtered;
                }
            };
            
            var promise = $q.all({
                org: $q(function(resolve, reject) {
                    OrgSvcs.get({ organizationId: params.org, entityType: '' }, function(org) {
                        $rootScope.mruOrg = org;
                        resolve(org);
                    }, function(error) {
                        reject(error);
                    });
                }),
                members: $q(function(resolve, reject) {
                    OrgSvcs.query({ organizationId: params.org, entityType: 'members' }, function(members) {
                        resolve(members);
                    }, function(error) {
                        reject(error);
                    });
                }),
                apps: $q(function(resolve, reject) {
                    OrgSvcs.query({ organizationId: params.org, entityType: 'applications' }, function(apps) {
                        $scope.filteredApps = apps;
                        resolve(apps);
                    }, function(error) {
                        reject(error);
                    });
                })
            });
            PageLifecycle.loadPage('OrgApps', promise, $scope);
        }])

}
