/// <reference path="apimanPlugin.ts"/>
/// <reference path="services.ts"/>
module Apiman {

    export var EditRoleController = _module.controller("Apiman.EditRoleController",
        ['$q', '$scope', '$location', 'ApimanSvcs', 'PageLifecycle', 'Logger',
        ($q, $scope, $location, ApimanSvcs, PageLifecycle, Logger) => {
            var params = $location.search();
            var allPermissions     = ['orgView', 'orgEdit', 'orgAdmin',
                                      'planView','planEdit','planAdmin',
                                      'svcView', 'svcEdit', 'svcAdmin',
                                      'appView', 'appEdit', 'appAdmin'];
            $scope.isValid = true;
            $scope.rolePermissions = {};
            angular.forEach(allPermissions, function(value) {
                $scope.rolePermissions[value] = false;
            });
            
            var validate = function() {
                var atLeastOne = false;
                angular.forEach($scope.rolePermissions, function(value,key) {
                    if (value == true) {
                        atLeastOne = true;
                    }
                });
                return atLeastOne;
            };
            
            $scope.$watch('rolePermissions', function(newValue) {
                $scope.isValid = validate();
            }, true);
            
            var promise = $q.all({
                role: $q(function(resolve, reject) {
                    ApimanSvcs.get({ entityType: 'roles', secondaryType: params.role }, function(role) {
                        angular.forEach(role.permissions, function(name) {
                            $scope.rolePermissions[name] = true;
                        });
                        resolve(role);
                    }, function(error) {
                        reject(error);
                    });
                })
            });
            
            $scope.updateRole  = function() {
                $scope.updateButton.state = 'in-progress';
                var permissions = [];
                angular.forEach($scope.rolePermissions, function(value,key) {
                    if (value == true) {
                        permissions.push(key);
                    }
                });
                var role:any = {};
                role.name = $scope.role.name;
                role.description = $scope.role.description;
                role.permissions = permissions;
                role.autoGrant = $scope.role.autoGrant;
                ApimanSvcs.update({ entityType: 'roles', secondaryType: $scope.role.id }, role, function(reply) {
                     $location.path(pluginName + '/admin-roles.html');
                }, function(error) {
                    if (error.status == 409) {
                        $location.path('apiman/error-409.html');
                    } else {
                        alert("ERROR=" + error.status + " " + error.statusText);
                    }
                    $scope.updateButton.state = 'error';
                });
            }
            
            $scope.deleteRole  = function() {
                $scope.deleteButton.state = 'in-progress';
                ApimanSvcs.delete({ entityType: 'roles', secondaryType: $scope.role.id }, function(reply) {
                    $location.path(pluginName + '/admin-roles.html');
                }, function(error) {
                    if (error.status == 409) {
                        $location.path('apiman/error-409.html');
                    } else {
                        alert("ERROR=" + error.status + " " + error.statusText);
                    }
                    $scope.deleteButton.state = 'error';
                });
            }
            
            PageLifecycle.loadPage('EditRole', promise, $scope);
    }])

}
