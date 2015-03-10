/// <reference path='../../includes.ts'/>
module ApimanCurrentUser {

    export var _module = angular.module('ApimanCurrentUser', ['ApimanServices']);

    export var CurrentUser = _module.factory('CurrentUser', ['$q', '$rootScope', 'CurrentUserSvcs', 'Logger',
        function($q, $rootScope, CurrentUserSvcs, Logger) {
            var promise = $q(function(resolve, reject) {
                CurrentUserSvcs.get({ what: 'info' }, function(currentUser) {
                    Logger.log("Successfully grabbed currentuser/info for {0}.", currentUser.username);
                    $rootScope.currentUser = currentUser;
                    var permissions = {};
                    if (currentUser.permissions) {
                        for (var i = 0; i < currentUser.permissions.length; i++) {
                            var perm = currentUser.permissions[i];
                            var permid = perm.organizationId + '||' + perm.name;
                            permissions[permid] = true;
                        }
                    }
                    $rootScope.permissions = permissions;
                    resolve(currentUser);
                }, function(error) {
                    reject(error);
                });
            });
            return {
                promise: promise,
                getCurrentUser: function() {
                    return $rootScope.currentUser;
                },
                getCurrentUserOrgs: function() {
                    var orgs = {};
                    var perms = $rootScope.currentUser.permissions;
                    for (var i = 0; i < perms.length; i++) {
                        var perm = perms[i];
                        orgs[perm.organizationId] = true;
                    }
                    var rval = [];
                    angular.forEach(orgs, function(value, key) {
                      this.push(key);
                    }, rval);
                    return rval;
                },
                hasPermission: function(organizationId, permission) {
                    if (organizationId) {
                        var permid = organizationId + '||' + permission;
                        return $rootScope.permissions[permid];
                    } else {
                        return false;
                    }
                }
            };
        }]);

}
