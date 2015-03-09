/// <reference path="apimanPlugin.ts"/>
module Apiman {

    export var NewOrgController = _module.controller("Apiman.NewOrgController",
        ['$q', '$location', '$scope', 'OrgSvcs', 'PageLifecycle', ($q, $location, $scope, OrgSvcs, PageLifecycle) => {
            $scope.saveNewOrg = function() {
                OrgSvcs.save($scope.org, function(reply) {
                    $location.path('apiman/org-plans.html').search('org', reply.id);
                }, function(error) {
                    if (error.status == 409) {
                        $location.path('apiman/error-409.html');
                    } else {
                        alert("ERROR=" + error.status + " " + error.statusText);
                    }
                });
            };
            
            PageLifecycle.loadPage('NewOrg', undefined, $scope);
        }]);

}
