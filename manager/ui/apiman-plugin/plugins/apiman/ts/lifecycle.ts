/// <reference path="../../includes.ts"/>
module ApimanPageLifecycle {

    export var pageTitles = {
        "page.title.admin-gateways": "apiman - Admin - Gateways",
        "page.title.admin-plugins": "apiman - Admin - Plugins",
        "page.title.admin-roles": "apiman - Admin - Roles",
        "page.title.admin-policyDefs": "apiman - Admin - Policy Definitions",
        "page.title.app-activity": "apiman - {0} (Activity)",
        "page.title.app-apis": "apiman - {0} (APIs)",
        "page.title.app-contracts": "apiman - {0} (Contracts)",
        "page.title.app-overview": "apiman - {0} (Overview)",
        "page.title.app-policies": "apiman - {0} (Policies)",
        "page.title.consumer-org": "apiman - Organization {0}",
        "page.title.consumer-orgs": "apiman - Organizations",
        "page.title.consumer-service": "apiman - Service {0}",
        "page.title.consumer-services": "apiman - Services",
        "page.title.dashboard": "apiman - Home",
        "page.title.edit-gateway": "apiman - Edit Gateway",
        "page.title.edit-policy": "apiman - Edit Policy",
        "page.title.edit-policyDef": "apiman - Edit Policy Definition",
        "page.title.edit-role": "apiman - Edit Role",
        "page.title.import-policyDefs": "apiman - Import Policy Definition(s)",
        "page.title.import-services": "apiman - Import Service(s)",
        "page.title.new-app": "apiman - New Application",
        "page.title.new-app-version": "apiman - New Application Version",
        "page.title.new-contract": "apiman - New Contract",
        "page.title.new-gateway": "apiman - New Gateway",
        "page.title.new-member": "apiman - Add Member",
        "page.title.new-org": "apiman - New Organization",
        "page.title.new-plan": "apiman - New Plan",
        "page.title.new-plan-version": "apiman - New Plan Version",
        "page.title.new-plugin": "apiman - Add Plugin",
        "page.title.new-policy": "apiman - Add Policy",
        "page.title.new-role": "apiman - New Role",
        "page.title.new-service": "apiman - New Service",
        "page.title.new-service-version": "apiman - New Service Version",
        "page.title.org-activity": "apiman - {0} (Activity)",
        "page.title.org-apps": "apiman - {0} (Applications)",
        "page.title.org-manage-members": "apiman - {0} (Manage Members)",
        "page.title.org-members": "apiman - {0} (Members)",
        "page.title.org-plans": "apiman - {0} (Plans)",
        "page.title.org-services": "apiman - {0} (Services)",
        "page.title.plan-activity": "apiman - {0} (Activity)",
        "page.title.plan-overview": "apiman - {0} (Overview)",
        "page.title.plan-policies": "apiman - {0} (Policies)",
        "page.title.plugin-details": "apiman - Plugin Details",
        "page.title.policy-defs": "apiman - Admin - Policy Definitions",
        "page.title.service-activity": "apiman - {0} (Activity)",
        "page.title.service-contracts": "apiman - {0} (Contracts)",
        "page.title.service-endpoint": "apiman - {0} (Endpoint)",
        "page.title.service-impl": "apiman - {0} (Implementation)",
        "page.title.service-overview": "apiman - {0} (Overview)",
        "page.title.service-plans": "apiman - {0} (Plans)",
        "page.title.service-policies": "apiman - {0} (Policies)",
        "page.title.user-activity": "apiman - {0} (Activity)",
        "page.title.user-apps": "apiman - {0} (Applications)",
        "page.title.user-orgs": "apiman - {0} (Organizations)",
        "page.title.user-profile": "apiman - User Profile",
        "page.title.user-services": "apiman - {0} (Services)"
    };
    
    var formatMessage = function(theArgs) {
        var now = new Date();
        var msg = theArgs[0];
        if (theArgs.length > 1) {
            for (var i = 1; i < theArgs.length; i++) {
                msg = msg.replace('{'+(i-1)+'}', theArgs[i]);
            }
        }
        return msg;
    };

    export var _module = angular.module("ApimanPageLifecycle", []);

    export var PageLifecycle = _module.factory('PageLifecycle', ['Logger', '$rootScope', function(Logger, $rootScope) {
        return {
            setPageTitle: function(titleKey, params) {
                var pattern = pageTitles['page.title.' + titleKey];
                if (pattern) {
                    var args = [];
                    args.push(pattern);
                    args = args.concat(params);
                    var title = formatMessage(args);
                    document.title = title;
                } else {
                    document.title = pattern;
                }
            },
            loadPage: function(pageName, dataPromise, $scope, handler) {
                Logger.log("|{0}| >> Loading page.", pageName);
                $rootScope.pageState = 'loading';
                if (dataPromise) {
                    dataPromise.then(function(data) {
                        var count = 0;
                        angular.forEach(data, function(value, key) {
                            Logger.debug("|{0}| >> Binding {1} to $scope.", pageName, key);
                            this[key] = value;
                            count++;
                        }, $scope);
                        $rootScope.pageState = 'loaded';
                        if (handler) {
                            handler();
                        }
                        Logger.log("|{0}| >> Page successfully loaded: {1} data packets loaded", pageName, count);
                    }, function(reason) {
                        $rootScope.pageState = 'error';
                        $rootScope.error = reason;
                        Logger.error(reason);
                        alert("Page Load Error: " + reason);
                    });
                } else {
                    $rootScope.pageState = 'loaded';
                    Logger.log("|{0}| >> Page successfully loaded (no packets).", pageName);
                    if (handler) {
                        handler();
                    }
                }
            }
        }
    }]);

}