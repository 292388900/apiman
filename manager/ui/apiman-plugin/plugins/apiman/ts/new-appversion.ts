/// <reference path="apimanPlugin.ts"/>
module Apiman {

    export var NewAppVersionController = _module.controller("Apiman.NewAppVersionController",
        ['$q', '$location', '$scope', 'OrgSvcs', 'PageLifecycle', ($q, $location, $scope, OrgSvcs, PageLifecycle) => {
            var params = $location.search();
            $scope.saveNewAppVersion = function() {
                OrgSvcs.save({ organizationId: params.org, entityType: 'applications', entityId: params.app, versionsOrActivity: 'versions', version: ''}, $scope.appversion, function(reply) {
                    $location.path(Apiman.pluginName + '/app-overview.html').search('org', params.org).search('app', params.app).search('version', reply.version);
                }, function(error) {
                    if (error.status == 409) {
                        $location.path('apiman/error-409.html');
                    } else {
                        alert("ERROR=" + error.status + " " + error.statusText);
                    }
                });
            };
            
            PageLifecycle.loadPage('NewAppVersion', undefined, $scope);
        }]);

}
